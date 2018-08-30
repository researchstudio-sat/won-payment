package won.payment.paypal.bot.action.effect;

import java.net.URI;
import java.util.Set;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.jena.rdf.model.Model;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.analyzation.agreement.ProposalAcceptedEvent;
import won.bot.framework.eventbot.event.impl.analyzation.proposal.ProposalReceivedEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.WonMessageReceivedOnConnectionEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.payment.paypal.bot.event.ConversationAnalyzationCommandEvent;
import won.payment.paypal.bot.event.MessageRetractedEvent;
import won.payment.paypal.bot.event.ProposalRejectedEvent;
import won.payment.paypal.bot.impl.PaypalBotContextWrapper;
import won.protocol.agreement.AgreementProtocolState;
import won.protocol.agreement.effect.MessageEffect;
import won.protocol.agreement.effect.MessageEffectType;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;

/**
 * Analyzes all incoming messages and breaks down
 * the message effects (accepts, proposes, rejects,
 * retracts) and publishes suitable events for them.
 * 
 * @author schokobaer
 *
 */
public class MessageEffectBrokerAction extends BaseEventBotAction {

	public MessageEffectBrokerAction(EventListenerContext eventListenerContext) {
		super(eventListenerContext);
	}

	@Override
	protected void doRun(Event event, EventListener executingListener) throws Exception {

		EventListenerContext ctx = getEventListenerContext();
		if (ctx.getBotContextWrapper() instanceof PaypalBotContextWrapper
				&& event instanceof MessageFromOtherNeedEvent) {

			MessageFromOtherNeedEvent messageEvent = (MessageFromOtherNeedEvent) event;
			
			// Analyze for message effects
			if(!breakDownEffects((BaseNeedAndConnectionSpecificEvent) event)) {
				ctx.getEventBus().publish(new ConversationAnalyzationCommandEvent(messageEvent.getCon()));
			}
		}

	}

	/**
	 * Finds out the message effects of the incoming message and publishes suitable
	 * events. If no message effects are available it returns false, otherwise true.
	 * 
	 * @param event
	 * @return true if message effects are available otherwise false.
	 */
	private boolean breakDownEffects(BaseNeedAndConnectionSpecificEvent event) {

		EventListenerContext ctx = getEventListenerContext();

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
		if (wonMessage == null) {
			return false;
		}
		AgreementProtocolState agreementProtocolState = AgreementProtocolState.of(connectionUri,
				getEventListenerContext().getLinkedDataSource());
		Set<MessageEffect> messageEffects = agreementProtocolState.getEffects(wonMessage.getMessageURI());
		MutableBoolean result = new MutableBoolean(false);
		messageEffects.forEach(messageEffect -> {
			result.setValue(true);
			if (messageEffect.getType().equals(MessageEffectType.ACCEPTS)) {
				Model agreementPayload = agreementProtocolState
						.getAgreement(messageEffect.asAccepts().getAcceptedMessageUri());
				if (!agreementPayload.isEmpty()) {
					ctx.getEventBus().publish(new ProposalAcceptedEvent(connection,
							messageEffect.asAccepts().getAcceptedMessageUri(), agreementPayload));
				}
			} else if (messageEffect.getType().equals(MessageEffectType.PROPOSES)) {
				ctx.getEventBus()
						.publish(new ProposalReceivedEvent(connection, (WonMessageReceivedOnConnectionEvent) event));
			} else if (messageEffect.getType().equals(MessageEffectType.REJECTS)) {
				URI uri = messageEffect.asRejects().getRejectedMessageUri();
				ctx.getEventBus().publish(new ProposalRejectedEvent(connection, uri));
			} else if (messageEffect.getType().equals(MessageEffectType.RETRACTS)) {
				URI uri = messageEffect.asRetracts().getRetractedMessageUri();
				ctx.getEventBus().publish(new MessageRetractedEvent(connection, uri));
			}
		});
		
		return result.booleanValue();

	}

	private static Connection makeConnection(URI needURI, URI remoteNeedURI, URI connectionURI) {
		Connection con = new Connection();
		con.setConnectionURI(connectionURI);
		con.setNeedURI(needURI);
		con.setRemoteNeedURI(remoteNeedURI);
		return con;
	}

}
