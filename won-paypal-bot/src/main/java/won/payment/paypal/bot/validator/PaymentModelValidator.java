package won.payment.paypal.bot.validator;

import java.util.regex.Pattern;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.protocol.vocabulary.WONPAY;

/**
 * Validator for a paypal payment model.
 * 
 * @author schokobaer
 *
 */
public class PaymentModelValidator {

	private static final Logger logger = LoggerFactory.getLogger(PaymentModelValidator.class);

	public static final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$",
			Pattern.CASE_INSENSITIVE);
	public static final Pattern CURRENCY_PATTERN = Pattern.compile("^(EUR|USD|GBP|BTC)$", Pattern.CASE_INSENSITIVE);

	/**
	 * Validates the first resource of the model.
	 * 
	 * @param model
	 *            Payment Model.
	 * @throws Exception
	 *             If something is wrong.
	 */
	public void validate(Model model) throws Exception {
		validate(model.listSubjects().next());
	}

	/**
	 * Validates a resource with the given Paypal payment data.
	 * 
	 * @param baseRes
	 *            Resource to validate.
	 * @throws Exception
	 *             If something is wrong.
	 */
	public void validate(Resource baseRes) throws Exception {

		// get Resource
		if (baseRes == null) {
			throw new Exception("No Resource defined");
		}

		// Must haves: amount, currency, email, secret, counterpart

		// Amount
		if (!baseRes.hasProperty(WONPAY.HAS_AMOUNT)) {
			throw new Exception("No amount defined");
		} else if (baseRes.getProperty(WONPAY.HAS_AMOUNT).getLiteral().getDouble() <= 0) {
			throw new Exception("Negative amount defined");
		}

		// Currency
		if (!baseRes.hasProperty(WONPAY.HAS_CURRENCY)) {
			throw new Exception("No currency defined");
		} else if (!CURRENCY_PATTERN.matcher(baseRes.getProperty(WONPAY.HAS_CURRENCY).getLiteral().getString())
				.find()) {
			throw new Exception(
					"No valid curreny defined: " + baseRes.getProperty(WONPAY.HAS_CURRENCY).getLiteral().getString());
		}

		// Receiver
		if (!baseRes.hasProperty(WONPAY.HAS_RECEIVER)) {
			throw new Exception("No receiver defined");
		} else if (!EMAIL_ADDRESS_PATTERN.matcher(baseRes.getProperty(WONPAY.HAS_RECEIVER).getLiteral().getString())
				.find()) {
			throw new Exception("Unvalid receiver defined. Receiver must be an email address");
		}

		// Secret
		if (!baseRes.hasProperty(WONPAY.HAS_SECRET)) {
			throw new Exception("No secret defined");
		}

		// Counterpart
		if (!baseRes.hasProperty(WONPAY.HAS_NEED_COUNTERPART)) {
			throw new Exception("No counterpart need defined");
		}

	}

}
