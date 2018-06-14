package won.payment.paypal.bot.action.agreement;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionEvent;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionMetEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.payment.paypal.bot.impl.PaypalBotContextWrapper;
import won.payment.paypal.bot.util.InformationExtractor;
import won.protocol.model.Connection;
import won.utils.goals.GoalInstantiationResult;

public class PreconditionMetAction extends BaseEventBotAction {

	public PreconditionMetAction(EventListenerContext eventListenerContext) {
		super(eventListenerContext);
	}

	@Override
	protected void doRun(Event event, EventListener executingListener) throws Exception {
		EventListenerContext ctx = getEventListenerContext();

        if(ctx.getBotContextWrapper() instanceof PaypalBotContextWrapper && event instanceof PreconditionMetEvent) {
            Connection connection = ((BaseNeedAndConnectionSpecificEvent) event).getCon();

            PaypalBotContextWrapper botContextWrapper = (PaypalBotContextWrapper) ctx.getBotContextWrapper();

            GoalInstantiationResult preconditionEventPayload = ((PreconditionEvent) event).getPayload();

            //logger.info("Precondition Met: " + preconditionEventPayload);
            
            Double amount = InformationExtractor.getAmount(preconditionEventPayload);
            String currency = InformationExtractor.getCurrency(preconditionEventPayload);
            String receiver = InformationExtractor.getReceiver(preconditionEventPayload);
            String secret = InformationExtractor.getSecret(preconditionEventPayload);
            String counterpart = InformationExtractor.getCounterpart(preconditionEventPayload);
            
            logger.info("Amount: " + amount);
            logger.info("Currency: " + currency);
            logger.info("Receiver: " + receiver);
            logger.info("Secret: " + secret);
            logger.info("Counterpart: " + counterpart);
            
//            DepartureAddress departureAddress = InformationExtractor.getDepartureAddress(preconditionEventPayload);
//            DestinationAddress destinationAddress = InformationExtractor.getDestinationAddress(preconditionEventPayload);
//
//            final ParseableResult checkOrderResponse = new ParseableResult(botContextWrapper.getMobileBooking().checkOrder(departureAddress, destinationAddress));
//            final String preconditionUri = ((PreconditionEvent) event).getPreconditionUri();
//
//            if(!checkOrderResponse.isError()) {
//                final ConnectionMessageCommandEvent connectionMessageCommandEvent = new ConnectionMessageCommandEvent(connection, preconditionEventPayload.getInstanceModel());
//
//                ctx.getEventBus().subscribe(ConnectionMessageCommandResultEvent.class, new ActionOnFirstEventListener(ctx, new CommandResultFilter(connectionMessageCommandEvent), new BaseEventBotAction(ctx) {
//                    @Override
//                    protected void doRun(Event event, EventListener executingListener) throws Exception {
//                        ConnectionMessageCommandResultEvent connectionMessageCommandResultEvent = (ConnectionMessageCommandResultEvent) event;
//                        if(connectionMessageCommandResultEvent.isSuccess()){
//                            Model agreementMessage = WonRdfUtils.MessageUtils.textMessage("Ride from " + departureAddress + " to " + destinationAddress + ": "
//                                    + checkOrderResponse + "....Do you want to confirm the taxi order? Then accept the proposal");
//                            WonRdfUtils.MessageUtils.addProposes(agreementMessage, ((ConnectionMessageCommandSuccessEvent) connectionMessageCommandResultEvent).getWonMessage().getMessageURI());
//                            ctx.getEventBus().publish(new ConnectionMessageCommandEvent(connection, agreementMessage));
//                        }else{
//                            logger.error("FAILURERESPONSEEVENT FOR PROPOSAL PAYLOAD");
//                        }
//                    }
//                }));
//
//                ctx.getEventBus().publish(connectionMessageCommandEvent);
//            }else {
//                Model errorMessage = WonRdfUtils.MessageUtils.textMessage(checkOrderResponse.toString());
//                ctx.getEventBus().publish(new ConnectionMessageCommandEvent(connection, errorMessage));
//            }
        }

	}

}
