package won.payment.paypal.bot.model;

/**
 * Status of a payment.
 * 
 * @author schokobaer
 *
 */
public enum PaymentStatus {

	NOWHERE, // Connection created; Merchant Connection stored; Merchant does not have accepted the message yet
	BUILDING, // Merchant is passing information to the bot, until the goal is reached and the merchant accepts the proposal
	MERCHANTACCEPTED, // Merchant has accepted a proposal, but the payment could not be published yet
	PUBLISHED, // Merchant has accepted the Payment Proposal; Connection got created to buyer but he has not accepted it yet
	DENIED, // The buyer has denied the connection
	
	ACCEPTED, // Connection (and also Payment) is accepted by buyer but not generated yet on Paypal
	GENERATED, // Bot has generated the payment
	
	
	COMPLETED, // Buyer has executed the payment
	EXPIRED, // Payment expired on Paypal
	FAILURE, // There was a failure on the payment
	

}
