package won.payment.paypal.service.impl;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.paypal.exception.ClientActionRequiredException;
import com.paypal.exception.HttpErrorException;
import com.paypal.exception.InvalidCredentialException;
import com.paypal.exception.InvalidResponseDataException;
import com.paypal.exception.MissingCredentialException;
import com.paypal.exception.SSLConfigurationException;
import com.paypal.sdk.exceptions.OAuthException;
import com.paypal.svcs.services.AdaptivePaymentsService;
import com.paypal.svcs.types.ap.PayRequest;
import com.paypal.svcs.types.ap.PayResponse;
import com.paypal.svcs.types.ap.PaymentDetailsRequest;
import com.paypal.svcs.types.ap.PaymentDetailsResponse;
import com.paypal.svcs.types.ap.Receiver;
import com.paypal.svcs.types.ap.ReceiverList;
import com.paypal.svcs.types.common.AckCode;
import com.paypal.svcs.types.common.ErrorData;

import won.payment.paypal.service.util.Config;

public class PaypalPaymentService {

	private interface RequestSuccess {
		void success(Object response);
	}

	private interface RequestFailure {
		void fail(List<ErrorData> errors) throws Exception;
	}

	/**
	 * Creates a new Paypal Payment. Returns the pay key.
	 * @param receiver
	 * @param amount
	 * @param currencyCode
	 * @param feePayer
	 * @param trackingId
	 * @return
	 * @throws Exception
	 */
	public String create(String receiver, Double amount, String currencyCode, String feePayer, String trackingId) throws Exception {

		List<Receiver> receivers = new LinkedList<>();

		// Set receiver
		Receiver rec = new Receiver();
		rec.setAmount(amount);
		rec.setEmail(receiver);
		receivers.add(rec);

		ReceiverList receiverList = new ReceiverList(receivers);

		PayRequest pay = new PayRequest();
		pay.setRequestEnvelope(Config.getEnvelope());
		pay.setActionType("CREATE");
		pay.setCurrencyCode(currencyCode);
		pay.setFeesPayer(feePayer);

		pay.setReturnUrl("https://example.com/success");
		pay.setCancelUrl("https://example.com/error");

		pay.setReceiverList(receiverList);

		if (trackingId != null) {
			pay.setTrackingId(trackingId);
		}

		StringBuilder strBuilder = new StringBuilder();
		
		executeRequest(pay, response -> {
			strBuilder.append(((PayResponse)response).getPayKey());
		}, errors -> {
			throw new Exception();
		});
		
		return strBuilder.toString();
	}

	public String create(String receiver, Double amount, String currencyCode, String feePayer) throws Exception {
		return create(receiver, amount, currencyCode, feePayer, null);
	}

	/**
	 * Creates a new Paypal Payment with Sender as fee payer. Returns pay key.
	 * @param receiver
	 * @param amount
	 * @param currencyCode
	 * @return
	 * @throws Exception
	 */
	public String create(String receiver, Double amount, String currencyCode) throws Exception {
		return create(receiver, amount, currencyCode, "SENDER", null);
	}

	/**
	 * Generates the URL for executing a payment manually.
	 * 
	 * @param payKey
	 *            The payKey of the Payment to execute.
	 * @return URL-String
	 */
	public String getPaymentUrl(String payKey) {
		String url = "https://www.sandbox.paypal.com/cgi-bin/webscr?cmd=_ap-payment&paykey=" + payKey;
		return url;
	}

	/**
	 * Cancels the Transaction for the given Paykey.
	 * 
	 * @param payKey
	 *            PayKey of an existing Payment genereated before.
	 */
	public void cancel(String payKey) {
		// TODO: Implement
	}

	public PaypalPaymentStatus validate(String payKey) throws Exception {
		PaypalPaymentStatus status = PaypalPaymentStatus.ERROR;
		final StringBuilder strBuilder = new StringBuilder();
		
		PaymentDetailsRequest req = new PaymentDetailsRequest();
		req.setPayKey(payKey);
		req.setRequestEnvelope(Config.getEnvelope());
				
		executeRequest(req, response -> {
			strBuilder.append(((PaymentDetailsResponse) response).getStatus());
		}, errors -> {
			throw new Exception();
		});
		
		status = PaypalPaymentStatus.fromValue(strBuilder.toString()); 
		return status;
	}

	private void executeRequest(Object req, RequestSuccess success, RequestFailure failure) throws Exception {

		AdaptivePaymentsService aps = Config.getAPS();
		
		try {
			AckCode ack = AckCode.FAILURE;
			Object response = null;
			List<ErrorData> errors = null;
			
			if (req instanceof PayRequest) {
				PayResponse payResponse = aps.pay((PayRequest)req);
				ack = payResponse.getResponseEnvelope().getAck();
				response = payResponse;
				errors = payResponse.getError();
			}
			else if (req instanceof PaymentDetailsRequest) {
				PaymentDetailsResponse paymentDetailsResponse = aps.paymentDetails((PaymentDetailsRequest)req);
				ack = paymentDetailsResponse.getResponseEnvelope().getAck();
				response = paymentDetailsResponse;
				errors = paymentDetailsResponse.getError();
			}
			
			switch (ack) {
			case SUCCESS:
				success.success(response);
				break;
			case SUCCESSWITHWARNING:
				success.success(response);
				break;

			default:
				failure.fail(errors);
				break;
			}
			
		} catch (SSLConfigurationException | InvalidCredentialException | HttpErrorException
				| InvalidResponseDataException | ClientActionRequiredException | MissingCredentialException
				| OAuthException | IOException | InterruptedException e) {
			throw new Exception(e);
		}
	}
}
