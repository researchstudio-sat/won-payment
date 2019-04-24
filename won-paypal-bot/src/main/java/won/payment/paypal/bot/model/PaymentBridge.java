package won.payment.paypal.bot.model;

import java.io.Serializable;

import won.protocol.model.Connection;

/**
 * Entity which represent a payment. It hold the connections between merchant
 * and buyer, the status in which the payment is.
 * 
 * @author schokobaer
 */
public class PaymentBridge implements Serializable {
    // TODO: add static final ID to prevent invalid id errors from different
    // compilers
    // TODO: think about renaming merchantConnection
    private Connection merchantConnection;
    private String payKey;
    private PaymentStatus status = PaymentStatus.NOWHERE;

    public Connection getMerchantConnection() {
        return merchantConnection;
    }

    public void setMerchantConnection(Connection merchantConnectionUri) {
        this.merchantConnection = merchantConnectionUri;
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
