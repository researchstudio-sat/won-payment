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
 * Eventhandler which will be invoked when a connection was closed by the
 * merchant or the buyer.
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
                logger.debug("Unexpected closure in the Atom {}", con.getAtomURI());
            }
        }
    }

    /**
     * Simply removes the connection from the payment bridge. If both are closed the
     * atom will be closed.
     * 
     * @param bridge Bridge where to connections are.
     * @param con Connection which was closed.
     */
    private void closeConnection(PaymentBridge bridge, Connection con) {
        // Merchant closure
        if (bridge.getConnection() != null
                        && bridge.getConnection().getConnectionURI().equals(con.getConnectionURI())) {
            bridge.setConnection(null);
            logger.debug("Merchant has closed the connection after completion in the Atom {}", con.getAtomURI());
        }
        PaypalBotContextWrapper.instance(getEventListenerContext()).putOpenBridge(con.getAtomURI(), bridge);
        closeAtom(bridge, con);
    }

    /**
     * Closes the Atom if the payment bridge's connections are empty.
     * 
     * @param bridge PaymentBridge to check.
     * @param con Connection which was closed.
     */
    private void closeAtom(PaymentBridge bridge, Connection con) {
        // Both closed
        if (bridge.getConnection() == null /* && bridge.getBuyerConnection() == null */) {
            PaypalBotContextWrapper.instance(getEventListenerContext()).removeOpenBridge(con.getAtomURI());
            getEventListenerContext().getEventBus().publish(new DeactivateAtomCommandEvent(con.getAtomURI()));
            logger.debug("Atom gets closed {}", con.getAtomURI());
        }
    }

    /**
     * Informs the counterpart if the paypal payment is already generated or
     * accepted.
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
            logger.info("Unexpected closure from merchant with connection {} in the atom {}", con.toString(),
                            con.getAtomURI().toString());
            bridge.setConnection(null);
        } else {
            // TODO: verify that this never happens
            // throw exception and write log msg
        }
        PaypalBotContextWrapper.instance(getEventListenerContext()).putOpenBridge(con.getAtomURI(), bridge);
        closeAtom(bridge, con);
        // TODO: can this be deleted?
        /*
         * if (bridge.getStatus() == PaymentStatus.PP_ACCEPTED || bridge.getStatus() ==
         * PaymentStatus.GENERATED) { if (bridge.getMerchantConnection() != null &&
         * bridge.getMerchantConnection().getConnectionURI().equals(con.getConnectionURI
         * ())) { bridge.setMerchantConnection(null);
         * makeTextMsg("ATTENTION: THE MERCHANT HAS LEFT. DO NOT EXECUTE THE PAYMENT!",
         * bridge.getBuyerConnection());
         * logger.debug("Merchant has closed the connection in status {}" +
         * " in the Atom {}", bridge.getStatus().name(), con.getAtomURI()); } if
         * (bridge.getBuyerConnection() != null &&
         * bridge.getBuyerConnection().getConnectionURI().equals(con.getConnectionURI())
         * ) { bridge.setBuyerConnection(null);
         * makeTextMsg("The Buyer has left. Type 'payment check' for validating the payment or wait "
         * + "until you get informed", bridge.getMerchantConnection());
         * logger.debug("Buyer has closed the connection in status {}" +
         * " in the Atom {}", bridge.getStatus().name(), con.getAtomURI()); } } else if
         * (bridge.getStatus() == PaymentStatus.BUILDING) { // If the merchant left,
         * tell the buyer not to do anything and leave the channel // If the buyer left
         * just tell the merchant // If the buyer declines the connection just tell the
         * merchant if (bridge.getMerchantConnection() != null &&
         * bridge.getMerchantConnection().getConnectionURI().equals(con.getConnectionURI
         * ())) { bridge.setMerchantConnection(null);
         * makeTextMsg("ATTENTION: THE MERCHANT HAS LEFT. PLEASE CONTACT THE MERCHANT!",
         * bridge.getBuyerConnection());
         * logger.debug("Merchant has closed the connection in status {}" +
         * " in the Atom {}", bridge.getStatus().name(), con.getAtomURI()); } else if
         * (bridge.getBuyerConnection() != null &&
         * bridge.getBuyerConnection().getConnectionURI().equals(con.getConnectionURI())
         * ) { bridge.setBuyerConnection(null);
         * makeTextMsg("The Buyer has left without accepting. Change the payment and validate it again."
         * , bridge.getMerchantConnection());
         * logger.debug("Buyer has closed the connection in status {}" +
         * " in the Atom {}", bridge.getStatus().name(), con.getAtomURI()); } else if
         * (bridge.getBuyerConnection() == null) {
         * makeTextMsg("The buyer declined the connection. Change the payment and validate it again."
         * , bridge.getMerchantConnection());
         * bridge.setStatus(PaymentStatus.BUYER_DENIED);
         * logger.debug("Buyer has closed the connection in status {}" +
         * " in the Atom {}", bridge.getStatus().name(), con.getAtomURI()); } } else {
         * // Can only be the merchant if (bridge.getMerchantConnection() != null &&
         * bridge.getMerchantConnection().getConnectionURI().equals(con.getConnectionURI
         * ())) { bridge.setMerchantConnection(null);
         * logger.debug("Merchant has closed the connection in status {}" +
         * " in the Atom {}", bridge.getStatus().name(), con.getAtomURI()); } else { //
         * Error ?! OR status == Nothing
         * logger.debug("Merchant has closed the connection in status {}" +
         * " in the Atom {}", bridge.getStatus().name(), con.getAtomURI()); } }
         * closeAtom(bridge, con);
         */
    }
}
