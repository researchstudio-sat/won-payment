package won.payment.paypal.bot.impl;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

import won.bot.framework.bot.context.BotContext;
import won.bot.framework.bot.context.FactoryBotContextWrapper;
import won.payment.paypal.bot.model.PaymentBridge;
import won.payment.paypal.service.impl.PaypalPaymentService;

/**
 * Simple BotContextWrapper, which manages the open payment bridges.
 * 
 * @author schokobaer
 */
public class PaypalBotContextWrapper extends FactoryBotContextWrapper {
    private final String OPEN_PAYMENT_BRIDGES = getBotName() + ":openpaymentbridges";
    private PaypalPaymentService paypalService;
    private Long schedulingInterval;

    public PaypalBotContextWrapper(BotContext botContext, String botName) {
        super(botContext, botName);
    }

    public void putOpenBridge(URI atomUri, PaymentBridge bridge) {
        this.getBotContext().saveToObjectMap(OPEN_PAYMENT_BRIDGES, atomUri.toString(), bridge);
    }

    public PaymentBridge getOpenBridge(URI atomUri) {
        return (PaymentBridge) this.getBotContext().loadFromObjectMap(OPEN_PAYMENT_BRIDGES, atomUri.toString());
    }

    public Map<String, PaymentBridge> getOpenBridges() {
        // Conversion from https://stackoverflow.com/a/34497492
        return this.getBotContext().loadObjectMap(OPEN_PAYMENT_BRIDGES).entrySet().stream()
                .filter(e -> e.getValue() instanceof PaymentBridge)
                .collect(Collectors.toMap(e -> e.getKey(), e -> (PaymentBridge) e.getValue()));
    }

    public void removeOpenBridge(URI atomUri) {
        this.getBotContext().removeFromObjectMap(OPEN_PAYMENT_BRIDGES, atomUri.toString());
    }

    public void setPaypalService(PaypalPaymentService paypalService) {
        this.paypalService = paypalService;
    }

    public PaypalPaymentService getPaypalService() {
        return paypalService;
    }

    public Long getSchedulingInterval() {
        return schedulingInterval;
    }

    public void setSchedulingInterval(Long schedulingInterval) {
        this.schedulingInterval = schedulingInterval;
    }
}
