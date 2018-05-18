package won.payment.paypal.bot.impl;

import won.bot.framework.bot.base.FactoryBot;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.behaviour.BotBehaviour;
import won.bot.framework.eventbot.behaviour.EagerlyPopulateCacheBehaviour;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.impl.factory.FactoryHintEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherNeedEvent;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.payment.paypal.bot.action.CreateFactoryOfferAction;
import won.payment.paypal.bot.action.MerchantMessageReceiverAction;

public class PaypalBot extends FactoryBot {

	@Override
	protected void initializeFactoryEventListeners() {

		EventBus bus = getEventBus();
		EventListenerContext ctx = getEventListenerContext();

		// eagerly cache RDF data
		BotBehaviour eagerlyCacheBehaviour = new EagerlyPopulateCacheBehaviour(ctx);
		eagerlyCacheBehaviour.activate();

		// Factory Hint Event
		bus.subscribe(FactoryHintEvent.class,
				new ActionOnEventListener(ctx, "FactoryHintEvent", new CreateFactoryOfferAction(ctx)));

		// Communication with the Merchant
		bus.subscribe(MessageFromOtherNeedEvent.class,
				new ActionOnEventListener(ctx, new MerchantMessageReceiverAction(ctx)));

	}

}
