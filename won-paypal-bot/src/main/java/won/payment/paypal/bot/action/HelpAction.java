package won.payment.paypal.bot.action;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.listener.EventListener;
import won.payment.paypal.bot.event.SimpleMessageReceivedEvent;
import won.payment.paypal.bot.impl.PaypalBot;
import won.payment.paypal.bot.impl.PaypalBotContextWrapper;
import won.payment.paypal.bot.model.PaymentBridge;
import won.payment.paypal.bot.model.PaymentStatus;
import won.protocol.model.Connection;

public class HelpAction extends BaseEventBotAction {
    public HelpAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventListenerContext ctx = getEventListenerContext();

        if (event instanceof SimpleMessageReceivedEvent) {
            SimpleMessageReceivedEvent simpleMsgEvent = (SimpleMessageReceivedEvent) event;
            Connection con = simpleMsgEvent.getCon();

            PaymentBridge bridge = PaypalBotContextWrapper.paymentBridge(ctx, con);
            if (bridge.getConnection() != null
                    && con.getConnectionURI().equals(bridge.getConnection().getConnectionURI())) {
                checkPaymentStatus(bridge);
            }
        }
    }

    private void checkPaymentStatus(PaymentBridge bridge) {
        EventBus bus = getEventListenerContext().getEventBus();
        Connection con = bridge.getConnection();
        PaymentStatus status = bridge.getStatus();

        if (status == PaymentStatus.PAYMODEL_ACCEPTED) {
            // Wait until the payment is generated.
            bus.publish(PaypalBot.makeProcessingMessage("Wait until the PayPal Payment is generated...", con));
        } else if (status == PaymentStatus.PP_ACCEPTED) {
            // Wait until the payment is completed.
            bus.publish(PaypalBot.makeProcessingMessage("Wait until the payment is completed...", con));
        } else if (status == PaymentStatus.COMPLETED) {
            // Payment completed. Close connection.
            bus.publish(PaypalBot.makeProcessingMessage("Payment completed. Close the connection!", con));
        } else if (status == PaymentStatus.EXPIRED) {
            // Payment expired. Close connection.
            bus.publish(PaypalBot.makeProcessingMessage("Payment expired. Close the connection!", con));
        } else if (status == PaymentStatus.FAILURE) {
            // Payment failed. Close connection.
            bus.publish(PaypalBot.makeProcessingMessage("Payment failed. Close the connection!", con));
        }
    }
}
