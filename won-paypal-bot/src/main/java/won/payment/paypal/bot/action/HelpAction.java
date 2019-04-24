package won.payment.paypal.bot.action;

import org.apache.jena.rdf.model.Model;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.payment.paypal.bot.event.SimpleMessageReceivedEvent;
import won.payment.paypal.bot.impl.PaypalBotContextWrapper;
import won.payment.paypal.bot.model.PaymentBridge;
import won.payment.paypal.bot.model.PaymentStatus;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;

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
            if (bridge.getMerchantConnection() != null
                            && con.getConnectionURI().equals(bridge.getMerchantConnection().getConnectionURI())) {
                handleMerchant(bridge);
            } else {
                // TODO: defined and throw exception and add log msg
                // Should not be possible
            }
        }
    }

    // TODO: think about moving this to a public method somewhere
    private void makeTextMsg(String msg, Connection con) {
        if (con == null) {
            return;
        }
        Model model = WonRdfUtils.MessageUtils.processingMessage(msg);
        getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(con, model));
    }

    // TODO: think about renaming this and maybe changing parameters
    private void handleMerchant(PaymentBridge bridge) {
        if (bridge.getStatus() == PaymentStatus.PAYMODEL_ACCEPTED) {
            makeTextMsg("Wait until the PayPal Payment is generated...", bridge.getMerchantConnection());
            // Wait until the pp is generated.
        } else if (bridge.getStatus() == PaymentStatus.PP_ACCEPTED) {
            // Wait until the payment got executed
            makeTextMsg("Wait until the payment is completerd...", bridge.getMerchantConnection());
        } else if (bridge.getStatus() == PaymentStatus.COMPLETED) {
            // Payment completed. close the con
            makeTextMsg("Payment completed. Close the connection!", bridge.getMerchantConnection());
        } else if (bridge.getStatus() == PaymentStatus.EXPIRED) {
            // Payment expired. close the con
            makeTextMsg("Payment expired. Close the connection!", bridge.getMerchantConnection());
        } else if (bridge.getStatus() == PaymentStatus.FAILURE) {
            // Payment failed. close the con
            makeTextMsg("Payment failed. Close the connection!", bridge.getMerchantConnection());
        }
    }
}
