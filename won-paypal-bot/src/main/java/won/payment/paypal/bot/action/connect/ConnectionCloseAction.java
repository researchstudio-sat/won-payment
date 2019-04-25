package won.payment.paypal.bot.action.connect;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.deactivate.DeactivateAtomCommandEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.CloseFromOtherAtomEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.payment.paypal.bot.impl.PaypalBotContextWrapper;
import won.payment.paypal.bot.model.PaymentBridge;
import won.payment.paypal.bot.model.PaymentStatus;
import won.protocol.model.Connection;

/**
 * Eventhandler which will be invoked when a connection was closed by the user.
 * 
 * @author schokobaer
 */
public class ConnectionCloseAction extends BaseEventBotAction {
    public ConnectionCloseAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        if (event instanceof CloseFromOtherAtomEvent) {
            CloseFromOtherAtomEvent closeEvent = (CloseFromOtherAtomEvent) event;
            Connection con = closeEvent.getCon();
            PaymentBridge bridge = PaypalBotContextWrapper.instance(getEventListenerContext())
                            .getOpenBridge(con.getAtomURI());
            if (bridge.getStatus().ordinal() >= PaymentStatus.COMPLETED.ordinal()) {
                closeConnection(bridge, con);
            } else {
                unexpectedClosure(bridge, con);
                logger.debug("Connection to Atom {} was unexpectedly closed.", con.getAtomURI());
            }
        }
    }

    /**
     * Removes the connection from the payment bridge. If all connections are
     * closed, the atom will be closed.
     * 
     * @param bridge Bridge where to connections are.
     * @param con Connection which was closed.
     */
    private void closeConnection(PaymentBridge bridge, Connection con) {
        // Merchant closure
        if (bridge.getConnection() != null
                        && bridge.getConnection().getConnectionURI().equals(con.getConnectionURI())) {
            bridge.setConnection(null);
            logger.debug("Connection to Atom {} was closed.", con.getAtomURI());
        }
        PaypalBotContextWrapper.instance(getEventListenerContext()).putOpenBridge(con.getAtomURI(), bridge);
        closeAtom(bridge, con);
    }

    /**
     * Close the Atom if there are no active payment bridge connections.
     * 
     * @param bridge PaymentBridge to check.
     * @param con Connection which was closed.
     */
    private void closeAtom(PaymentBridge bridge, Connection con) {
        if (bridge.getConnection() == null /* && bridge.getBuyerConnection() == null */) {
            PaypalBotContextWrapper.instance(getEventListenerContext()).removeOpenBridge(con.getAtomURI());
            getEventListenerContext().getEventBus().publish(new DeactivateAtomCommandEvent(con.getAtomURI()));
            logger.debug("Closing Atom {}", con.getAtomURI());
        }
    }

    /**
     * Handle connections that were closed before the payment was completed. TODO:
     * this can probably be merged with closeAtom().
     * 
     * @param bridge Bridge of the connection.
     * @param con Connection which was closed.
     */
    private void unexpectedClosure(PaymentBridge bridge, Connection con) {
        // Fix here everything !!!
        bridge.setStatus(PaymentStatus.FAILURE);
        if (bridge.getConnection() != null
                        && bridge.getConnection().getConnectionURI().equals(con.getConnectionURI())) {
            // Merchant closed
            logger.info("Connection {} to Atom {} was closed unexpectedly.", con.toString(),
                            con.getAtomURI().toString());
            bridge.setConnection(null);
        }
        PaypalBotContextWrapper.instance(getEventListenerContext()).putOpenBridge(con.getAtomURI(), bridge);
        closeAtom(bridge, con);
    }
}
