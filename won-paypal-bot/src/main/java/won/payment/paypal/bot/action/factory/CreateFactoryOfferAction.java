package won.payment.paypal.bot.action.factory;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.Charset;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.RDF;

import won.bot.framework.bot.context.FactoryBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.action.impl.atomlifecycle.AbstractCreateAtomAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.connect.ConnectCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.factory.FactoryHintEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.filter.impl.CommandResultFilter;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnFirstEventListener;
import won.payment.paypal.bot.event.connect.ComplexConnectCommandEvent;
import won.payment.paypal.bot.impl.PaypalBotContextWrapper;
import won.payment.paypal.bot.model.PaymentBridge;
import won.payment.paypal.bot.model.PaymentStatus;
import won.payment.paypal.bot.util.ResourceManager;
import won.payment.paypal.bot.util.WonPayRdfUtils;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.model.Connection;
import won.protocol.model.AtomGraphType;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.DefaultAtomModelWrapper;
import won.protocol.util.AtomModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONPAY;

/**
 * Creates a new atom for a payment.
 *
 * @author schokobaer
 */
public class CreateFactoryOfferAction extends AbstractCreateAtomAction {
    private static final URI STUB_ATOM_URI = URI.create("http://example.com/content");
    private static final URI STUB_SHAPES_URI = URI.create("http://example.com/shapes");
    // TODO: change opening msg
    private static final String OPENING_MSG = "Hello low-order creature! " + "I am the mighty Paypal Bot. "
                    + "You awakened me from my sleep. " + "My destiny is to satisfy your commercial necessities. "
                    + "So you want to receive some money from an other poor soul..?";
    private static final String goalString;
    static {
        goalString = ResourceManager.getResourceAsString("/temp/goals.trig");
    }

    public CreateFactoryOfferAction(EventListenerContext eventListenerContext, URI... sockets) {
        super(eventListenerContext, (eventListenerContext.getBotContextWrapper()).getAtomCreateListName(), false, true,
                        sockets);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        if (!(event instanceof FactoryHintEvent)) {
            logger.error("CreateFactoryOfferAction can only handle FactoryHintEvent");
            return;
        }
        FactoryHintEvent factoryHintEvent = (FactoryHintEvent) event;
        EventBus bus = getEventListenerContext().getEventBus();
        EventListenerContext ctx = getEventListenerContext();
        final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();
        Model factoryOfferModel = createFactoryOfferFromTemplate(ctx, factoryHintEvent.getFactoryAtomURI(),
                        factoryHintEvent.getRequesterURI());
        URI factoryOfferURI = WonRdfUtils.AtomUtils.getAtomURI(factoryOfferModel);
        logger.debug("creating shapes model with factory atom URI {}", factoryHintEvent.getFactoryAtomURI());
        Model shapesModel = createShapesModelFromTemplate(ctx, factoryHintEvent.getFactoryAtomURI());
        logger.debug("creating factoryoffer on won node {} with content {} ", wonNodeUri,
                        StringUtils.abbreviate(RdfUtils.toString(factoryOfferModel), 150));
        WonMessage createAtomMessage = createWonMessage(ctx.getWonNodeInformationService(), factoryOfferURI, wonNodeUri,
                        factoryOfferModel, shapesModel);
        EventBotActionUtils.rememberInList(ctx, factoryOfferURI, uriListName);
        EventListener successCallback = successEvent -> {
            logger.debug("factoryoffer creation successful, new atom URI is {}", factoryOfferURI);
            // publish connect between the specific offer and the requester atom
            ((FactoryBotContextWrapper) ctx.getBotContextWrapper()).addFactoryAtomURIOfferRelation(factoryOfferURI,
                            factoryHintEvent.getFactoryAtomURI());
            // TODO: WonPayRdfUtils.getPaymentModelUri(successEvent.con)
            String paymentUri = WonPayRdfUtils.getPaymentModelUri(factoryOfferURI);
            Model paymentModel = ModelFactory.createDefaultModel();
            paymentModel.createResource(paymentUri).addProperty(RDF.type, WONPAY.PAYMENT);
            logger.info("Created new payment resource {}", paymentUri);
            ComplexConnectCommandEvent connectCommandEvent = new ComplexConnectCommandEvent(factoryOfferURI,
                            factoryHintEvent.getRequesterURI(), OPENING_MSG, paymentModel);
            ctx.getEventBus().subscribe(ConnectCommandSuccessEvent.class, new ActionOnFirstEventListener(ctx,
                            new CommandResultFilter(connectCommandEvent), new BaseEventBotAction(ctx) {
                                @Override
                                protected void doRun(Event event, EventListener executingListener) throws Exception {
                                    if (event instanceof ConnectCommandSuccessEvent) {
                                        logger.info("Created successfully a connection to merchant");
                                        ConnectCommandSuccessEvent connectSuccessEvent = (ConnectCommandSuccessEvent) event;
                                        Connection con = connectSuccessEvent.getCon();
                                        URI atomUri = connectSuccessEvent.getAtomURI();
                                        PaymentBridge bridge = new PaymentBridge();
                                        bridge.setConnection(con);
                                        bridge.setStatus(PaymentStatus.NOWHERE);
                                        ((PaypalBotContextWrapper) getEventListenerContext().getBotContextWrapper())
                                                        .putOpenBridge(atomUri, bridge);
                                    }
                                }
                            }));
            bus.publish(connectCommandEvent);
        };
        EventListener failureCallback = failureEvent -> {
            String textMessage = WonRdfUtils.MessageUtils
                            .getTextMessage(((FailureResponseEvent) failureEvent).getFailureMessage());
            logger.debug("factoryoffer creation failed for atom URI {}, original message URI {}: {}",
                            new Object[] { factoryOfferURI,
                                            ((FailureResponseEvent) failureEvent).getOriginalMessageURI(),
                                            textMessage });
            EventBotActionUtils.removeFromList(getEventListenerContext(), factoryOfferURI, uriListName);
        };
        EventBotActionUtils.makeAndSubscribeResponseListener(createAtomMessage, successCallback, failureCallback,
                        getEventListenerContext());
        logger.debug("registered listeners for response to message URI {}", createAtomMessage.getMessageURI());
        getEventListenerContext().getWonMessageSender().sendWonMessage(createAtomMessage);
        logger.debug("factoryoffer creation message sent with message URI {}", createAtomMessage.getMessageURI());
    }

