package won.payment.paypal.service.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.paypal.svcs.services.AdaptivePaymentsService;
import com.paypal.svcs.types.common.DetailLevelCode;
import com.paypal.svcs.types.common.RequestEnvelope;

public final class Config {

	private static AdaptivePaymentsService aps = null;
	
	public static final AdaptivePaymentsService getAPS() {
		//AdaptivePaymentsService aps = new AdaptivePaymentsService(Config.getAccountConfig());
		//return aps;
		
		if (aps != null) {
			return aps;
		}
		
		try {
			aps = new AdaptivePaymentsService(Config.class.getClassLoader().getResourceAsStream("payment/paypal.properties"));
			return aps;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static final RequestEnvelope getEnvelope() {
		RequestEnvelope envelope = new RequestEnvelope("en_US");
		envelope.setDetailLevel(DetailLevelCode.RETURNALL);
		return envelope;
	}
	
	public static final Map<String, String> getAccountConfig() {
		Map<String, String> configMap = new HashMap<String, String>();
		configMap.putAll(getConfig());

		// Account Credential
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
	
	public static final Map<String,String> getConfig(){
		Map<String,String> configMap = new HashMap<String,String>();
		
		// Endpoints are varied depending on whether sandbox OR live is chosen for mode
		configMap.put("mode", "sandbox");
		
		// These values are defaulted in SDK. If you want to override default values, uncomment it and add your value.
		// configMap.put("http.ConnectionTimeOut", "5000");
		// configMap.put("http.Retry", "2");
		// configMap.put("http.ReadTimeOut", "30000");
		// configMap.put("http.MaxConnection", "100");
		return configMap;
	}
}
