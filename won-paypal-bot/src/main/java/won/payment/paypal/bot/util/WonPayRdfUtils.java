package won.payment.paypal.bot.util;

import java.net.URI;

import won.protocol.model.Connection;

public class WonPayRdfUtils {

	public static String getPaymentModelUri(URI needUri) {
		return needUri.toString() + "/payment";
	}
	
	public static String getPaymentModelUri(Connection con) {
		return getPaymentModelUri(con.getNeedURI());
	}
	
}
