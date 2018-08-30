package won.payment.paypal.bot.action.connect;

import java.util.Collections;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.RSS;

import com.paypal.svcs.types.ap.PayRequest;
import com.paypal.svcs.types.ap.Receiver;
import com.paypal.svcs.types.ap.ReceiverList;

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
import won.payment.paypal.bot.util.InformationExtractor;
import won.payment.paypal.service.impl.PaypalPaymentService;
import won.protocol.agreement.AgreementProtocolState;
import won.protocol.model.Connection;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonConversationUtils;
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
			} else if (bridge.getBuyerConnection() != null &&
					con.getConnectionURI().equals(bridge.getBuyerConnection().getConnectionURI())) {
				logger.info("buyer accepted the connection");
				//proposePaymentToBuyer(con);
				generatePayment(con);
			} else {
				logger.error("OpenFromOtherNeedEvent from not registered connection URI {}", con.toString());
			}
		}
		
	}
	
	private void generatePayment(Connection con) {
		EventListenerContext ctx = getEventListenerContext();
		PaymentBridge bridge = PaypalBotContextWrapper.instance(ctx).getOpenBridge(con.getNeedURI());
		bridge.setStatus(PaymentStatus.ACCEPTED);
		PaypalBotContextWrapper.instance(ctx).putOpenBridge(con.getNeedURI(), bridge);
		
		
		AgreementProtocolState state = WonConversationUtils.getAgreementProtocolState(bridge.getMerchantConnection().getConnectionURI(),
				ctx.getLinkedDataSource());
		Dataset dataset = state.getAgreements();
		Model agreements = dataset.getUnionModel();
		
		Model payload = agreements;
		
		Double amount = InformationExtractor.getAmount(payload);
        String currency = InformationExtractor.getCurrency(payload);
        String receiver = InformationExtractor.getReceiver(payload);
        String feePayer = InformationExtractor.getFeePayer(payload);
        String expirationTime = InformationExtractor.getExpirationTime(payload);
        String invoiceDetails = InformationExtractor.getInvoiceDetails(payload);
        String invoiceId = InformationExtractor.getInvoiceId(payload);
        Double tax = InformationExtractor.getTax(payload);
		// TODO: Tax
        
        try {
        	PayRequest pay = new PayRequest();
    		// Set receiver
    		Receiver rec = new Receiver();
    		rec.setAmount(amount);
    		rec.setEmail(receiver);
    		pay.setReceiverList(new ReceiverList(Collections.singletonList(rec)));
    		pay.setCurrencyCode(currency);
    		
    		if (feePayer != null) {
    			feePayer = feePayer.equals(WONPAY.FEE_PAYER_SENDER.getURI()) ? "SENDER" : "RECEIVER";
    			pay.setFeesPayer(feePayer);
    		}
    		if (expirationTime != null) {
    			pay.setPayKeyDuration(expirationTime);
    		}
    		if (invoiceDetails != null) {
    			pay.setMemo(invoiceDetails);
    		}
    		if (invoiceId != null) {
    			rec.setInvoiceId(invoiceId);
    		}   		
    		

        	PaypalPaymentService paypalService = PaypalBotContextWrapper.instance(ctx).getPaypalService();
			String payKey = paypalService.create(pay);
			String url = paypalService.getPaymentUrl(payKey);

			bridge.setStatus(PaymentStatus.GENERATED);
			bridge.setPayKey(payKey);
			PaypalBotContextWrapper.instance(ctx).putOpenBridge(con.getNeedURI(), bridge);
			
			// Print pay link to buyer; Tell merchant everything is fine
			Model respBuyer = WonRdfUtils.MessageUtils.processingMessage("Click on this link for executing the payment: \n" + url);
			RdfUtils.findOrCreateBaseResource(respBuyer).addProperty(RSS.link, new ResourceImpl(url));
			Model respMerch = WonRdfUtils.MessageUtils.processingMessage("Payment was generated. Waiting for the buyer to execute ...");
			
			ctx.getEventBus().publish(new ConnectionMessageCommandEvent(con, respBuyer));
			ctx.getEventBus().publish(new ConnectionMessageCommandEvent(bridge.getMerchantConnection(), respMerch));
			logger.info("Paypal Payment generated with payKey={}", payKey);
		} catch (Exception e) {
			logger.warn("Paypal payment could not be generated.", e);
		}
	}

}
