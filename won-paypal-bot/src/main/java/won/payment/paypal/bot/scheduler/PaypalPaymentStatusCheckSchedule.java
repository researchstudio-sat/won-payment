package won.payment.paypal.bot.scheduler;

import java.util.Iterator;
import java.util.TimerTask;

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.payment.paypal.bot.impl.PaypalBotContextWrapper;
import won.payment.paypal.bot.model.PaymentBridge;
import won.payment.paypal.bot.model.PaymentStatus;
import won.payment.paypal.service.impl.PaypalPaymentService;
import won.payment.paypal.service.impl.PaypalPaymentStatus;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;

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
        Iterator<PaymentBridge> itr = ((PaypalBotContextWrapper) ctx.getBotContextWrapper()).getOpenBridges();
        while (itr.hasNext()) {
            PaymentBridge bridge = itr.next();
            if (bridge.getStatus() == PaymentStatus.PP_ACCEPTED) {
                String payKey = bridge.getPayKey();
                if (payKey != null) {
                    checkPayment(payKey, bridge);
                }
            }
        }
    }

    // TODO: think about moving this to a public method somewhere
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
     * @param payKey PayKey of the paypay payment.
     * @param bridge The payment bridge of the payment.
     */
    private void checkPayment(String payKey, PaymentBridge bridge) {
        try {
            PaypalPaymentService paypalService = PaypalBotContextWrapper.instance(ctx).getPaypalService();
            PaypalPaymentStatus status = paypalService.validate(payKey);
            if (status == PaypalPaymentStatus.COMPLETED) {
                bridge.setStatus(PaymentStatus.COMPLETED);
                logger.info("Payment completed with payKey {}", payKey);
                makeTextMsg("The payment was completed! You can now close this connection.", bridge.getConnection());
            } else if (status == PaypalPaymentStatus.EXPIRED) {
                logger.info("Paypal Payment expired with payKey={}", payKey);
                makeTextMsg("The payment link expired! Type 'accept' to generate a new one.", bridge.getConnection());
                bridge.setStatus(PaymentStatus.EXPIRED);
            }
            PaypalBotContextWrapper.instance(ctx).putOpenBridge(bridge.getConnection().getAtomURI(), bridge);
        } catch (Exception e) {
            logger.warn("Paypal payment check failed.", e);
        }
    }
}
