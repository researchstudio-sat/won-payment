package won.payment.paypal.bot.util;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.springframework.util.StopWatch;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.behaviour.CrawlConnectionDataBehaviour;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.crawlconnection.CrawlConnectionCommandEvent;
import won.protocol.agreement.AgreementProtocolState;
import won.protocol.model.Connection;
import won.protocol.util.WonConversationUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONPAY;

public class EventCrawler {

	public interface MessageFinder {
		List<URI> findMessages(AgreementProtocolState state);
	}

	public interface MessageReferrer {
		Model referToMessages(Model messageModel, URI... targetUris);
	}

	public interface TextMessageMaker {
		String makeTextMessage(Duration queryDuration, AgreementProtocolState state, URI... uris);
	}

	public static void referToEarlierMessages(EventListenerContext ctx, EventBus bus, Connection con,
			MessageFinder messageFinder, MessageReferrer messageReferrer, TextMessageMaker textMessageMaker) {
		// initiate crawl behaviour
		CrawlConnectionCommandEvent command = new CrawlConnectionCommandEvent(con.getNeedURI(), con.getConnectionURI());
		CrawlConnectionDataBehaviour crawlConnectionDataBehaviour = new CrawlConnectionDataBehaviour(ctx, command,
				Duration.ofSeconds(60));
		final StopWatch crawlStopWatch = new StopWatch();
		crawlStopWatch.start("crawl");
		AgreementProtocolState state = WonConversationUtils.getAgreementProtocolState(con.getConnectionURI(),
				ctx.getLinkedDataSource());
		crawlStopWatch.stop();
		Duration crawlDuration = Duration.ofMillis(crawlStopWatch.getLastTaskTimeMillis());
		// Model messageModel = WonRdfUtils.MessageUtils
		// .textMessage("Finished crawl in " + getDurationString(crawlDuration) + "
		// seconds. The dataset has "
		// + state.getConversationDataset().asDatasetGraph().size() + " rdf graphs.");
		// eventListenerContext.getEventBus().publish(new
		// ConnectionMessageCommandEvent(con, messageModel));
		Model messageModel = makeReferringMessage(state, messageFinder, messageReferrer, textMessageMaker);
		ctx.getEventBus().publish(new ConnectionMessageCommandEvent(con, messageModel));
		crawlConnectionDataBehaviour.activate();
	}

	private static  Model makeReferringMessage(AgreementProtocolState state, MessageFinder messageFinder,
			MessageReferrer messageReferrer, TextMessageMaker textMessageMaker) {
		int origPrio = Thread.currentThread().getPriority();
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		StopWatch queryStopWatch = new StopWatch();
		queryStopWatch.start("query");
		List<URI> targetUris = messageFinder.findMessages(state);
		URI[] targetUriArray = targetUris.toArray(new URI[targetUris.size()]);
		queryStopWatch.stop();
		Thread.currentThread().setPriority(origPrio);
		Duration queryDuration = Duration.ofMillis(queryStopWatch.getLastTaskTimeMillis());
		Model messageModel = WonRdfUtils.MessageUtils
				.textMessage(textMessageMaker.makeTextMessage(queryDuration, state, targetUriArray));
		return messageReferrer.referToMessages(messageModel, targetUriArray);
	}
	
	public static void propose(EventListenerContext ctx, EventBus bus, Connection con, boolean allowOwnClauses,
			boolean allowCounterpartClauses, int count) {
//		try {
//			Thread.sleep(PROPOSAL_WAIT_TIME);
//		} catch (InterruptedException e) {
//
//		}
		referToEarlierMessages(ctx, bus, con, state -> {
			return state.getNLatestMessageUris(m -> {
				URI ownNeedUri = con.getNeedURI();
				URI remoteNeedUri = con.getRemoteNeedURI();
				return ownNeedUri != null && ownNeedUri.equals(m.getSenderNeedURI()) && allowOwnClauses
						|| remoteNeedUri != null && remoteNeedUri.equals(m.getSenderNeedURI())
								&& allowCounterpartClauses;

			}, count).subList(0, count);
		}, (messageModel, uris) -> WonRdfUtils.MessageUtils.addProposes(messageModel, uris),
				(Duration queryDuration, AgreementProtocolState state, URI... uris) -> {
					if (uris == null || uris.length == 0 || uris[0] == null) {
						return "Sorry, I cannot propose the messages - I did not find any.";
					}
					Optional<String> proposedString = state.getTextMessage(uris[0]);
					return proposedString.get();
				});
	}

