package won.payment.paypal.bot.action;

import org.apache.jena.rdf.model.Model;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.BaseNeedAndConnectionSpecificEvent;
import won.bot.framework.eventbot.event.ConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.MessageEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;

public class MerchantMessageReceiverAction extends BaseEventBotAction {

	public MerchantMessageReceiverAction(EventListenerContext eventListenerContext) {
		super(eventListenerContext);
	}

	@Override
	protected void doRun(Event event, EventListener executingListener) throws Exception {
		ConnectionSpecificEvent messageEvent = (ConnectionSpecificEvent) event;
		if (messageEvent instanceof MessageEvent) {
			EventListenerContext ctx = getEventListenerContext();
			EventBus bus = ctx.getEventBus();

			Connection con = ((BaseNeedAndConnectionSpecificEvent) messageEvent).getCon();
			WonMessage msg = ((MessageEvent) messageEvent).getWonMessage();

			handleMessage(msg, con, bus);
		}

	}

	private void makeTextMsg(String msg, Connection con) {
		Model model = WonRdfUtils.MessageUtils.textMessage(msg);
		getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(con, model));
	}

	private void printHelp(Connection con) {
		makeTextMsg("I need the payment data from you. "
				+ "After you gave me all the payment data, type 'payment validate'. "
				+ "I will sumerize the payment data and propose it to you. "
				+ "After you accept that bundle I will create the payment and open a connection"
				+ " to the counterpart and send him the link for the payment execution. "
				+ "After the payment is complete I will inform you with a message in this connection. "
				+ "Here are the payment details and the formart you should send it. The '*' means it "
				+ "is optional but not necesery", con);
		makeTextMsg("pay_amount: <amount> (e.g. 10)", con);
		makeTextMsg("pay_currency: <currency> (e.g. EUR)", con);
		makeTextMsg("pay_receiver: <receiver_email> (e.g. test@won.org)", con);
		makeTextMsg("won_counterpart: <counterpart URI of the other need> "
				+ "(e.g. https://matchat.org/won/resource/need/1234abcd)", con);
		makeTextMsg("won_secret: <a secret you make with the counterpart> (e.g. oursecretword)", con);

	}

	private void offerHelp(Connection con) {
		makeTextMsg("If do not know how to hanlde me type 'help'", con);
	}

	private void handleMessage(WonMessage wonMsg, Connection con, EventBus bus) {

		String msg = WonRdfUtils.MessageUtils.getTextMessage(wonMsg).trim();

		if ("help".equals(msg)) {
			printHelp(con);
		} else if ("asdf".equals(msg)) {
			makeTextMsg("Did not see that comming ...", con);
		} else if ("payment validate".equals(msg)) {

		} else if ("payment check".equals(msg)) {

		} else if ("close".equals(msg)) {

		} else if (msg.startsWith("counterpart: ")) {
			
		} else {
			offerHelp(con);
		}

	}
	
	private void openCounterpartConnection(String needUri, Connection con) {
		
	}

}
