package won.payment.paypal.bot.impl;

import java.util.Timer;

import won.bot.framework.bot.base.FactoryBot;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.behaviour.BotBehaviour;
import won.bot.framework.eventbot.behaviour.EagerlyPopulateCacheBehaviour;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.impl.analyzation.agreement.ProposalAcceptedEvent;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionMetEvent;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionUnmetEvent;
import won.bot.framework.eventbot.event.impl.analyzation.proposal.ProposalReceivedEvent;
import won.bot.framework.eventbot.event.impl.factory.FactoryHintEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.CloseFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.OpenFromOtherNeedEvent;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.payment.paypal.bot.action.connect.ConnectionAcceptedAction;
import won.payment.paypal.bot.action.connect.ConnectionCloseAction;
import won.payment.paypal.bot.action.connect.ConnectionDenierAction;
import won.payment.paypal.bot.action.connect.ExecuteComplexConnectCommandAction;
import won.payment.paypal.bot.action.effect.MessageEffectBrokerAction;
import won.payment.paypal.bot.action.factory.CreateFactoryOfferAction;
import won.payment.paypal.bot.action.precondition.GoalAnalyzationAction;
import won.payment.paypal.bot.action.precondition.MessageRetractedAction;
import won.payment.paypal.bot.action.precondition.PreconditionMetAction;
import won.payment.paypal.bot.action.precondition.PreconditionUnmetAction;
import won.payment.paypal.bot.action.proposal.ProposalAcceptedAction;
import won.payment.paypal.bot.action.proposal.ProposalReceivedAction;
import won.payment.paypal.bot.action.proposal.ProposalRejectedAction;
import won.payment.paypal.bot.event.ComplexConnectCommandEvent;
import won.payment.paypal.bot.event.ConversationAnalyzationCommandEvent;
import won.payment.paypal.bot.event.MessageRetractedEvent;
import won.payment.paypal.bot.event.ProposalRejectedEvent;
import won.payment.paypal.bot.scheduler.PaypalPaymentStatusCheckSchedule;

/**
 * The bot which subscribes for the Events.
 * 
 * @author schokobaer
 *
 */
public class PaypalBot extends FactoryBot {

	private static final Long SCHEDULER_INTERVAL = 60 * 1000L;

	private Timer paymentCheckTimer;

	@Override
	protected void initializeFactoryEventListeners() {
		
		EventBus bus = getEventBus();
		EventListenerContext ctx = getEventListenerContext();
		
		//AnalyzeBehaviour analyzeBehaviour = new AnalyzeBehaviour(ctx);
		//analyzeBehaviour.activate();
				
		
		// eagerly cache RDF data
		BotBehaviour eagerlyCacheBehaviour = new EagerlyPopulateCacheBehaviour(ctx);
		eagerlyCacheBehaviour.activate();

		
		// Factory Hint Event
		bus.subscribe(FactoryHintEvent.class,
			new ActionOnEventListener(
				ctx,
				"FactoryHintEvent",
				new CreateFactoryOfferAction(ctx)
			)
		);
		
		// Counterpart accepted the connection
		bus.subscribe(OpenFromOtherNeedEvent.class, new ActionOnEventListener(ctx, new ConnectionAcceptedAction(ctx)));
		
		//Analyzation Events
		bus.subscribe(PreconditionUnmetEvent.class,
            new ActionOnEventListener(
                ctx,
                "PreconditionUnmetEvent",
                new PreconditionUnmetAction(ctx)
            )
        );
 
        bus.subscribe(PreconditionMetEvent.class,
            new ActionOnEventListener(
                ctx,
                "PreconditionMetEvent",
                new PreconditionMetAction(ctx)
            )
        );

        bus.subscribe(ProposalAcceptedEvent.class,
            new ActionOnEventListener(
                ctx,
                "ProposalAcceptedEvent",
                new ProposalAcceptedAction(ctx)
            )
        );
        
        bus.subscribe(ProposalRejectedEvent.class,
            new ActionOnEventListener(
                ctx,
                "ProposalRejectedEvent",
                new ProposalRejectedAction(ctx)
            )
        );
        
        bus.subscribe(ProposalReceivedEvent.class,
            new ActionOnEventListener(
                ctx,
                "ProposalReceivedEvent",
                new ProposalReceivedAction(ctx)
            )
        );
        
        bus.subscribe(MessageRetractedEvent.class,
            new ActionOnEventListener(
                ctx,
                "MessageRetractedEvent",
                new MessageRetractedAction(ctx)
            )
        );
		
        // Incoming message effect broker
        bus.subscribe(MessageFromOtherNeedEvent.class, new ActionOnEventListener(ctx, new MessageEffectBrokerAction(ctx)));
        
        // SHAQL Validation for analyzation events
        bus.subscribe(ConversationAnalyzationCommandEvent.class, new ActionOnEventListener(ctx, new GoalAnalyzationAction(ctx)));
		
        // ComplexConnectCommandEvent
        bus.subscribe(ComplexConnectCommandEvent.class, new ActionOnEventListener(ctx, new ExecuteComplexConnectCommandAction(ctx)));

		// Client closes the connection
		bus.subscribe(CloseFromOtherNeedEvent.class,
				new ActionOnEventListener(ctx, new ConnectionCloseAction(ctx)));

		// If someone wants to connect to a instance
		// Need then send a deny message and close the connection
		bus.subscribe(ConnectFromOtherNeedEvent.class, new ActionOnEventListener(ctx, new ConnectionDenierAction(ctx)));

		
		
		
		// Start PaypalPaymentStatusCheckScheduler
		PaypalPaymentStatusCheckSchedule statusScheduler = new PaypalPaymentStatusCheckSchedule(
				getEventListenerContext());
		paymentCheckTimer = new Timer(true);
		paymentCheckTimer.scheduleAtFixedRate(statusScheduler, SCHEDULER_INTERVAL, SCHEDULER_INTERVAL);
	}
	

}
