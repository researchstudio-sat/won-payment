package won.payment.paypal.bot.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.jena.shared.NotFoundException;

public class ResourceManager {

	public static String getResourceAsString(String path) {
		InputStream is  = ResourceManager.class.getResourceAsStream(path);
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(is, writer, Charsets.UTF_8);
        } catch (IOException e) {
            throw new NotFoundException("failed to load resource: " + path);
        } finally {
            try {
                is.close();
            } catch (Exception e) {
            }
        }
        return writer.toString();
	}
	
}
