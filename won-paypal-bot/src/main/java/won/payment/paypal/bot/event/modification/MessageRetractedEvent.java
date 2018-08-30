package won.payment.paypal.bot.event.modification;

import java.net.URI;

import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.protocol.model.Connection;

/**
 * When a incoming message retracts an other message.
 * @author schokobaer
 *
 */
public class MessageRetractedEvent extends BaseNeedAndConnectionSpecificEvent {

	private URI retractedMessageUri;
	
	public MessageRetractedEvent(Connection con, URI retractedMessageUri) {
		super(con);
		this.retractedMessageUri = retractedMessageUri;
	}

	public URI getRetractedMessageUri() {
		return retractedMessageUri;
	}
	
}
