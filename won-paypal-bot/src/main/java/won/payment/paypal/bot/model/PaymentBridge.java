package won.payment.paypal.bot.model;

import java.io.Serializable;

import won.protocol.model.Connection;

/**
 * Entity which represent a payment. It hold the connections between merchant
 * and buyer, the status in which the payment is.
 * 
 * @author schokobaer
 *
 */
public class PaymentBridge implements Serializable {

	private Connection merchantConnection;
	private Connection buyerConnection;
	private String payKey;
	private PaymentStatus status = PaymentStatus.NOWHERE;

	public Connection getMerchantConnection() {
		return merchantConnection;
	}

	public void setMerchantConnection(Connection merchantConnectionUri) {
		this.merchantConnection = merchantConnectionUri;
	}

	public Connection getBuyerConnection() {
		return buyerConnection;
	}

	public void setBuyerConnection(Connection buyerConnectionUri) {
		this.buyerConnection = buyerConnectionUri;
	}

	public PaymentStatus getStatus() {
		return status;
	}

	public void setStatus(PaymentStatus status) {
		this.status = status;
	}

	public String getPayKey() {
		return payKey;
	}

	public void setPayKey(String payKey) {
		this.payKey = payKey;
	}

}
