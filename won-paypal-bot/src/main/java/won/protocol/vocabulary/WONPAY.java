package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
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

	private static Model m = ModelFactory.createDefaultModel();

	// Propertys
	public static final Property HAS_AMOUNT = m.createProperty(BASE_URI + "hasAmount");
	public static final Property HAS_CURRENCY = m.createProperty(BASE_URI + "hasCurrency");
	public static final Property HAS_RECEIVER = m.createProperty(BASE_URI + "hasReceiver");
	public static final Property HAS_FEE_PAYER = m.createProperty(BASE_URI + "hasFeePayer");
	public static final Property HAS_TAX = m.createProperty(BASE_URI + "hasTax");
	public static final Property HAS_INVOICE_NUMBER = m.createProperty(BASE_URI + "hasInvoiceNumber");
	public static final Property HAS_INVOICE_DETAILS = m.createProperty(BASE_URI + "hasInvoiceDetails");
	public static final Property HAS_EXPIRATION_TIME = m.createProperty(BASE_URI + "haseExpirationTime");
	public static final Property HAS_PAYMENT_STATE = m.createProperty(BASE_URI + "hasState");
	public static final Property REFERS_TO = m.createProperty(BASE_URI + "refersTo");

	public static final Property HAS_PAYPAL_PAYKEY = m.createProperty(BASE_URI + "hasPaypalPayKey");

	public static final Property HAS_SECRET = m.createProperty(BASE_URI + "hasSecret");
	public static final Property HAS_NEED_COUNTERPART = m.createProperty(BASE_URI + "hasNeedCounterpart");

	// Payments
	public static final Resource PAYMENT = m.createResource(MODEL_URI + "Payment");
	public static final Resource PAYMENT_SUMMARY = m.createResource(MODEL_URI + "PaymentSummary");

	// PaymentFeePayer
	public static final Resource FEE_PAYER_SENDER = m.createResource(MODEL_URI + "Sender").addLiteral(RDFS.label, "SENDER");
	public static final Resource FEE_PAYER_RECEIVER = m.createResource(MODEL_URI + "Receiver").addLiteral(RDFS.label, "RECEIVER");

}
