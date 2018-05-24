package won.payment.paypal.bot.impl;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import won.bot.framework.bot.base.FactoryBot;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.EventBotAction;
import won.bot.framework.eventbot.behaviour.BotBehaviour;
import won.bot.framework.eventbot.behaviour.EagerlyPopulateCacheBehaviour;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.impl.analyzation.agreement.ProposalAcceptedEvent;
import won.bot.framework.eventbot.event.impl.analyzation.proposal.ProposalReceivedEvent;
import won.bot.framework.eventbot.event.impl.analyzation.proposal.ProposalSentEvent;
import won.bot.framework.eventbot.event.impl.factory.FactoryHintEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.OpenFromOtherNeedEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.payment.paypal.bot.action.BuyerMessageReceiverAction;
import won.payment.paypal.bot.action.ConnectionDenyerAction;
import won.payment.paypal.bot.action.CreateFactoryOfferAction;
import won.payment.paypal.bot.action.MessageBrokerAction;
import won.payment.paypal.bot.model.PaymentBridge;
import won.payment.paypal.bot.scheduler.PaypalPaymentStatusCheckSchedule;
import won.payment.paypal.bot.action.MerchantMessageReceiverAction;
import won.payment.paypal.service.impl.PaypalPaymentService;

public class PaypalBot extends FactoryBot {

	private static final Long SCHEDULER_INTERVAL = 60 * 1000L;
	
	private Map<URI, PaymentBridge> openBridges = new HashMap<>();
	private Timer paymentCheckTimer;
	
	@Override
	protected void initializeFactoryEventListeners() {
		
		EventBus bus = getEventBus();
		EventListenerContext ctx = getEventListenerContext();

		// eagerly cache RDF data
//		BotBehaviour eagerlyCacheBehaviour = new EagerlyPopulateCacheBehaviour(ctx);
//		eagerlyCacheBehaviour.activate();

		// Factory Hint Event
		bus.subscribe(FactoryHintEvent.class,
				new ActionOnEventListener(ctx, "FactoryHintEvent", new CreateFactoryOfferAction(ctx, openBridges)));

		// Broker for Merchant and Buyer Messages
		EventBotAction merchantAction = new MerchantMessageReceiverAction(ctx, openBridges);
		EventBotAction buyerAction = new BuyerMessageReceiverAction(ctx, openBridges);
		EventListener broker = new ActionOnEventListener(ctx, new MessageBrokerAction(ctx, openBridges, 
				merchantAction, buyerAction));
		bus.subscribe(MessageFromOtherNeedEvent.class, broker);
		bus.subscribe(OpenFromOtherNeedEvent.class, broker);
		
		// If someone wants to connect to a instance
		// Need then send a deny message and close the connection
		bus.subscribe(ConnectFromOtherNeedEvent.class, 
				new ActionOnEventListener(ctx, new ConnectionDenyerAction(ctx)));
		
		
		// Start PaypalPaymentStatusCheckScheduler
		PaypalPaymentStatusCheckSchedule statusScheduler = new PaypalPaymentStatusCheckSchedule(getEventListenerContext(), openBridges);
		paymentCheckTimer = new Timer(true);
		paymentCheckTimer.scheduleAtFixedRate(statusScheduler, SCHEDULER_INTERVAL, SCHEDULER_INTERVAL);
	}

}
