package won.payment.paypal.bot.impl;

import won.bot.framework.bot.context.BotContext;
import won.bot.framework.bot.context.FactoryBotContextWrapper;

/**
 * Simple BotContextWrapper.
 * 
 * @author schokobaer
 *
 */
public class PaypalBotContextWrapper extends FactoryBotContextWrapper {

	public PaypalBotContextWrapper(BotContext botContext, String botName) {
		super(botContext, botName);
	}

}
