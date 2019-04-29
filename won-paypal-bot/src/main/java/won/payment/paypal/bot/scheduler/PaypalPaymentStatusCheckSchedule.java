package won.payment.paypal.bot.scheduler;

import java.util.Map;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.bot.framework.eventbot.EventListenerContext;
import won.payment.paypal.bot.impl.PaypalBot;
import won.payment.paypal.bot.impl.PaypalBotContextWrapper;
import won.payment.paypal.bot.model.PaymentContext;
import won.payment.paypal.bot.model.PaymentStatus;
import won.payment.paypal.service.impl.PaypalPaymentService;
import won.payment.paypal.service.impl.PaypalPaymentStatus;

/**
 * Scheduler which crawls for open Payments which are in the state GENERATED and
 * checks for the completion.
 * 
 * @author schokobaer
 */
public class PaypalPaymentStatusCheckSchedule extends TimerTask {
    private static final Logger logger = LoggerFactory.getLogger(PaypalPaymentStatusCheckSchedule.class);
    private EventListenerContext ctx;

    public PaypalPaymentStatusCheckSchedule(EventListenerContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void run() {
        Map<String, PaymentContext> payContexts = ((PaypalBotContextWrapper) ctx.getBotContextWrapper())
                .getPaymentContexts();
        payContexts.values().stream().forEach(payCtx -> {
            if (payCtx.getStatus() == PaymentStatus.PP_ACCEPTED) {
                checkPayment(payCtx);
            }
        });
    }

    /**
     * Makes the Paypal-API call to check the payment status.
     * 
     * @param payCtx The payment context of the payment.
     */
    private void checkPayment(PaymentContext payCtx) {
        if (payCtx == null || payCtx.getPayKey() == null) {
            return;
        }
        String payKey = payCtx.getPayKey();
        PaypalBotContextWrapper botCtx = (PaypalBotContextWrapper) ctx.getBotContextWrapper();
        try {
            PaypalPaymentService paypalService = botCtx.getPaypalService();
            PaypalPaymentStatus status = paypalService.validate(payKey);
            if (status == PaypalPaymentStatus.COMPLETED) {
                payCtx.setStatus(PaymentStatus.COMPLETED);
                logger.info("Payment completed with payKey {}", payKey);
                ctx.getEventBus().publish(PaypalBot.makeProcessingMessage(
                        "The payment was completed! You can now close this connection.", payCtx.getConnection()));
            } else if (status == PaypalPaymentStatus.EXPIRED) {
                logger.info("Paypal Payment expired with payKey={}", payKey);
                ctx.getEventBus().publish(PaypalBot.makeProcessingMessage(
                        "The payment link expired! Type 'accept' to generate a new one.", payCtx.getConnection()));
                payCtx.setStatus(PaymentStatus.EXPIRED);
            }
            botCtx.setPaymentContext(payCtx.getConnection().getAtomURI(), payCtx);
        } catch (Exception e) {
            logger.warn("Paypal payment check failed.", e);
        }
    }
}
