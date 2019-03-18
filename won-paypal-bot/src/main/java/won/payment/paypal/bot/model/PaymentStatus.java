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
	
	// Payment states:
	PAYMODEL_ACCEPTED, // Merchant has accepted a paymodel proposal, but the PP is not generated yet
	GENERATED,  // PP got generated and proposed to the merchant
	
	// PP-Merchant states:
	PP_DENIED, // Merchant has rejected the pp, and got a cancelation-proposal for the payment
	PP_ACCEPTED, // Merchant has accepted the PP proposal
	
	// PayPal-Payment states:
	COMPLETED, // Payment completed on Paypal
	EXPIRED, // Payment expired on Paypal
	FAILURE, // Payment failed on Paypal

}
