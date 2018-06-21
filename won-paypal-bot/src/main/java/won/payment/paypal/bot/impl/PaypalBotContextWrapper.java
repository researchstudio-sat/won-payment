package won.payment.paypal.bot.impl;

import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import won.bot.framework.bot.context.BotContext;
import won.bot.framework.bot.context.FactoryBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.payment.paypal.bot.model.PaymentBridge;
import won.payment.paypal.service.impl.PaypalPaymentService;
import won.protocol.model.Connection;

/**
 * Simple BotContextWrapper, which manages the open payment bridges.
 * 
 * @author schokobaer
 *
 */
public class PaypalBotContextWrapper extends FactoryBotContextWrapper {
	
	private static final String OPEN_PAYMENT_BRIDGES = ":openpaymentbridges";
	private PaypalPaymentService paypalService;
	
	public PaypalBotContextWrapper(BotContext botContext, String botName) {
		super(botContext, botName);
	}
	
	public void putOpenBridge(URI needUri, PaymentBridge bridge) {
		this.getBotContext().saveToObjectMap(OPEN_PAYMENT_BRIDGES, needUri.toString(), bridge);
	}
	
	public PaymentBridge getOpenBridge(URI needUri) {
		return (PaymentBridge) this.getBotContext().loadFromObjectMap(OPEN_PAYMENT_BRIDGES, needUri.toString());
	}
	
	public Iterator<PaymentBridge> getOpenBridges() {
		Map<String, Object> map = this.getBotContext().loadObjectMap(OPEN_PAYMENT_BRIDGES);
		Collection<PaymentBridge> col = new LinkedList<>();
		for (Object obj : map.values()) {
			col.add((PaymentBridge)obj);
		}
		return col.iterator();
	}
	
	public void removeOpenBridge(URI needUri) {
		this.getBotContext().removeFromObjectMap(OPEN_PAYMENT_BRIDGES, needUri.toString());
	}
	
	public static PaypalBotContextWrapper instance(EventListenerContext ctx) {
		return ((PaypalBotContextWrapper)ctx.getBotContextWrapper());
	}
	
	public static PaymentBridge paymentBridge(EventListenerContext ctx, Connection con) {
		return instance(ctx).getOpenBridge(con.getNeedURI());
	}

	public void setPaypalService(PaypalPaymentService paypalService) {
		this.paypalService = paypalService;		
	}

	public PaypalPaymentService getPaypalService() {
		return paypalService;
	}
	
	

}
