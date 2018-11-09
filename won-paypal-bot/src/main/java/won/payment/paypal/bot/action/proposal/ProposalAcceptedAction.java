package won.payment.paypal.bot.action.proposal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RSS;

import com.paypal.svcs.types.ap.PayRequest;
import com.paypal.svcs.types.ap.Receiver;
import com.paypal.svcs.types.ap.ReceiverList;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.analyzation.agreement.ProposalAcceptedEvent;
import won.bot.framework.eventbot.event.impl.command.connect.ConnectCommandEvent;
import won.bot.framework.eventbot.event.impl.command.connect.ConnectCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandResultEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.OpenFromOtherNeedEvent;
import won.bot.framework.eventbot.filter.impl.CommandResultFilter;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnFirstEventListener;
import won.payment.paypal.bot.event.connect.ComplexConnectCommandEvent;
import won.payment.paypal.bot.impl.PaypalBotContextWrapper;
import won.payment.paypal.bot.model.PaymentBridge;
import won.payment.paypal.bot.model.PaymentModelWrapper;
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
import won.protocol.vocabulary.WONMOD;
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
				generatePP(proposalAcceptedEvent);
			} else if (bridge.getStatus() == PaymentStatus.GENERATED) {
				// Merchant has accepted the pp
				connectToBuyer(proposalAcceptedEvent);
			} else if (bridge.getStatus() == PaymentStatus.PP_DENIED) {
				// Merchant has accepted the cancelation of the paymodel
				// TODO: Implement
				cancelPaymodel(proposalAcceptedEvent);
			} else if (bridge.getStatus() == PaymentStatus.BUYER_DENIED) {
				// Buyer has denied the connection and merchant accepts
				// to rebuild the paymodel
				cancelAll(proposalAcceptedEvent);
			} else if (bridge.getStatus() == PaymentStatus.BUYER_OPENED) {
				publishPayKey(con);
			}

		}

	}

	/**
	 * Sets bridge.status to building and retracts the payment summary message from
	 * the cancelation.
	 * @context Merchant.
	 * @param proposalAcceptedEvent
	 */
	private void cancelPaymodel(ProposalAcceptedEvent proposalAcceptedEvent) {
		Connection con = proposalAcceptedEvent.getCon();
		EventListenerContext ctx = getEventListenerContext();
		PaymentBridge bridge = PaypalBotContextWrapper.instance(ctx).getOpenBridge(con.getNeedURI());
		bridge.setStatus(PaymentStatus.BUILDING);
		PaypalBotContextWrapper.instance(ctx).putOpenBridge(con.getNeedURI(), bridge);

		AgreementProtocolState agreementProtocolState = AgreementProtocolState.of(con.getConnectionURI(),
				getEventListenerContext().getLinkedDataSource());

		Model conversation = agreementProtocolState.getConversationDataset().getUnionModel();

		StmtIterator itr = conversation.listStatements(null, RDF.type, WONPAY.PAYMENT_SUMMARY);
		String paymentSummaryUri = null;

		while (itr.hasNext()) {
			Resource subj = itr.next().getSubject();
			if (!conversation.listStatements(null, WONMOD.RETRACTS, subj).hasNext()) {
				paymentSummaryUri = subj.getURI();
			}
		}

		if (paymentSummaryUri == null) {
			return;
		}

		try {
			Model retractResponse = WonRdfUtils.MessageUtils.retractsMessage(new URI(paymentSummaryUri));
			retractResponse = WonRdfUtils.MessageUtils.addMessage(retractResponse,
					"You can now edit the paymodel again.");
			getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(con, retractResponse));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Simply calls the cancelPaymodel method and informs the
	 * buyer to wait for the new payment offer.
	 * 
	 * @context Merchant.
	 * @param proposalAcceptedEvent
	 */
	private void cancelAll(ProposalAcceptedEvent proposalAcceptedEvent) {
		// TODO: Implement
		cancelPaymodel(proposalAcceptedEvent);
		// Send the buyer the info, that the merchant is preparing a new offer
		Connection con = proposalAcceptedEvent.getCon();
		EventListenerContext ctx = getEventListenerContext();
		PaymentBridge bridge = PaypalBotContextWrapper.instance(ctx).getOpenBridge(con.getNeedURI());
		makeTextMsg("Merchant is prepearing a new payment. Wait for it ...", bridge.getBuyerConnection());
	}

	private void makeTextMsg(String msg, Connection con) {
		if (con == null) {
			return;
		}
		Model model = WonRdfUtils.MessageUtils.processingMessage(msg);
		getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(con, model));
	}

	/**
	 * Generates the PP, sends the link and paykey to the merchant and proposes this
	 * message.
	 * 
	 * @context Merchant.
	 * @param event the accepted paymodel event.
	 */
	private void generatePP(ProposalAcceptedEvent event) {
		Connection con = event.getCon();
		EventListenerContext ctx = getEventListenerContext();
		PaymentBridge bridge = PaypalBotContextWrapper.instance(ctx).getOpenBridge(con.getNeedURI());
		bridge.setStatus(PaymentStatus.PAYMODEL_ACCEPTED);
		PaypalBotContextWrapper.instance(ctx).putOpenBridge(con.getNeedURI(), bridge);

		// TODO: Send info, that a payment is getting generated rn

		PaymentModelWrapper paymodel = new PaymentModelWrapper(event.getPayload());
		
		try {
			PayRequest pay = paymodel.toPayRequest();

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

			final ConnectionMessageCommandEvent connectionMessageCommandEvent = new ConnectionMessageCommandEvent(con,
					response);
			ctx.getEventBus().subscribe(ConnectionMessageCommandResultEvent.class, new ActionOnFirstEventListener(ctx,
					new CommandResultFilter(connectionMessageCommandEvent), new BaseEventBotAction(ctx) {
						@Override
						protected void doRun(Event event, EventListener executingListener) throws Exception {
							ConnectionMessageCommandResultEvent connectionMessageCommandResultEvent = (ConnectionMessageCommandResultEvent) event;
							if (connectionMessageCommandResultEvent.isSuccess()) {
								Model agreementMessage = WonRdfUtils.MessageUtils.processingMessage(
										"Check the generated PayPal payment. If you accept it will be published "
												+ "to the buyer.");
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

		} catch (Exception e) {
			logger.warn("Paypal payment could not be generated.", e);
			makeTextMsg("PayPal payment could not be generated: " + e.getMessage(), con);
			bridge.setStatus(PaymentStatus.FAILURE);
			PaypalBotContextWrapper.instance(ctx).putOpenBridge(con.getNeedURI(), bridge);
			
		}
	}

	/**
	 * TODO: Docu.
	 * 
	 * @context Merchant.
	 * @param event
	 * @throws URISyntaxException
	 */
	private void connectToBuyer(ProposalAcceptedEvent event) throws URISyntaxException {		
		EventListenerContext ctx = getEventListenerContext();
		Connection con = event.getCon();
		PaymentBridge bridge = PaypalBotContextWrapper.paymentBridge(ctx, con);

		// If there is already an active connection to the buyer, then just send the proposal
		if (bridge.getBuyerConnection() != null) {
			proposePayModelToBuyer(bridge.getBuyerConnection());
			makeTextMsg("Proposing the new payment to the buyer.", con);
			return;
		}
		
		AgreementProtocolState state = WonConversationUtils.getAgreementProtocolState(
				bridge.getMerchantConnection().getConnectionURI(), ctx.getLinkedDataSource());
		Dataset dataset = state.getAgreements();
		Model agreements = dataset.getUnionModel();

		String paymentUri = WonPayRdfUtils.getPaymentModelUri(con);
		Model paymodel = agreements.listStatements(new ResourceImpl(paymentUri), null, (RDFNode) null).toModel();
		PaymentModelWrapper paymentWrapper = new PaymentModelWrapper(paymodel);

		String openingMsg = "Payment request with secret: " + paymentWrapper.getSecret()
				+ "\nAccept the connection to receive the payment.";
		Model secretModel = ModelFactory.createDefaultModel();
		secretModel.createResource().addProperty(WONPAY.HAS_SECRET, paymentWrapper.getSecret());

		// Only post secret in the payload
		ComplexConnectCommandEvent connectCommandEvent = new ComplexConnectCommandEvent(con.getNeedURI(),
				paymentWrapper.getCounterpartNeedUri(), openingMsg, secretModel);
		ctx.getEventBus().subscribe(ConnectCommandSuccessEvent.class, new ActionOnFirstEventListener(ctx,
				new CommandResultFilter(connectCommandEvent), new BaseEventBotAction(ctx) {

					@Override
					protected void doRun(Event event, EventListener executingListener) throws Exception {

						if (event instanceof ConnectCommandSuccessEvent) {
							logger.info("created successfully a connection to buyer");
							ConnectCommandSuccessEvent connectSuccessEvent = (ConnectCommandSuccessEvent) event;
							URI needUri = connectSuccessEvent.getNeedURI();
							Connection buyerCon = connectSuccessEvent.getCon();
							bridge.setBuyerConnection(buyerCon);
							bridge.setStatus(PaymentStatus.PP_ACCEPTED);
							PaypalBotContextWrapper.instance(ctx).putOpenBridge(needUri, bridge);
							makeTextMsg("Waiting for the buyer to accept the payment.", bridge.getMerchantConnection());
						}

					}

				}));

		ctx.getEventBus().publish(connectCommandEvent);
	}
	
	/**
	 * Is only needed, if the connection to the buyer is already established
	 * and we can directly send him the new paymodel offer.
	 * 
	 * @context Merchant.
	 * @param con
	 */
	private void proposePayModelToBuyer(Connection buyerConn) {
		EventListenerContext ctx = getEventListenerContext();
		PaymentBridge bridge = PaypalBotContextWrapper.instance(ctx).getOpenBridge(buyerConn.getNeedURI());
		bridge.setStatus(PaymentStatus.BUYER_OPENED);
		PaypalBotContextWrapper.instance(ctx).putOpenBridge(buyerConn.getNeedURI(), bridge);
		
		// Get paymodel out of merchants agreement protokoll
		AgreementProtocolState merchantAgreementProtocolState = AgreementProtocolState.of(bridge.getMerchantConnection().getConnectionURI(),
				getEventListenerContext().getLinkedDataSource());

		Model conversation = merchantAgreementProtocolState.getConversationDataset().getUnionModel();
		String paymodelUri = WonPayRdfUtils.getPaymentModelUri(bridge.getMerchantConnection());
		
		Model paymodel = conversation.listStatements(new ResourceImpl(paymodelUri), null, (RDFNode)null).toModel();
		paymodel = WonRdfUtils.MessageUtils.addMessage(paymodel, "Payment summary"); // TODO: Add the amount, currency, etc. ...

		// Remove unnecesry statements (counterpart)
		paymodel.removeAll(null, WONPAY.HAS_NEED_COUNTERPART, null);

		// Publish paymodel with proposal
		final ConnectionMessageCommandEvent connectionMessageCommandEvent = new ConnectionMessageCommandEvent(buyerConn,
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
							ctx.getEventBus().publish(new ConnectionMessageCommandEvent(buyerConn, agreementMessage));
						} else {
							logger.error("FAILURERESPONSEEVENT FOR PROPOSAL PAYLOAD");
						}
					}
				}));

		ctx.getEventBus().publish(connectionMessageCommandEvent);
		
	}
	
	/**
	 * @context Buyer.
	 * @param con
	 */
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
		Model respMerch = WonRdfUtils.MessageUtils.processingMessage("Buyer accepted the payment. "
				+ "Waiting for the buyer to execute the payment ...");
		
		ctx.getEventBus().publish(new ConnectionMessageCommandEvent(con, respBuyer));
		ctx.getEventBus().publish(new ConnectionMessageCommandEvent(bridge.getMerchantConnection(), respMerch));
		
	}

}
