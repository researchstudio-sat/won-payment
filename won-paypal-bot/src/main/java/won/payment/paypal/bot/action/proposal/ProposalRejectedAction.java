package won.payment.paypal.bot.action.proposal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.RDF;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.CloseFromOtherNeedEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.payment.paypal.bot.event.proposal.ProposalRejectedEvent;
import won.payment.paypal.bot.impl.PaypalBotContextWrapper;
import won.payment.paypal.bot.model.PaymentBridge;
import won.payment.paypal.bot.model.PaymentStatus;
import won.protocol.agreement.AgreementProtocolState;
import won.protocol.agreement.effect.ProposalType;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONAGR;
import won.protocol.vocabulary.WONPAY;

/**
 * Get invoked when the user rejects a proposal. The payment summary that
 * belongs to the proposal will be retracted then.
 * 
 * @author schokobaer
 *
 */
public class ProposalRejectedAction extends BaseEventBotAction {

	public ProposalRejectedAction(EventListenerContext eventListenerContext) {
		super(eventListenerContext);
	}

	@Override
	protected void doRun(Event event, EventListener executingListener) throws Exception {
		EventListenerContext ctx = getEventListenerContext();

		if (ctx.getBotContextWrapper() instanceof PaypalBotContextWrapper && event instanceof ProposalRejectedEvent) {
			logger.info("Proposal rejected");
			ProposalRejectedEvent rejectsEvent = (ProposalRejectedEvent) event;

			Connection con = ((BaseNeedAndConnectionSpecificEvent) event).getCon();
			PaymentBridge bridge = PaypalBotContextWrapper.paymentBridge(ctx, con);

			if (bridge.getStatus() == PaymentStatus.BUILDING) {
				paymodelRejected(rejectsEvent);
			} else if (bridge.getStatus() == PaymentStatus.GENERATED) {
				ppDenied(rejectsEvent);
			} else if (bridge.getStatus() == PaymentStatus.PP_DENIED) {
				cancelPaymodelDenied(rejectsEvent);
			} else if (bridge.getStatus() == PaymentStatus.BUYER_DENIED) {
				cancelAllDenied(rejectsEvent);
			} else if (bridge.getStatus() == PaymentStatus.BUYER_OPENED) {
				buyerDenied(con);
			}

		}
	}

	/**
	 * Retracts the Payment summary which belongs to the rejected proposal.
	 * 
	 * @context Merchant.
	 * @param event
	 */
	private void paymodelRejected(ProposalRejectedEvent event) {
		Connection con = event.getCon();
		URI paySummaryUri = event.getProposalUri();

		retractPaymentSummary(con, paySummaryUri);
	}

