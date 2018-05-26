package won.payment.paypal.service.impl;

/**
 * Response status of a paypal payment from the API.
 * 
 * @author schokobaer
 *
 */
public enum PaypalPaymentStatus {

	CREATED("CREATED"), COMPLETED("COMPLETED"), ERROR("ERROR"), EXPIRED("EXPIRED"), INCOMPLETE(
			"INCOMPLETE"), PROCESSING("PROCESSING"), PENDING("PENDING"), REVERSALERROR("REVERSALERROR");

	private String strValue;

	private PaypalPaymentStatus(String val) {
		this.strValue = val;
	}

	public static PaypalPaymentStatus fromValue(String v) {
		for (PaypalPaymentStatus c : values()) {
			if (c.strValue.equals(v)) {
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}
}
