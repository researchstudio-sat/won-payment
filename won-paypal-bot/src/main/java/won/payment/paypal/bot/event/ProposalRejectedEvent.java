package won.payment.paypal.bot.event;

import java.net.URI;

import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.protocol.model.Connection;

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
