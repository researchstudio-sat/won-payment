package won.payment.paypal.bot.action;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.behaviour.AnalyzeBehaviour;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionMetEvent;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionUnmetEvent;
import won.bot.framework.eventbot.listener.EventListener;

public class StubAction extends BaseEventBotAction {

	public StubAction(EventListenerContext eventListenerContext) {
		super(eventListenerContext);
	}
	
	@Override
	protected void doRun(Event event, EventListener executingListener) throws Exception {
		logger.info("Received a {}", event.getClass().getSimpleName());
		
//		if (event instanceof PreconditionUnmetEvent) {
//			logger.info("Condition Unmet");
//		} else if (event instanceof PreconditionMetEvent) {
//			logger.info("Condition Met");
//		}
		
	}

}
