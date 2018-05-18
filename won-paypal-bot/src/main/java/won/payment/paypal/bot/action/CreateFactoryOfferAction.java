package won.payment.paypal.bot.action;

import java.io.ByteArrayInputStream;
import java.net.URI;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import won.bot.framework.bot.context.FactoryBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.action.impl.needlifecycle.AbstractCreateNeedAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.connect.ConnectCommandEvent;
import won.bot.framework.eventbot.event.impl.factory.FactoryHintEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.payment.paypal.bot.util.ResourceManager;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.model.NeedContentPropertyType;
import won.protocol.model.NeedGraphType;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.DefaultNeedModelWrapper;
import won.protocol.util.NeedModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;

public class CreateFactoryOfferAction extends AbstractCreateNeedAction {

	private static final URI STUB_NEED_URI = URI.create("http://example.com/content");
    private static final URI STUB_SHAPES_URI = URI.create("http://example.com/shapes");

    private static final String goalString;

    static {
        goalString = ResourceManager.getResourceAsString("/temp/goals_withoutmax.trig");
    }
	
	public CreateFactoryOfferAction(EventListenerContext eventListenerContext, URI... facets) {
        super(eventListenerContext, (eventListenerContext.getBotContextWrapper()).getNeedCreateListName(), false, true, facets);
    }
	
	@Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        if(!(event instanceof FactoryHintEvent)) {
            logger.error("CreateFactoryOfferAction can only handle FactoryHintEvent");
            return;
        }
        FactoryHintEvent factoryHintEvent = (FactoryHintEvent) event;

        EventBus bus = getEventListenerContext().getEventBus();
        EventListenerContext ctx = getEventListenerContext();
        final URI wonNodeUri = ctx.getNodeURISource().getNodeURI();

        Model factoryOfferModel = createFactoryOfferFromTemplate(ctx, factoryHintEvent.getFactoryNeedURI(), factoryHintEvent.getRequesterURI());
        URI factoryOfferURI = WonRdfUtils.NeedUtils.getNeedURI(factoryOfferModel);
        Model shapesModel = createShapesModelFromTemplate(ctx, factoryHintEvent.getFactoryNeedURI());

        logger.debug("creating factoryoffer on won node {} with content {} ", wonNodeUri, StringUtils.abbreviate(RdfUtils.toString(factoryOfferModel), 150));

        WonMessage createNeedMessage = createWonMessage(ctx.getWonNodeInformationService(), factoryOfferURI, wonNodeUri, factoryOfferModel, shapesModel);
        EventBotActionUtils.rememberInList(ctx, factoryOfferURI, uriListName);

        EventListener successCallback = successEvent -> {
            logger.debug("factoryoffer creation successful, new need URI is {}", factoryOfferURI);
            //publish connect between the specific offer and the requester need
            ((FactoryBotContextWrapper) ctx.getBotContextWrapper()).addFactoryNeedURIOfferRelation(factoryOfferURI, factoryHintEvent.getFactoryNeedURI());
            bus.publish(new ConnectCommandEvent(factoryOfferURI, factoryHintEvent.getRequesterURI()));
        };

        EventListener failureCallback = failureEvent -> {
            String textMessage = WonRdfUtils.MessageUtils.getTextMessage(((FailureResponseEvent) failureEvent).getFailureMessage());

            logger.debug("factoryoffer creation failed for need URI {}, original message URI {}: {}", new Object[]{factoryOfferURI, ((FailureResponseEvent) failureEvent).getOriginalMessageURI(), textMessage});
            EventBotActionUtils.removeFromList(getEventListenerContext(), factoryOfferURI, uriListName);
        };

        EventBotActionUtils.makeAndSubscribeResponseListener(createNeedMessage, successCallback, failureCallback, getEventListenerContext());

        logger.debug("registered listeners for response to message URI {}", createNeedMessage.getMessageURI());
        getEventListenerContext().getWonMessageSender().sendWonMessage(createNeedMessage);
        logger.debug("factoryoffer creation message sent with message URI {}", createNeedMessage.getMessageURI());

    }

    private Model createFactoryOfferFromTemplate(EventListenerContext ctx, URI factoryNeedURI, URI requesterURI){
        //TODO: retrieve real template from factory
        Dataset factoryNeedDataSet = ctx.getLinkedDataSource().getDataForResource(factoryNeedURI);
        DefaultNeedModelWrapper factoryNeedModelWrapper = new DefaultNeedModelWrapper(factoryNeedDataSet);

        Dataset requesterNeedDataSet = ctx.getLinkedDataSource().getDataForResource(requesterURI);
        DefaultNeedModelWrapper requesterNeedModelWrapper = new DefaultNeedModelWrapper(requesterNeedDataSet);

        String connectTitle =  factoryNeedModelWrapper.getSomeTitleFromIsOrAll() + "<->" + requesterNeedModelWrapper.getSomeTitleFromIsOrAll();

        DefaultNeedModelWrapper needModelWrapper = new DefaultNeedModelWrapper(ctx.getWonNodeInformationService().generateNeedURI(ctx.getNodeURISource().getNodeURI()).toString());

        needModelWrapper.setTitle(NeedContentPropertyType.IS, connectTitle);
        needModelWrapper.setDescription(NeedContentPropertyType.IS, "This is a automatically created need by the TaxiBot");
        needModelWrapper.addFlag(WON.NO_HINT_FOR_COUNTERPART);
        needModelWrapper.addFlag(WON.NO_HINT_FOR_ME);
        needModelWrapper.setShapesGraphReference(STUB_SHAPES_URI);

        for(URI facet : facets){
            needModelWrapper.addFacetUri(facet.toString());
        }

        return needModelWrapper.copyNeedModel(NeedGraphType.NEED);
    }

    private Model createShapesModelFromTemplate(EventListenerContext ctx, URI factoryNeedURI) {
        Dataset dataset = DatasetFactory.createGeneral();
        RDFDataMgr.read(dataset, new ByteArrayInputStream(goalString.getBytes()), RDFFormat.TRIG.getLang());

        return dataset.getUnionModel();
    }



    private WonMessage createWonMessage(
        WonNodeInformationService wonNodeInformationService, URI needURI, URI wonNodeURI, Model needModel, Model shapesModel) throws WonMessageBuilderException {

        NeedModelWrapper needModelWrapper = new NeedModelWrapper(needModel, null);

        needModelWrapper.addFlag(WON.NO_HINT_FOR_ME);
        needModelWrapper.addFlag(WON.NO_HINT_FOR_COUNTERPART);

        RdfUtils.replaceBaseURI(needModel, needURI.toString());

        Dataset contentDataset = DatasetFactory.createGeneral();

        contentDataset.addNamedModel(STUB_NEED_URI.toString(), needModel);
        contentDataset.addNamedModel(STUB_SHAPES_URI.toString(), shapesModel);

        return WonMessageBuilder.setMessagePropertiesForCreate(
                wonNodeInformationService.generateEventURI(wonNodeURI),
                needURI,
                wonNodeURI)
                .addContent(contentDataset)
                .build();
    }
	
}
