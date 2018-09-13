package won.payment.paypal.bot.action.precondition;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.RDF;
import org.topbraid.shacl.vocabulary.SH;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionEvent;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionMetEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandResultEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandSuccessEvent;
import won.bot.framework.eventbot.filter.impl.CommandResultFilter;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnFirstEventListener;
import won.payment.paypal.bot.impl.PaypalBotContextWrapper;
import won.payment.paypal.bot.model.PaymentBridge;
import won.payment.paypal.bot.model.PaymentStatus;
import won.payment.paypal.bot.util.InformationExtractor;
import won.payment.paypal.bot.util.WonPayRdfUtils;
import won.payment.paypal.bot.validator.PaymentModelValidator;
import won.protocol.agreement.AgreementProtocolState;
import won.protocol.model.Connection;
import won.protocol.model.NeedState;
import won.protocol.util.NeedModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONPAY;

/**
 * Shows the report of the SHACL validation. Also retracts
 * old proposals if available.
 * 
 * @author schokobaer
 *
 */
public class PreconditionMetAction extends BaseEventBotAction {

	private PaymentModelValidator validator = new PaymentModelValidator();
	
	public PreconditionMetAction(EventListenerContext eventListenerContext) {
		super(eventListenerContext);
	}

	@Override
	protected void doRun(Event event, EventListener executingListener) throws Exception {
		EventListenerContext ctx = getEventListenerContext();

        if(ctx.getBotContextWrapper() instanceof PaypalBotContextWrapper && event instanceof PreconditionMetEvent) {
            Connection con = ((BaseNeedAndConnectionSpecificEvent) event).getCon();
            PaymentBridge bridge = PaypalBotContextWrapper.paymentBridge(ctx, con);
            
            if (bridge.getStatus() != PaymentStatus.BUILDING) {
            	return;
            }
            
            // Denie buyer msgs
            if (bridge.getBuyerConnection() != null && 
            		bridge.getBuyerConnection().getConnectionURI().equals(con.getConnectionURI())) {
            	return;
            }
            
            Model preconditionEventPayload = ((PreconditionEvent) event).getPayload().getInstanceModel();

            logger.info("Precondition Met");
            
            Double amount = InformationExtractor.getAmount(preconditionEventPayload);
            String currency = InformationExtractor.getCurrency(preconditionEventPayload);
            String receiver = InformationExtractor.getReceiver(preconditionEventPayload);
            String secret = InformationExtractor.getSecret(preconditionEventPayload);
            String counterpart = InformationExtractor.getCounterpart(preconditionEventPayload);
            String feePayer = InformationExtractor.getFeePayer(preconditionEventPayload);
            Double tax = InformationExtractor.getTax(preconditionEventPayload);
            String invoiceId = InformationExtractor.getInvoiceId(preconditionEventPayload);
            String invoiceDetails = InformationExtractor.getInvoiceDetails(preconditionEventPayload);
            String expirationTime = InformationExtractor.getExpirationTime(preconditionEventPayload);
            
            logger.info("Amount: " + amount);
            logger.info("Currency: " + currency);
            logger.info("Receiver: " + receiver);
            logger.info("Secret: " + secret);
            logger.info("Counterpart: " + counterpart);
            logger.info("FeePayer: " + feePayer);
            logger.info("Tax: " + tax);
            logger.info("InvoiceId: " + invoiceId);
            logger.info("InvoiceDetails: " + invoiceDetails);
            logger.info("ExpirationTime: " + expirationTime);
            
            try {
            	validator.validate(preconditionEventPayload, con);
            	if (!retractOldPropose(con, preconditionEventPayload)) {
            		return;
            	}
            	if (!counterpartAccessible(counterpart, con)) {
            		return;
            	}
            	
            	RdfUtils.findOrCreateBaseResource(preconditionEventPayload).addProperty(RDF.type, WONPAY.PAYMENT_SUMMARY);
    			WonRdfUtils.MessageUtils.addProcessing(preconditionEventPayload, "Payment summary");
    			WonRdfUtils.MessageUtils.addMessage(preconditionEventPayload, "Payment summary");
            	final ConnectionMessageCommandEvent connectionMessageCommandEvent = new ConnectionMessageCommandEvent(con, preconditionEventPayload);

                ctx.getEventBus().subscribe(ConnectionMessageCommandResultEvent.class, new ActionOnFirstEventListener(ctx, new CommandResultFilter(connectionMessageCommandEvent), new BaseEventBotAction(ctx) {
                    @Override
                    protected void doRun(Event event, EventListener executingListener) throws Exception {
                        ConnectionMessageCommandResultEvent connectionMessageCommandResultEvent = (ConnectionMessageCommandResultEvent) event;
                        if(connectionMessageCommandResultEvent.isSuccess()){
                            Model agreementMessage = WonRdfUtils.MessageUtils.processingMessage(currency + " " + amount + " to " + receiver +
                            		"....Do you want to confirm the payment? Then accept the proposal. After accepting the payment is published"
                            		+ " to the counterpart and you won't be able to cancle it!");
                            WonRdfUtils.MessageUtils.addProposes(agreementMessage, ((ConnectionMessageCommandSuccessEvent) connectionMessageCommandResultEvent).getWonMessage().getMessageURI());
                            ctx.getEventBus().publish(new ConnectionMessageCommandEvent(con, agreementMessage));
                            bridge.setStatus(PaymentStatus.BUILDING);
                            PaypalBotContextWrapper.instance(ctx).putOpenBridge(con.getNeedURI(), bridge);
                        }else{
                            logger.error("FAILURERESPONSEEVENT FOR PROPOSAL PAYLOAD");
                        }
                    }
                }));

                ctx.getEventBus().publish(connectionMessageCommandEvent);
            }
            catch (Exception e) {
            	Model errorMessage = WonRdfUtils.MessageUtils.textMessage(e.getMessage());
                ctx.getEventBus().publish(new ConnectionMessageCommandEvent(con, errorMessage));
            }
        }

	}
	
