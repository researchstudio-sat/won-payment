package won.payment.paypal.bot.action.proposal;

import org.apache.jena.rdf.model.Model;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.analyzation.proposal.ProposalReceivedEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;

/**
 * A simple Action that only rejects incoming proposals and tells the user that
 * proposals will never be accepted.
 * 
 * @author schokobaer
 */
public class ProposalReceivedAction extends BaseEventBotAction {
    public ProposalReceivedAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventListenerContext ctx = getEventListenerContext();
        if (event instanceof ProposalReceivedEvent) {
            ProposalReceivedEvent proposalReceivedEvent = (ProposalReceivedEvent) event;
            Connection con = proposalReceivedEvent.getCon();
            // TODO: edit messages
            // TODO: can the branches be merged?
            // TODO: think about propose-to-cancel-behaviour
            if (proposalReceivedEvent.hasProposesEvents()) {
                Model rejectModel = WonRdfUtils.MessageUtils.textMessage("I do not accept proposals");
                rejectModel = WonRdfUtils.MessageUtils.addRejects(rejectModel, proposalReceivedEvent.getProposalUri());
                ctx.getEventBus().publish(new ConnectionMessageCommandEvent(con, rejectModel));
            } else if (proposalReceivedEvent.hasProposesToCancelEvents()) {
                Model rejectModel = WonRdfUtils.MessageUtils.textMessage("You can not cancel anymore!");
                rejectModel = WonRdfUtils.MessageUtils.addRejects(rejectModel, proposalReceivedEvent.getProposalUri());
                ctx.getEventBus().publish(new ConnectionMessageCommandEvent(con, rejectModel));
            }
        }
    }
}
