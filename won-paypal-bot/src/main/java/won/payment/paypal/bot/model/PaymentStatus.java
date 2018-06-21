package won.payment.paypal.bot.model;

/**
 * Status of a payment.
 * 
 * @author schokobaer
 *
 */
public enum PaymentStatus {

	NOWHERE, // Connection created; Merchant Connection stored; Merchant does not have accepted the message yet
	GOALUNSATISFIED, // Merchant accepted the connection; The payment goal is not satisfied yet
	GOALSATISFIED, // Goal is satisfied and proposed to Merchant but he has not accepted it yet
	PUBLISHED, // Merchant has accepted the Payment Proposal; Connection got created to buyer but he has not accepted it yet
	DENIED, // The buyer has denied the connection or rejected the proposal
	PROPOSED, // Payment is proposed to buyer, but has not accepted it yet
	ACCEPTED, // Payment is accepted by buyer but not generated yet on Paypal
	GENERATED, // Bot has generated the payment
	
	
	COMPLETED, // Buyer has executed the payment
	EXPIRED, // Payment expired on Paypal
	FAILURE, // There was a failure on the payment
	

}
