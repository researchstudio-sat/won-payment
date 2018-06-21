package won.payment.paypal.bot.scheduler;

import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.payment.paypal.bot.impl.PaypalBotContextWrapper;
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

	public PaypalPaymentStatusCheckSchedule(EventListenerContext ctx, PaypalPaymentService paypalService) {
		this.ctx = ctx;
		this.paypalService = paypalService;
	}

	@Override
	public void run() {

		Iterator<PaymentBridge> itr = ((PaypalBotContextWrapper)ctx.getBotContextWrapper()).getOpenBridges();
		while (itr.hasNext()) {
			PaymentBridge bridge = itr.next();
			if (bridge.getStatus() == PaymentStatus.GENERATED) {
				String payKey = bridge.getPayKey();
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
		Model model = WonRdfUtils.MessageUtils.processingMessage(msg);
		ctx.getEventBus().publish(new ConnectionMessageCommandEvent(con, model));
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
				bridge.setStatus(PaymentStatus.EXPIRED);
			}
			
			PaypalBotContextWrapper.instance(ctx).putOpenBridge(bridge.getMerchantConnection().getNeedURI(), bridge);

		} catch (Exception e) {
			logger.warn("Paypal payment check failed.", e);
		}
	}

}
