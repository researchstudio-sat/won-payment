package won.payment.paypal.bot.action;

import java.net.URI;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.bot.framework.eventbot.event.ConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.MessageEvent;
import won.bot.framework.eventbot.event.impl.command.close.CloseCommandEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.payment.paypal.bot.model.PaymentBridge;
import won.payment.paypal.bot.model.PaymentStatus;
import won.payment.paypal.bot.util.EventCrawler;
import won.payment.paypal.bot.util.WonPaymentRdfUtils;
import won.payment.paypal.service.impl.PaypalPaymentService;
import won.payment.paypal.service.impl.PaypalPaymentStatus;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONPAY;

/**
 * Actionhandler for received messages from the buyer.
 * @author schokobaer
 *
 */
public class BuyerMessageReceiverAction extends BaseEventBotAction {

	private PaypalPaymentService paypalService;
	private Map<URI, PaymentBridge> openPayments;

	public BuyerMessageReceiverAction(EventListenerContext eventListenerContext, Map<URI, PaymentBridge> openPayments, PaypalPaymentService paypalService) {
		super(eventListenerContext);
		this.openPayments = openPayments;
		this.paypalService = paypalService;
	}

	@Override
	protected void doRun(Event event, EventListener executingListener) throws Exception {

		ConnectionSpecificEvent messageEvent = (ConnectionSpecificEvent) event;
		if (messageEvent instanceof MessageEvent) {
			EventListenerContext ctx = getEventListenerContext();
			EventBus bus = ctx.getEventBus();

			Connection con = ((BaseNeedAndConnectionSpecificEvent) messageEvent).getCon();
			WonMessage msg = ((MessageEvent) messageEvent).getWonMessage();

			handleMessage(msg, con, bus);
		}

	}

