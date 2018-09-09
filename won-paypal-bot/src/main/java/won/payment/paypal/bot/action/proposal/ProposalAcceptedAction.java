package won.payment.paypal.bot.action.proposal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.RSS;

import com.paypal.svcs.types.ap.PayRequest;
import com.paypal.svcs.types.ap.Receiver;
import com.paypal.svcs.types.ap.ReceiverList;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.analyzation.agreement.ProposalAcceptedEvent;
import won.bot.framework.eventbot.event.impl.command.connect.ConnectCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandResultEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandSuccessEvent;
import won.bot.framework.eventbot.filter.impl.CommandResultFilter;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnFirstEventListener;
import won.payment.paypal.bot.event.connect.ComplexConnectCommandEvent;
import won.payment.paypal.bot.impl.PaypalBotContextWrapper;
import won.payment.paypal.bot.model.PaymentBridge;
import won.payment.paypal.bot.model.PaymentStatus;
import won.payment.paypal.bot.util.InformationExtractor;
import won.payment.paypal.bot.util.WonPayRdfUtils;
import won.payment.paypal.service.impl.PaypalPaymentService;
import won.protocol.agreement.AgreementProtocolState;
import won.protocol.model.Connection;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonConversationUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONPAY;

/**
 * After a proposal got accepted this Action will be invoked.
 * 
 * @author schokobaer
 *
 */
public class ProposalAcceptedAction extends BaseEventBotAction {

	public ProposalAcceptedAction(EventListenerContext eventListenerContext) {
		super(eventListenerContext);
	}

	@Override
	protected void doRun(Event event, EventListener executingListener) throws Exception {
		
		EventListenerContext ctx = getEventListenerContext();
		
		if (event instanceof ProposalAcceptedEvent) {
			ProposalAcceptedEvent proposalAcceptedEvent = (ProposalAcceptedEvent) event;
            Connection con = proposalAcceptedEvent.getCon();
            PaymentBridge bridge = PaypalBotContextWrapper.paymentBridge(ctx, con);
            
            if (bridge.getStatus() == PaymentStatus.BUILDING) {
            	// Merchant has accepted the paymodel proposal
            	bridge.setStatus(PaymentStatus.PAYMODEL_ACCEPTED);
            	PaypalBotContextWrapper.instance(ctx).putOpenBridge(bridge.getMerchantConnection().getNeedURI(), bridge);
            	generatePP(proposalAcceptedEvent);
            } else if (bridge.getStatus() == PaymentStatus.GENERATED) {
            	// Merchant has accepted the pp
            	// TODO: Implement
            	connectToBuyer(proposalAcceptedEvent);
            } else if (bridge.getStatus() == PaymentStatus.PP_DENIED) {
            	// Merchant has accepted the cancelation of the paymodel
            	// TODO: Implement
            	cancelPaymodel(proposalAcceptedEvent);
            } else if (bridge.getStatus() == PaymentStatus.BUYER_DENIED) {
            	// Buyer has denied the connection and merchant accepts
            	// to rebuild the paymodel
            	cancelAll(proposalAcceptedEvent);
            }
                        
            
		}

	}
	
	private void cancelPaymodel(ProposalAcceptedEvent proposalAcceptedEvent) {
		// TODO: Implement
	}
	
	private void cancelAll(ProposalAcceptedEvent proposalAcceptedEvent) {
		// TODO: Implement		
	}

