package won.payment.paypal.bot.action.precondition;

import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.analyzation.agreement.ProposalAcceptedEvent;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionMetEvent;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionUnmetEvent;
import won.bot.framework.eventbot.event.impl.analyzation.proposal.ProposalReceivedEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.WonMessageReceivedOnConnectionEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.payment.paypal.bot.impl.PaypalBotContextWrapper;
import won.payment.paypal.bot.model.PaymentBridge;
import won.payment.paypal.bot.model.PaymentStatus;
import won.protocol.agreement.AgreementProtocolState;
import won.protocol.agreement.effect.MessageEffect;
import won.protocol.agreement.effect.MessageEffectType;
import won.protocol.agreement.effect.ProposalType;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.util.NeedModelWrapper;
import won.protocol.util.WonConversationUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.utils.goals.GoalInstantiationProducer;
import won.utils.goals.GoalInstantiationResult;

/**
 * This action executes an analyzation of the messages in the connection if the
 * goal is not met yet. Then the shaql report is responded into the connection.
 * 
 * @author schokobaer
 *
 */
public class GoalAnalyzationAction extends BaseEventBotAction {

	public GoalAnalyzationAction(EventListenerContext eventListenerContext) {
		super(eventListenerContext);
	}

	@Override
	protected void doRun(Event event, EventListener executingListener) throws Exception {

		EventListenerContext ctx = getEventListenerContext();
		if (ctx.getBotContextWrapper() instanceof PaypalBotContextWrapper
				&& event instanceof BaseNeedAndConnectionSpecificEvent) {
			Connection con = ((BaseNeedAndConnectionSpecificEvent) event).getCon();
			PaymentBridge bridge = PaypalBotContextWrapper.paymentBridge(ctx, con);

			if (bridge.getStatus().ordinal() > PaymentStatus.BUILDING.ordinal()) {
				//return;
			}

			// Analyze for precondition met / unmet
			analyze((BaseNeedAndConnectionSpecificEvent) event);

		}
	}

	/**
	 * Analyzes if the shape if confirm. Then publishes a
	 * {@link PreconditionMetEvent} or a {@link PreconditionUnmetEvent}.
	 * If the incomming event was a special event like a accept then analyzation
	 * is stopped and an appropriate event is published.
	 * 
	 * @param event
	 */
	private void analyze(BaseNeedAndConnectionSpecificEvent event) {
		EventListenerContext ctx = getEventListenerContext();

		LinkedDataSource linkedDataSource = ctx.getLinkedDataSource();

		URI needUri = event.getNeedURI();
		URI remoteNeedUri = event.getRemoteNeedURI();
		URI connectionUri = event.getConnectionURI();
		Connection connection = makeConnection(needUri, remoteNeedUri, connectionUri);
		WonMessage wonMessage = event instanceof ConnectionMessageCommandSuccessEvent
				? ((ConnectionMessageCommandSuccessEvent) event).getWonMessage()
				: event instanceof WonMessageReceivedOnConnectionEvent
						? ((WonMessageReceivedOnConnectionEvent) event).getWonMessage()
						: null;

		// Check for message effects
		if (wonMessage != null) {
			MutableBoolean stopAnalyzation = new MutableBoolean(false);
			AgreementProtocolState agreementProtocolState = AgreementProtocolState.of(connectionUri,
					getEventListenerContext().getLinkedDataSource());
			Set<MessageEffect> messageEffects = agreementProtocolState.getEffects(wonMessage.getMessageURI());
			messageEffects.forEach(messageEffect -> {
				if (messageEffect.getType().equals(MessageEffectType.ACCEPTS)) {
					Model agreementPayload = agreementProtocolState
							.getAgreement(messageEffect.asAccepts().getAcceptedMessageUri());
					if (!agreementPayload.isEmpty()) {
						stopAnalyzation.setValue(true);
						ctx.getEventBus().publish(new ProposalAcceptedEvent(connection,
								messageEffect.asAccepts().getAcceptedMessageUri(), agreementPayload));
					}
				}
				else if (messageEffect.getType().equals(MessageEffectType.PROPOSES)) {
					stopAnalyzation.setValue(true);
                    ctx.getEventBus().publish(new ProposalReceivedEvent(connection, (WonMessageReceivedOnConnectionEvent) event));
				}
				else if (messageEffect.getType().equals(MessageEffectType.REJECTS)) {
					stopAnalyzation.setValue(true);
					// TODO: Publish a ProposalRejectedEvent (not existing yet)
                    // ctx.getEventBus().publish(null);
				}
			});
			
			if (stopAnalyzation.booleanValue()) {
				return;
			}
		}

		Dataset needDataset = linkedDataSource.getDataForResource(needUri);
		Collection<Resource> goalsInNeed = new NeedModelWrapper(needDataset).getGoals();

		// Things to do for each individual message regardless of it being received or
		// sent
		Dataset remoteNeedDataset = ctx.getLinkedDataSource().getDataForResource(remoteNeedUri);
		Dataset conversationDataset = null; // Initialize with null, to ensure some form of lazy init for the
											// conversationDataset
		GoalInstantiationProducer goalInstantiationProducer = null;

		for (Resource goal : goalsInNeed) {
			String preconditionUri = getUniqueGoalId(goal, needDataset, connectionUri);

			conversationDataset = WonConversationUtils.getAgreementProtocolState(connectionUri, linkedDataSource)
					.getConversationDataset();
			goalInstantiationProducer = getGoalInstantiationProducerLazyInit(goalInstantiationProducer, needDataset,
					remoteNeedDataset, conversationDataset);

			GoalInstantiationResult result = goalInstantiationProducer.findInstantiationForGoal(goal);

			if (result.getShaclReportWrapper().isConform()) {
				ctx.getEventBus().publish(new PreconditionMetEvent(connection, preconditionUri, result));
			} else {
				ctx.getEventBus().publish(new PreconditionUnmetEvent(connection, preconditionUri, result));
			}
		}

	}

