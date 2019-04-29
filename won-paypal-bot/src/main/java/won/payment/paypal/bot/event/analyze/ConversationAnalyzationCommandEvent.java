package won.payment.paypal.bot.event.analyze;

import won.bot.framework.eventbot.event.BaseAtomAndConnectionSpecificEvent;
import won.protocol.model.Connection;

/**
 * To start an analyzation of the conversation.
 * 
 * @author schokobaer
 */
public class ConversationAnalyzationCommandEvent extends BaseAtomAndConnectionSpecificEvent {
    public ConversationAnalyzationCommandEvent(Connection con) {
        super(con);
    }
}
