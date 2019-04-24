package won.payment.paypal.bot.action.connect;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.BaseAtomAndConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.close.CloseCommandEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.WonMessageReceivedOnConnectionEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.model.Connection;

/**
 * Sends a message and then immediately closes the connection.
 * 
 * @author schokobaer
 *
 */
public class ConnectionDenierAction extends BaseEventBotAction {

//	private static final Long WAITING_TIME = 5000L;

	public ConnectionDenierAction(EventListenerContext eventListenerContext) {
		super(eventListenerContext);
	}

	@Override
	protected void doRun(Event event, EventListener executingListener) throws Exception {
		if (!(event instanceof ConnectFromOtherAtomEvent)) {
			return;
		}

		if (event instanceof WonMessageReceivedOnConnectionEvent) {
			EventListenerContext ctx = getEventListenerContext();
			EventBus bus = ctx.getEventBus();
			Connection con = ((BaseAtomAndConnectionSpecificEvent) event).getCon();

			/*
			 * // Send msg String msg =
			 * "I am the master here. Only I am opening connections. Get off ..."; Model
			 * model = WonRdfUtils.MessageUtils.textMessage(msg);
			 * getEventListenerContext().getEventBus().publish(new
			 * ConnectionMessageCommandEvent(con, model));
			 * 
			 * // Wait some time so the client can read the message try {
			 * Thread.sleep(WAITING_TIME); } catch (InterruptedException e) {
			 * 
			 * }
			 */

			logger.info("Atom {} tryed to connect to atom {} with connection {}", con.getTargetAtomURI(),
					con.getAtomURI(), con.getConnectionURI());

			// Close Connection
			bus.publish(new CloseCommandEvent(con));
		}

	}
}
