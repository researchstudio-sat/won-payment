package won.payment.paypal.bot.action.agreement;

import org.apache.jena.rdf.model.Model;

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
import won.payment.paypal.bot.util.InformationExtractor;
import won.payment.paypal.bot.validator.PaymentModelValidator;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;
import won.utils.goals.GoalInstantiationResult;

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

            PaypalBotContextWrapper botContextWrapper = (PaypalBotContextWrapper) ctx.getBotContextWrapper();

            GoalInstantiationResult preconditionEventPayload = ((PreconditionEvent) event).getPayload();

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

            final String preconditionUri = ((PreconditionEvent) event).getPreconditionUri();

            Model paymentModel = preconditionEventPayload.getInstanceModel();
            
            try {
            	validator.validate(paymentModel, con);
            	Model payloadModel = preconditionEventPayload.getInstanceModel();
            	WonRdfUtils.MessageUtils.addProcessing(payloadModel, "Payment summary");
            	final ConnectionMessageCommandEvent connectionMessageCommandEvent = new ConnectionMessageCommandEvent(con, payloadModel);

                ctx.getEventBus().subscribe(ConnectionMessageCommandResultEvent.class, new ActionOnFirstEventListener(ctx, new CommandResultFilter(connectionMessageCommandEvent), new BaseEventBotAction(ctx) {
                    @Override
                    protected void doRun(Event event, EventListener executingListener) throws Exception {
                        ConnectionMessageCommandResultEvent connectionMessageCommandResultEvent = (ConnectionMessageCommandResultEvent) event;
                        if(connectionMessageCommandResultEvent.isSuccess()){
                            Model agreementMessage = WonRdfUtils.MessageUtils.processingMessage(currency + " " + amount + " to " + receiver +
                            		"....Do you want to confirm the payment? Then accept the proposal");
                            WonRdfUtils.MessageUtils.addProposes(agreementMessage, ((ConnectionMessageCommandSuccessEvent) connectionMessageCommandResultEvent).getWonMessage().getMessageURI());
                            ctx.getEventBus().publish(new ConnectionMessageCommandEvent(con, agreementMessage));
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

}
