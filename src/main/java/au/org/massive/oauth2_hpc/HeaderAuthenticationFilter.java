package au.org.massive.oauth2_hpc;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

/**
 * Authentication filter to extract the user name (usually email) from HTTP headers
 * @author jrigby
 *
 */
public class HeaderAuthenticationFilter extends RequestHeaderAuthenticationFilter {
	
	private static final String HEADER = Settings.getInstance().getUpstreamAuthHeaderName();
	
	public HeaderAuthenticationFilter() {
		super();
		setPrincipalRequestHeader(HEADER);
		setExceptionIfHeaderMissing(false);
	}

}
