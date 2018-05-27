package won.payment.paypal.bot.action;

import static won.payment.paypal.bot.util.WonPaymentRdfUtils.PAY_AMOUNT;
import static won.payment.paypal.bot.util.WonPaymentRdfUtils.PAY_COUNTERPART;
import static won.payment.paypal.bot.util.WonPaymentRdfUtils.PAY_CURRENCY;
import static won.payment.paypal.bot.util.WonPaymentRdfUtils.PAY_FEE_PAYER;
import static won.payment.paypal.bot.util.WonPaymentRdfUtils.PAY_INVOICE_DETAILS;
import static won.payment.paypal.bot.util.WonPaymentRdfUtils.PAY_INVOICE_NUMBER;
import static won.payment.paypal.bot.util.WonPaymentRdfUtils.PAY_RECEIVER;
import static won.payment.paypal.bot.util.WonPaymentRdfUtils.PAY_SECRET;
import static won.payment.paypal.bot.util.WonPaymentRdfUtils.PAY_TAX;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
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
import won.bot.framework.eventbot.event.impl.command.connect.ConnectCommandEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.payment.paypal.bot.model.PaymentBridge;
import won.payment.paypal.bot.model.PaymentStatus;
import won.payment.paypal.bot.util.EventCrawler;
import won.payment.paypal.bot.util.WonPaymentRdfUtils;
import won.payment.paypal.bot.validator.PaymentModelValidator;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONPAY;

/**
 * Actionhandler for received messages from the merchant.
 * 
 * @author schokobaer
 *
 */
public class MerchantMessageReceiverAction extends BaseEventBotAction {

	private Map<URI, PaymentBridge> openPayments;

	public MerchantMessageReceiverAction(EventListenerContext eventListenerContext,
			Map<URI, PaymentBridge> openPayments) {
		super(eventListenerContext);
		this.openPayments = openPayments;
	}

	@Override
	protected void doRun(Event event, EventListener executingListener) throws Exception {
		ConnectionSpecificEvent messageEvent = (ConnectionSpecificEvent) event;
		EventListenerContext ctx = getEventListenerContext();
		EventBus bus = ctx.getEventBus();

		if (messageEvent instanceof MessageEvent) {
			Connection con = ((BaseNeedAndConnectionSpecificEvent) messageEvent).getCon();
			WonMessage msg = ((MessageEvent) messageEvent).getWonMessage();

			if (WonPaymentRdfUtils.isAcceptMessage(msg)) {
				handleAccept(msg, con, bus);
			} else {
				handleMessage(msg, con, bus);
			}
		}

	}

