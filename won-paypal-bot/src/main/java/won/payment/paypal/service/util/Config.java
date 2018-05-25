package won.payment.paypal.service.util;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;

import com.paypal.svcs.services.AdaptivePaymentsService;
import com.paypal.svcs.types.common.DetailLevelCode;
import com.paypal.svcs.types.common.RequestEnvelope;

public final class Config {

	private static AdaptivePaymentsService aps = null;

	private static final String MODE = "paypal.api.mode";
	private static final String USERNAME = "paypal.api.acct1.UserName";
	private static final String PASSWORD = "paypal.api.acct1.Password";
	private static final String SIGNATURE = "paypal.api.acct1.Signature";
	private static final String APPID = "paypal.api.acct1.AppId";

	public static final AdaptivePaymentsService getAPS() {
		// AdaptivePaymentsService aps = new
		// AdaptivePaymentsService(Config.getAccountConfig());
		// return aps;

		if (aps != null) {
			return aps;
		}

		aps = new AdaptivePaymentsService(getConfig());
		return aps;

	}

	public static final RequestEnvelope getEnvelope() {
		RequestEnvelope envelope = new RequestEnvelope("en_US");
		envelope.setDetailLevel(DetailLevelCode.RETURNALL);
		return envelope;
	}

	public static final Map<String, String> getConfig() {
		Map<String, String> configMap = new HashMap<String, String>();
/*
		// Account Credential
		if (System.getProperty(MODE) != null) {
			configMap.put("mode", System.getProperty(MODE));
		}
		if (System.getProperty(USERNAME) != null) {
			configMap.put("acct1.UserName", System.getProperty(USERNAME));
		}
		if (System.getProperty(PASSWORD) != null) {
			configMap.put("acct1.Password", System.getProperty(PASSWORD));
		}
		if (System.getProperty(SIGNATURE) != null) {
			configMap.put("acct1.Signature", System.getProperty(SIGNATURE));
		}
		if (System.getProperty(APPID) != null) {
			configMap.put("acct1.AppId", System.getProperty(APPID));
		}*/
		
		// Change to config file properties
		configMap.put("mode", "sandbox");
		configMap.put("acct1.UserName", "test_api1.won.org");
		configMap.put("acct1.Password", "RY9LWMA5CYA8GF5V");
		configMap.put("acct1.Signature", "AovEzlPCsQMDpmPF8wyyNnan-Or2ACAcja7JlneaFv2yA2.SHCHe18ci");
		configMap.put("acct1.AppId", "APP-80W284485P519543T");

		// Sample Certificate credential
		// configMap.put("acct2.UserName", "certuser_biz_api1.paypal.com");
		// configMap.put("acct2.Password", "D6JNKKULHN3G5B8A");
		// configMap.put("acct2.CertKey", "password");
		// configMap.put("acct2.CertPath", "resource/sdk-cert.p12");
		// configMap.put("acct2.AppId", "APP-80W284485P519543T");

		return configMap;
	}
	

}
