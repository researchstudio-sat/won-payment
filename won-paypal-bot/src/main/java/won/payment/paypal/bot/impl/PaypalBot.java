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
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.factory.FactoryHintEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.CloseFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.OpenFromOtherAtomEvent;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.payment.paypal.bot.action.HelpAction;
import won.payment.paypal.bot.action.analyze.GoalAnalyzationAction;
import won.payment.paypal.bot.action.connect.ConnectionAcceptedAction;
import won.payment.paypal.bot.action.connect.ConnectionCloseAction;
import won.payment.paypal.bot.action.connect.ConnectionDenierAction;
import won.payment.paypal.bot.action.connect.ExecuteComplexConnectCommandAction;
import won.payment.paypal.bot.action.effect.MessageEffectBrokerAction;
import won.payment.paypal.bot.action.factory.CreateFactoryOfferAction;
import won.payment.paypal.bot.action.modification.MessageRetractedAction;
import won.payment.paypal.bot.action.precondition.PreconditionMetAction;
import won.payment.paypal.bot.action.precondition.PreconditionUnmetAction;
import won.payment.paypal.bot.action.proposal.ProposalAcceptedAction;
import won.payment.paypal.bot.action.proposal.ProposalReceivedAction;
import won.payment.paypal.bot.action.proposal.ProposalRejectedAction;
import won.payment.paypal.bot.event.SimpleMessageReceivedEvent;
import won.payment.paypal.bot.event.analyze.ConversationAnalyzationCommandEvent;
import won.payment.paypal.bot.event.connect.ComplexConnectCommandEvent;
import won.payment.paypal.bot.event.modification.MessageRetractedEvent;
import won.payment.paypal.bot.event.proposal.ProposalRejectedEvent;
import won.payment.paypal.bot.scheduler.PaypalPaymentStatusCheckSchedule;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;

/**
 * The bot which subscribes for the Events.
 * 
 * @author schokobaer
 */
public class PaypalBot extends FactoryBot {
        private Timer paymentCheckTimer;

        public static ConnectionMessageCommandEvent makeProcessingMessage(String msg, Connection con) {
                return new ConnectionMessageCommandEvent(con, WonRdfUtils.MessageUtils.processingMessage(msg));
        }

        @Override
        protected void initializeFactoryEventListeners() {
                EventBus bus = getEventBus();
                EventListenerContext ctx = getEventListenerContext();
                // AnalyzeBehaviour analyzeBehaviour = new AnalyzeBehaviour(ctx);
                // analyzeBehaviour.activate();
                // eagerly cache RDF data
                BotBehaviour eagerlyCacheBehaviour = new EagerlyPopulateCacheBehaviour(ctx);
                eagerlyCacheBehaviour.activate();
                // Factory Hint Event
                bus.subscribe(FactoryHintEvent.class,
                                new ActionOnEventListener(ctx, "FactoryHintEvent", new CreateFactoryOfferAction(ctx)));
                // TODO: remove unused Events
                // Counterpart accepted the connection
                bus.subscribe(OpenFromOtherAtomEvent.class,
                                new ActionOnEventListener(ctx, new ConnectionAcceptedAction(ctx)));
                // Analyzation Events
                bus.subscribe(PreconditionUnmetEvent.class, new ActionOnEventListener(ctx, "PreconditionUnmetEvent",
                                new PreconditionUnmetAction(ctx)));
                bus.subscribe(PreconditionMetEvent.class,
                                new ActionOnEventListener(ctx, "PreconditionMetEvent", new PreconditionMetAction(ctx)));
                bus.subscribe(ProposalAcceptedEvent.class, new ActionOnEventListener(ctx, "ProposalAcceptedEvent",
                                new ProposalAcceptedAction(ctx)));
                bus.subscribe(ProposalRejectedEvent.class, new ActionOnEventListener(ctx, "ProposalRejectedEvent",
                                new ProposalRejectedAction(ctx)));
                bus.subscribe(ProposalReceivedEvent.class, new ActionOnEventListener(ctx, "ProposalReceivedEvent",
                                new ProposalReceivedAction(ctx)));
                bus.subscribe(MessageRetractedEvent.class, new ActionOnEventListener(ctx, "MessageRetractedEvent",
                                new MessageRetractedAction(ctx)));
                // Incoming message effect broker
                bus.subscribe(MessageFromOtherAtomEvent.class,
                                new ActionOnEventListener(ctx, new MessageEffectBrokerAction(ctx)));
                // Incoming simple Messages
                bus.subscribe(SimpleMessageReceivedEvent.class, new ActionOnEventListener(ctx, new HelpAction(ctx)));
                // SHAQL Validation for analyzation events
                bus.subscribe(ConversationAnalyzationCommandEvent.class,
                                new ActionOnEventListener(ctx, new GoalAnalyzationAction(ctx)));
                // ComplexConnectCommandEvent
                bus.subscribe(ComplexConnectCommandEvent.class,
                                new ActionOnEventListener(ctx, new ExecuteComplexConnectCommandAction(ctx)));
                // Client closes the connection
                bus.subscribe(CloseFromOtherAtomEvent.class,
                                new ActionOnEventListener(ctx, new ConnectionCloseAction(ctx)));
                // If someone wants to connect to a instance
                // Atom then send a deny message and close the connection
                bus.subscribe(ConnectFromOtherAtomEvent.class,
                                new ActionOnEventListener(ctx, new ConnectionDenierAction(ctx)));
                // Start PaypalPaymentStatusCheckScheduler
                PaypalPaymentStatusCheckSchedule statusScheduler = new PaypalPaymentStatusCheckSchedule(
                                getEventListenerContext());
                paymentCheckTimer = new Timer(true);
                Long interval = ((PaypalBotContextWrapper) getBotContextWrapper()).getSchedulingInterval();
                paymentCheckTimer.scheduleAtFixedRate(statusScheduler, interval, interval);
        }
}
