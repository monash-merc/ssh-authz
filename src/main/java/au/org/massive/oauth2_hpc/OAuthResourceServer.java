package au.org.massive.oauth2_hpc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationManager;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

/**
 * Configuration for the OAuth2 resource server
 * @author jrigby
 *
 */
@Configuration
@EnableResourceServer
@EnableWebSecurity
public class OAuthResourceServer extends ResourceServerConfigurerAdapter {
	
	public static final String RESOURCE_ID = "authorize_key_resource";
	
	@Autowired
	private JwtAccessTokenConverter jwtAccessTokenConverter;

	@Override
	public void configure(HttpSecurity http) throws Exception {
		http.requestMatchers().antMatchers("/api/v1/**")
		.and().authorizeRequests().anyRequest().access(OAuthScopes.HPC_ACCOUNT_ACCESS.toSpringExpression());
	}
	
	@Bean
	public AuthenticationManager authenticationManager() {
		OAuth2AuthenticationManager authenticationManager = new OAuth2AuthenticationManager();
		authenticationManager.setTokenServices(defaultTokenServices());
		return authenticationManager;
	}
	
	@Bean
	public ResourceServerTokenServices defaultTokenServices() {
		DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
		defaultTokenServices.setTokenEnhancer(jwtAccessTokenConverter);
		defaultTokenServices.setTokenStore(new JwtTokenStore(jwtAccessTokenConverter));
		return defaultTokenServices;
	}

	@Override
	public void configure(ResourceServerSecurityConfigurer resources)
			throws Exception {
		resources.authenticationManager(authenticationManager())
			.tokenServices(defaultTokenServices())
			.resourceId(RESOURCE_ID);
	}

}
