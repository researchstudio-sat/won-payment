package won.payment.paypal.bot.action;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.listener.EventListener;

/**
 * Just logs which event was received.
 * 
 * @author schokobaer
 */
public class StubAction extends BaseEventBotAction {
    public StubAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        logger.info("Received a {}", event.getClass().getSimpleName());
    }
}
