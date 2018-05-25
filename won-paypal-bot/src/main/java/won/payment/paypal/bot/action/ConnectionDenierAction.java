package won.payment.paypal.bot.action;

import org.apache.jena.rdf.model.Model;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.close.CloseCommandEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.WonMessageReceivedOnConnectionEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;

/**
 * Sends a message and then immediately closes the connection.
 * 
 * @author Andreas
 *
 */
public class ConnectionDenierAction extends BaseEventBotAction {

	private static final Long WAITING_TIME = 5000L;

	public ConnectionDenierAction(EventListenerContext eventListenerContext) {
		super(eventListenerContext);
	}

	@Override
	protected void doRun(Event event, EventListener executingListener) throws Exception {
		if (!(event instanceof ConnectFromOtherNeedEvent)) {
			return;
		}

		if (event instanceof WonMessageReceivedOnConnectionEvent) {
			EventListenerContext ctx = getEventListenerContext();
			EventBus bus = ctx.getEventBus();
			Connection con = ((BaseNeedAndConnectionSpecificEvent) event).getCon();

			/*
			// Send msg
			String msg = "I am the master here. Only I am opening connections. Get off ...";
			Model model = WonRdfUtils.MessageUtils.textMessage(msg);
			getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(con, model));

			// Wait some time so the client can read the message
			try {
				Thread.sleep(WAITING_TIME);
			} catch (InterruptedException e) {

			}
			*/

			// Close Connection
			bus.publish(new CloseCommandEvent(con));
		}

	}
}
