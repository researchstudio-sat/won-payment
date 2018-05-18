package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

public class WONPAY {

	public static final String BASE_URI = "http://purl.org/webofneeds/payment#";
	public static final String MODEL_URI = "http://purl.org/webofneeds/paymentmodel#";
	
	private static Model m = ModelFactory.createDefaultModel();
	
	// Propertys
	public static Property HAS_AMOUNT = m.createProperty(BASE_URI + "hasAmount");
	public static Property HAS_CURRENCY = m.createProperty(BASE_URI + "hasCurrency");
	public static Property HAS_RECEIVER = m.createProperty(BASE_URI + "hasReceiver");
	public static Property HAS_FEE_PAYER = m.createProperty(BASE_URI + "hasFeePayer");
	public static Property HAS_TAX = m.createProperty(BASE_URI + "hasTax");
	public static Property HAS_INVOICE_NUMBER = m.createProperty(BASE_URI + "hasInvoiceNumber");
	public static Property HAS_INVOICE_DETAILS = m.createProperty(BASE_URI + "hasInvoiceDetails");
	public static Property HAS_PAYMENT_STATE = m.createProperty(BASE_URI + "hasState");
	public static Property REFERS_TO = m.createProperty(BASE_URI + "refersTo");
	
	public static Property HAS_PAYPAL_TX_KEY = m.createProperty(BASE_URI + "hasPaypalTxKey");
	
	
	// PaymentStates
	public static final Resource PAYMENT_STATE_CANCELED = m.createResource(MODEL_URI + "Canceled");
	public static final Resource PAYMENT_STATE_WAITING = m.createResource(MODEL_URI + "Waiting");
	public static final Resource PAYMENT_STATE_COMPLETE = m.createResource(MODEL_URI + "Complete");
	
	// Payments
	public static final Resource PAYMENT = m.createResource(MODEL_URI + "Payment");
	public static final Resource PAYPAL_PAYMENT = m.createResource(MODEL_URI + "PaypalPayment");
	public static final Resource CASH_PAYMENT = m.createResource(MODEL_URI + "Cash");

	// PaymentFeePayer
	public static final Resource FEE_PAYER_SENDER = m.createResource(MODEL_URI + "Sender");
	public static final Resource FEE_PAYER_RECEIVER = m.createResource(MODEL_URI + "Receiver");
	
	/**
	 * RDFS
	 * 
	 * Payment a class
	 * PaypalPayment subClassOf Payment
	 * Cash subClassOf Payment
	 * 
	 * 
	 * 
	 */
}
