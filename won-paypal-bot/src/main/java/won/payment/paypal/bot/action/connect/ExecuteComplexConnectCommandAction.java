package won.payment.paypal.bot.action.connect;

import java.net.URI;

import org.apache.jena.query.Dataset;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.impl.wonmessage.execCommand.ExecuteSendMessageCommandAction;
import won.bot.framework.eventbot.event.impl.command.MessageCommandFailureEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandNotSentEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.command.connect.ConnectCommandFailureEvent;
import won.bot.framework.eventbot.event.impl.command.connect.ConnectCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.SuccessResponseEvent;
import won.payment.paypal.bot.event.connect.ComplexConnectCommandEvent;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

public class ExecuteComplexConnectCommandAction extends ExecuteSendMessageCommandAction<ComplexConnectCommandEvent> {

	public ExecuteComplexConnectCommandAction(EventListenerContext eventListenerContext) {
		super(eventListenerContext);
	}

	@Override
	protected MessageCommandFailureEvent createRemoteNodeFailureEvent(ComplexConnectCommandEvent originalCommand,
			WonMessage messageSent, FailureResponseEvent failureResponseEvent) {
		return new ConnectCommandFailureEvent(originalCommand, failureResponseEvent.getNeedURI(), failureResponseEvent.getRemoteNeedURI(), failureResponseEvent.getConnectionURI());
	}

	@Override
	protected MessageCommandSuccessEvent createRemoteNodeSuccessEvent(ComplexConnectCommandEvent originalCommand,
			WonMessage messageSent, SuccessResponseEvent successResponseEvent) {
		return new ConnectCommandSuccessEvent(originalCommand, successResponseEvent.getNeedURI(), successResponseEvent.getRemoteNeedURI(), successResponseEvent.getConnectionURI());
	}

	@Override
	protected MessageCommandFailureEvent createLocalNodeFailureEvent(ComplexConnectCommandEvent originalCommand,
			WonMessage messageSent, FailureResponseEvent failureResponseEvent) {
		return new ConnectCommandFailureEvent(originalCommand, failureResponseEvent.getNeedURI(), failureResponseEvent.getRemoteNeedURI(), failureResponseEvent.getConnectionURI());
	}

	@Override
	protected MessageCommandSuccessEvent createLocalNodeSuccessEvent(ComplexConnectCommandEvent originalCommand,
			WonMessage messageSent, SuccessResponseEvent successResponseEvent) {
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected MessageCommandNotSentEvent createMessageNotSentEvent(ComplexConnectCommandEvent originalCommand,
			String message) {
		return new MessageCommandNotSentEvent<ComplexConnectCommandEvent>(message, originalCommand);
	}

	@Override
	protected WonMessage createWonMessage(ComplexConnectCommandEvent connectCommandEvent)
			throws WonMessageBuilderException {
				
		WonNodeInformationService wonNodeInformationService =
                getEventListenerContext().getWonNodeInformationService();

        Dataset localNeedRDF =
                getEventListenerContext().getLinkedDataSource().getDataForResource(connectCommandEvent.getNeedURI());
        Dataset remoteNeedRDF =
                getEventListenerContext().getLinkedDataSource().getDataForResource(connectCommandEvent.getRemoteNeedURI());

        URI localWonNode = WonRdfUtils.NeedUtils.getWonNodeURIFromNeed(localNeedRDF, connectCommandEvent.getNeedURI());
        URI remoteWonNode = WonRdfUtils.NeedUtils.getWonNodeURIFromNeed(remoteNeedRDF, connectCommandEvent.getRemoteNeedURI());


        return
                WonMessageBuilder.setMessagePropertiesForConnect(
                        wonNodeInformationService.generateEventURI(
                                localWonNode),
                        connectCommandEvent.getLocalFacet(),
                        connectCommandEvent.getNeedURI(),
                        localWonNode,
                        connectCommandEvent.getRemoteFacet(),
                        connectCommandEvent.getRemoteNeedURI(),
                        remoteWonNode,
                        connectCommandEvent.getWelcomeMessage())
                		.addContent(connectCommandEvent.getPayload())
                        .build();
	}

}
