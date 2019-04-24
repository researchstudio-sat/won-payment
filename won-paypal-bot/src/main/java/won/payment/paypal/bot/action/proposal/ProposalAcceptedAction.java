package won.payment.paypal.bot.action.proposal;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RSS;

import com.paypal.svcs.types.ap.PayRequest;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.analyzation.agreement.ProposalAcceptedEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandResultEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandSuccessEvent;
import won.bot.framework.eventbot.filter.impl.CommandResultFilter;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnFirstEventListener;
import won.payment.paypal.bot.impl.PaypalBotContextWrapper;
import won.payment.paypal.bot.model.PaymentBridge;
import won.payment.paypal.bot.model.PaymentModelWrapper;
import won.payment.paypal.bot.model.PaymentStatus;
import won.payment.paypal.service.impl.PaypalPaymentService;
import won.protocol.agreement.AgreementProtocolState;
import won.protocol.model.Connection;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONMOD;
import won.protocol.vocabulary.WONPAY;

/**
 * After a proposal got accepted this Action will be invoked.
 * 
 * @author schokobaer
 */
public class ProposalAcceptedAction extends BaseEventBotAction {
    public ProposalAcceptedAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventListenerContext ctx = getEventListenerContext();
        if (event instanceof ProposalAcceptedEvent) {
            ProposalAcceptedEvent proposalAcceptedEvent = (ProposalAcceptedEvent) event;
            Connection con = proposalAcceptedEvent.getCon();
            PaymentBridge bridge = PaypalBotContextWrapper.paymentBridge(ctx, con);
            if (bridge.getStatus() == PaymentStatus.BUILDING) {
                // Merchant has accepted the paymodel proposal
                generatePP(proposalAcceptedEvent);
            } else if (bridge.getStatus() == PaymentStatus.GENERATED) {
                // Merchant has accepted the pp
                // connectToBuyer(proposalAcceptedEvent);
            } else if (bridge.getStatus() == PaymentStatus.PP_DENIED) {
                // Merchant has accepted the cancellation of the paymodel
                // TODO: Implement
                cancelPaymodel(proposalAcceptedEvent);
            }
        }
    }

    /**
     * Sets bridge.status to building and retracts the payment summary message from
     * the cancellation.
     * 
     * @context Merchant.
     * @param proposalAcceptedEvent
     */
    private void cancelPaymodel(ProposalAcceptedEvent proposalAcceptedEvent) {
        Connection con = proposalAcceptedEvent.getCon();
        EventListenerContext ctx = getEventListenerContext();
        PaymentBridge bridge = PaypalBotContextWrapper.instance(ctx).getOpenBridge(con.getAtomURI());
        bridge.setStatus(PaymentStatus.BUILDING);
        PaypalBotContextWrapper.instance(ctx).putOpenBridge(con.getAtomURI(), bridge);
        AgreementProtocolState agreementProtocolState = AgreementProtocolState.of(con.getConnectionURI(),
                        getEventListenerContext().getLinkedDataSource());
        Model conversation = agreementProtocolState.getConversationDataset().getUnionModel();
        StmtIterator itr = conversation.listStatements(null, RDF.type, WONPAY.PAYMENT_SUMMARY);
        String paymentSummaryUri = null;
        while (itr.hasNext()) {
            Resource subj = itr.next().getSubject();
            if (!conversation.listStatements(null, WONMOD.retracts, subj).hasNext()) {
                paymentSummaryUri = subj.getURI();
            }
        }
        if (paymentSummaryUri == null) {
            return;
        }
        try {
            Model retractResponse = WonRdfUtils.MessageUtils.retractsMessage(new URI(paymentSummaryUri));
            retractResponse = WonRdfUtils.MessageUtils.addMessage(retractResponse,
                            "You can now edit the paymodel again.");
            getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(con, retractResponse));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    // TODO: think about moving this to a public method somewhere
    private void makeTextMsg(String msg, Connection con) {
        if (con == null) {
            return;
        }
        Model model = WonRdfUtils.MessageUtils.processingMessage(msg);
        getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(con, model));
    }

    /**
     * Generates the PP, sends the link and paykey to the merchant and proposes this
     * message.
     * 
     * @context Merchant.
     * @param event the accepted paymodel event.
     */
    private void generatePP(ProposalAcceptedEvent event) {
        Connection con = event.getCon();
        EventListenerContext ctx = getEventListenerContext();
        PaymentBridge bridge = PaypalBotContextWrapper.instance(ctx).getOpenBridge(con.getAtomURI());
        bridge.setStatus(PaymentStatus.PAYMODEL_ACCEPTED);
        PaypalBotContextWrapper.instance(ctx).putOpenBridge(con.getAtomURI(), bridge);
        // TODO: Send info, that a payment is getting generated rn
        PaymentModelWrapper paymodel = new PaymentModelWrapper(event.getPayload());
        try {
            PayRequest pay = paymodel.toPayRequest();
            PaypalPaymentService paypalService = PaypalBotContextWrapper.instance(ctx).getPaypalService();
            String payKey = paypalService.create(pay);
            logger.info("Paypal Payment generated with payKey={}", payKey);
            String url = paypalService.getPaymentUrl(payKey);
            bridge.setStatus(PaymentStatus.GENERATED);
            bridge.setPayKey(payKey);
            PaypalBotContextWrapper.instance(ctx).putOpenBridge(con.getAtomURI(), bridge);
            // Print pay link to merchant; Propose it to him
            Model response = WonRdfUtils.MessageUtils.processingMessage("Generated PayPal payment: \n" + url);
            RdfUtils.findOrCreateBaseResource(response).addProperty(RSS.link, new ResourceImpl(url));
            RdfUtils.findOrCreateBaseResource(response).addProperty(WONPAY.HAS_PAYPAL_PAYKEY, payKey);
            final ConnectionMessageCommandEvent connectionMessageCommandEvent = new ConnectionMessageCommandEvent(con,
                            response);
            ctx.getEventBus().subscribe(ConnectionMessageCommandResultEvent.class, new ActionOnFirstEventListener(ctx,
                            new CommandResultFilter(connectionMessageCommandEvent), new BaseEventBotAction(ctx) {
                                @Override
                                protected void doRun(Event event, EventListener executingListener) throws Exception {
                                    ConnectionMessageCommandResultEvent connectionMessageCommandResultEvent = (ConnectionMessageCommandResultEvent) event;
                                    if (connectionMessageCommandResultEvent.isSuccess()) {
                                        Model agreementMessage = WonRdfUtils.MessageUtils.processingMessage(
                                                        "Check the generated PayPal payment and accept it.");
                                        WonRdfUtils.MessageUtils.addProposes(agreementMessage,
                                                        ((ConnectionMessageCommandSuccessEvent) connectionMessageCommandResultEvent)
                                                                        .getWonMessage().getMessageURI());
                                        ctx.getEventBus().publish(
                                                        new ConnectionMessageCommandEvent(con, agreementMessage));
                                    } else {
                                        logger.error("FAILURERESPONSEEVENT FOR PROPOSAL PAYLOAD");
                                    }
                                }
                            }));
            ctx.getEventBus().publish(connectionMessageCommandEvent);
        } catch (Exception e) {
            logger.warn("Paypal payment could not be generated.", e);
            makeTextMsg("PayPal payment could not be generated: " + e.getMessage(), con);
            bridge.setStatus(PaymentStatus.FAILURE);
            PaypalBotContextWrapper.instance(ctx).putOpenBridge(con.getAtomURI(), bridge);
        }
    }
}
