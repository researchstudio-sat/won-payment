package won.payment.paypal.bot.action.precondition;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.topbraid.shacl.vocabulary.SH;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionEvent;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionUnmetEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.payment.paypal.bot.impl.PaypalBotContextWrapper;
import won.payment.paypal.bot.model.PaymentBridge;
import won.payment.paypal.bot.model.PaymentStatus;
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

		if (ctx.getBotContextWrapper() instanceof PaypalBotContextWrapper && event instanceof PreconditionUnmetEvent) {
			Connection con = ((BaseNeedAndConnectionSpecificEvent) event).getCon();
			PaymentBridge bridge = PaypalBotContextWrapper.paymentBridge(ctx, con);

			if (bridge.getStatus() != PaymentStatus.GOALUNSATISFIED) {
				return;
			}

			logger.info("Precondition unmet");

			GoalInstantiationResult preconditionEventPayload = ((PreconditionEvent) event).getPayload();

			Model messageModel = WonRdfUtils.MessageUtils
					.processingMessage("Payment not possible yet, missing necessary values.");
			String respondWith = "Payment not possible yet, missing necessary Values: \n";
			for (ValidationResultWrapper validationResultWrapper : preconditionEventPayload.getShaclReportWrapper()
					.getValidationResults()) {
				if (validationResultWrapper.getResultPath() != null || validationResultWrapper.getFocusNode() != null) {
					String path = validationResultWrapper.getResultPath().getLocalName();
					if (path != null && !path.isEmpty()) {
						respondWith += path + ": ";
					} else {
						path = validationResultWrapper.getFocusNode().getLocalName();
						respondWith += !path.isEmpty() ? path + ": " : "";
					}
				}

				respondWith += validationResultWrapper.getResultMessage() + " \n";
				Resource report = messageModel.createResource();

				if (validationResultWrapper.getFocusNode() != null) {
					report.addProperty(SH.focusNode, validationResultWrapper.getFocusNode());
				}
				if (validationResultWrapper.getDetail() != null) {
					report.addProperty(SH.detail, validationResultWrapper.getDetail());
				}
				if (validationResultWrapper.getResultPath() != null) {
					report.addProperty(SH.resultPath, validationResultWrapper.getResultPath());
				}
				if (validationResultWrapper.getResultSeverity() != null) {
					report.addProperty(SH.resultSeverity, validationResultWrapper.getResultSeverity());
				}
				if (validationResultWrapper.getValue() != null) {
					report.addProperty(SH.value, validationResultWrapper.getValue());
				}
				if (validationResultWrapper.getResultMessage() != null) {
					report.addProperty(SH.resultMessage, validationResultWrapper.getResultMessage());
				}

				WonRdfUtils.MessageUtils.addToMessage(messageModel, SH.result, report);
				// WonRdfUtils.MessageUtils.addToMessage(messageModel, RDF.first,
				// preconditionEventPayload.getShaclReportWrapper().getReport());
			}
			logger.info(respondWith);

			// Model messageModel = WonRdfUtils.MessageUtils.processingMessage(respondWith);

			getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(con, messageModel));
		}

	}

}
