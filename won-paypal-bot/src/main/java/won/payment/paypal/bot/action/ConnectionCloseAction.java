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
import won.payment.paypal.bot.impl.PaypalBotContextWrapper;
import won.payment.paypal.bot.model.PaymentBridge;
import won.payment.paypal.bot.model.PaymentStatus;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;

/**
 * Eventhandler which will be invoked when a connection was closed by the
 * merchant or the buyer.
 * 
 * @author schokobaer
 *
 */
public class ConnectionCloseAction extends BaseEventBotAction {

	public ConnectionCloseAction(EventListenerContext eventListenerContext) {
		super(eventListenerContext);
	}

	private void makeTextMsg(String msg, Connection con) {
		if (con == null) {
			return;
		}
		Model model = WonRdfUtils.MessageUtils.textMessage(msg);
		getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(con, model));
	}

	@Override
	protected void doRun(Event event, EventListener executingListener) throws Exception {

		if (event instanceof CloseFromOtherNeedEvent) {
			CloseFromOtherNeedEvent closeEvent = (CloseFromOtherNeedEvent) event;
			Connection con = closeEvent.getCon();
			PaymentBridge bridge = ((PaypalBotContextWrapper)getEventListenerContext().getBotContextWrapper()).getOpenBridge(con.getNeedURI());

			if (bridge.getStatus() == PaymentStatus.COMPLETED) {
				closePaymentBridge(bridge, con);
			} else if (bridge.getStatus() == PaymentStatus.DENIED) {
				// Do nothing
			} else {
				unexpectedClosure(bridge, con);
				logger.debug("Unexpected closure in the Need {}", con.getNeedURI());
			}
		}

	}

	/**
	 * Simply removes the connection from the payment bridge. If both are closed the
	 * need will be closed.
	 * 
	 * @param bridge
	 *            Bridge where to connections are.
	 * @param con
	 *            Connection which was closed.
	 */
	private void closePaymentBridge(PaymentBridge bridge, Connection con) {
		// Merchant closure
		if (bridge.getMerchantConnection() != null
				&& bridge.getMerchantConnection().getConnectionURI().equals(con.getConnectionURI())) {
			bridge.setMerchantConnection(null);
			logger.debug("Merchant has closed the connection after completion in the Need {}", con.getNeedURI());
		}

		// Buyer closure
		if (bridge.getBuyerConnection() != null
				&& bridge.getBuyerConnection().getConnectionURI().equals(con.getConnectionURI())) {
			bridge.setBuyerConnection(null);
			logger.debug("Buyer has closed the connection after completion in the Need {}", con.getNeedURI());
		}

		closeNeed(bridge, con);
	}

	/**
	 * Informs the counterpart if the paypal payment is already generated or
	 * accepted.
	 * 
	 * @param bridge
	 *            Bridge of the connection.
	 * @param con
	 *            Connection which was closed.
	 */
	private void unexpectedClosure(PaymentBridge bridge, Connection con) {

		if (bridge.getStatus() == PaymentStatus.ACCEPTED || bridge.getStatus() == PaymentStatus.GENERATED) {
			if (bridge.getMerchantConnection() != null
					&& bridge.getMerchantConnection().getConnectionURI().equals(con.getConnectionURI())) {
				bridge.setMerchantConnection(null);
				makeTextMsg("ATTENTION: THE MERCHANT HAS LEFT. DO NOT EXECUTE THE PAYMENT!",
						bridge.getBuyerConnection());
				logger.debug("Merchant has closed the connection in status {}" + " in the Need {}",
						bridge.getStatus().name(), con.getNeedURI());
			}

			if (bridge.getBuyerConnection() != null
					&& bridge.getBuyerConnection().getConnectionURI().equals(con.getConnectionURI())) {
				bridge.setBuyerConnection(null);
				makeTextMsg("The Buyer has left. Type 'payment check' for validating the payment or wait "
						+ "until you get informed", bridge.getMerchantConnection());
				logger.debug("Buyer has closed the connection in status {}" + " in the Need {}",
						bridge.getStatus().name(), con.getNeedURI());
			}
		} else if (bridge.getStatus() == PaymentStatus.PUBLISHED) {
			// If the merchant left, tell the buyer not to do anything and leave the channel
			// If the buyer left just tell the merchant
			// If the buyer declines the connection just tell the merchant
			if (bridge.getMerchantConnection() != null
					&& bridge.getMerchantConnection().getConnectionURI().equals(con.getConnectionURI())) {
				bridge.setMerchantConnection(null);
				makeTextMsg("ATTENTION: THE MERCHANT HAS LEFT. PLEASE CONTACT THE MERCHANT!",
						bridge.getBuyerConnection());
				logger.debug("Merchant has closed the connection in status {}" + " in the Need {}",
						bridge.getStatus().name(), con.getNeedURI());
			} else if (bridge.getBuyerConnection() != null
					&& bridge.getBuyerConnection().getConnectionURI().equals(con.getConnectionURI())) {
				bridge.setBuyerConnection(null);
				makeTextMsg("The Buyer has left without accepting. Change the payment and validate it again.",
						bridge.getMerchantConnection());
				logger.debug("Buyer has closed the connection in status {}" + " in the Need {}",
						bridge.getStatus().name(), con.getNeedURI());
			} else if (bridge.getBuyerConnection() == null) {
				makeTextMsg("The buyer declined the connection. Change the payment and validate it again.",
						bridge.getMerchantConnection());
				bridge.setStatus(PaymentStatus.DENIED);
				logger.debug("Buyer has closed the connection in status {}" + " in the Need {}",
						bridge.getStatus().name(), con.getNeedURI());
			}
			
		} else {
			// Can only be the merchant
			if (bridge.getMerchantConnection() != null
					&& bridge.getMerchantConnection().getConnectionURI().equals(con.getConnectionURI())) {
				bridge.setMerchantConnection(null);
				logger.debug("Merchant has closed the connection in status {}" + " in the Need {}",
						bridge.getStatus().name(), con.getNeedURI());
			} else {
				// Error ?! OR status == Nothing
				logger.debug("Merchant has closed the connection in status {}" + " in the Need {}",
						bridge.getStatus().name(), con.getNeedURI());
			}
		}

		closeNeed(bridge, con);

	}

	/**
	 * Closes the Need if the payment bridge's connections are empty.
	 * 
	 * @param bridge
	 *            PaymentBridge to check.
	 * @param con
	 *            Connection which was closed.
	 */
	private void closeNeed(PaymentBridge bridge, Connection con) {
		// Both closed
		if (bridge.getMerchantConnection() == null && bridge.getBuyerConnection() == null) {
			((PaypalBotContextWrapper)getEventListenerContext().getBotContextWrapper()).removeOpenBridge(con.getNeedURI());
			getEventListenerContext().getEventBus().publish(new DeactivateNeedCommandEvent(con.getNeedURI()));
			logger.debug("Need gets closed {}", con.getNeedURI());
		}
	}

}
