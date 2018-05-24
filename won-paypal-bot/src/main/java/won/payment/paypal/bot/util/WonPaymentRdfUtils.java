package won.payment.paypal.bot.util;

import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

import won.protocol.message.WonMessage;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONAGR;
import won.protocol.vocabulary.WONPAY;

public class WonPaymentRdfUtils {

	public static final String PAY_AMOUNT = "pay_amount";
	public static final String PAY_CURRENCY = "pay_currency";
	public static final String PAY_RECEIVER = "pay_receiver";
	public static final String PAY_TAX = "pay_tax";
	public static final String PAY_INVOICE_NUMBER = "pay_invoicenumber";
	public static final String PAY_INVOICE_DETAILS = "pay_invoicedetails";
	public static final String PAY_FEE_PAYER = "pay_feepayer";
	public static final String PAY_SECRET = "pay_secret";
	public static final String PAY_COUNTERPART = "pay_counterpart";

	public static Model createModelWithBaseResource() {
		Model model = ModelFactory.createDefaultModel();
		model.setNsPrefix("", "no:uri");
		model.createResource(model.getNsPrefixURI(""));
		return model;
	}

	public static Resource createResource(Model model) {
		Resource baseRes = model.createResource(model.getNsPrefixURI(""));
		return baseRes;
	}

	/**
	 * Generates a Payment Model given by the Details.
	 * 
	 * @param paymentDetails
	 *            Details of Payment.
	 * @return A Model with the parsed Details.
	 */
	public static Model generatePaymentModel(Map<String, String> paymentDetails) {
		Model model = createModelWithBaseResource();
		Resource msgResource = createResource(model);

		String message = "";

		msgResource.addProperty(RDF.type, WONPAY.PAYPAL_PAYMENT);
		
		// Amount
		if (paymentDetails.containsKey(PAY_AMOUNT)) {
			Double amount = Double.parseDouble(paymentDetails.get(PAY_AMOUNT));
			msgResource.addLiteral(WONPAY.HAS_AMOUNT, amount);
			message += "Amount: " + amount.toString() + "\n";
		}

		// Currency
		if (paymentDetails.containsKey(PAY_CURRENCY)) {
			msgResource.addProperty(WONPAY.HAS_CURRENCY, paymentDetails.get(PAY_CURRENCY));
			message += "Currency: " + paymentDetails.get(PAY_CURRENCY) + "\n";
		}

		// Receiver
		if (paymentDetails.containsKey(PAY_RECEIVER)) {
			msgResource.addProperty(WONPAY.HAS_RECEIVER, paymentDetails.get(PAY_RECEIVER));
			message += "Receiver: " + paymentDetails.get(PAY_RECEIVER) + "\n";
		}

		// Tax
		if (paymentDetails.containsKey(PAY_TAX)) {
			Double tax = Double.parseDouble(paymentDetails.get(PAY_TAX).replace("€", ""));
			msgResource.addLiteral(WONPAY.HAS_TAX, tax);
			message += "Tax: € " + tax + "\n";
		}

		// Invoice Number
		if (paymentDetails.containsKey(PAY_INVOICE_NUMBER)) {
			msgResource.addLiteral(WONPAY.HAS_INVOICE_NUMBER, paymentDetails.get(PAY_INVOICE_NUMBER));
			message += "Invoice Number: " + paymentDetails.get(PAY_INVOICE_NUMBER) + "\n";
		}

		// Invoice Details
		if (paymentDetails.containsKey(PAY_INVOICE_DETAILS)) {
			msgResource.addLiteral(WONPAY.HAS_INVOICE_DETAILS, paymentDetails.get(PAY_INVOICE_DETAILS));
			message += "Invoice Details: " + paymentDetails.get(PAY_INVOICE_DETAILS) + "\n";
		}

		// Fee Payer
		if (paymentDetails.containsKey(PAY_FEE_PAYER)) {
			String feePayer = paymentDetails.get(PAY_FEE_PAYER).toLowerCase();
			message += paymentDetails.get(PAY_FEE_PAYER) + "\n";
			if ("sender".equals(feePayer)) {
				msgResource.addProperty(WONPAY.HAS_FEE_PAYER, WONPAY.FEE_PAYER_SENDER);
			} else if ("receiver".equals(feePayer)) {
				msgResource.addProperty(WONPAY.HAS_FEE_PAYER, WONPAY.FEE_PAYER_RECEIVER);
			}
		}

		// Secret
		if (paymentDetails.containsKey(PAY_SECRET)) {
			msgResource.addProperty(WONPAY.HAS_SECRET, paymentDetails.get(PAY_SECRET));
			message += "Secret: " + paymentDetails.get(PAY_SECRET) + "\n";
		}

		// Counterpart
		if (paymentDetails.containsKey(PAY_COUNTERPART)) {
			msgResource.addProperty(WONPAY.HAS_NEED_COUNTERPART, paymentDetails.get(PAY_COUNTERPART));
			message += "Need Counterpart: " + paymentDetails.get(PAY_COUNTERPART) + "\n";
		}

		// Message
		msgResource.addProperty(WON.HAS_TEXT_MESSAGE, message, "en");

		return model;
	}

	public static Model generatePaypalKeyMessage(Resource reference, String payKey, String msg) {
		Model model = createModelWithBaseResource();
		Resource baseRes = createResource(model);

//		baseRes.addProperty(WONPAY.REFERS_TO, reference);
		baseRes.addProperty(WONPAY.HAS_PAYPAL_TX_KEY, payKey);
		baseRes.addProperty(WON.HAS_TEXT_MESSAGE, msg);

		return model;
	}
	
	public static boolean isAcceptMessage(WonMessage msg) {
		
		Model model = msg.getCompleteDataset().getUnionModel();
		
		StmtIterator iterator = model.listStatements();
		while (iterator.hasNext()) {
			Statement stmt = iterator.next();
			Property prop = stmt.getPredicate();
			if (prop.equals(WONAGR.ACCEPTS)) {
				return true;
			}
		}
		
		return false;
	}

}
