package won.payment.paypal.bot.action.connect;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.wonmessage.OpenFromOtherAtomEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.payment.paypal.bot.event.analyze.ConversationAnalyzationCommandEvent;
import won.payment.paypal.bot.impl.PaypalBotContextWrapper;
import won.payment.paypal.bot.model.PaymentContext;
import won.payment.paypal.bot.model.PaymentStatus;
import won.protocol.model.Connection;

/**
 * When the counterpart has accepted the connection, this action will be
 * invoked. It changes the state of the paymentContext and generates the payment
 * and sends the link to the buyer.
 * 
 * @author schokobaer
 */
public class ConnectionAcceptedAction extends BaseEventBotAction {
    public ConnectionAcceptedAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        if (event instanceof OpenFromOtherAtomEvent) {
            PaypalBotContextWrapper botCtx = (PaypalBotContextWrapper) getEventListenerContext().getBotContextWrapper();
            Connection con = ((OpenFromOtherAtomEvent) event).getCon();
            PaymentContext payCtx = botCtx.getPaymentContext(con.getAtomURI());
            if (payCtx.getConnection() != null
                    && con.getConnectionURI().equals(payCtx.getConnection().getConnectionURI())) {
                logger.info("Connection accepted by user for connection {}", con.toString());
                payCtx.setStatus(PaymentStatus.BUILDING);
                botCtx.setPaymentContext(con.getAtomURI(), payCtx);
                getEventListenerContext().getEventBus().publish(new ConversationAnalyzationCommandEvent(con));
            } else {
                logger.error("Unexpected OpenFromOtherAtomEvent from unregistered connection URI {}", con.toString());
            }
        }
    }
}
