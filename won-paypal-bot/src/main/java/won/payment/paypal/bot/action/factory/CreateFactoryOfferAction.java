package won.payment.paypal.bot.action.factory;

import java.io.ByteArrayInputStream;
import java.net.URI;

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
import won.bot.framework.eventbot.action.impl.needlifecycle.AbstractCreateNeedAction;
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
import won.protocol.model.NeedGraphType;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.DefaultNeedModelWrapper;
import won.protocol.util.NeedModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONPAY;

/**
 * Creates a new need for a payment.
 *
 * @author schokobaer
 *
 */
public class CreateFactoryOfferAction extends AbstractCreateNeedAction {

    private static final URI STUB_NEED_URI = URI.create("http://example.com/content");
    private static final URI STUB_SHAPES_URI = URI.create("http://example.com/shapes");

    private static final String OPENING_MSG = "Hello low-order creature! " + "I am the mighty Paypal Bot. "
            + "You awakened me from my sleep. " + "My destiny is to satisfy your commercial necessities. "
            + "So you want to receive some money from an other poor soul..?";

    private static final String goalString;

    static {
        goalString = ResourceManager.getResourceAsString("/temp/goals.trig");
    }

    public CreateFactoryOfferAction(EventListenerContext eventListenerContext, URI... facets) {
        super(eventListenerContext, (eventListenerContext.getBotContextWrapper()).getNeedCreateListName(), false, true,
                facets);
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

        Model factoryOfferModel = createFactoryOfferFromTemplate(ctx, factoryHintEvent.getFactoryNeedURI(),
                factoryHintEvent.getRequesterURI());
        URI factoryOfferURI = WonRdfUtils.NeedUtils.getNeedURI(factoryOfferModel);
        Model shapesModel = createShapesModelFromTemplate(ctx, factoryHintEvent.getFactoryNeedURI());

        logger.debug("creating factoryoffer on won node {} with content {} ", wonNodeUri,
                StringUtils.abbreviate(RdfUtils.toString(factoryOfferModel), 150));

        WonMessage createNeedMessage = createWonMessage(ctx.getWonNodeInformationService(), factoryOfferURI, wonNodeUri,
                factoryOfferModel, shapesModel);
        EventBotActionUtils.rememberInList(ctx, factoryOfferURI, uriListName);

        EventListener successCallback = successEvent -> {
            logger.debug("factoryoffer creation successful, new need URI is {}", factoryOfferURI);
            // publish connect between the specific offer and the requester need
            ((FactoryBotContextWrapper) ctx.getBotContextWrapper()).addFactoryNeedURIOfferRelation(factoryOfferURI,
                    factoryHintEvent.getFactoryNeedURI());
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
                        URI needUri = connectSuccessEvent.getNeedURI();
                        PaymentBridge bridge = new PaymentBridge();
                        bridge.setMerchantConnection(con);
                        bridge.setStatus(PaymentStatus.NOWHERE);
                        ((PaypalBotContextWrapper) getEventListenerContext().getBotContextWrapper())
                                .putOpenBridge(needUri, bridge);
                    }

                }
            }));

            bus.publish(connectCommandEvent);
        };

        EventListener failureCallback = failureEvent -> {
            String textMessage = WonRdfUtils.MessageUtils
                    .getTextMessage(((FailureResponseEvent) failureEvent).getFailureMessage());

            logger.debug("factoryoffer creation failed for need URI {}, original message URI {}: {}", new Object[] {
                    factoryOfferURI, ((FailureResponseEvent) failureEvent).getOriginalMessageURI(), textMessage });
            EventBotActionUtils.removeFromList(getEventListenerContext(), factoryOfferURI, uriListName);
        };

        EventBotActionUtils.makeAndSubscribeResponseListener(createNeedMessage, successCallback, failureCallback,
                getEventListenerContext());

        logger.debug("registered listeners for response to message URI {}", createNeedMessage.getMessageURI());
        getEventListenerContext().getWonMessageSender().sendWonMessage(createNeedMessage);
        logger.debug("factoryoffer creation message sent with message URI {}", createNeedMessage.getMessageURI());

    }

    private Model createFactoryOfferFromTemplate(EventListenerContext ctx, URI factoryNeedURI, URI requesterURI) {
        // TODO: retrieve real template from factory
        Dataset factoryNeedDataSet = ctx.getLinkedDataSource().getDataForResource(factoryNeedURI);
        DefaultNeedModelWrapper factoryNeedModelWrapper = new DefaultNeedModelWrapper(factoryNeedDataSet);

        Dataset requesterNeedDataSet = ctx.getLinkedDataSource().getDataForResource(requesterURI);
        DefaultNeedModelWrapper requesterNeedModelWrapper = new DefaultNeedModelWrapper(requesterNeedDataSet);

		/*String connectTitle = factoryNeedModelWrapper.getSomeTitleFromIsOrAll() + " <-> "
				+ (requesterNeedModelWrapper.getSomeTitleFromIsOrAll() != null
						? requesterNeedModelWrapper.getSomeTitleFromIsOrAll()
						: requesterNeedModelWrapper.getNeedModel()
								.listStatements(null, WON.HAS_SEARCH_STRING, (RDFNode) null).next().getObject())*/;

        String connectTitle = "PaymentBot Need";

        final URI needURI = ctx.getWonNodeInformationService().generateNeedURI(ctx.getNodeURISource().getNodeURI());
        DefaultNeedModelWrapper needModelWrapper = new DefaultNeedModelWrapper(needURI.toString());
        needModelWrapper.setTitle(connectTitle);
        needModelWrapper.setDescription("This is a automatically created need by the PaypalBot");
        needModelWrapper.addFlag(WON.NO_HINT_FOR_COUNTERPART);
        needModelWrapper.addFlag(WON.NO_HINT_FOR_ME);
        needModelWrapper.setShapesGraphReference(STUB_SHAPES_URI);

        int i = 1;
        for(URI facet : facets){
            needModelWrapper.addFacet(needURI + "#facet" + i, facet.toString());
            i++;
        }

        return needModelWrapper.copyNeedModel(NeedGraphType.NEED);
    }

    private Model createShapesModelFromTemplate(EventListenerContext ctx, URI factoryNeedURI) {
        Dataset dataset = DatasetFactory.createGeneral();
        RDFDataMgr.read(dataset, new ByteArrayInputStream(goalString.getBytes()), RDFFormat.TRIG.getLang());

        Model shapeModel = dataset.getUnionModel();
        shapeModel.setNsPrefix("pay", WONPAY.BASE_URI);

        return shapeModel;
    }

    private WonMessage createWonMessage(WonNodeInformationService wonNodeInformationService, URI needURI,
                                        URI wonNodeURI, Model needModel, Model shapesModel) throws WonMessageBuilderException {

        NeedModelWrapper needModelWrapper = new NeedModelWrapper(needModel, null);

        needModelWrapper.addFlag(WON.NO_HINT_FOR_ME);
        needModelWrapper.addFlag(WON.NO_HINT_FOR_COUNTERPART);

        RdfUtils.replaceBaseURI(needModel, needURI.toString(), true);

        Dataset contentDataset = DatasetFactory.createGeneral();

        contentDataset.addNamedModel(STUB_NEED_URI.toString(), needModel);
        contentDataset.addNamedModel(STUB_SHAPES_URI.toString(), shapesModel);

        return WonMessageBuilder.setMessagePropertiesForCreate(wonNodeInformationService.generateEventURI(wonNodeURI),
                needURI, wonNodeURI).addContent(contentDataset).build();
    }

}
