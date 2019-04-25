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
 */
public class WONPAY {
    public static final String BASE_URI = "https://w3id.org/won/payment#";
    public static final String MODEL_URI = "https://w3id.org/won/payment#";
    private static final Model m = ModelFactory.createDefaultModel();
    // Properties
    public static final Property HAS_PAYPAL_PAYKEY = m.createProperty(BASE_URI + "hasPaypalPayKey");
    public static final Property HAS_ATOM_COUNTERPART = m.createProperty(BASE_URI + "hasAtomCounterpart");
    // Payments
    public static final Resource PAYMENT = m.createResource(MODEL_URI + "Payment");
    public static final Resource PAYMENT_SUMMARY = m.createResource(MODEL_URI + "PaymentSummary");
    // PaymentFeePayer
    public static final Resource FEE_PAYER_SENDER = m.createResource(MODEL_URI + "Sender").addLiteral(RDFS.label,
                    "SENDER");
    public static final Resource FEE_PAYER_RECEIVER = m.createResource(MODEL_URI + "Receiver").addLiteral(RDFS.label,
                    "EACHRECEIVER");
    // currently not used. may be useful or modelling payment requirements in the
    // future.
    // public static class CUR {
    // public static final String BASE_URI = "http://www.w3.org/2007/ont/currency#";
    // private static final Model m = ModelFactory.createDefaultModel();
    // public static final Resource CURRENCY = m.createResource(BASE_URI +
    // "Currency");
    // public static final Property CODE = m.createProperty(BASE_URI + "code");
    // public static final Property SIGN = m.createProperty(BASE_URI + "sign");
    // public static final Resource AUD = m.createResource(BASE_URI +
    // "AUD").addLiteral(CODE, "AUD");
    // public static final Resource BRL = m.createResource(BASE_URI +
    // "BRL").addLiteral(CODE, "BRL").addLiteral(SIGN,
    // "R$");
    // public static final Resource EUR = m.createResource(BASE_URI +
    // "EUR").addLiteral(CODE, "EUR").addLiteral(SIGN,
    // "€");
    // public static final Resource JPY = m.createResource(BASE_URI +
    // "JPY").addLiteral(CODE, "JPY").addLiteral(SIGN,
    // "¥");
    // public static final Resource GBP = m.createResource(BASE_URI +
    // "GBP").addLiteral(CODE, "GBP").addLiteral(SIGN,
    // "£");
    // public static final Resource USD = m.createResource(BASE_URI +
    // "USD").addLiteral(CODE, "USD").addLiteral(SIGN,
    // "$");
    // }
}
