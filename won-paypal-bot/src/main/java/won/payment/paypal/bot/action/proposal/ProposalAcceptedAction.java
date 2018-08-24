package won.payment.paypal.bot.action.proposal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Selector;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.rdf.model.impl.SelectorImpl;
import org.apache.jena.vocabulary.RSS;

import com.paypal.svcs.types.ap.PayRequest;
import com.paypal.svcs.types.ap.Receiver;
import com.paypal.svcs.types.ap.ReceiverList;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.analyzation.agreement.ProposalAcceptedEvent;
import won.bot.framework.eventbot.event.impl.command.connect.ConnectCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.filter.impl.CommandResultFilter;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnFirstEventListener;
import won.payment.paypal.bot.event.ComplexConnectCommandEvent;
import won.payment.paypal.bot.impl.PaypalBotContextWrapper;
import won.payment.paypal.bot.model.PaymentBridge;
import won.payment.paypal.bot.model.PaymentStatus;
import won.payment.paypal.bot.util.InformationExtractor;
import won.payment.paypal.service.impl.PaypalPaymentService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.model.Connection;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONPAY;

/**
 * After a proposal got accepted this Action will be invoked.
 * 
 * @author schokobaer
 *
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
            	// Merchant has accepted the payment proposal
            	bridge.setStatus(PaymentStatus.MERCHANTACCEPTED);
            	PaypalBotContextWrapper.instance(ctx).putOpenBridge(bridge.getMerchantConnection().getNeedURI(), bridge);
            	connectToBuyer(proposalAcceptedEvent);
            } else {
            	return;
            }
                        
            
		}

	}
	
	private void makeTextMsg(String msg, Connection con) {
		if (con == null) {
			return;
		}
		Model model = WonRdfUtils.MessageUtils.processingMessage(msg);
		getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(con, model));
	}
	
	private void connectToBuyer(ProposalAcceptedEvent event) throws URISyntaxException {
		EventListenerContext ctx = getEventListenerContext();
		Connection con = event.getCon();
        PaymentBridge bridge = PaypalBotContextWrapper.paymentBridge(ctx, con);
        
		Model payload = event.getPayload();
		Double amount = InformationExtractor.getAmount(payload);
        String currency = InformationExtractor.getCurrency(payload);
        String receiver = InformationExtractor.getReceiver(payload);
        String secret = InformationExtractor.getSecret(payload);
		URI counterPartNeedUri = new URI(InformationExtractor.getCounterpart(payload));
		payload.setNsPrefix("pay", WONPAY.BASE_URI);
		
		
		Model removeModels = payload.listStatements(null, WONPAY.HAS_NEED_COUNTERPART, (RDFNode)null).toModel();
		removeModels.add(payload.listStatements(null, WON.HAS_TEXT_MESSAGE, (RDFNode)null).toModel());
		removeModels.add(payload.listStatements(null, WON.IS_PROCESSING, (RDFNode)null).toModel());
		payload.remove(removeModels);
		
		String openingMsg = "Payment request\nAmount: " + currency + " " + amount + "\nReceiver: " + receiver + "\nSecret: " + secret
				+ "\nAccept the Connection to generate the payment and receive the execution link.";
		        
        //logger.info("merchant accepted proposal for " + payload.listStatements().next().getResource().getLocalName());
        
        //ConnectCommandEvent connectCommandEvent = new ConnectCommandEvent(con.getNeedURI(), counterPartNeedUri, openingMsg);
		ComplexConnectCommandEvent connectCommandEvent = new ComplexConnectCommandEvent(con.getNeedURI(), counterPartNeedUri, openingMsg, payload);
        ctx.getEventBus().subscribe(ConnectCommandSuccessEvent.class, new ActionOnFirstEventListener(ctx, new CommandResultFilter(connectCommandEvent), new BaseEventBotAction(ctx) {

			@Override
			protected void doRun(Event event, EventListener executingListener) throws Exception {
				
				if (event instanceof ConnectCommandSuccessEvent) {
					logger.info("created successfully a connection to buyer");
					ConnectCommandSuccessEvent connectSuccessEvent = (ConnectCommandSuccessEvent)event;
					URI needUri = connectSuccessEvent.getNeedURI();
					Connection buyerCon = connectSuccessEvent.getCon();
					bridge.setBuyerConnection(buyerCon);
					bridge.setStatus(PaymentStatus.PUBLISHED);
					PaypalBotContextWrapper.instance(ctx).putOpenBridge(needUri, bridge);
					makeTextMsg("Payment published to buyer.", bridge.getMerchantConnection());
				}
				
			}
        	
        }));
        
        ctx.getEventBus().publish(connectCommandEvent);
        //sendComplexConnectMessageToBuyer(connectCommandEvent);
	}

}
