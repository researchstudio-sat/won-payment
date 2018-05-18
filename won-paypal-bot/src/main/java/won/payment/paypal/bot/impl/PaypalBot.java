package won.payment.paypal.bot.impl;

import won.bot.framework.bot.base.FactoryBot;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.behaviour.AnalyzeBehaviour;
import won.bot.framework.eventbot.behaviour.BotBehaviour;
import won.bot.framework.eventbot.behaviour.EagerlyPopulateCacheBehaviour;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.impl.factory.FactoryHintEvent;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.payment.paypal.bot.action.CreateFactoryOfferAction;

public class PaypalBot extends FactoryBot {

	@Override
	protected void initializeFactoryEventListeners() {

		EventBus bus = getEventBus();
		EventListenerContext ctx = getEventListenerContext();
		
		AnalyzeBehaviour analyzeBehaviour = new AnalyzeBehaviour(ctx);
        analyzeBehaviour.activate();

        //eagerly cache RDF data
        BotBehaviour eagerlyCacheBehaviour = new EagerlyPopulateCacheBehaviour(ctx);
        eagerlyCacheBehaviour.activate();
		
		//Other Events
        bus.subscribe(FactoryHintEvent.class,
                new ActionOnEventListener(
                    ctx,
                    "FactoryHintEvent",
                    new CreateFactoryOfferAction(ctx)
                )
            );
		
	}

}
