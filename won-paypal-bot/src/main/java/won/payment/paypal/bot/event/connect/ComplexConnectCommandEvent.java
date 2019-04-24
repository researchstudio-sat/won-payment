package won.payment.paypal.bot.event.connect;

import java.net.URI;
import java.util.Optional;

import org.apache.jena.rdf.model.Model;

import won.bot.framework.eventbot.event.BaseAtomSpecificEvent;
import won.bot.framework.eventbot.event.TargetAtomSpecificEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandEvent;
import won.bot.framework.eventbot.event.impl.command.connect.ConnectCommandEvent;
import won.protocol.message.WonMessageType;

// TODO: is there a simple connect event? If not, rename clas
// also consider making this a subclass of ConnectCommandEvent

/**
 * To publish a more complex connection Opening message with more content
 * then a simple text message.
 *
 * @author schokobaer
 *
 */
public class ComplexConnectCommandEvent extends BaseAtomSpecificEvent implements MessageCommandEvent, TargetAtomSpecificEvent {

    private Model payload;
    private ConnectCommandEvent connectCommandEvent;

    public ComplexConnectCommandEvent(URI atomURI, URI targetAtomURI, String welcomeMsg, Model payload) {
        super(atomURI);
        connectCommandEvent = new ConnectCommandEvent(atomURI, targetAtomURI, welcomeMsg);
        this.payload = payload;
    }

    public Model getPayload() {
        return payload;
    }

    public URI getAtomURI() {
        return connectCommandEvent.getAtomURI();
    }

    public URI getTargetAtomURI() {
        return connectCommandEvent.getTargetAtomURI();
    }

    public Optional<URI> getLocalSocket() {
        return connectCommandEvent.getLocalSocket();
    }

    public Optional<URI> getTargetSocket() {
        return connectCommandEvent.getTargetSocket();
    }

    @Override
    public WonMessageType getWonMessageType() {
        return WonMessageType.CONNECT;
    }

    public String getWelcomeMessage() {
        return connectCommandEvent.getWelcomeMessage();
    }


}
