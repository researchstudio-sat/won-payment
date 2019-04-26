package won.payment.paypal.bot.action.connect;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.deactivate.DeactivateAtomCommandEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.CloseFromOtherAtomEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.payment.paypal.bot.impl.PaypalBotContextWrapper;
import won.payment.paypal.bot.model.PaymentContext;
import won.payment.paypal.bot.model.PaymentStatus;
import won.protocol.model.Connection;

/**
 * Eventhandler which will be invoked when a connection was closed by the user.
 * 
 * @author schokobaer
 */
public class ConnectionCloseAction extends BaseEventBotAction {
    private EventListenerContext ctx;
    private PaypalBotContextWrapper botCtx;

    public ConnectionCloseAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        if (event instanceof CloseFromOtherAtomEvent) {
            ctx = getEventListenerContext();
            botCtx = (PaypalBotContextWrapper) ctx.getBotContextWrapper();
            CloseFromOtherAtomEvent closeEvent = (CloseFromOtherAtomEvent) event;
            Connection con = closeEvent.getCon();
            PaymentContext payCtx = botCtx.getPaymentContext(con.getAtomURI());
            // Always close atom and connection (if existing) and set status accordingly.
            if (payCtx.getStatus().ordinal() >= PaymentStatus.COMPLETED.ordinal()) {
                // Connection closed after the payment was completed
                closeConnection(payCtx, con);
            } else {
                if (payCtx.getStatus() == PaymentStatus.PP_ACCEPTED) {
                    // Connection closed after the payment link was generated and published,
                    // but before the payment was completed.
                    logger.debug("Connection {} to Atom {} was closed by the user.", con.toString(), con.getAtomURI());
                    payCtx.setStatus(PaymentStatus.COMPLETED);
                } else {
                    // Connection closed before the payment link was published.
                    logger.debug("Connection {} to Atom {} was unexpectedly closed.", con.toString(), con.getAtomURI());
                    payCtx.setStatus(PaymentStatus.FAILURE);
                }
                if (payCtx.getConnection() != null
                        && payCtx.getConnection().getConnectionURI().equals(con.getConnectionURI())) {
                    payCtx.setConnection(null);
                }
                botCtx.setPaymentContext(con.getAtomURI(), payCtx);
                closeAtom(payCtx, con);
            }
        }
    }

    /**
     * Removes the connection from the payment context. If all connections are
     * closed, the atom will be closed.
     * 
     * @param payCtx Payment context which contains atom and connection URIs
     * @param con    Connection which was closed.
     */
    private void closeConnection(PaymentContext payCtx, Connection con) {
        // Merchant closure
        if (payCtx.getConnection() != null
                && payCtx.getConnection().getConnectionURI().equals(con.getConnectionURI())) {
            payCtx.setConnection(null);
            logger.debug("Connection to Atom {} was closed.", con.getAtomURI());
        }
        botCtx.setPaymentContext(con.getAtomURI(), payCtx);
        closeAtom(payCtx, con);
    }

    /**
     * Close the Atom if there are no active payment context connections.
     * 
     * @param payCtx PaymentContext to check.
     * @param con    Connection which was closed.
     */
    private void closeAtom(PaymentContext payCtx, Connection con) {
        if (payCtx.getConnection() == null) {
            botCtx.removePaymentContext(con.getAtomURI());
            ctx.getEventBus().publish(new DeactivateAtomCommandEvent(con.getAtomURI()));
            logger.debug("Closing Atom {}", con.getAtomURI());
        }
    }
}
