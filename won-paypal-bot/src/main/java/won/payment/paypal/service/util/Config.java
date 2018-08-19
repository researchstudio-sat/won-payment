package won.payment.paypal.service.util;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;

import com.paypal.svcs.services.AdaptivePaymentsService;
import com.paypal.svcs.types.common.DetailLevelCode;
import com.paypal.svcs.types.common.RequestEnvelope;

import won.payment.paypal.service.impl.AdaptivePaymentsServiceStub;

/**
 * Config holder for Paypal-API.
 * 
 * @author schokobaer
 *
 */
public final class Config {

	private AdaptivePaymentsService aps = null;
	
	private String mode;
	private String username;
	private String password;
	private String signature;
	private String appId;

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	/**
	 * Returns a singelton of a paypalservice.
	 * 
	 * @return PaypalService.
	 */
	public AdaptivePaymentsService getAPS() {
		// AdaptivePaymentsService aps = new
		// AdaptivePaymentsService(Config.getAccountConfig());
		// return aps;

		if (aps != null) {
			return aps;
		}

		aps = new AdaptivePaymentsService(getConfig());
		
		// TODO: Stub
		aps = new AdaptivePaymentsServiceStub(getConfig());
		
		return aps;

	}

	/**
	 * Envelope for language en_US and detail level ALL.
	 * 
	 * @return RequestEnvelope for a paypal request.
	 */
	public RequestEnvelope getEnvelope() {
		RequestEnvelope envelope = new RequestEnvelope("en_US");
		envelope.setDetailLevel(DetailLevelCode.RETURNALL);
		return envelope;
	}

	/**
	 * Reads the config from paypal-bot.properties and returns them in a map.
	 * 
	 * @return Map with config.
	 */
	public Map<String, String> getConfig() {
		Map<String, String> configMap = new HashMap<String, String>();

		configMap.put("mode", mode);
		configMap.put("acct1.UserName", username);
		configMap.put("acct1.Password", password);
		configMap.put("acct1.Signature", signature);
		configMap.put("acct1.AppId", appId);
		
		// Change to config file properties
//		configMap.put("mode", "sandbox");
//		configMap.put("acct1.UserName", "test_api1.won.org");
//		configMap.put("acct1.Password", "RY9LWMA5CYA8GF5V");
//		configMap.put("acct1.Signature", "AovEzlPCsQMDpmPF8wyyNnan-Or2ACAcja7JlneaFv2yA2.SHCHe18ci");
//		configMap.put("acct1.AppId", "APP-80W284485P519543T");

		// Sample Certificate credential
		// configMap.put("acct2.UserName", "certuser_biz_api1.paypal.com");
		// configMap.put("acct2.Password", "D6JNKKULHN3G5B8A");
		// configMap.put("acct2.CertKey", "password");
		// configMap.put("acct2.CertPath", "resource/sdk-cert.p12");
		// configMap.put("acct2.AppId", "APP-80W284485P519543T");

		return configMap;
	}

}
