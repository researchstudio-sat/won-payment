package won.payment.paypal.bot.action;

import java.net.URI;
import java.util.Map;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.EventBotAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.bot.framework.eventbot.event.ConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.MessageEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.payment.paypal.bot.model.PaymentBridge;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;

public class MessageBrokerAction extends BaseEventBotAction {

	private EventBotAction merchantAction;
	private EventBotAction buyerAction;
	private Map<URI, PaymentBridge> openBridges;

	public MessageBrokerAction(EventListenerContext eventListenerContext, Map<URI, PaymentBridge> openBridges,
			EventBotAction merchantAction, EventBotAction buyerAction) {
		super(eventListenerContext);
		this.openBridges = openBridges;
		this.merchantAction = merchantAction;
		this.buyerAction = buyerAction;
	}

	@Override
	protected void doRun(Event event, EventListener executingListener) throws Exception {

		ConnectionSpecificEvent messageEvent = (ConnectionSpecificEvent) event;
		if (messageEvent instanceof BaseNeedAndConnectionSpecificEvent) {

			Connection con = ((BaseNeedAndConnectionSpecificEvent) messageEvent).getCon();

			// Check for Bridge
			URI needUri = con.getNeedURI();
			if (openBridges.containsKey(needUri)) {
				PaymentBridge bridge = openBridges.get(needUri);
				if (bridge.getMerchantConnection() == null) {
					bridge.setMerchantConnection(con);
				} else if (!con.getConnectionURI().equals(bridge.getMerchantConnection().getConnectionURI())) {
					bridge.setBuyerConnection(con);
				}

				// Break down
				if (bridge.getMerchantConnection() != null &&
						con.getConnectionURI().equals(bridge.getMerchantConnection().getConnectionURI())) {
					merchantAction.getActionTask(event, executingListener).run();
				} else if (bridge.getBuyerConnection() != null &&
						con.getConnectionURI().equals(bridge.getBuyerConnection().getConnectionURI())) {
					buyerAction.getActionTask(event, executingListener).run();
				} else {
					// Error !?
				}
			} else {
				// Error !?
			}

		}

	}

}
