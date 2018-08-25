package won.payment.paypal.bot.event;

import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.protocol.model.Connection;

public class ConversationAnalyzationCommandEvent extends BaseNeedAndConnectionSpecificEvent {

	public ConversationAnalyzationCommandEvent(Connection con) {
		super(con);
	}
	
}
