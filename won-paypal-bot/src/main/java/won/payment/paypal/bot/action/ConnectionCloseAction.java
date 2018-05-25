package won.payment.paypal.bot.action;

import java.net.URI;
import java.util.Map;

import org.apache.jena.rdf.model.Model;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.command.deactivate.DeactivateNeedCommandEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.CloseFromOtherNeedEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.payment.paypal.bot.model.PaymentBridge;
import won.payment.paypal.bot.model.PaymentStatus;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;

public class ConnectionCloseAction extends BaseEventBotAction {

	private Map<URI, PaymentBridge> openPayments;

	public ConnectionCloseAction(EventListenerContext eventListenerContext, Map<URI, PaymentBridge> openPayments) {
		super(eventListenerContext);
		this.openPayments = openPayments;
	}

	private void makeTextMsg(String msg, Connection con) {
		Model model = WonRdfUtils.MessageUtils.textMessage(msg);
		getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(con, model));
	}

	@Override
	protected void doRun(Event event, EventListener executingListener) throws Exception {

		if (event instanceof CloseFromOtherNeedEvent) {
			CloseFromOtherNeedEvent closeEvent = (CloseFromOtherNeedEvent) event;
			Connection con = closeEvent.getCon();
			PaymentBridge bridge = openPayments.get(con.getNeedURI());

			if (bridge.getStatus() == PaymentStatus.COMPLETED) {
				closePaymentBridge(bridge, con);
			} else if (bridge.getStatus() == PaymentStatus.DENIED) {
				// Do nothing
			} else {
				unexpectedClosure(bridge, con);
			}
		}

	}

	private void closePaymentBridge(PaymentBridge bridge, Connection con) {
		// Merchant closure
		if (bridge.getMerchantConnection() != null
				&& bridge.getMerchantConnection().getConnectionURI().equals(con.getConnectionURI())) {
			bridge.setMerchantConnection(null);
		}

		// Buyer closure
		if (bridge.getBuyerConnection() != null
				&& bridge.getBuyerConnection().getConnectionURI().equals(con.getConnectionURI())) {
			bridge.setBuyerConnection(null);
		}

		// Both closed
		if (bridge.getMerchantConnection() == null && bridge.getBuyerConnection() == null) {
			openPayments.remove(con.getNeedURI());
			getEventListenerContext().getEventBus().publish(new DeactivateNeedCommandEvent(con.getNeedURI()));
		}
	}

	private void unexpectedClosure(PaymentBridge bridge, Connection con) {
		// TODO: ??
	}

}
