package won.payment.paypal.bot.scheduler;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

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

public class PaypalPaymentStatusCheckSchedule extends TimerTask {

	private PaypalPaymentService paypalService = new PaypalPaymentService();
	private EventListenerContext ctx;
	private Map<URI, PaymentBridge> openBridges;
	
	public PaypalPaymentStatusCheckSchedule(EventListenerContext ctx, Map<URI, PaymentBridge> openBridges) {
		this.ctx = ctx;
		this.openBridges = openBridges;
	}
	
	@Override
	public void run() {
				
		for (PaymentBridge bridge: openBridges.values()) {
			if (bridge.getStatus() == PaymentStatus.CREATED) {
				String payKey = lookForOpenPayment(bridge);
				if (payKey != null) {
					checkPayment(payKey, bridge);
				}
			}
		}
				
	}
	
	private void makeTextMsg(String msg, Connection con) {
		Model model = WonRdfUtils.MessageUtils.textMessage(msg);
		ctx.getEventBus().publish(new ConnectionMessageCommandEvent(con, model));
	}
	
	private String lookForOpenPayment(PaymentBridge bridge) {
		Resource baseRes = EventCrawler.getLatestPaymentPayKey(bridge.getBuyerConnection(), ctx);
		if (baseRes == null) {
			return null;
		}
		String payKey = baseRes.getProperty(WONPAY.HAS_PAYPAL_TX_KEY).getObject().asLiteral().getString();
		return payKey;
	}
	
	private void checkPayment(String payKey, PaymentBridge bridge) {
		try {
			PaypalPaymentStatus status = paypalService.validate(payKey);

			if (status == PaypalPaymentStatus.COMPLETED) {
				bridge.setStatus(PaymentStatus.COMPLETED);
				makeTextMsg("The payment is completed! You can now close the connection.", bridge.getBuyerConnection());
				makeTextMsg("The payment is completed! You can now close the connection.", bridge.getMerchantConnection());
			} else if (status == PaypalPaymentStatus.EXPIRED) {
				// TODO: Set payment as expired
			}

		} catch (Exception e) {
			// LOG IT
		}
	}

}
