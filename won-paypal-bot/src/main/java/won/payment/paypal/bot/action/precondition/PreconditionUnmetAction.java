package won.payment.paypal.bot.action.precondition;

import java.util.Collection;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionEvent;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionUnmetEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.payment.paypal.bot.impl.PaypalBotContextWrapper;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;
import won.utils.goals.GoalInstantiationResult;
import won.utils.shacl.ValidationResultWrapper;

public class PreconditionUnmetAction extends BaseEventBotAction {

	public PreconditionUnmetAction(EventListenerContext eventListenerContext) {
		super(eventListenerContext);
	}

	@Override
	protected void doRun(Event event, EventListener executingListener) throws Exception {
		EventListenerContext ctx = getEventListenerContext();

        if(ctx.getBotContextWrapper() instanceof PaypalBotContextWrapper && event instanceof PreconditionUnmetEvent) {
            Connection con = ((BaseNeedAndConnectionSpecificEvent) event).getCon();

            logger.info("Precondition unmet");
            
            GoalInstantiationResult preconditionEventPayload = ((PreconditionEvent) event).getPayload();

            String respondWith = "Payment not possible yet, missing necessary Values: \n";
            for (ValidationResultWrapper validationResultWrapper : preconditionEventPayload.getShaclReportWrapper().getValidationResults()) {
				respondWith += validationResultWrapper.getResultMessage() + "\n";
			}
            logger.info(respondWith);
            

            //Model messageModel = WonRdfUtils.MessageUtils.textMessage(respondWith);
            //TODO: Create Message that tells the other side which preconditions(shapes) are not yet met in a better way and not just by pushing a string into the conversation
            //getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(con, messageModel));
        }

	}

}
