package won.payment.paypal.bot.event;
import won.protocol.vocabulary.WONCON;

import org.apache.jena.rdf.model.RDFNode;

import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.WonMessageReceivedOnConnectionEvent;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.vocabulary.WON;

public class SimpleMessageReceivedEvent extends WonMessageReceivedOnConnectionEvent {
    public SimpleMessageReceivedEvent(Connection con, WonMessage wonMessage) {
        super(con, wonMessage);
    }

    public SimpleMessageReceivedEvent(MessageFromOtherAtomEvent event) {
        this(event.getCon(), event.getWonMessage());
    }

    public String getMessage() {
        try {
            return this.getWonMessage().getMessageContent().getUnionModel()
                            .listStatements(null, WONCON.text, (RDFNode) null).next().getObject().asLiteral()
                            .getString();
        } catch (Exception e) {
            return null;
        }
    }
}
