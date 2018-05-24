package won.payment.paypal.bot.model;

public enum PaymentStatus {

	UNPUBLISHED,		// Merchant is giving the payment details to the bot
	PUBLISHED,			// Merchant validated and accepted the payment from the bot
	ACCEPTED,			// Buyer has accepted the payment
	DENIED,				// Buyer has denied the payment
	CREATED,			// Bot has generated the payment
	COMPLETED,			// Buyer has executed the payment
	
}
