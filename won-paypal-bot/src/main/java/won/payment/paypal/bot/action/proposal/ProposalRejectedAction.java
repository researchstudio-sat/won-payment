package won.payment.paypal.bot.action.proposal;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.payment.paypal.bot.event.ConversationAnalyzationCommandEvent;
import won.payment.paypal.bot.event.MessageRetractedEvent;
import won.payment.paypal.bot.event.ProposalRejectedEvent;
import won.payment.paypal.bot.impl.PaypalBotContextWrapper;
import won.payment.paypal.bot.model.PaymentBridge;
import won.payment.paypal.bot.model.PaymentStatus;
import won.protocol.agreement.AgreementProtocolState;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONAGR;
import won.protocol.vocabulary.WONMSG;

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

			if (bridge.getStatus() != PaymentStatus.BUILDING) {
				return;
			}

			retractPaymentSummary(rejectsEvent);
		}		
	}
	
	private void retractPaymentSummary(ProposalRejectedEvent event) {
		AgreementProtocolState agreementProtocolState = AgreementProtocolState.of(event.getConnectionURI(),
				getEventListenerContext().getLinkedDataSource());
		
		Model proposalModel = agreementProtocolState.getRejectedProposal(event.getProposalUri());
		
		StmtIterator itr = proposalModel.listStatements(null, WON.HAS_TEXT_MESSAGE, "Payment summary");
		if (!itr.hasNext()) {
			return;
		}
		Resource paymentSummary = itr.next().getSubject();
		
		try {
			Model retractResponse = WonRdfUtils.MessageUtils.retractsMessage(new URI(paymentSummary.getURI()));
			getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(event.getCon(), retractResponse));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
	}

}
