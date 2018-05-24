package won.payment.paypal.bot.util;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import won.bot.framework.eventbot.EventListenerContext;
import won.payment.paypal.bot.model.PaymentBridge;
import won.payment.paypal.service.impl.PaypalPaymentService;
import won.payment.paypal.service.impl.PaypalPaymentStatus;

public class PaymentStatusCheckSchedule implements Runnable {

	private PaypalPaymentService paypalService;
	private EventListenerContext ctx;
	private Map<URI, PaymentBridge> openBridges;
	
	public PaymentStatusCheckSchedule(EventListenerContext ctx, Map<URI, PaymentBridge> openBridges) {
		this.ctx = ctx;
		this.openBridges = openBridges;
	}
	
	@Override
	public void run() {
		
		List<URI> completedPayments = new LinkedList<>(); 
		
		for (PaymentBridge bridge: openBridges.values()) {
			if (bridge.getMerchantConnection() != null &&
					bridge.getBuyerConnection() != null) {
				String payKey = lookForOpenPayment(bridge);
				if (payKey != null) {
					if (checkPayment(payKey)) {
						completedPayments.add(bridge.getMerchantConnection().getNeedURI());
					}
				}
			}
		}
		
		// Remove completed payments
		for (URI uri : completedPayments) {
			openBridges.remove(uri);
		}
		
	}
	
	private String lookForOpenPayment(PaymentBridge bridge) {
		
		return null;
	}
	
	private boolean checkPayment(String payKey) {
		try {
			PaypalPaymentStatus status = paypalService.validate(payKey);
			if (status == PaypalPaymentStatus.COMPLETED) {
				return true;
			}
		} catch (Exception e) {

		}
		return false;
	}

}