	/**
	 * Makes a proposal to reject the paymodel out of the agreement protocol.
	 * 
	 * @context Merchant.
	 * @param event
	 */
	private void ppDenied(ProposalRejectedEvent event) {
		EventListenerContext ctx = getEventListenerContext();
		Connection con = ((BaseNeedAndConnectionSpecificEvent) event).getCon();
		PaymentBridge bridge = PaypalBotContextWrapper.paymentBridge(ctx, con);

		bridge.setStatus(PaymentStatus.PP_DENIED);
		PaypalBotContextWrapper.instance(ctx).putOpenBridge(bridge.getMerchantConnection().getNeedURI(), bridge);

		// Cancelation for paymodel
		AgreementProtocolState agreementProtocolState = AgreementProtocolState.of(con.getConnectionURI(),
				getEventListenerContext().getLinkedDataSource());
		URI acceptsMsgUri = agreementProtocolState.getLatestAcceptsMessageSentByNeed(con.getRemoteNeedURI());
		Model conversation = agreementProtocolState.getConversationDataset().getUnionModel();
		String proposalMsgUri = conversation
				.listStatements(new ResourceImpl(acceptsMsgUri.toString()), WONAGR.ACCEPTS, (RDFNode) null).next()
				.getObject().asResource().getURI();

		try {
			Model cancelationModel = WonRdfUtils.MessageUtils.proposesToCancelMessage(new URI(proposalMsgUri));
			cancelationModel = WonRdfUtils.MessageUtils.addMessage(cancelationModel,
					"So would you like to edit the payment model?" + " Then accept this cancelation.");

			ctx.getEventBus().publish(new ConnectionMessageCommandEvent(con, cancelationModel));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Merchant did not want to edit the paymodel. So the pp would not change, so we
	 * just repropose the same pp to him ...
	 * 
	 * @context Merchant.
	 * @param event
	 */
	private void cancelPaymodelDenied(ProposalRejectedEvent event) {
		EventListenerContext ctx = getEventListenerContext();
		Connection con = ((BaseNeedAndConnectionSpecificEvent) event).getCon();
		PaymentBridge bridge = PaypalBotContextWrapper.paymentBridge(ctx, con);

		bridge.setStatus(PaymentStatus.GENERATED);
		PaypalBotContextWrapper.instance(ctx).putOpenBridge(bridge.getMerchantConnection().getNeedURI(), bridge);

		// Find the last paykey message and propose it again
		AgreementProtocolState agreementProtocolState = AgreementProtocolState.of(con.getConnectionURI(),
				getEventListenerContext().getLinkedDataSource());
		Model conversation = agreementProtocolState.getConversationDataset().getUnionModel();
		String payKeyMsgUri = conversation.listStatements(null, WONPAY.HAS_PAYPAL_PAYKEY, bridge.getPayKey()).next()
				.getSubject().getURI();

		try {
			Model agreementMessage = WonRdfUtils.MessageUtils.processingMessage("You did not change the "
					+ "payment model, so i propose the same "
					+ "PayPal-Payment to you again. Check it. If you accept it will be published " + "to the buyer.");
			WonRdfUtils.MessageUtils.addProposes(agreementMessage, new URI(payKeyMsgUri));
			ctx.getEventBus().publish(new ConnectionMessageCommandEvent(con, agreementMessage));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Merchant got offered to cancel all. But he did no want to.
	 * So we just propose the last paymentmodel again to the buyer.
	 * 
	 * @context Merchant.
	 * @param event
	 * @throws URISyntaxException
	 */
	private void cancelAllDenied(ProposalRejectedEvent event) throws URISyntaxException {
		// Again propose the same paymodel, which was already proposed before
		// and rejected by the buyer
		EventListenerContext ctx = getEventListenerContext();
		Connection con = event.getCon();
		PaymentBridge bridge = PaypalBotContextWrapper.instance(ctx).getOpenBridge(con.getNeedURI());
		bridge.setStatus(PaymentStatus.BUYER_OPENED);
		PaypalBotContextWrapper.instance(ctx).putOpenBridge(bridge.getMerchantConnection().getNeedURI(), bridge);

		// Find last rejected proposal from buyer
		AgreementProtocolState agreementProtocolState = AgreementProtocolState
				.of(bridge.getBuyerConnection().getConnectionURI(), ctx.getLinkedDataSource());
		Model conversation = agreementProtocolState.getConversationDataset().getUnionModel();
		final StringBuilder propUriStringbuilder = new StringBuilder();
		agreementProtocolState.getNthLatestMessage(m -> {
			if (m.getSenderNeedURI().equals(bridge.getBuyerConnection().getRemoteNeedURI()) && m.isRejectsMessage()) {
				m.getRejects().forEach(propUri -> {
					if (!propUriStringbuilder.toString().isEmpty()) {
						propUriStringbuilder.delete(0, propUriStringbuilder.toString().length());
					}
					propUriStringbuilder.append(propUri.toString());
				});
				return true;
			}
			return false;
		}, 1);
		String proposalUri = propUriStringbuilder.toString();
		String originUri = conversation
				.listStatements(new ResourceImpl(proposalUri), WONAGR.PROPOSES, (RDFNode) null)
				.next()
				.getObject()
				.asResource()
				.toString();

		// Propose it again
		Model proposalModel = WonRdfUtils.MessageUtils.proposesMessage(new URI(originUri));
		proposalModel = WonRdfUtils.MessageUtils.addMessage(proposalModel,
				"Merchant rejected a revision of the payment."
						+ " Accept the same old payment to receive the PayPal link to execute it.");

		// Merchant response
		Model merchantResponseModel = WonRdfUtils.MessageUtils
				.textMessage("Proposed the same payment again to the buyer.");

		ctx.getEventBus().publish(new ConnectionMessageCommandEvent(bridge.getBuyerConnection(), proposalModel));
		ctx.getEventBus().publish(new ConnectionMessageCommandEvent(con, merchantResponseModel));
	}

	/**
	 * Buyer denied the paymodel, so we propose the merchant to cancel both, the
	 * paymodel and the pp, so he can start over again ...
	 * 
	 * @context Buyer.
	 * @param con
	 */
	private void buyerDenied(Connection con) {
		EventListenerContext ctx = getEventListenerContext();
		PaymentBridge bridge = PaypalBotContextWrapper.instance(ctx).getOpenBridge(con.getNeedURI());
		bridge.setStatus(PaymentStatus.BUYER_DENIED);
		PaypalBotContextWrapper.instance(ctx).putOpenBridge(bridge.getMerchantConnection().getNeedURI(), bridge);

		// Cancelation of paymodel and pp
		AgreementProtocolState agreementProtocolState = AgreementProtocolState
				.of(bridge.getMerchantConnection().getConnectionURI(), ctx.getLinkedDataSource());
		Model conversation = agreementProtocolState.getConversationDataset().getUnionModel();
		List<URI> proposalsToCancelUris = new LinkedList<>();
		agreementProtocolState.getAgreementUris().forEach(propUri -> {
			proposalsToCancelUris.add(propUri);
		});

		Model merchantResponse = WonRdfUtils.MessageUtils.proposesToCancelMessage(proposalsToCancelUris.get(0),
				proposalsToCancelUris.get(1));
		merchantResponse = WonRdfUtils.MessageUtils.addMessage(merchantResponse,
				"Buyer denied this payment. You want to edit the paymodel?");
		ctx.getEventBus().publish(new ConnectionMessageCommandEvent(bridge.getMerchantConnection(), merchantResponse));

	}

	private void retractPaymentSummary(Connection con, URI paymentSummaryUri) {
		AgreementProtocolState agreementProtocolState = AgreementProtocolState.of(con.getConnectionURI(),
				getEventListenerContext().getLinkedDataSource());

		Model proposalModel = agreementProtocolState.getRejectedProposal(paymentSummaryUri);

		StmtIterator itr = proposalModel.listStatements(null, RDF.type, WONPAY.PAYMENT_SUMMARY);
		if (!itr.hasNext()) {
			return;
		}
		Resource paymentSummary = itr.next().getSubject();

		try {
			Model retractResponse = WonRdfUtils.MessageUtils.retractsMessage(new URI(paymentSummary.getURI()));
			getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(con, retractResponse));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

	}

}
