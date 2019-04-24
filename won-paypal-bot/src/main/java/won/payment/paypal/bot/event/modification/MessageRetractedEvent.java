package won.payment.paypal.bot.event.modification;

import java.net.URI;

import won.bot.framework.eventbot.event.BaseAtomAndConnectionSpecificEvent;
import won.protocol.model.Connection;

// TODO: why is this a separate class? this case should be handled by won

/**
 * When a incoming message retracts an other message.
 * @author schokobaer
 *
 */
public class MessageRetractedEvent extends BaseAtomAndConnectionSpecificEvent {

	private URI retractedMessageUri;
	
	public MessageRetractedEvent(Connection con, URI retractedMessageUri) {
		super(con);
		this.retractedMessageUri = retractedMessageUri;
	}

	public URI getRetractedMessageUri() {
		return retractedMessageUri;
	}
	
}
