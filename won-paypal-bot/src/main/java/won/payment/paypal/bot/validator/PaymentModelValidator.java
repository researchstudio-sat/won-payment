package won.payment.paypal.bot.validator;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.topbraid.shacl.vocabulary.SH;

import won.bot.framework.eventbot.EventListenerContext;
import won.payment.paypal.bot.model.PaymentModelWrapper;
import won.payment.paypal.bot.util.WonPayRdfUtils;
import won.protocol.model.Connection;
import won.protocol.model.NeedState;
import won.protocol.util.NeedModelWrapper;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONPAY;

/**
 * Validator for a paypal payment model.
 * 
 * @author schokobaer
 *
 */
public class PaymentModelValidator {

	private static final List<String> SUPPORTED_CURRENCIES =
            Arrays.asList(
                new String[] {
                    "AUD", "BRL", "CAD", "CZK", "DKK", "EUR",
                    "HKD", "HUF", "ILS", "JPY", "MYR", "MXN",
                    "NOK", "NZD", "PHP", "PLN", "GBP", "SGD",
                    "SEK", "CHF", "TWD", "THB", "USD"
                }
            );

	private EventListenerContext ctx;
	private ValidatorFactory factory;

	public PaymentModelValidator(EventListenerContext ctx) {
		this.ctx = ctx;
		factory = Validation.buildDefaultValidatorFactory();
	}

	public void validate(PaymentModelWrapper payment, Connection con) throws Exception {
		// Check standard constraints
		Set<ConstraintViolation<PaymentModelWrapper>> violations = factory.getValidator().validate(payment);
		if (!violations.isEmpty()) {
			throw new Exception(violations.iterator().next().getMessage());
		}

		// Left overs:
		// - Currency in CURRENCY-list
		// - Counterpart Accessible
		// - FeePayer is one of the Resources defined in WONPAY
		// - 5m < expiration < 30d

		// Currency
		if (!SUPPORTED_CURRENCIES.contains(payment.getCurrency())) {
			throw new Exception("Not a supported currency");
		}

		// Counterpart accessible
		if (!counterpartAccessible(payment.getCounterpartNeedUri(), con)) {
			throw new Exception("Counterpart not accessible");
		}

		// Fee Payer
		if (!WONPAY.FEE_PAYER_SENDER.equals(payment.getFeePayer())
				&& !WONPAY.FEE_PAYER_RECEIVER.equals(payment.getFeePayer())) {
			throw new Exception("Unvalid fees payer defined");
		}

		// Expiration
		if (!validExpiration(payment.getExpirationTime())) {
			throw new Exception("Unvalid expiration time");
		}
	}

	private boolean validExpiration(String expiration) throws DatatypeConfigurationException {
		DatatypeFactory dtf = DatatypeFactory.newInstance();
		Duration duration = dtf.newDuration(expiration);
		return !(duration.isShorterThan(dtf.newDuration(true, 0, 0, 0, 0, 5, 0))
				|| duration.isLongerThan(dtf.newDuration(true, 0, 0, 30, 0, 0, 0)));
	}

	/**
	 * Checks if the given Need uri is available and not deactivated.
	 * 
	 * @param counterpartNeedUri
	 *            Need Uri.
	 * @param con
	 *            Connection of the merchant need.
	 * @return true if accessible, otherwise false.
	 */
	private boolean counterpartAccessible(URI counterpartNeedUri, Connection con) {
		Model result = WonRdfUtils.MessageUtils.processingMessage("Error with the counterpart need");
		Resource report = result.createResource();
		result = WonRdfUtils.MessageUtils.addToMessage(result, SH.result, report);
		report.addProperty(SH.value, new ResourceImpl(counterpartNeedUri.toString()));
		report.addProperty(SH.resultPath, WONPAY.HAS_NEED_COUNTERPART);
		report.addProperty(SH.resultSeverity, SH.Violation);
		report.addProperty(SH.focusNode, new ResourceImpl(WonPayRdfUtils.getPaymentModelUri(con)));

		try {
			Dataset remoteNeedRDF = ctx.getLinkedDataSource().getDataForResource(counterpartNeedUri);
			if (remoteNeedRDF == null) {
				throw new NullPointerException();
			}
			NeedModelWrapper needWrapper = new NeedModelWrapper(remoteNeedRDF);
			if (needWrapper.getNeedState() == NeedState.ACTIVE) {
				// Need active
				return true;
			}
			// Need inactive
			report.addProperty(SH.resultMessage, "Counterpart need is inactive");
		} catch (NullPointerException e) {
			// Need not accessible
			report.addProperty(SH.resultMessage, "Counterpart need is not accessible");
		}

		// ConnectionMessageCommandEvent response = new
		// ConnectionMessageCommandEvent(con, result);
		// ctx.getEventBus().publish(response);

		return false;
	}

}
