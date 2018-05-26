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

/**
 * Rdf Utils for Payment Messages.
 * 
 * @author schokobaer
 *
 */
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

		String strAmount = "";
		String strCurrency = "";
		String strReceiver = "";
		String strSecret = "";
		String strCounterpart = "";
		String strFee = "Fee-Payer: SENDER \n";
		String strTax = "";
		String strInvoiceNumber = "";
		String strInvoiceDetails = "";

		// Defaults
		msgResource.addProperty(RDF.type, WONPAY.PAYPAL_PAYMENT);
		msgResource.addProperty(WONPAY.HAS_FEE_PAYER, WONPAY.FEE_PAYER_SENDER);

		// Amount
		if (paymentDetails.containsKey(PAY_AMOUNT)) {
			Double amount = Double.parseDouble(paymentDetails.get(PAY_AMOUNT));
			msgResource.addLiteral(WONPAY.HAS_AMOUNT, amount);
			strAmount = "Amount: " + amount.toString() + " \n";
		}

		// Currency
		if (paymentDetails.containsKey(PAY_CURRENCY)) {
			msgResource.addProperty(WONPAY.HAS_CURRENCY, paymentDetails.get(PAY_CURRENCY));
			strCurrency = "Currency: " + paymentDetails.get(PAY_CURRENCY) + " \n";
		}

		// Receiver
		if (paymentDetails.containsKey(PAY_RECEIVER)) {
			msgResource.addProperty(WONPAY.HAS_RECEIVER, paymentDetails.get(PAY_RECEIVER));
			strReceiver = "Receiver: " + paymentDetails.get(PAY_RECEIVER) + " \n";
		}

		// Tax
		if (paymentDetails.containsKey(PAY_TAX)) {
			// TODO: How to handle the tax?
			Double tax = Double.parseDouble(paymentDetails.get(PAY_TAX).replace("€", ""));
			msgResource.addLiteral(WONPAY.HAS_TAX, tax);
			strTax = "Tax: € " + tax + " \n";
		}

		// Invoice Number
		if (paymentDetails.containsKey(PAY_INVOICE_NUMBER)) {
			msgResource.addLiteral(WONPAY.HAS_INVOICE_NUMBER, paymentDetails.get(PAY_INVOICE_NUMBER));
			strInvoiceNumber = "Invoice Number: " + paymentDetails.get(PAY_INVOICE_NUMBER) + " \n";
		}

		// Invoice Details
		if (paymentDetails.containsKey(PAY_INVOICE_DETAILS)) {
			msgResource.addLiteral(WONPAY.HAS_INVOICE_DETAILS, paymentDetails.get(PAY_INVOICE_DETAILS));
			strInvoiceDetails = "Invoice Details: " + paymentDetails.get(PAY_INVOICE_DETAILS) + " \n";
		}

		// Fee Payer
		if (paymentDetails.containsKey(PAY_FEE_PAYER)) {
			String feePayer = paymentDetails.get(PAY_FEE_PAYER).toUpperCase();
			strFee = "Fee-Payer: " + feePayer + " \n";
			if ("SENDER".equals(feePayer)) {
				msgResource.addProperty(WONPAY.HAS_FEE_PAYER, WONPAY.FEE_PAYER_SENDER);
			} else if ("RECEIVER".equals(feePayer)) {
				msgResource.addProperty(WONPAY.HAS_FEE_PAYER, WONPAY.FEE_PAYER_RECEIVER);
			}
		}

		// Secret
		if (paymentDetails.containsKey(PAY_SECRET)) {
			msgResource.addProperty(WONPAY.HAS_SECRET, paymentDetails.get(PAY_SECRET));
			strSecret = "Secret: " + paymentDetails.get(PAY_SECRET) + " \n";
		}

		// Counterpart
		if (paymentDetails.containsKey(PAY_COUNTERPART)) {
			msgResource.addProperty(WONPAY.HAS_NEED_COUNTERPART, paymentDetails.get(PAY_COUNTERPART));
			strCounterpart += "Need Counterpart: " + paymentDetails.get(PAY_COUNTERPART) + " \n";
		}

		// Message
		String message = strAmount + strCurrency + strTax + strFee + strReceiver + strInvoiceNumber + strInvoiceDetails
				+ strSecret + strCounterpart;
		msgResource.addProperty(WON.HAS_TEXT_MESSAGE, message, "en");

		return model;
	}

	/**
	 * Generates a message with a paypal transaction key (payKey).
	 * 
	 * @param reference
	 *            the refered paypal payment (outdated).
	 * @param payKey
	 *            The paykey of the transaction.
	 * @param msg
	 *            the message to send.
	 * @return Model with all needed data.
	 */
	public static Model generatePaypalKeyMessage(Resource reference, String payKey, String msg) {
		Model model = createModelWithBaseResource();
		Resource baseRes = createResource(model);

		// baseRes.addProperty(WONPAY.REFERS_TO, reference);
		baseRes.addProperty(WONPAY.HAS_PAYPAL_TX_KEY, payKey);
		baseRes.addProperty(WON.HAS_TEXT_MESSAGE, msg);

		return model;
	}

	/**
	 * Validates if the given message is a accept message.
	 * 
	 * @param msg
	 *            WonMessage received from counterpart.
	 * @return true if accept message otherwise false.
	 */
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
