package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;

/**
 * RDF Resources for payment statements.
 * 
 * @author schokobaer
 *
 */
public class WONPAY {

	public static final String BASE_URI = "http://purl.org/webofneeds/payment#";
	public static final String MODEL_URI = "http://purl.org/webofneeds/payment#";

	private static final Model m = ModelFactory.createDefaultModel();

	// Properties
	public static final Property HAS_PAYPAL_PAYKEY = m.createProperty(BASE_URI + "hasPaypalPayKey");

	// public static final Property HAS_SECRET = m.createProperty(BASE_URI + "hasSecret");
	public static final Property HAS_NEED_COUNTERPART = m.createProperty(BASE_URI + "hasNeedCounterpart");

	// Payments
	public static final Resource PAYMENT = m.createResource(MODEL_URI + "Payment");
	public static final Resource PAYMENT_SUMMARY = m.createResource(MODEL_URI + "PaymentSummary");

	// PaymentFeePayer
	public static final Resource FEE_PAYER_SENDER = m.createResource(MODEL_URI + "Sender").addLiteral(RDFS.label, "SENDER");
	public static final Resource FEE_PAYER_RECEIVER = m.createResource(MODEL_URI + "Receiver").addLiteral(RDFS.label, "EACHRECEIVER");


	public static class CUR {
		public static final String BASE_URI = "http://www.w3.org/2007/ont/currency#";
		private static final Model m = ModelFactory.createDefaultModel();

		public static final Resource CURRENCY = m.createResource(BASE_URI + "Currency");
		public static final Property CODE = m.createProperty(BASE_URI + "code");
		public static final Property SIGN = m.createProperty(BASE_URI + "sign");

		public static final Resource AUD = m.createResource(BASE_URI + "AUD").addLiteral(CODE, "AUD");
		public static final Resource BRL = m.createResource(BASE_URI + "BRL").addLiteral(CODE, "BRL").addLiteral(SIGN, "R$");
		public static final Resource CAD = m.createResource(BASE_URI + "CAD").addLiteral(CODE, "CAD");
		public static final Resource CZK = m.createResource(BASE_URI + "CZK").addLiteral(CODE, "CZK");
		public static final Resource DKK = m.createResource(BASE_URI + "DKK").addLiteral(CODE, "DKK");
		public static final Resource EUR = m.createResource(BASE_URI + "EUR").addLiteral(CODE, "EUR").addLiteral(SIGN, "€");
		public static final Resource HKD = m.createResource(BASE_URI + "HKD").addLiteral(CODE, "HKD");
		public static final Resource HUF = m.createResource(BASE_URI + "HUF").addLiteral(CODE, "HUF");
		public static final Resource ILS = m.createResource(BASE_URI + "ILS").addLiteral(CODE, "ILS");
		public static final Resource JPY = m.createResource(BASE_URI + "JPY").addLiteral(CODE, "JPY").addLiteral(SIGN, "¥");
		public static final Resource MYR = m.createResource(BASE_URI + "MYR").addLiteral(CODE, "MYR");
		public static final Resource MXN = m.createResource(BASE_URI + "MXN").addLiteral(CODE, "MXN");
		public static final Resource NOK = m.createResource(BASE_URI + "NOK").addLiteral(CODE, "NOK");
		public static final Resource NZD = m.createResource(BASE_URI + "NZD").addLiteral(CODE, "NZD");
		public static final Resource PHP = m.createResource(BASE_URI + "PHP").addLiteral(CODE, "PHP");
		public static final Resource PLN = m.createResource(BASE_URI + "PLN").addLiteral(CODE, "PLN");
		public static final Resource GBP = m.createResource(BASE_URI + "GBP").addLiteral(CODE, "GBP").addLiteral(SIGN, "£");
		public static final Resource SGD = m.createResource(BASE_URI + "SGD").addLiteral(CODE, "SGD");
		public static final Resource SEK = m.createResource(BASE_URI + "SEK").addLiteral(CODE, "SEK");
		public static final Resource CHF = m.createResource(BASE_URI + "CHF").addLiteral(CODE, "CHF");
		public static final Resource TWD = m.createResource(BASE_URI + "TWD").addLiteral(CODE, "TWD");
		public static final Resource THB = m.createResource(BASE_URI + "THB").addLiteral(CODE, "THB");
		public static final Resource USD = m.createResource(BASE_URI + "USD").addLiteral(CODE, "USD").addLiteral(SIGN, "$");

	}
}