	/**
	 * If the new proposal is the same as the last one, then it returns false
	 * and nothing should be done. If it is different, this method publishes a
	 * retract message for the old proposal and returns true which means the new
	 * proposal should be published.
	 * 
	 * @param con
	 * @param newProposal
	 * @return true if new proposal is different then the old one
	 */
	private boolean retractOldPropose(Connection con, Model newProposal) {
		
		AgreementProtocolState agreementProtocolState = AgreementProtocolState.of(con.getConnectionURI(),
				getEventListenerContext().getLinkedDataSource());
		
		URI lastProposalUri = agreementProtocolState.getLatestPendingProposal();
		Model lastProposal = lastProposalUri != null ? agreementProtocolState.getPendingProposal(lastProposalUri) : null;
		
		if (lastProposal == null) {
			return true;
		}
		Resource lastPayment = lastProposal.listResourcesWithProperty(RDF.type, WONPAY.PAYMENT).next();
		Resource newPayment = newProposal.listResourcesWithProperty(RDF.type, WONPAY.PAYMENT).next();
		lastProposal = ModelFactory.createDefaultModel().add(lastPayment.listProperties());
		newProposal = ModelFactory.createDefaultModel().add(newPayment.listProperties());
		
		if (lastProposal.isIsomorphicWith(newProposal)) {
			logger.debug("Same proposal as the last one");
			// Stop publishing a new one ???
			return false;
		}
		
		// Find out payment summary URI
		StringBuilder paymentSummaryUriBuilder = new StringBuilder();
		agreementProtocolState.getPendingProposal(lastProposalUri).listStatements(null, RDF.type, WONPAY.PAYMENT_SUMMARY).forEachRemaining(stmt -> {
			paymentSummaryUriBuilder.append(stmt.getSubject().getURI());
		});
		
		// Retract the old proposal
		try {
			Model retractResponse = WonRdfUtils.MessageUtils.retractsMessage(lastProposalUri, new URI(paymentSummaryUriBuilder.toString()));
			getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(con, retractResponse));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		return true;
	}
	
	/**
	 * Checks if the given Need uri is available and not deactivated.
	 * 
	 * @param counterpartNeedUri Need Uri.
	 * @param con Connection of the merchant need.
	 * @return true if accessible, otherwise false.
	 */
	private boolean counterpartAccessible(String counterpartNeedUri, Connection con) {		
		Model result = WonRdfUtils.MessageUtils.processingMessage("Error with the counterpart need");
		Resource report = result.createResource();
		result = WonRdfUtils.MessageUtils.addToMessage(result, SH.result, report);
		report.addProperty(SH.value, new ResourceImpl(counterpartNeedUri));
		report.addProperty(SH.resultPath, WONPAY.HAS_NEED_COUNTERPART);
		report.addProperty(SH.resultSeverity, SH.Violation);
		report.addProperty(SH.focusNode, new ResourceImpl(WonPayRdfUtils.getPaymentModelUri(con)));
		
		
		try {
			Dataset remoteNeedRDF =
			        getEventListenerContext().getLinkedDataSource().getDataForResource(new URI(counterpartNeedUri));
			if (remoteNeedRDF == null) {
				throw new NullPointerException();
			}
			NeedModelWrapper needWrapper = new NeedModelWrapper(remoteNeedRDF);
			if (needWrapper.getNeedState() == NeedState.ACTIVE) {
				// Need active
				return true;
			}
			// Need inactive
			report.addProperty(SH.resultMessage, "Counterpart need is inactive");
		} catch (NullPointerException | URISyntaxException e) {
			// Need not accessible
			report.addProperty(SH.resultMessage, "Counterpart need is not accessible");
		}
		
		ConnectionMessageCommandEvent response = new ConnectionMessageCommandEvent(con, result);
		getEventListenerContext().getEventBus().publish(response);
		
		return false;
	}

}
