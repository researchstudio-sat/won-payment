package won.payment.paypal.bot.event.analyze;

import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.protocol.model.Connection;

// TODO: why is this a separate class? maybe replace all occurences with superclass

/**
 * To start an analyzation of the conversation.
 * 
 * @author schokobaer
 *
 */
public class ConversationAnalyzationCommandEvent extends BaseNeedAndConnectionSpecificEvent {

	public ConversationAnalyzationCommandEvent(Connection con) {
		super(con);
	}
	
}
