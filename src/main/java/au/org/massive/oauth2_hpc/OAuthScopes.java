package au.org.massive.oauth2_hpc;

/**
 * Defines the scopes used in the OAuth2 auth flow
 * @author jrigby
 *
 */
public enum OAuthScopes {
	HPC_ACCOUNT_ACCESS;
	
	private OAuthScopes() {
	}
	
	/**
	 * Convenience method to create the Spring security expression
	 * @return Spring Security expression
	 */
	public String toSpringExpression() {
		return "#oauth2.hasScope('"+this.name()+"')";
	}
}
