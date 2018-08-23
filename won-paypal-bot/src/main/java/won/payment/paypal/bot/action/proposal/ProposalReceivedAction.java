package won.payment.paypal.bot.action.proposal;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.behaviour.AnalyzeBehaviour;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.analyzation.proposal.ProposalReceivedEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.message.WonMessageType;

/**
 * TODO: DELETE ME
 * @author schokobaer
 *
 */
@Deprecated
public class ProposalReceivedAction extends BaseEventBotAction {
	
	public ProposalReceivedAction(EventListenerContext eventListenerContext, AnalyzeBehaviour analyzeBehaviour) {
		super(eventListenerContext);
	}
	
	@Override
	protected void doRun(Event event, EventListener executingListener) throws Exception {
		
		ProposalReceivedEvent proposalReceivedEvent = (ProposalReceivedEvent) event;
				
		String proposeType = proposalReceivedEvent.isMixed() ? "mixed" :
							proposalReceivedEvent.hasProposesEvents() ? "proposes" :
								"cancel" ;
		
		logger.info("Received " + proposeType + " Proposal");

	}

}
