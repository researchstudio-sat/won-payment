package won.payment.paypal.bot.action.connect;

import org.apache.jena.rdf.model.Model;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandResultEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.OpenFromOtherNeedEvent;
import won.bot.framework.eventbot.filter.impl.CommandResultFilter;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnFirstEventListener;
import won.payment.paypal.bot.event.analyze.ConversationAnalyzationCommandEvent;
import won.payment.paypal.bot.impl.PaypalBotContextWrapper;
import won.payment.paypal.bot.model.PaymentBridge;
import won.payment.paypal.bot.model.PaymentModelWrapper;
import won.payment.paypal.bot.model.PaymentStatus;
import won.protocol.agreement.AgreementProtocolState;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONPAY;

/**
 * When the counterpart has accepted the connection, this action will be invoked.
 * It changes the sate of the bridge and generates the payment and sends the link
 * to the buyer.
 * 
 * @author schokobaer
 *
 */
public class ConnectionAcceptedAction extends BaseEventBotAction {

	public ConnectionAcceptedAction(EventListenerContext eventListenerContext) {
		super(eventListenerContext);
	}

	@Override
	protected void doRun(Event event, EventListener executingListener) throws Exception {
		
		if (event instanceof OpenFromOtherNeedEvent) {
			EventListenerContext ctx = getEventListenerContext();
			Connection con = ((OpenFromOtherNeedEvent) event).getCon();
			PaymentBridge bridge = PaypalBotContextWrapper.instance(ctx).getOpenBridge(con.getNeedURI());
			
			if (bridge.getMerchantConnection() != null &&
					con.getConnectionURI().equals(bridge.getMerchantConnection().getConnectionURI())) {
				logger.info("merchant accepted the connection");
				bridge.setStatus(PaymentStatus.BUILDING);
				PaypalBotContextWrapper.instance(ctx).putOpenBridge(con.getNeedURI(), bridge);
				ctx.getEventBus().publish(new ConversationAnalyzationCommandEvent(con));
			} else if (bridge.getStatus() == PaymentStatus.PP_ACCEPTED) {
				logger.info("buyer accepted the connection");
				proposePayModelToBuyer(con);
			} else {
				logger.error("OpenFromOtherNeedEvent from not registered connection URI {}", con.toString());
			}
		}
		
	}
	
	/**
	 * @context Buyer.
	 * @param con
	 */
	private void proposePayModelToBuyer(Connection con) {
		EventListenerContext ctx = getEventListenerContext();
		PaymentBridge bridge = PaypalBotContextWrapper.instance(ctx).getOpenBridge(con.getNeedURI());
		bridge.setStatus(PaymentStatus.BUYER_OPENED);
		PaypalBotContextWrapper.instance(ctx).putOpenBridge(con.getNeedURI(), bridge);
		
		// Get paymodel out of merchants agreement protokoll
		AgreementProtocolState merchantAgreementProtocolState = AgreementProtocolState.of(bridge.getMerchantConnection().getConnectionURI(),
				getEventListenerContext().getLinkedDataSource());

		Model conversation = merchantAgreementProtocolState.getConversationDataset().getUnionModel();
		//String paymodelUri = WonPayRdfUtils.getPaymentModelUri(bridge.getMerchantConnection());
		
		Model paymodel = conversation;
		PaymentModelWrapper paymentWrapper = new PaymentModelWrapper(paymodel);
        //TODO: JUST PUSH THE PAYMENT MODEL 'DETAIL' INSTEAD AS A MESSAGE (Structure see payment-detail)
		String paymentText = "Amount: " + paymentWrapper.getCurrencySymbol() + " " + paymentWrapper.getAmount() + "\nReceiver: " + paymentWrapper.getReceiver(); 
		paymodel = WonRdfUtils.MessageUtils.textMessage(paymentText); // TODO: Add the amount, currency, etc. ...

		// Remove unnecesry statements (counterpart)
		paymodel.removeAll(null, WONPAY.HAS_NEED_COUNTERPART, null);

		// Publish paymodel with proposal
		final ConnectionMessageCommandEvent connectionMessageCommandEvent = new ConnectionMessageCommandEvent(con,
				paymodel);
		ctx.getEventBus().subscribe(ConnectionMessageCommandResultEvent.class, new ActionOnFirstEventListener(ctx,
				new CommandResultFilter(connectionMessageCommandEvent), new BaseEventBotAction(ctx) {
					@Override
					protected void doRun(Event event, EventListener executingListener) throws Exception {
						ConnectionMessageCommandResultEvent connectionMessageCommandResultEvent = (ConnectionMessageCommandResultEvent) event;
						if (connectionMessageCommandResultEvent.isSuccess()) {
							Model agreementMessage = WonRdfUtils.MessageUtils.processingMessage(
									"Accept the paymet to receive the PayPal link to execute it.");
							WonRdfUtils.MessageUtils.addProposes(agreementMessage,
									((ConnectionMessageCommandSuccessEvent) connectionMessageCommandResultEvent)
											.getWonMessage().getMessageURI());
							ctx.getEventBus().publish(new ConnectionMessageCommandEvent(con, agreementMessage));
						} else {
							logger.error("FAILURERESPONSEEVENT FOR PROPOSAL PAYLOAD");
						}
					}
				}));

		ctx.getEventBus().publish(connectionMessageCommandEvent);
		
	}

}
