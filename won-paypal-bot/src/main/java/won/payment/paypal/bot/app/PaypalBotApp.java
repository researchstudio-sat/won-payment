package won.payment.paypal.bot.app;

import org.springframework.boot.SpringApplication;
//import org.springframework.context.ConfigurableApplicationContext;

/**
 * Entry Point for the Bot.
 * 
 * @author schokobaer
 *
 */
public class PaypalBotApp {

	public static void main(String[] args) throws Exception {
		SpringApplication app = new SpringApplication(new Object[] { "classpath:/spring/app/paypalBotApp.xml" });
		app.setWebEnvironment(false);
		app.run(args);
		// use for debugging purposes:
		// ConfigurableApplicationContext applicationContext = app.run(args);
		// Thread.sleep(5*60*1000);
		// app.exit(applicationContext);
	}

}
