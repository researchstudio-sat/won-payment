package won.payment.paypal.bot.event.connect;

import java.net.URI;
import java.util.Optional;

import org.apache.jena.rdf.model.Model;

import won.bot.framework.eventbot.event.BaseNeedSpecificEvent;
import won.bot.framework.eventbot.event.RemoteNeedSpecificEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandEvent;
import won.bot.framework.eventbot.event.impl.command.connect.ConnectCommandEvent;
import won.protocol.message.WonMessageType;

/**
 * To publish a more complex connection Opening message with more content
 * then a simple text message.
 *
 * @author schokobaer
 *
 */
public class ComplexConnectCommandEvent extends BaseNeedSpecificEvent implements MessageCommandEvent, RemoteNeedSpecificEvent {

    private Model payload;
    private ConnectCommandEvent connectCommandEvent;

    public ComplexConnectCommandEvent(URI needURI, URI remoteNeedURI, String welcomeMsg, Model payload) {
        super(needURI);
        connectCommandEvent = new ConnectCommandEvent(needURI, remoteNeedURI, welcomeMsg);
        this.payload = payload;
    }

    public Model getPayload() {
        return payload;
    }

    public URI getNeedURI() {
        return connectCommandEvent.getNeedURI();
    }

    public URI getRemoteNeedURI() {
        return connectCommandEvent.getRemoteNeedURI();
    }

    public Optional<URI> getLocalFacet() {
        return connectCommandEvent.getLocalFacet();
    }

    public Optional<URI> getRemoteFacet() {
        return connectCommandEvent.getRemoteFacet();
    }

    @Override
    public WonMessageType getWonMessageType() {
        return WonMessageType.CONNECT;
    }

    public String getWelcomeMessage() {
        return connectCommandEvent.getWelcomeMessage();
    }


}