	private static String getUniqueGoalId(Resource goal, Dataset needDataset, URI connectionURI) {
		if (goal.getURI() != null) {
			return goal.getURI();
		} else {
			NeedModelWrapper needWrapper = new NeedModelWrapper(needDataset);

			Model dataModel = needWrapper.getDataGraph(goal);
			Model shapesModel = needWrapper.getShapesGraph(goal);
			String strGraphs = "";

			if (dataModel != null) {
				StringWriter sw = new StringWriter();
				RDFDataMgr.write(sw, dataModel, Lang.NQUADS);
				String content = sw.toString();
				String dataGraphName = needWrapper.getDataGraphName(goal);
				strGraphs += replaceBlankNode(content, dataGraphName);
			}

			if (shapesModel != null) {
				StringWriter sw = new StringWriter();
				RDFDataMgr.write(sw, shapesModel, Lang.NQUADS);
				String content = sw.toString();
				String shapesGraphName = needWrapper.getShapesGraphName(goal);
				strGraphs += replaceBlankNode(content, shapesGraphName);
			}

			String[] statements = strGraphs.split("\n");
			Arrays.sort(statements);
			String strStatements = Arrays.toString(statements);
			// java.security.MessageDigest -> SHA256

			try {
				MessageDigest digest = MessageDigest.getInstance("SHA-256");
				byte[] hash = digest.digest(strStatements.getBytes(StandardCharsets.UTF_8));
				String strHash = new String(Base64.getEncoder().encode(hash));
				return strHash;
			} catch (NoSuchAlgorithmException e) {
				return strStatements;
			}

		}
	}

	private static String replaceBlankNode(String strModel, String replaceUri) {

		while (strModel.contains("_:")) {
			int pos = strModel.indexOf("_:");
			int end = pos + 35;
			strModel = strModel.substring(0, pos) + replaceUri + strModel.substring(end);
		}

		return strModel;
	}

	private GoalInstantiationProducer getGoalInstantiationProducerLazyInit(
			GoalInstantiationProducer goalInstantiationProducer, Dataset needDataset, Dataset remoteNeedDataset,
			Dataset conversationDataset) {
		if (goalInstantiationProducer == null) {
			return new GoalInstantiationProducer(needDataset, remoteNeedDataset, conversationDataset,
					"http://example.org/", "http://example.org/blended/");
		} else {
			return goalInstantiationProducer;
		}
	}

	private static Connection makeConnection(URI needURI, URI remoteNeedURI, URI connectionURI) {
		Connection con = new Connection();
		con.setConnectionURI(connectionURI);
		con.setNeedURI(needURI);
		con.setRemoteNeedURI(remoteNeedURI);
		return con;
	}

}
