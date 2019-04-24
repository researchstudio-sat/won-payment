package won.payment.paypal.bot.action.analyze;

import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.BaseAtomAndConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionMetEvent;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionUnmetEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.payment.paypal.bot.event.analyze.ConversationAnalyzationCommandEvent;
import won.payment.paypal.bot.impl.PaypalBotContextWrapper;
import won.protocol.model.Connection;
import won.protocol.util.AtomModelWrapper;
import won.protocol.util.WonConversationUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.utils.goals.GoalInstantiationProducer;
import won.utils.goals.GoalInstantiationResult;

/**
 * This action executes an analyzation of the messages in the connection. It
 * publishes {@link PreconditionMetEvent} and {@link PreconditionUnmetEvent}
 * events.
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
				&& event instanceof ConversationAnalyzationCommandEvent) {
			
			// Analyze for precondition met / unmet
			analyze((BaseAtomAndConnectionSpecificEvent) event);

		}
	}

	/**
	 * Analyzes if the shape if confirm. Then publishes a
	 * {@link PreconditionMetEvent} or a {@link PreconditionUnmetEvent}.
	 * 
	 * @param event
	 */
	private void analyze(BaseAtomAndConnectionSpecificEvent event) {
		EventListenerContext ctx = getEventListenerContext();

		LinkedDataSource linkedDataSource = ctx.getLinkedDataSource();

		URI atomUri = event.getAtomURI();
		URI targetAtomUri = event.getTargetAtomURI();
		URI connectionUri = event.getConnectionURI();
		Connection connection = makeConnection(atomUri, targetAtomUri, connectionUri);
		

		Dataset atomDataset = linkedDataSource.getDataForResource(atomUri);
		Collection<Resource> goalsInAtom = new AtomModelWrapper(atomDataset).getGoals();

		// Things to do for each individual message regardless of it being received or
		// sent
		Dataset targetAtomDataset = ctx.getLinkedDataSource().getDataForResource(targetAtomUri);
		Dataset conversationDataset = null; // Initialize with null, to ensure some form of lazy init for the
											// conversationDataset
		GoalInstantiationProducer goalInstantiationProducer = null;

		for (Resource goal : goalsInAtom) {
			String preconditionUri = getUniqueGoalId(goal, atomDataset, connectionUri);

			conversationDataset = WonConversationUtils.getAgreementProtocolState(connectionUri, linkedDataSource)
					.getConversationDataset();
			goalInstantiationProducer = getGoalInstantiationProducerLazyInit(goalInstantiationProducer, atomDataset,
					targetAtomDataset, conversationDataset);

			GoalInstantiationResult result = goalInstantiationProducer.findInstantiationForGoal(goal);

			if (result.getShaclReportWrapper().isConform()) {
				ctx.getEventBus().publish(new PreconditionMetEvent(connection, preconditionUri, result));
			} else {
				ctx.getEventBus().publish(new PreconditionUnmetEvent(connection, preconditionUri, result));
			}
		}

	}

	private static String getUniqueGoalId(Resource goal, Dataset atomDataset, URI connectionURI) {
		if (goal.getURI() != null) {
			return goal.getURI();
		} else {
			AtomModelWrapper atomWrapper = new AtomModelWrapper(atomDataset);

			Model dataModel = atomWrapper.getDataGraph(goal);
			Model shapesModel = atomWrapper.getShapesGraph(goal);
			String strGraphs = "";

			if (dataModel != null) {
				StringWriter sw = new StringWriter();
				RDFDataMgr.write(sw, dataModel, Lang.NQUADS);
				String content = sw.toString();
				String dataGraphName = atomWrapper.getDataGraphName(goal);
				strGraphs += replaceBlankNode(content, dataGraphName);
			}

			if (shapesModel != null) {
				StringWriter sw = new StringWriter();
				RDFDataMgr.write(sw, shapesModel, Lang.NQUADS);
				String content = sw.toString();
				String shapesGraphName = atomWrapper.getShapesGraphName(goal);
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
			GoalInstantiationProducer goalInstantiationProducer, Dataset atomDataset, Dataset targetAtomDataset,
			Dataset conversationDataset) {
		if (goalInstantiationProducer == null) {
			return new GoalInstantiationProducer(atomDataset, targetAtomDataset, conversationDataset,
					"http://example.org/", "http://example.org/blended/");
		} else {
			return goalInstantiationProducer;
		}
	}

	private static Connection makeConnection(URI atomURI, URI targetAtomURI, URI connectionURI) {
		Connection con = new Connection();
		con.setConnectionURI(connectionURI);
		con.setAtomURI(atomURI);
		con.setTargetAtomURI(targetAtomURI);
		return con;
	}

}
