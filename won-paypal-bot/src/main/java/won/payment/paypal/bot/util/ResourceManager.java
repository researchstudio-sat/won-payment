package won.payment.paypal.bot.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.jena.shared.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages resources.
 * 
 * @author schokobaer
 */
public class ResourceManager {
    private static final Logger logger = LoggerFactory.getLogger(ResourceManager.class);

    /**
     * Returns the given resource as string.
     * 
     * @param path Path of the resource in the folder.
     * @return String of the content of the resource.
     */
    public static String getResourceAsString(String path) {
        InputStream is = ResourceManager.class.getResourceAsStream(path);
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(is, writer, Charsets.UTF_8);
        } catch (IOException e) {
            logger.error("Could not read Resource", e);
            throw new NotFoundException("failed to load resource: " + path);
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                logger.error("Could not close Resource", e);
            }
        }
        return writer.toString();
    }
}
