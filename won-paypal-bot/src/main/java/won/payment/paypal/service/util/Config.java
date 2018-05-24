package won.payment.paypal.service.util;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;

import com.paypal.svcs.services.AdaptivePaymentsService;
import com.paypal.svcs.types.common.DetailLevelCode;
import com.paypal.svcs.types.common.RequestEnvelope;

public final class Config {

	private static AdaptivePaymentsService aps = null;

	@Value("${paypal.api.mode}")
	private String mode;
	@Value("${paypal.api.acct1.UserName}")
	private String username;
	@Value("${paypal.api.acct1.Password}")
	private String password;
	@Value("${paypal.api.acct1.Signature}")
	private String signature;
	@Value("${paypal.api.acct1.AppId}")
	private String appId;

	private Config() {
	}

	public static final AdaptivePaymentsService getAPS() {
		// AdaptivePaymentsService aps = new
		// AdaptivePaymentsService(Config.getAccountConfig());
		// return aps;

		if (aps != null) {
			return aps;
		}

		Config config = new Config();
		aps = new AdaptivePaymentsService(config.getConfig());
		return aps;

	}

	public static final RequestEnvelope getEnvelope() {
		RequestEnvelope envelope = new RequestEnvelope("en_US");
		envelope.setDetailLevel(DetailLevelCode.RETURNALL);
		return envelope;
	}

	public final Map<String, String> getConfig() {
		Map<String, String> configMap = new HashMap<String, String>();

		// Account Credential
//		configMap.put("mode", mode);
//		configMap.put("acct1.UserName", username);
//		configMap.put("acct1.Password", password);
//		configMap.put("acct1.Signature", signature);
//		configMap.put("acct1.AppId", appId);
		
		// TODO: Change to config file properties
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
