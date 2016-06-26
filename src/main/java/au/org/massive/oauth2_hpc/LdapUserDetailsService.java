package au.org.massive.oauth2_hpc;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.log4j.Logger;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Queries an LDAP server for a uid based on a search filter
 * @author jrigby
 *
 */
public class LdapUserDetailsService implements UserDetailsService {
	
	private static final Logger log = Logger.getLogger(LdapUserDetailsService.class.getName());
	private static final Settings settings = Settings.getInstance();

	/**
	 * Queries LDAP for the given user
	 * @param userName used in the search filter
	 */
	@Override
	public UserDetails loadUserByUsername(String userName)
			throws UsernameNotFoundException {
		UserDetailsCache userCache = new UserDetailsCache();
		UserDetails userFromCache = userCache.getUserDetails(userName);
		if (userFromCache != null) {
			log.info("User lookup cache hit: mapped user "+userName+" to "+userFromCache.getUsername());
			return userFromCache;
		} else {
			log.info("User lookup cache miss; querying LDAP...");
		}
		try {
			// Set up the environment for creating the initial context
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			env.put(Context.PROVIDER_URL, settings.getLdapProviderUrl());

			// Authenticate if credentials were provided
			if (settings.getLdapSecurityPrincipal() != null) {
				env.put(Context.SECURITY_AUTHENTICATION, "simple");
				env.put(Context.SECURITY_PRINCIPAL, settings.getLdapSecurityPrincipal());
				env.put(Context.SECURITY_CREDENTIALS, settings.getLdapSecurityPassword());
			}
			DirContext ctx = new InitialDirContext(env);
			String searchFilter = String.format(settings.getLdapSearchFilter(), escapeLDAPSearchFilter(userName));
			SearchControls searchControls = new SearchControls();
			if (settings.getLdapSearchSubtree()) {
				searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			}
			NamingEnumeration<SearchResult> results = ctx.search(settings.getLdapSearchBaseDn(), searchFilter, searchControls);
			if (results.hasMore()) {
				UserDetails userObject = createUserObject(results.next(), userName);
				log.info("Mapped "+userName+" to LDAP uid "+userObject.getUsername());
				userCache.saveUserDetails(userName, userObject);
				log.info("User details stored in cache.");
				return userObject;
			} else {
				log.info("Could not find "+userName+" in LDAP");
				throw new UsernameNotFoundException(userName);
			}
		} catch (NamingException e) {
			e.printStackTrace();
			throw new UsernameNotFoundException(userName, e);
		}
		
	}
	
	/**
	 * Converts an LDAP search result into a {@link UserDetails} object
	 * @param massiveLdapSearchResult the ldap search result
	 * @return a UserDetails object
	 * @throws NamingException if the <pre>uid</pre> attribute does not exist
	 */
	private UserDetails createUserObject(SearchResult massiveLdapSearchResult, String email) throws NamingException {
		String userId = (String) massiveLdapSearchResult.getAttributes().get("uid").get();
		List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
		return new UserDetailsImpl(userId, "password", email, authorities);
	}

	/**
	 * Prevent LDAP injection
	 * @param filter LDAP filter string to escape
	 * @return escaped string
	 */
	private String escapeLDAPSearchFilter(String filter) {
	       StringBuilder sb = new StringBuilder();
	       for (int i = 0; i < filter.length(); i++) {
	           char curChar = filter.charAt(i);
	           switch (curChar) {
	               case '\\':
	                   sb.append("\\5c");
	                   break;
	               case '*':
	                   sb.append("\\2a");
	                   break;
	               case '(':
	                   sb.append("\\28");
	                   break;
	               case ')':
	                   sb.append("\\29");
	                   break;
	               case '\u0000': 
	                   sb.append("\\00"); 
	                   break;
	               default:
	                   sb.append(curChar);
	           }
	       }
	       return sb.toString();
	}
}