	private void makeTextMsg(String msg, Connection con) {
		Model model = WonRdfUtils.MessageUtils.textMessage(msg);
		getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(con, model));
	}

	private void printHelp(Connection con) {
		makeTextMsg("I need the payment data from you. "
				+ "After you gave me all the payment data, type 'payment validate'. "
				+ "I will sumerize the payment data and propose it to you. "
				+ "After you accept that bundle I will create the payment and open a connection"
				+ " to the counterpart and send him the link for the payment execution. "
				+ "After the payment is complete I will inform you with a message in this connection. "
				+ "Here are the payment details and the formart you should send it. The '*' means it "
				+ "is optional but not necesery", con);
		makeTextMsg("pay_amount: <amount> (e.g. 10)", con);
		makeTextMsg("pay_currency: <currency> (e.g. EUR)", con);
		makeTextMsg("pay_receiver: <receiver_email> (e.g. test@won.org)", con);
		makeTextMsg("won_counterpart: <counterpart URI of the other need> "
				+ "(e.g. https://matchat.org/won/resource/need/1234abcd)", con);
		makeTextMsg("won_secret: <a secret you make with the counterpart> (e.g. oursecretword)", con);

	}

	/**
	 * Differs between payment status and checks the command sent by the merchant.
	 * Then forwards it to the corresponding method.
	 * 
	 * @param wonMsg
	 *            Received WonMessage.
	 * @param con
	 *            The connection where the WonMessage was received.
	 * @param bus
	 *            The bus for publishing new events.
	 */
	private void handleMessage(WonMessage wonMsg, Connection con, EventBus bus) {

		PaymentStatus status = openPayments.get(con.getNeedURI()).getStatus();
		String msg = WonRdfUtils.MessageUtils.getTextMessage(wonMsg).trim();

		if (status == PaymentStatus.UNPUBLISHED || status == PaymentStatus.DENIED) {
			if ("help".equals(msg)) {
				printHelp(con);
			} else if ("payment validate".equals(msg)) {
				validate(con, bus);
			} else if (msg.startsWith("pay_")) {
				paymentdataSent(msg, con, bus);
			} else if (msg.startsWith("payment sample ")) {
				String needUri = msg.substring(15);
				sample(needUri, bus, con);
			} else {
				makeTextMsg("If do not know how to hanlde me type 'help'", con);
			}
		} else if (status == PaymentStatus.PUBLISHED) {
			makeTextMsg("You have to wait for the payment to be accepted or denied.", con);
		} else if (status == PaymentStatus.ACCEPTED) {
			makeTextMsg("The payment got accepted. It will be created now.", con);
		} else if (status == PaymentStatus.DENIED) {
			// Should not exists ?!?
		} else if (status == PaymentStatus.GENERATED) {
			makeTextMsg("The payment was created. You have to wait for the buyer to complete it.", con);
		} else if (status == PaymentStatus.COMPLETED) {
			makeTextMsg("The payment was successfully completed. You can now close the connection.", con);
		} else {
			makeTextMsg("The payment has an unexpected status: " + status.name() + ". Try again", con);
			logger.warn("Unexpected Payment Status={}", status.name());
		}

	}

	/**
	 * Is invoked when the received WonMessage was a accept message. If the
	 * WonMessage was a paypal payment it will be published.
	 * 
	 * @param msg
	 *            Received WonMessage.
	 * @param con
	 *            The connection where the WonMessage was received.
	 * @param bus
	 *            The bus for publishing new events.
	 */
	private void handleAccept(WonMessage msg, Connection con, EventBus bus) {

		PaymentStatus status = openPayments.get(con.getNeedURI()).getStatus();

		if (status == PaymentStatus.UNPUBLISHED || status == PaymentStatus.DENIED) {
			// TODO: Check if the acceptMessage was a payment
			Resource payment = EventCrawler.getLastPaymentEvent(con, getEventListenerContext());
			if (payment != null) {
				publish(payment, con, bus);
			}
		} else {
			makeTextMsg("The payment is already published to the buyer. This has no effects", con);
		}
	}

	/**
	 * Invoked when a message starts with 'pay_'. Splits the sent key-value-pairs
	 * and validates them. If all are valid the message will be proposed to the
	 * merchant.
	 * 
	 * @param msg
	 *            Received WonMessage.
	 * @param con
	 *            The connection where the WonMessage was received.
	 * @param bus
	 *            The bus for publishing new events.
	 */
	private void paymentdataSent(String msg, Connection con, EventBus bus) {

		String[] parts = msg.split(";");
		boolean valid = true;

		for (String field : parts) {
			int indexVal = field.indexOf(":");
			if (indexVal > 0) {
				String key = field.substring(0, indexVal).trim();
				String value = field.substring(indexVal + 1).trim();
				try {
					validateField(key, value, con);
				} catch (Exception e) {
					valid = false;
					makeTextMsg(e.getMessage(), con);
				}
			}
		}

		if (valid) {
			EventCrawler.propose(getEventListenerContext(), bus, con, false, true, 1);
		}
	}

	/**
	 * Validates a simple key-value-pair. If it is not valid an exception is thrown
	 * with the cause.
	 * 
	 * @param key
	 *            Key of the key-value-pair.
	 * @param value
	 *            Value of the key-value-pair.
	 * @throws Exception
	 *             Is an illegalArgument exception.
	 */
	private void validateField(String key, String value, Connection con) throws Exception {
		if (key.equals(PAY_AMOUNT)) {
			Double amount = Double.parseDouble(value);
			if (amount <= 0) {
				throw new Exception("Negative amount is not allowed");
			}
		} else if (key.equals(PAY_CURRENCY)) {
			// if (!value.equals("€") && !value.toUpperCase().equals("EUR")) {
			// throw new Exception("Only EUR supported at the moment");
			// }
		} else if (key.equals(PAY_COUNTERPART)) {
			if (con.getRemoteNeedURI().toString().equals(value)) {
				throw new Exception("You have to put an other needs URI as counterpart");
			}
		} else if (key.equals(PAY_FEE_PAYER)) {
			if (!value.toUpperCase().equals("SENDER") && !value.toUpperCase().equals("RECEIVER")) {
				throw new Exception("Fee payer must be 'SENDER' or 'RECEIVER'");
			}
		} else if (key.equals(PAY_INVOICE_DETAILS)) {

		} else if (key.equals(PAY_INVOICE_NUMBER)) {

		} else if (key.equals(PAY_RECEIVER)) {

		} else if (key.equals(PAY_SECRET)) {

		} else if (key.equals(PAY_TAX)) {

		} else {
			logger.debug("Unvalid payment argument by merchant: {}", key);
			throw new Exception("Not a valid payment argument");
		}
	}

	/**
	 * Validates the agreement protocol. If all needed data is available a paypal
	 * payment message is generated and proposed to the merchant.
	 * 
	 * @param con
	 *            The connection where the WonMessage was received.
	 * @param bus
	 *            The bus for publishing new events.
	 */
	private void validate(Connection con, EventBus bus) {
		Map<String, String> payDetails = EventCrawler.crawlPaymentDetails(con, getEventListenerContext());
		Model model = WonPaymentRdfUtils.generatePaymentModel(payDetails);
		PaymentModelValidator paymentValidator = new PaymentModelValidator();
		boolean isValid = false;

		try {
			paymentValidator.validate(model, con);
			isValid = true;
		} catch (Exception e) {
			makeTextMsg(e.getMessage(), con);
			logger.debug("Validation error", e);
		}

		if (isValid) {
			bus.publish(new ConnectionMessageCommandEvent(con, model));
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
			}
			EventCrawler.propose(getEventListenerContext(), bus, con, true, false, 1);
		}
	}

	/**
	 * When the merchant has accepted the proposed paypal payment this method will
	 * be invoked. It opens a new connection to the buyer and makes the request for
	 * the given payment.
	 * 
	 * @param payment
	 *            The payment which was crawled from the agreement protocol.
	 * @param con
	 *            The connection where the WonMessage was received.
	 * @param bus
	 *            The bus for publishing new events.
	 */
	private void publish(Resource payment, Connection con, EventBus bus) {
		String needUri = payment.getProperty(WONPAY.HAS_NEED_COUNTERPART).getObject().asLiteral().getString();
		String msg = "Payment Request: \n" + "Amount: "
				+ payment.getProperty(WONPAY.HAS_AMOUNT).getObject().asLiteral().getString() + " "
				+ payment.getProperty(WONPAY.HAS_CURRENCY).getObject().asLiteral().getString() + " \n" + "Secret: "
				+ payment.getProperty(WONPAY.HAS_SECRET).getObject().asLiteral().getString() + "  \n\n"
				+ "Type 'accept' or 'denie'.";
		try {
			openPayments.get(con.getNeedURI()).setStatus(PaymentStatus.PUBLISHED);
			bus.publish(new ConnectCommandEvent(con.getNeedURI(), new URI(needUri), msg));
			makeTextMsg("Proposed payment. Waiting for counterpart need to accept ...", con);
		} catch (URISyntaxException e) {
			logger.warn("Could not create URI", e);
		}
	}

	/**
	 * Creates a sample Payment Message and proposes it.
	 * 
	 * @param ctx
	 *            The Context of the bot.
	 * @param con
	 *            The connection where the WonMessage was received.
	 * @param bus
	 *            The bus for publishing new events.
	 */
	private void sample(String needUri, EventBus bus, Connection con) {
		Map<String, String> payDetails = new HashMap<>();
		payDetails.put(WonPaymentRdfUtils.PAY_AMOUNT, "12");
		payDetails.put(WonPaymentRdfUtils.PAY_CURRENCY, "EUR");
		payDetails.put(WonPaymentRdfUtils.PAY_RECEIVER, "test@won.org");
		payDetails.put(WonPaymentRdfUtils.PAY_SECRET, "samplesecret123");
		payDetails.put(WonPaymentRdfUtils.PAY_COUNTERPART, needUri);

		Model model = WonPaymentRdfUtils.generatePaymentModel(payDetails);
		bus.publish(new ConnectionMessageCommandEvent(con, model));
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
		EventCrawler.propose(getEventListenerContext(), bus, con, true, false, 1);
	}

}