	private void makeTextMsg(String msg, Connection con) {
		if (con == null) {
			return;
		}
		Model model = WonRdfUtils.MessageUtils.textMessage(msg);
		getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(con, model));
	}

	/**
	 * Differs between payment status and checks the command sent by the buyer.
	 * Then forwards it to the corresponding method.
	 * 
	 * @param wonMsg Received WonMessage.
	 * @param con The connection where the WonMessage was received.
	 * @param bus The bus for publishing new events.
	 */
	private void handleMessage(WonMessage wonMsg, Connection con, EventBus bus) {

		PaymentStatus status = openPayments.get(con.getNeedURI()).getStatus();
		String msg = WonRdfUtils.MessageUtils.getTextMessage(wonMsg).trim();
		msg = msg != null ? msg : "";

		if (status == PaymentStatus.GOALSATISFIED) {
			if (msg.equals("accept")) {
				generate(con, bus);
			} else if (msg.equals("deny")) {
				deny(con, bus);
			} else {
				String resp = "Type 'accept' for generating the payment and receiving the payment link. \n"
						+ "Type 'deny' to deny the payment and close the connection.";
				makeTextMsg(resp, con);
			}
		} else if (status == PaymentStatus.PUBLISHED) {
			makeTextMsg("The payment will be generated. Please be patient.", con);
		} else if (status == PaymentStatus.DENIED) {
			// Can not exists, because connection got closed before
		} else if (status == PaymentStatus.GENERATED) {
			if (msg.equals("check")) {
				check(con, bus);
			} else {
				makeTextMsg("Type 'check' after you completed the payment to validate it.", con);
			}
		} else if (status == PaymentStatus.COMPLETED) {
			makeTextMsg("The payment is completed. You can now close the connection.", con);
		} else {
			makeTextMsg("The payment has an unexpected status: " + status.name() + ". Contact the merchant", con);
			logger.warn("Unexpected Payment Status={}", status.name());
		}

	}

	/**
	 * Crawls for the last accepted payment in the merchants agreement protocol.
	 * Then creates a paypal payment and returns the execution link. The status will be
	 * set to ACCEPTED. After the payment was generated it will be set to GENERATED.
	 * 
	 * @param con The Connection where the accept was received.
	 * @param bus The event bus for publishing new events.
	 */
	private void generate(Connection con, EventBus bus) {
		openPayments.get(con.getNeedURI()).setStatus(PaymentStatus.PUBLISHED);
		Connection merchantCon = openPayments.get(con.getNeedURI()).getMerchantConnection();
		makeTextMsg("The counterpart need has accepted the payment request! Generating the payment ...", merchantCon);
		makeTextMsg("Generating the payment ...", con);

		Resource baseRes = EventCrawler.getLastPaymentEvent(merchantCon, getEventListenerContext());
		Double amount = baseRes.getProperty(WONPAY.HAS_AMOUNT).getObject().asLiteral().getDouble();
		String currency = baseRes.getProperty(WONPAY.HAS_CURRENCY).getObject().asLiteral().getString();
		String receiver = baseRes.getProperty(WONPAY.HAS_RECEIVER).getObject().asLiteral().getString();
		String feePayer = baseRes.getProperty(WONPAY.HAS_FEE_PAYER).getObject().equals(WONPAY.FEE_PAYER_SENDER)
				? "SENDER"
				: "RECEIVER";
		// TODO: InvoiceNumber, InvoiceDetails, Tax

		try {
			String payKey = paypalService.create(receiver, amount, currency, feePayer);
			String url = paypalService.getPaymentUrl(payKey);

			Model buyerResponse = WonPaymentRdfUtils.generatePaypalKeyMessage(null, payKey,
					"Click on the link to execute the payment: " + url);
			Model merchantResponse = WonPaymentRdfUtils.generatePaypalKeyMessage(null, payKey,
					"Payment successfuly created. PayKey: " + payKey
							+ " \n Waiting for counterpart to complete payment ...");

			openPayments.get(con.getNeedURI()).setStatus(PaymentStatus.GENERATED);

			bus.publish(new ConnectionMessageCommandEvent(con, buyerResponse));
			bus.publish(new ConnectionMessageCommandEvent(merchantCon, merchantResponse));
			logger.info("Paypal Payment generated with payKey={}", payKey);
		} catch (Exception e) {
			makeTextMsg("Something went wrong. Try again or contact your merchant: " + e.getMessage(), con);
			makeTextMsg("Something went wrong: " + e.getMessage(), merchantCon);
			logger.warn("Paypal payment could not be generated.", e);
		}
	}

	/**
	 * Sets the payment status to DENIED and closes the buyers connection.
	 * 
	 * @param con The Connection where the deny was received.
	 * @param bus The event bus for publishing new events.
	 */
	private void deny(Connection con, EventBus bus) {
		openPayments.get(con.getNeedURI()).setStatus(PaymentStatus.DENIED);
		Connection merchantCon = openPayments.get(con.getNeedURI()).getMerchantConnection();
		makeTextMsg("The counterpart need has denied the payment request!", merchantCon);
		openPayments.get(con.getNeedURI()).setBuyerConnection(null);
		bus.publish(new CloseCommandEvent(con));
	}

	/**
	 * Crawls the buyers event for the last payKey and validates it with the Paypal-API.
	 * On success the status will be set to COMPLETED.
	 * 
	 * @param con The Connection where the check was received.
	 * @param bus The event bus for publishing new events.
	 */
	private void check(Connection con, EventBus bus) {
		Connection merchantCon = openPayments.get(con.getNeedURI()).getMerchantConnection();
		Resource baseRes = EventCrawler.getLatestPaymentPayKey(con, getEventListenerContext());

		if (baseRes == null) {
			makeTextMsg("You need to accept the payment first", con);
			return;
		}

		String payKey = baseRes.getProperty(WONPAY.HAS_PAYPAL_TX_KEY).getObject().asLiteral().getString();

		try {
			makeTextMsg("Checking the payment status ...", con);
			PaypalPaymentStatus status = paypalService.validate(payKey);

			if (status == PaypalPaymentStatus.COMPLETED) {
				openPayments.get(con.getNeedURI()).setStatus(PaymentStatus.COMPLETED);
				makeTextMsg("The payment is completed! You can now close the connection.", con);
				makeTextMsg("The payment is completed! You can now close the connection.", merchantCon);
				logger.info("Paypal payment completed with payKey={}", payKey);
			} else if (status == PaypalPaymentStatus.EXPIRED) {
				makeTextMsg("The payment is expired! Type 'accept' to generate a new one.", con);
				logger.info("Paypal Payment expired with payKey={}", payKey);
				openPayments.get(con.getNeedURI()).setStatus(PaymentStatus.GOALSATISFIED);
			} else {
				makeTextMsg("The payment is not completed yet. Type 'check' after you completed the payment.", con);
			}

		} catch (Exception e) {
			makeTextMsg("Something went wrong: " + e.getMessage(), con);
			logger.warn("Paypal payment check failed.", e);
		}
	}

}
