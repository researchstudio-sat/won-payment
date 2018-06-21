package won.payment.paypal.bot.action;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandResultEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.OpenFromOtherNeedEvent;
import won.bot.framework.eventbot.filter.impl.CommandResultFilter;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnFirstEventListener;
import won.payment.paypal.bot.impl.PaypalBotContextWrapper;
import won.payment.paypal.bot.model.PaymentBridge;
import won.payment.paypal.bot.model.PaymentStatus;
import won.payment.paypal.bot.util.InformationExtractor;
import won.protocol.agreement.AgreementProtocolState;
import won.protocol.model.Connection;
import won.protocol.util.WonConversationUtils;
import won.protocol.util.WonRdfUtils;

/**
 * When the counterpart has accepted the connection, this action will be invoked.
 * It changes the sate of the bridge and proposes the payment to the buyer.
 * 
 * @author schokobaer
 *
 */
public class ConnectionAcceptedAction extends BaseEventBotAction {

	public ConnectionAcceptedAction(EventListenerContext eventListenerContext) {
		super(eventListenerContext);
	}

	@Override
	protected void doRun(Event event, EventListener executingListener) throws Exception {
		
		if (event instanceof OpenFromOtherNeedEvent) {
			EventListenerContext ctx = getEventListenerContext();
			Connection con = ((OpenFromOtherNeedEvent) event).getCon();
			PaymentBridge bridge = PaypalBotContextWrapper.instance(ctx).getOpenBridge(con.getNeedURI());
			
			if (bridge.getMerchantConnection() != null &&
					con.toString().equals(bridge.getMerchantConnection().toString())) {
				logger.info("merchant accepted the connection");
				bridge.setStatus(PaymentStatus.GOALUNSATISFIED);
				PaypalBotContextWrapper.instance(ctx).putOpenBridge(con.getNeedURI(), bridge);
			} else if (bridge.getBuyerConnection() != null &&
					con.toString().equals(bridge.getBuyerConnection().toString())) {
				logger.info("buyer accepted the connection");
				PaypalBotContextWrapper.instance(ctx).putOpenBridge(con.getNeedURI(), bridge);
				proposePaymentToBuyer(con);
			} else {
				logger.error("OpenFromOtherNeedEvent from not registered connection URI {}", con.toString());
			}
		}
		
	}
	
	private void proposePaymentToBuyer(Connection con) {
		EventListenerContext ctx = getEventListenerContext();
		PaymentBridge bridge = PaypalBotContextWrapper.instance(ctx).getOpenBridge(con.getNeedURI());
		
		AgreementProtocolState state = WonConversationUtils.getAgreementProtocolState(con.getConnectionURI(),
				ctx.getLinkedDataSource());
		Dataset dataset = state.getAgreements();
		Model agreements = dataset.getUnionModel();
				
		WonRdfUtils.MessageUtils.addProcessing(agreements, "Payment summary");
		final ConnectionMessageCommandEvent connectionMessageCommandEvent = new ConnectionMessageCommandEvent(con, agreements);
		
		ctx.getEventBus().subscribe(ConnectionMessageCommandResultEvent.class, new ActionOnFirstEventListener(ctx, new CommandResultFilter(connectionMessageCommandEvent), new BaseEventBotAction(ctx) {
            @Override
            protected void doRun(Event event, EventListener executingListener) throws Exception {
                ConnectionMessageCommandResultEvent connectionMessageCommandResultEvent = (ConnectionMessageCommandResultEvent) event;
                if(connectionMessageCommandResultEvent.isSuccess()){
                    Model agreementMessage = WonRdfUtils.MessageUtils.processingMessage("You want to generate the payment? Then accept the proposal.");
                    WonRdfUtils.MessageUtils.addProposes(agreementMessage, ((ConnectionMessageCommandSuccessEvent) connectionMessageCommandResultEvent).getWonMessage().getMessageURI());
                    ctx.getEventBus().publish(new ConnectionMessageCommandEvent(con, agreementMessage));
                    bridge.setStatus(PaymentStatus.PROPOSED);
                    PaypalBotContextWrapper.instance(ctx).putOpenBridge(con.getNeedURI(), bridge);
                }else{
                    logger.error("FAILURERESPONSEEVENT FOR PROPOSAL PAYLOAD");
                }
            }
        }));

        ctx.getEventBus().publish(connectionMessageCommandEvent);
		
		
	}

}