	public static void accept(EventListenerContext ctx, EventBus bus, Connection con) {
		referToEarlierMessages(ctx, bus, con, state -> {
			URI uri = state.getLatestPendingProposal(Optional.empty(), Optional.of(con.getRemoteNeedURI()));
			return uri == null ? Collections.EMPTY_LIST : Arrays.asList(uri);
		}, (messageModel, uris) -> WonRdfUtils.MessageUtils.addAccepts(messageModel, uris),
				(Duration queryDuration, AgreementProtocolState state, URI... uris) -> {
					if (uris == null || uris.length == 0 || uris[0] == null) {
						return "Sorry, I cannot accept any proposal - I did not find pending proposals";
					}
					return "Ok, I am hereby accepting your latest proposal (uri: " + uris[0] + ").";
				});
	}
	
	
	/**
	 * Crawls the events in the connection and searchs for payment details.
	 * 
	 * @param con
	 *            Connection to crawl through
	 * @param ctx
	 *            Context to get the data source
	 * @return Key Value Map with payment Details
	 */
	public static Map<String, String> crawlPaymentDetails(Connection con, EventListenerContext ctx) {
		Map<String, String> payDetails = new LinkedHashMap<>();
		AgreementProtocolState state = WonConversationUtils.getAgreementProtocolState(con.getConnectionURI(),
				ctx.getLinkedDataSource());

		// Sparql method
		// Dataset datasource = state.getConversationDataset();
		// String query = "select ?o \n"
		// + "where { \n"
		// + " ?s ?p ?o \n"
		// + "}";
		// try (QueryExecution qexec= QueryExecutionFactory.create(query, datasource)) {
		// ResultSet resultSet = qexec.execSelect();
		// while (resultSet.hasNext()) {
		// QuerySolution soln = resultSet.nextSolution();
		// RDFNode node = soln.get("o");
		// System.out.println(node.asLiteral().getString());
		// }
		// return payDetails;
		// }
		// catch (Exception e) {
		// e.printStackTrace();
		// }

		// Safe method
		Dataset dataset = state.getAgreements();
		Model agreements = dataset.getUnionModel();
		StmtIterator iterator = agreements.listStatements();
		while (iterator.hasNext()) {
			Statement stmt = iterator.next();
			Property prop = stmt.getPredicate();
			if (prop.equals(WON.HAS_TEXT_MESSAGE)) {
				RDFNode obj = stmt.getObject();
				String text = obj.asLiteral().getString();
				String[] parts = text.split(";");
				for (String field : parts) {
					field = field.trim();
					if (field.startsWith("pay_")) {
						int posEnd = field.indexOf(":", 4);
						if (posEnd > 0) {
							String key = field.substring(0, posEnd).toLowerCase();
							String val = field.substring(posEnd + 1).trim();
//							if (!payDetails.containsKey(key)) {
								payDetails.put(key, val);
//							}
						}
					}
				}
			}
		}
		return payDetails;
	}
	
	public static Resource getLastPaymentEvent(Connection con, EventListenerContext ctx) {
		AgreementProtocolState state = WonConversationUtils.getAgreementProtocolState(con.getConnectionURI(),
				ctx.getLinkedDataSource());
		Dataset dataset = state.getAgreements();
		Model agreements = dataset.getUnionModel();
		StmtIterator iterator = agreements.listStatements();
		while (iterator.hasNext()) {
			Statement stmt = iterator.next();
			Property prop = stmt.getPredicate();
			if (prop.equals(RDF.type) && stmt.getObject().isResource() &&
					stmt.getObject().asResource().equals(WONPAY.PAYPAL_PAYMENT)) {
				return stmt.getSubject();
			}
		}
		return null;
	}
	
	public static Resource getLatestPaymentPayKey(Connection con, EventListenerContext ctx) {
		Dataset dataset = WonLinkedDataUtils.getConversationAndNeedsDataset(con.getConnectionURI(), ctx.getLinkedDataSource());
		Model data = dataset.getUnionModel();
		
		StmtIterator iterator = data.listStatements();
		while (iterator.hasNext()) {
			Statement stmt = iterator.next();
			Property prop = stmt.getPredicate();
			if (prop.equals(WONPAY.HAS_PAYPAL_TX_KEY)) {
				return stmt.getSubject();
			}
		}
		
		return null;
	}
	
}
