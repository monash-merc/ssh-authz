package au.org.massive.oauth2_hpc;

import org.apache.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Main application entrypoint
 * @author jrigby
 *
 */
@SpringBootApplication
public class ApplicationEntrypoint {
	
	private static final Settings settings = Settings.getInstance();
	private static final Logger log = Logger.getLogger(OAuthServer.class.getName());

	/**
	 * Starts the inbuilt Tomcat server
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(ApplicationEntrypoint.class, args);
	}
	
	/**
	 * Sets the Tomcat server port / protocol according to the configuration file
	 * @return a configured container factory
	 */
	@Bean
	public EmbeddedServletContainerFactory tomcat() throws UnknownHostException {
	    TomcatEmbeddedServletContainerFactory myFactory = new TomcatEmbeddedServletContainerFactory();
	    if (!settings.getTomcatBindAddress().toLowerCase().equals("localhost") &&
				settings.getAuthenticaionMode() == AuthenticationMode.HTTP_HEADERS) {
	    	log.error("You are using the HTTP_HEADERS method of authentication but not binding to localhost. " +
					"Make sure you lock down your server to prevent malicious HTTP request headers being supplied.");
	    	if (!settings.ignoreSecurityWarnings()) {
	    		log.info("The server will now exit. Please set `ignore-security-warnings` to true to override this security check.");
				throw new RuntimeException("Security checks failed.");
			}
		}
		myFactory.setAddress(InetAddress.getByName(settings.getTomcatBindAddress()));
	    myFactory.setProtocol(settings.getTomcatProtocol());
	    myFactory.setPort(settings.getTomcatPort());
	    return myFactory;
	}

	@Bean
	public InternalResourceViewResolver setupViewResolver() {
		InternalResourceViewResolver resolver = new InternalResourceViewResolver();
		resolver.setPrefix("/jsp/");
		resolver.setSuffix(".jsp");
		return resolver;
	}
	
}
