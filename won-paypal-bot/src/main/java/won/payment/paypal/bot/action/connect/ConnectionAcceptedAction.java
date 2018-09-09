package won.payment.paypal.bot.action.connect;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.RSS;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.OpenFromOtherNeedEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.payment.paypal.bot.event.analyze.ConversationAnalyzationCommandEvent;
import won.payment.paypal.bot.impl.PaypalBotContextWrapper;
import won.payment.paypal.bot.model.PaymentBridge;
import won.payment.paypal.bot.model.PaymentStatus;
import won.payment.paypal.service.impl.PaypalPaymentService;
import won.protocol.model.Connection;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

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
				publishPayKey(con);
			} else {
				logger.error("OpenFromOtherNeedEvent from not registered connection URI {}", con.toString());
			}
		}
		
	}
	
	private void publishPayKey(Connection con) {
		EventListenerContext ctx = getEventListenerContext();
		PaymentBridge bridge = PaypalBotContextWrapper.instance(ctx).getOpenBridge(con.getNeedURI());
		bridge.setStatus(PaymentStatus.BUYER_ACCEPTED);
		PaypalBotContextWrapper.instance(ctx).putOpenBridge(con.getNeedURI(), bridge);
		PaypalPaymentService paypalService = PaypalBotContextWrapper.instance(ctx).getPaypalService();
		
		String payKey = bridge.getPayKey();
		String url = paypalService.getPaymentUrl(payKey);
		
		// Print pay link to buyer; Tell merchant everything is fine
		Model respBuyer = WonRdfUtils.MessageUtils.processingMessage("Click on this link for executing the payment: \n" + url);
		RdfUtils.findOrCreateBaseResource(respBuyer).addProperty(RSS.link, new ResourceImpl(url));
		Model respMerch = WonRdfUtils.MessageUtils.processingMessage("Buyer accepted the connection. "
				+ "Waiting for the buyer to execute the payment ...");
		
		ctx.getEventBus().publish(new ConnectionMessageCommandEvent(con, respBuyer));
		ctx.getEventBus().publish(new ConnectionMessageCommandEvent(bridge.getMerchantConnection(), respMerch));
		
	}

}
