package won.payment.paypal.bot.model;

import won.protocol.model.Connection;

public class PaymentBridge {

	private Connection merchantConnection;
	private Connection buyerConnection;
	private PaymentStatus status = PaymentStatus.UNPUBLISHED;
	
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
	
	
	
	
}
