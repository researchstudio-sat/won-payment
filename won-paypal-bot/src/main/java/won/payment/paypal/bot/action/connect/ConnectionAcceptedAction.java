package won.payment.paypal.bot.action.connect;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.wonmessage.OpenFromOtherNeedEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.payment.paypal.bot.event.analyze.ConversationAnalyzationCommandEvent;
import won.payment.paypal.bot.impl.PaypalBotContextWrapper;
import won.payment.paypal.bot.model.PaymentBridge;
import won.payment.paypal.bot.model.PaymentStatus;
import won.protocol.model.Connection;

/**
 * When the counterpart has accepted the connection, this action will be invoked.
 * It changes the sate of the bridge and generates the payment and sends the link
 * to the buyer.
 * TODO: can I delete this file completely?
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
					con.getConnectionURI().equals(bridge.getMerchantConnection().getConnectionURI())) {
				logger.info("merchant accepted the connection");
				bridge.setStatus(PaymentStatus.BUILDING);
				PaypalBotContextWrapper.instance(ctx).putOpenBridge(con.getNeedURI(), bridge);
				ctx.getEventBus().publish(new ConversationAnalyzationCommandEvent(con));
			} else {
				logger.error("OpenFromOtherNeedEvent from not registered connection URI {}", con.toString());
			}
		}
		
	}
}
