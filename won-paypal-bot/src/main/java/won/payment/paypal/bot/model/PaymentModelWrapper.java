package won.payment.paypal.bot.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;

import com.paypal.svcs.types.ap.PayRequest;
import com.paypal.svcs.types.ap.Receiver;
import com.paypal.svcs.types.ap.ReceiverList;

import won.payment.paypal.bot.util.InformationExtractor;
import won.protocol.vocabulary.WONPAY;

public class PaymentModelWrapper {

	@NotNull
	private String uri;

	@NotNull
	//@Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")
	@Email
	private String receiver;

	@NotNull
	@Digits(integer = 10, fraction = 2)
	@DecimalMin(inclusive = false, value = "0")
	private Double amount;

	@NotNull
	@Size(min = 3, max = 3)
	private String currency;

	@NotNull
	@Size(min = 4)
	private String secret;

	@NotNull
	private Resource counterpartNeed;

	@Size(max = 127)
	private String invoiceId;

	@Size(max = 1000)
	private String invoiceDetails;

	//@Pattern(regexp = "TODO") // TODO: Find out the regex for xsd:timespan
	private String expirationTime = "PT3H";

	private Resource feePayer = WONPAY.FEE_PAYER_SENDER;

	@Digits(integer = 10, fraction = 2)
	@DecimalMin(inclusive = false, value = "0")
	private Double tax;

	public PaymentModelWrapper() {

	}

	public PaymentModelWrapper(Model payload) {
		uri = InformationExtractor.getPayment(payload).getURI();
		amount = InformationExtractor.getAmount(payload);
		currency = InformationExtractor.getCurrency(payload);
		receiver = InformationExtractor.getReceiver(payload);
		secret = InformationExtractor.getSecret(payload);
		counterpartNeed = InformationExtractor.getCounterpart(payload);
		Resource feePayerResult = InformationExtractor.getFeePayer(payload); 
		if (feePayerResult != null) {
			feePayer = feePayerResult;
		}
		String expirationTimeResult = InformationExtractor.getExpirationTime(payload); 
		if (expirationTimeResult != null) {
			expirationTime = expirationTimeResult; 
		}
		invoiceDetails = InformationExtractor.getInvoiceDetails(payload);
		invoiceId = InformationExtractor.getInvoiceId(payload);
		tax = InformationExtractor.getTax(payload);
	}

	public String getUri() {
		return uri;
	}

	public String getReceiver() {
		return receiver;
	}

	public Double getAmount() {
		return amount;
	}

	public String getCurrency() {
		return currency;
	}

	public String getSecret() {
		return secret;
	}

	public Resource getCounterpartNeed() {
		return counterpartNeed;
	}
	
	public URI getCounterpartNeedUri() throws URISyntaxException {
		return new URI(counterpartNeed.getURI());
	}

	public String getInvoiceId() {
		return invoiceId;
	}

	public String getInvoiceDetails() {
		return invoiceDetails;
	}

	public String getExpirationTime() {
		return expirationTime;
	}

	public Resource getFeePayer() {
		return feePayer;
	}

	public Double getTax() {
		return tax;
	}

	public PayRequest toPayRequest() {
		PayRequest pay = new PayRequest();
		// Set receiver
		Receiver rec = new Receiver();
		rec.setAmount(amount);
		rec.setEmail(receiver);
		pay.setReceiverList(new ReceiverList(Collections.singletonList(rec)));
		pay.setCurrencyCode(currency);
		pay.setFeesPayer(feePayer.getProperty(RDFS.label).getString());

		if (expirationTime != null) {
			pay.setPayKeyDuration(expirationTime);
		}
		if (invoiceDetails != null) {
			pay.setMemo(invoiceDetails);
		}
		if (invoiceId != null) {
			rec.setInvoiceId(invoiceId);
		}
		return pay;
	}

}
