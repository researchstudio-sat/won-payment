package won.payment.paypal.bot.validator;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import won.payment.paypal.bot.model.PaymentModelWrapper;
import won.protocol.model.Connection;
import won.protocol.vocabulary.WONPAY;

/**
 * Validator for a paypal payment model. Should validate whether the payment
 * model would be accepted by paypal. Validation for the bot is already done via
 * SHACL constraints.
 * 
 * @author schokobaer
 */
public class PaymentModelValidator {
    private ValidatorFactory factory;

    public PaymentModelValidator() {
        factory = Validation.buildDefaultValidatorFactory();
    }

    public void validate(PaymentModelWrapper payment, Connection con) throws Exception {
        // Check standard constraints
        Set<ConstraintViolation<PaymentModelWrapper>> violations = factory.getValidator().validate(payment);
        if (!violations.isEmpty()) {
            throw new Exception(violations.iterator().next().getMessage());
        }
        // Leftovers:
        // - Counterpart Accessible
        // - FeePayer is one of the Resources defined in WONPAY
        // - 5m < expiration < 30d
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
}