    private Model createFactoryOfferFromTemplate(EventListenerContext ctx, URI factoryAtomURI, URI requesterURI) {
        // TODO: retrieve real template from factory
        // Dataset factoryAtomDataSet =
        // ctx.getLinkedDataSource().getDataForResource(factoryAtomURI);
        // DefaultAtomModelWrapper factoryAtomModelWrapper = new
        // DefaultAtomModelWrapper(factoryAtomDataSet);
        // Dataset requesterAtomDataSet =
        // ctx.getLinkedDataSource().getDataForResource(requesterURI);
        // DefaultAtomModelWrapper requesterAtomModelWrapper = new
        // DefaultAtomModelWrapper(requesterAtomDataSet);
        /*
         * String connectTitle = factoryAtomModelWrapper.getSomeTitleFromIsOrAll() +
         * " <-> " + (requesterAtomModelWrapper.getSomeTitleFromIsOrAll() != null ?
         * requesterAtomModelWrapper.getSomeTitleFromIsOrAll() :
         * requesterAtomModelWrapper.getAtomModel() .listStatements(null,
         * WON.HAS_SEARCH_STRING, (RDFNode) null).next().getObject())
         */;
        String connectTitle = "PaymentBot Atom";
        final URI atomURI = ctx.getWonNodeInformationService().generateAtomURI(ctx.getNodeURISource().getNodeURI());
        DefaultAtomModelWrapper atomModelWrapper = new DefaultAtomModelWrapper(atomURI.toString());
        atomModelWrapper.setTitle(connectTitle);
        atomModelWrapper.setDescription("This is a automatically created atom by the PaypalBot");
        atomModelWrapper.addFlag(WON.NoHintForCounterpart);
        atomModelWrapper.addFlag(WON.NoHintForMe);
        atomModelWrapper.setShapesGraphReference(STUB_SHAPES_URI);
        int i = 1;
        for (URI socket : sockets) {
            atomModelWrapper.addSocket(atomURI + "#socket" + i, socket.toString());
            i++;
        }
        return atomModelWrapper.copyAtomModel(AtomGraphType.ATOM);
    }

    private Model createShapesModelFromTemplate(EventListenerContext ctx, URI factoryAtomURI) {
        Dataset dataset = DatasetFactory.createGeneral();
        RDFDataMgr.read(dataset, new ByteArrayInputStream(goalString.getBytes(Charset.forName("UTF-8"))),
                        RDFFormat.TRIG.getLang());
        Model shapeModel = dataset.getUnionModel();
        shapeModel.setNsPrefix("pay", WONPAY.BASE_URI);
        return shapeModel;
    }

    private WonMessage createWonMessage(WonNodeInformationService wonNodeInformationService, URI atomURI,
                    URI wonNodeURI, Model atomModel, Model shapesModel) throws WonMessageBuilderException {
        AtomModelWrapper atomModelWrapper = new AtomModelWrapper(atomModel, null);
        atomModelWrapper.addFlag(WON.NoHintForMe);
        atomModelWrapper.addFlag(WON.NoHintForCounterpart);
        RdfUtils.replaceBaseURI(atomModel, atomURI.toString(), true);
        Dataset contentDataset = DatasetFactory.createGeneral();
        contentDataset.addNamedModel(STUB_ATOM_URI.toString(), atomModel);
        contentDataset.addNamedModel(STUB_SHAPES_URI.toString(), shapesModel);
        return WonMessageBuilder.setMessagePropertiesForCreate(wonNodeInformationService.generateEventURI(wonNodeURI),
                        atomURI, wonNodeURI).addContent(contentDataset).build();
    }
}
