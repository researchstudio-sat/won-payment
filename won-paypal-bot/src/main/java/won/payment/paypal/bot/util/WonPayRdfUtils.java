package won.payment.paypal.bot.util;

import java.net.URI;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

import won.protocol.model.Connection;
import won.protocol.vocabulary.WONPAY;

public class WonPayRdfUtils {

	public static String getPaymentModelUri(URI atomUri) {
		return atomUri.toString() + "/payment";
	}
	
	public static String getPaymentModelUri(Connection con) {
		return getPaymentModelUri(con.getAtomURI());
	}
	
	public static Model paymentSummary() {
		Model model = ModelFactory.createDefaultModel();
		model.setNsPrefix("", "no:uri");
		Resource res = model.createResource(model.getNsPrefixURI(""));
	    res.addProperty(RDF.type, WONPAY.PAYMENT_SUMMARY);
	    return model;
	}
	
}
