package won.payment.paypal.bot.action.proposal;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.behaviour.AnalyzeBehaviour;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.analyzation.proposal.ProposalReceivedEvent;
import won.bot.framework.eventbot.listener.EventListener;

/**
 * TODO: DELETE ME
 * @author schokobaer
 *
 */
public class ProposalReceivedAction extends BaseEventBotAction {

	private AnalyzeBehaviour analyzeBehaviour;
	
	public ProposalReceivedAction(EventListenerContext eventListenerContext, AnalyzeBehaviour analyzeBehaviour) {
		super(eventListenerContext);
		this.analyzeBehaviour = analyzeBehaviour;
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
