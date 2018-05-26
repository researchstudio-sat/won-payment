package won.payment.paypal.bot.model;

/**
 * Status of a payment.
 * 
 * @author schokobaer
 *
 */
public enum PaymentStatus {

	NOWHERE, // Not even the merchant has accepted the payment
	UNPUBLISHED, // Merchant is giving the payment details to the bot
	PUBLISHED, // Merchant validated and accepted the payment from the bot
	ACCEPTED, // Buyer has accepted the payment
	DENIED, // Buyer has denied the payment
	GENERATED, // Bot has generated the payment
	COMPLETED, // Buyer has executed the payment

}