	private void makeTextMsg(String msg, Connection con) {
		if (con == null) {
			return;
		}
		Model model = WonRdfUtils.MessageUtils.processingMessage(msg);
		getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(con, model));
	}
	
	/**
	 * Generates the PP, sends the link and paykey to the merchant and
	 * proposes this message.
	 * 
	 * @param event the accepted paymodel event.
	 */
	private void generatePP(ProposalAcceptedEvent event) {
		Connection con = event.getCon();
		EventListenerContext ctx = getEventListenerContext();
		PaymentBridge bridge = PaypalBotContextWrapper.instance(ctx).getOpenBridge(con.getNeedURI());
		bridge.setStatus(PaymentStatus.PAYMODEL_ACCEPTED);
		PaypalBotContextWrapper.instance(ctx).putOpenBridge(con.getNeedURI(), bridge);
		
		// TODO: Send info, that a payment is getting generated rn
		
		Model payload = event.getPayload();
		
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
			logger.info("Paypal Payment generated with payKey={}", payKey);
			String url = paypalService.getPaymentUrl(payKey);
			
			bridge.setStatus(PaymentStatus.GENERATED);
			bridge.setPayKey(payKey);
			PaypalBotContextWrapper.instance(ctx).putOpenBridge(con.getNeedURI(), bridge);
						
			// Print pay link to merchant; Propose it to him
			Model response = WonRdfUtils.MessageUtils.processingMessage("Generated PayPal payment: \n" + url);
			RdfUtils.findOrCreateBaseResource(response).addProperty(RSS.link, new ResourceImpl(url));
			RdfUtils.findOrCreateBaseResource(response).addProperty(WONPAY.HAS_PAYPAL_PAYKEY, payKey);
			
			final ConnectionMessageCommandEvent connectionMessageCommandEvent = new ConnectionMessageCommandEvent(con, response);
            ctx.getEventBus().subscribe(ConnectionMessageCommandResultEvent.class, new ActionOnFirstEventListener(ctx, new CommandResultFilter(connectionMessageCommandEvent), new BaseEventBotAction(ctx) {
                @Override
                protected void doRun(Event event, EventListener executingListener) throws Exception {
                    ConnectionMessageCommandResultEvent connectionMessageCommandResultEvent = (ConnectionMessageCommandResultEvent) event;
                    if(connectionMessageCommandResultEvent.isSuccess()){
                        Model agreementMessage = WonRdfUtils.MessageUtils.processingMessage("Check the generated PayPal payment. If you accept it will be published "
                        		+ "to the buyer.");
                        WonRdfUtils.MessageUtils.addProposes(agreementMessage, ((ConnectionMessageCommandSuccessEvent) connectionMessageCommandResultEvent).getWonMessage().getMessageURI());
                        ctx.getEventBus().publish(new ConnectionMessageCommandEvent(con, agreementMessage));
                    }else{
                        logger.error("FAILURERESPONSEEVENT FOR PROPOSAL PAYLOAD");
                    }
                }
            }));

            ctx.getEventBus().publish(connectionMessageCommandEvent);
			
		} catch (Exception e) {
			logger.warn("Paypal payment could not be generated.", e);
		}
	}
	
	private void connectToBuyer(ProposalAcceptedEvent event) throws URISyntaxException {
		EventListenerContext ctx = getEventListenerContext();
		Connection con = event.getCon();
        PaymentBridge bridge = PaypalBotContextWrapper.paymentBridge(ctx, con);
        
        AgreementProtocolState state = WonConversationUtils.getAgreementProtocolState(bridge.getMerchantConnection().getConnectionURI(),
				ctx.getLinkedDataSource());
		Dataset dataset = state.getAgreements();
		Model agreements = dataset.getUnionModel();
        
		String paymentUri = WonPayRdfUtils.getPaymentModelUri(con);
		Model paymodel = agreements.listStatements(new ResourceImpl(paymentUri), null, (RDFNode)null).toModel();
		Double amount = InformationExtractor.getAmount(paymodel);
        String currency = InformationExtractor.getCurrency(paymodel);
        String receiver = InformationExtractor.getReceiver(paymodel);
        String secret = InformationExtractor.getSecret(paymodel);
		URI counterPartNeedUri = new URI(InformationExtractor.getCounterpart(paymodel));
		paymodel.setNsPrefix("pay", WONPAY.BASE_URI);
		
		
		Model removeModels = paymodel.listStatements(null, WONPAY.HAS_NEED_COUNTERPART, (RDFNode)null).toModel();
		removeModels.add(paymodel.listStatements(null, WON.HAS_TEXT_MESSAGE, (RDFNode)null).toModel());
		removeModels.add(paymodel.listStatements(null, WON.IS_PROCESSING, (RDFNode)null).toModel());
		paymodel.remove(removeModels);
		
		String openingMsg = "Payment request\nAmount: " + currency + " " + amount + "\nReceiver: " + receiver + "\nSecret: " + secret
				+ "\n\nAccept the Connection to receive the payment execution link.";
		        
		ComplexConnectCommandEvent connectCommandEvent = new ComplexConnectCommandEvent(con.getNeedURI(), counterPartNeedUri, openingMsg, paymodel);
        ctx.getEventBus().subscribe(ConnectCommandSuccessEvent.class, new ActionOnFirstEventListener(ctx, new CommandResultFilter(connectCommandEvent), new BaseEventBotAction(ctx) {

			@Override
			protected void doRun(Event event, EventListener executingListener) throws Exception {
				
				if (event instanceof ConnectCommandSuccessEvent) {
					logger.info("created successfully a connection to buyer");
					ConnectCommandSuccessEvent connectSuccessEvent = (ConnectCommandSuccessEvent)event;
					URI needUri = connectSuccessEvent.getNeedURI();
					Connection buyerCon = connectSuccessEvent.getCon();
					bridge.setBuyerConnection(buyerCon);
					bridge.setStatus(PaymentStatus.PP_ACCEPTED);
					PaypalBotContextWrapper.instance(ctx).putOpenBridge(needUri, bridge);
					makeTextMsg("Payment published to buyer.", bridge.getMerchantConnection());
				}
				
			}
        	
        }));
        
        ctx.getEventBus().publish(connectCommandEvent);
	}

}
