package won.payment.paypal.bot.event.proposal;

import java.net.URI;

import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.protocol.model.Connection;

/**
 * When the user retracts a message.
 * 
 * @author schokobaer
 *
 */
public class ProposalRejectedEvent extends BaseNeedAndConnectionSpecificEvent {

	private URI proposalUri;
	
	public ProposalRejectedEvent(Connection con, URI proposalUri) {
		super(con);
		this.proposalUri = proposalUri;
	}

	public URI getProposalUri() {
		return proposalUri;
	}
	
}
