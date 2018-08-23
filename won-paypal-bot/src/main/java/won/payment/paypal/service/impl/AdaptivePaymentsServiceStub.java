package won.payment.paypal.service.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

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
import com.paypal.svcs.types.common.AckCode;
import com.paypal.svcs.types.common.ResponseEnvelope;

public class AdaptivePaymentsServiceStub extends AdaptivePaymentsService {

	private int payKeyIndex = 0;
	private Map<String, Integer> amountCalls = new HashMap<>();
	
	public AdaptivePaymentsServiceStub(Map<String, String> config) {
		super(config);
	}
	
	@Override
	public PayResponse pay(PayRequest payRequest) throws SSLConfigurationException, InvalidCredentialException,
			UnsupportedEncodingException, IOException, HttpErrorException, InvalidResponseDataException,
			ClientActionRequiredException, MissingCredentialException, InterruptedException, OAuthException {
		
		PayResponse payResponse = new PayResponse();
		ResponseEnvelope responseEnvelope = new ResponseEnvelope();
		responseEnvelope.setAck(AckCode.SUCCESS);
		payResponse.setResponseEnvelope(responseEnvelope);
		String payKey = "AP-" + (++payKeyIndex) + "-00";
		payResponse.setPayKey(payKey);
		amountCalls.put(payKey, 0);
		
		return payResponse;
	}
	
	@Override
	public PaymentDetailsResponse paymentDetails(PaymentDetailsRequest paymentDetailsRequest)
			throws SSLConfigurationException, InvalidCredentialException, UnsupportedEncodingException, IOException,
			HttpErrorException, InvalidResponseDataException, ClientActionRequiredException, MissingCredentialException,
			InterruptedException, OAuthException {

		PaymentDetailsResponse paymentDetailsResponse = new PaymentDetailsResponse();
		ResponseEnvelope responseEnvelope = new ResponseEnvelope();
		responseEnvelope.setAck(AckCode.SUCCESS);
		paymentDetailsResponse.setResponseEnvelope(responseEnvelope);
		
		if (amountCalls.get(paymentDetailsRequest.getPayKey()) == 0) {
			amountCalls.put(paymentDetailsRequest.getPayKey(), 1);
			paymentDetailsResponse.setStatus("CREATED");
		} else {
			paymentDetailsResponse.setStatus("COMPLETED");
		}
		
		return paymentDetailsResponse;
	}
	
}
