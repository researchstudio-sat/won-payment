package won.payment.paypal.bot.impl;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import won.bot.framework.bot.base.FactoryBot;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.EventBotAction;
import won.bot.framework.eventbot.behaviour.AnalyzeBehaviour;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.impl.analyzation.agreement.AgreementCancellationAcceptedEvent;
import won.bot.framework.eventbot.event.impl.analyzation.agreement.AgreementCancellationRequestedEvent;
import won.bot.framework.eventbot.event.impl.analyzation.agreement.ProposalAcceptedEvent;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionMetEvent;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionUnmetEvent;
import won.bot.framework.eventbot.event.impl.analyzation.proposal.ProposalReceivedEvent;
import won.bot.framework.eventbot.event.impl.factory.FactoryHintEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.CloseFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.OpenFromOtherNeedEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.payment.paypal.bot.action.BuyerMessageReceiverAction;
import won.payment.paypal.bot.action.ConnectionCloseAction;
import won.payment.paypal.bot.action.ConnectionDenierAction;
import won.payment.paypal.bot.action.CreateFactoryOfferAction;
import won.payment.paypal.bot.action.MerchantMessageReceiverAction;
import won.payment.paypal.bot.action.MessageBrokerAction;
import won.payment.paypal.bot.action.StubAction;
import won.payment.paypal.bot.action.agreement.PreconditionMetAction;
import won.payment.paypal.bot.action.precondition.PreconditionUnmetAction;
import won.payment.paypal.bot.model.PaymentBridge;
import won.payment.paypal.bot.scheduler.PaypalPaymentStatusCheckSchedule;
import won.payment.paypal.service.impl.PaypalPaymentService;

/**
 * The bot which subscribes for the Events.
 * 
 * @author schokobaer
 *
 */
public class PaypalBot extends FactoryBot {

	private static final Long SCHEDULER_INTERVAL = 60 * 1000L;

	private PaypalPaymentService paypalService;
	private Map<URI, PaymentBridge> openBridges = new HashMap<>();
	private Timer paymentCheckTimer;

	@Override
	protected void initializeFactoryEventListeners() {
		
		EventBus bus = getEventBus();
		EventListenerContext ctx = getEventListenerContext();

		AnalyzeBehaviour analyzeBehaviour = new AnalyzeBehaviour(ctx);
		analyzeBehaviour.activate();
		
		
		// eagerly cache RDF data
		// BotBehaviour eagerlyCacheBehaviour = new EagerlyPopulateCacheBehaviour(ctx);
		// eagerlyCacheBehaviour.activate();

		//Analyzation Events
        bus.subscribe(PreconditionMetEvent.class,
            new ActionOnEventListener(
                ctx,
                "PreconditionMetEvent",
                new PreconditionMetAction(ctx)
            )
        );

        bus.subscribe(PreconditionUnmetEvent.class,
            new ActionOnEventListener(
                ctx,
                "PreconditionUnmetEvent",
                new PreconditionUnmetAction(ctx)
            )
        );

        bus.subscribe(ProposalAcceptedEvent.class,
            new ActionOnEventListener(
                ctx,
                "ProposalAcceptedEvent",
                new StubAction(ctx)
            )
        );

        bus.subscribe(ProposalReceivedEvent.class,
             new ActionOnEventListener(
                 ctx,
                 "ProposalReceivedEvent",
                 new StubAction(ctx, analyzeBehaviour)
             )
        );

        bus.subscribe(AgreementCancellationAcceptedEvent.class,
            new ActionOnEventListener(
                ctx,
                "AgreementCancellationAcceptedEvent",
                new StubAction(ctx, analyzeBehaviour)
            )
        );

        bus.subscribe(AgreementCancellationRequestedEvent.class,
            new ActionOnEventListener(
                ctx,
                "AgreementCancellationAcceptedEvent",
                new StubAction(ctx, analyzeBehaviour)
            )
        );
		
		// Factory Hint Event
		bus.subscribe(FactoryHintEvent.class,
				new ActionOnEventListener(ctx, "FactoryHintEvent", new CreateFactoryOfferAction(ctx, openBridges)));

		// Broker for Merchant and Buyer Messages
		EventBotAction merchantAction = new MerchantMessageReceiverAction(ctx, openBridges);
		EventBotAction buyerAction = new BuyerMessageReceiverAction(ctx, openBridges, paypalService);
		EventListener broker = new ActionOnEventListener(ctx,
				new MessageBrokerAction(ctx, openBridges, merchantAction, buyerAction));
		//bus.subscribe(MessageFromOtherNeedEvent.class, broker);
		//bus.subscribe(OpenFromOtherNeedEvent.class, broker);

		// Client closes the connection
		bus.subscribe(CloseFromOtherNeedEvent.class,
				new ActionOnEventListener(ctx, new ConnectionCloseAction(ctx, openBridges)));

		// If someone wants to connect to a instance
		// Need then send a deny message and close the connection
		bus.subscribe(ConnectFromOtherNeedEvent.class, new ActionOnEventListener(ctx, new ConnectionDenierAction(ctx)));

		// Start PaypalPaymentStatusCheckScheduler
		PaypalPaymentStatusCheckSchedule statusScheduler = new PaypalPaymentStatusCheckSchedule(
				getEventListenerContext(), openBridges, paypalService);
		paymentCheckTimer = new Timer(true);
		//paymentCheckTimer.scheduleAtFixedRate(statusScheduler, SCHEDULER_INTERVAL, SCHEDULER_INTERVAL);
	}

	public PaypalPaymentService getPaypalService() {
		return paypalService;
	}

	public void setPaypalService(PaypalPaymentService paypalService) {
		this.paypalService = paypalService;
	}
	
	

}
