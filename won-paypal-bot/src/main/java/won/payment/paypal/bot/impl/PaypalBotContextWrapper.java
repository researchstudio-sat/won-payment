package won.payment.paypal.bot.impl;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

import won.bot.framework.bot.context.BotContext;
import won.bot.framework.bot.context.FactoryBotContextWrapper;
import won.payment.paypal.bot.model.PaymentContext;
import won.payment.paypal.service.impl.PaypalPaymentService;

/**
 * Simple BotContextWrapper, which manages the open payment contexts.
 * 
 * @author schokobaer
 */
public class PaypalBotContextWrapper extends FactoryBotContextWrapper {
    private final String ACTIVE_PAYMENT_CONTEXTS = getBotName() + ":paymentcontexts";
    private PaypalPaymentService paypalService;
    private Long schedulingInterval;

    public PaypalBotContextWrapper(BotContext botContext, String botName) {
        super(botContext, botName);
    }

    public void setPaymentContext(URI atomUri, PaymentContext payCtx) {
        this.getBotContext().saveToObjectMap(ACTIVE_PAYMENT_CONTEXTS, atomUri.toString(), payCtx);
    }

    public PaymentContext getPaymentContext(URI atomUri) {
        return (PaymentContext) this.getBotContext().loadFromObjectMap(ACTIVE_PAYMENT_CONTEXTS, atomUri.toString());
    }

    public Map<String, PaymentContext> getPaymentContexts() {
        // Conversion from https://stackoverflow.com/a/34497492
        return this.getBotContext().loadObjectMap(ACTIVE_PAYMENT_CONTEXTS).entrySet().stream()
                .filter(e -> e.getValue() instanceof PaymentContext)
                .collect(Collectors.toMap(e -> e.getKey(), e -> (PaymentContext) e.getValue()));
    }

    public void removePaymentContext(URI atomUri) {
        this.getBotContext().removeFromObjectMap(ACTIVE_PAYMENT_CONTEXTS, atomUri.toString());
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
