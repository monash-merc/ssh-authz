package au.org.massive.oauth2_hpc;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

/**
 * Authentication filter to extract the user name (email) from HTTP headers
 * @author jrigby
 *
 */
public class ShibbolethHeaderAuthenticationFilter extends RequestHeaderAuthenticationFilter {
	
	private static final String HEADER = "mail";
	
	public ShibbolethHeaderAuthenticationFilter() {
		super();
		setPrincipalRequestHeader(HEADER);
		setExceptionIfHeaderMissing(false);
	}

}
