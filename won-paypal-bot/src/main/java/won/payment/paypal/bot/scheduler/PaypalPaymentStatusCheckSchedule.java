package won.payment.paypal.bot.scheduler;

import java.net.URI;
import java.util.Map;
import java.util.TimerTask;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.payment.paypal.bot.model.PaymentBridge;
import won.payment.paypal.bot.model.PaymentStatus;
import won.payment.paypal.bot.util.EventCrawler;
import won.payment.paypal.service.impl.PaypalPaymentService;
import won.payment.paypal.service.impl.PaypalPaymentStatus;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONPAY;

/**
 * Scheduler which crawls for open Payments which are in the state GENERATED and
 * checks for the completion.
 * 
 * @author schokobaer
 *
 */
public class PaypalPaymentStatusCheckSchedule extends TimerTask {

	private static final Logger logger = LoggerFactory.getLogger(PaypalPaymentStatusCheckSchedule.class);

	private PaypalPaymentService paypalService;
	private EventListenerContext ctx;
	private Map<URI, PaymentBridge> openBridges;

	public PaypalPaymentStatusCheckSchedule(EventListenerContext ctx, Map<URI, PaymentBridge> openBridges, PaypalPaymentService paypalService) {
		this.ctx = ctx;
		this.openBridges = openBridges;
		this.paypalService = paypalService;
	}

	@Override
	public void run() {

		for (PaymentBridge bridge : openBridges.values()) {
			if (bridge.getStatus() == PaymentStatus.GENERATED) {
				String payKey = lookForOpenPayment(bridge);
				if (payKey != null) {
					checkPayment(payKey, bridge);
				}
			}
		}

	}

	private void makeTextMsg(String msg, Connection con) {
		if (con == null) {
			return;
		}
		Model model = WonRdfUtils.MessageUtils.textMessage(msg);
		ctx.getEventBus().publish(new ConnectionMessageCommandEvent(con, model));
	}

	/**
	 * Crawls for the payKey of the generated payment.
	 * 
	 * @param bridge
	 *            The payment bridge where the payment key should be searched.
	 * @return Paykey or null.
	 */
	private String lookForOpenPayment(PaymentBridge bridge) {
		Resource baseRes = EventCrawler.getLatestPaymentPayKey(bridge.getBuyerConnection(), ctx);
		if (baseRes == null) {
			return null;
		}
		String payKey = baseRes.getProperty(WONPAY.HAS_PAYPAL_TX_KEY).getObject().asLiteral().getString();
		return payKey;
	}

	/**
	 * Makes the Paypal-API call to check the payment status.
	 * 
	 * @param payKey
	 *            PayKey of the paypay payment.
	 * @param bridge
	 *            The payment bridge of the payment.
	 */
	private void checkPayment(String payKey, PaymentBridge bridge) {
		try {
			PaypalPaymentStatus status = paypalService.validate(payKey);

			if (status == PaypalPaymentStatus.COMPLETED) {
				bridge.setStatus(PaymentStatus.COMPLETED);
				makeTextMsg("The payment is completed! You can now close the connection.", bridge.getBuyerConnection());
				makeTextMsg("The payment is completed! You can now close the connection.",
						bridge.getMerchantConnection());
			} else if (status == PaypalPaymentStatus.EXPIRED) {
				makeTextMsg("The payment is expired! Type 'accept' to generate a new one.",
						bridge.getBuyerConnection());
				logger.info("Paypal Payment expired with payKey={}", payKey);
				bridge.setStatus(PaymentStatus.PUBLISHED);
			}

		} catch (Exception e) {
			logger.warn("Paypal payment check failed.", e);
		}
	}

}
