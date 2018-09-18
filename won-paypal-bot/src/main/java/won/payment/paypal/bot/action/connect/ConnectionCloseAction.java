package won.payment.paypal.bot.action.connect;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.ResourceImpl;

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
import won.protocol.agreement.AgreementProtocolState;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONAGR;

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
		Model model = WonRdfUtils.MessageUtils.processingMessage(msg);
		getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(con, model));
	}

	@Override
	protected void doRun(Event event, EventListener executingListener) throws Exception {

		if (event instanceof CloseFromOtherNeedEvent) {
			CloseFromOtherNeedEvent closeEvent = (CloseFromOtherNeedEvent) event;
			Connection con = closeEvent.getCon();
			PaymentBridge bridge = PaypalBotContextWrapper.instance(getEventListenerContext())
					.getOpenBridge(con.getNeedURI());

			if (bridge.getStatus().ordinal() >= PaymentStatus.COMPLETED.ordinal()) {
				closeConnection(bridge, con);
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
	private void closeConnection(PaymentBridge bridge, Connection con) {
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

		PaypalBotContextWrapper.instance(getEventListenerContext()).putOpenBridge(con.getNeedURI(), bridge);

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
			PaypalBotContextWrapper.instance(getEventListenerContext()).removeOpenBridge(con.getNeedURI());
			getEventListenerContext().getEventBus().publish(new DeactivateNeedCommandEvent(con.getNeedURI()));
			logger.debug("Need gets closed {}", con.getNeedURI());
		}
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

		// Fix here everything !!!
		bridge.setStatus(PaymentStatus.FAILURE);
		if (bridge.getBuyerConnection() != null
				&& bridge.getBuyerConnection().getConnectionURI().equals(con.getConnectionURI())) {
			// Buyer closed
			logger.info("Unexpected closure from buyer with connection {} in the need {}", con.toString(),
					con.getNeedURI().toString());
			bridge.setBuyerConnection(null);
			if (bridge.getMerchantConnection() != null) {
				makeTextMsg("Buyer closed the connection. The payment process failed. Please exit the connection.",
						bridge.getMerchantConnection());
			}
		} else if (bridge.getMerchantConnection() != null
				&& bridge.getMerchantConnection().getConnectionURI().equals(con.getConnectionURI())) {
			// Merchant closed
			logger.info("Unexpected closure from merchant with connection {} in the need {}", con.toString(),
					con.getNeedURI().toString());
			bridge.setMerchantConnection(null);
			if (bridge.getBuyerConnection() != null) {
				makeTextMsg("Merchant closed the connection. The payment process failed. Please exit the connection",
						bridge.getBuyerConnection());
			}
		} else {
			// Not possible. Neither merchant nor buyer who closed the connection !!!
		}

		PaypalBotContextWrapper.instance(getEventListenerContext()).putOpenBridge(con.getNeedURI(), bridge);

		closeNeed(bridge, con);

		/*
		 * if (bridge.getStatus() == PaymentStatus.PP_ACCEPTED || bridge.getStatus() ==
		 * PaymentStatus.GENERATED) { if (bridge.getMerchantConnection() != null &&
		 * bridge.getMerchantConnection().getConnectionURI().equals(con.getConnectionURI
		 * ())) { bridge.setMerchantConnection(null);
		 * makeTextMsg("ATTENTION: THE MERCHANT HAS LEFT. DO NOT EXECUTE THE PAYMENT!",
		 * bridge.getBuyerConnection());
		 * logger.debug("Merchant has closed the connection in status {}" +
		 * " in the Need {}", bridge.getStatus().name(), con.getNeedURI()); }
		 * 
		 * if (bridge.getBuyerConnection() != null &&
		 * bridge.getBuyerConnection().getConnectionURI().equals(con.getConnectionURI())
		 * ) { bridge.setBuyerConnection(null);
		 * makeTextMsg("The Buyer has left. Type 'payment check' for validating the payment or wait "
		 * + "until you get informed", bridge.getMerchantConnection());
		 * logger.debug("Buyer has closed the connection in status {}" +
		 * " in the Need {}", bridge.getStatus().name(), con.getNeedURI()); } } else if
		 * (bridge.getStatus() == PaymentStatus.BUILDING) { // If the merchant left,
		 * tell the buyer not to do anything and leave the channel // If the buyer left
		 * just tell the merchant // If the buyer declines the connection just tell the
		 * merchant if (bridge.getMerchantConnection() != null &&
		 * bridge.getMerchantConnection().getConnectionURI().equals(con.getConnectionURI
		 * ())) { bridge.setMerchantConnection(null);
		 * makeTextMsg("ATTENTION: THE MERCHANT HAS LEFT. PLEASE CONTACT THE MERCHANT!",
		 * bridge.getBuyerConnection());
		 * logger.debug("Merchant has closed the connection in status {}" +
		 * " in the Need {}", bridge.getStatus().name(), con.getNeedURI()); } else if
		 * (bridge.getBuyerConnection() != null &&
		 * bridge.getBuyerConnection().getConnectionURI().equals(con.getConnectionURI())
		 * ) { bridge.setBuyerConnection(null);
		 * makeTextMsg("The Buyer has left without accepting. Change the payment and validate it again."
		 * , bridge.getMerchantConnection());
		 * logger.debug("Buyer has closed the connection in status {}" +
		 * " in the Need {}", bridge.getStatus().name(), con.getNeedURI()); } else if
		 * (bridge.getBuyerConnection() == null) {
		 * makeTextMsg("The buyer declined the connection. Change the payment and validate it again."
		 * , bridge.getMerchantConnection());
		 * bridge.setStatus(PaymentStatus.BUYER_DENIED);
		 * logger.debug("Buyer has closed the connection in status {}" +
		 * " in the Need {}", bridge.getStatus().name(), con.getNeedURI()); }
		 * 
		 * } else { // Can only be the merchant if (bridge.getMerchantConnection() !=
		 * null &&
		 * bridge.getMerchantConnection().getConnectionURI().equals(con.getConnectionURI
		 * ())) { bridge.setMerchantConnection(null);
		 * logger.debug("Merchant has closed the connection in status {}" +
		 * " in the Need {}", bridge.getStatus().name(), con.getNeedURI()); } else { //
		 * Error ?! OR status == Nothing
		 * logger.debug("Merchant has closed the connection in status {}" +
		 * " in the Need {}", bridge.getStatus().name(), con.getNeedURI()); } }
		 * 
		 * closeNeed(bridge, con);
		 */

	}

}
