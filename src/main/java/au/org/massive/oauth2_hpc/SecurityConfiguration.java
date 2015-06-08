package au.org.massive.oauth2_hpc;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

/**
 * Service security configuration
 * @author jrigby
 *
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {
	
	private PreAuthenticatedAuthenticationProvider preAuthenticatedProvider;
	private UserDetailsService userDetailsService;

	public SecurityConfiguration() {
		super();
		
		userDetailsService = new LdapUserDetailsService();
        UserDetailsByNameServiceWrapper<PreAuthenticatedAuthenticationToken> wrapper = new UserDetailsByNameServiceWrapper<PreAuthenticatedAuthenticationToken>(
                userDetailsService);

        preAuthenticatedProvider = new PreAuthenticatedAuthenticationProvider();
        preAuthenticatedProvider.setPreAuthenticatedUserDetailsService(wrapper);
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth)
			throws Exception {
		auth.authenticationProvider(preAuthenticatedProvider);
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		ShibbolethHeaderAuthenticationFilter shibbolethFilter = new ShibbolethHeaderAuthenticationFilter();
		shibbolethFilter.setAuthenticationManager(authenticationManager());
		http.addFilter(shibbolethFilter)
				.authorizeRequests()
				.antMatchers("/oauth/authorize").fullyAuthenticated()
				.and()
				.csrf().disable();
	}

	@Override
	public void configure(WebSecurity web) throws Exception {
		super.configure(web);
		web.ignoring().antMatchers("/oauth/token_key");
	}

	
}
