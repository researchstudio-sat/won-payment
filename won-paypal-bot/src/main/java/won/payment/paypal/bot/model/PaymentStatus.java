package won.payment.paypal.bot.model;

/**
 * Status of a payment.
 * 
 * @author schokobaer
 */
public enum PaymentStatus {
    NOWHERE, // Connection request sent, but not yet accepted
    BUILDING, // Connection is established and information can be sent to the bot, until the
              // goal is reached and a payment proposal is accepted
    PAYMODEL_ACCEPTED, // Proposal was accepted, but the Paypal Payment (PP) is not generated yet
    GENERATED, // PP was generated and proposed
    PP_DENIED, // PP was rejected, cancellation-proposal for the payment was sent
    PP_ACCEPTED, // PP was accepted
    // PayPal-Payment states:
    COMPLETED, // Payment completed on Paypal
    EXPIRED, // Payment expired on Paypal
    FAILURE, // Payment failed on Paypal
}
