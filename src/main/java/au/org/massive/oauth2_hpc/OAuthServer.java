package au.org.massive.oauth2_hpc;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.builders.InMemoryClientDetailsServiceBuilder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

/**
 * OAuth2 authorisation server
 * @author jrigby
 *
 */
@Configuration
@EnableAuthorizationServer
public class OAuthServer extends AuthorizationServerConfigurerAdapter {
	
	private static Logger log = Logger.getLogger(OAuthServer.class.getName());
	private static Settings settings = Settings.getInstance();
	
	@Autowired
	private AuthenticationManager authenticationManager;
	
	@Autowired
	private ClientDetailsService clientDetailsService;

	@Override
	public void configure(AuthorizationServerEndpointsConfigurer endpoints)
			throws Exception {
		endpoints
		.tokenServices(defaultTokenServices())
		.authenticationManager(authenticationManager);
		
	}
	
	@Bean
	public JwtAccessTokenConverter jwtAccessTokenConverter() {
		JwtAccessTokenConverter jwtAccessTokenConverter = new JwtAccessTokenConverter();
		jwtAccessTokenConverter.setKeyPair(settings.getJWTSigningKeyPair());
		return jwtAccessTokenConverter;
	}
	
	@Bean
	public TokenEnhancerChain tokenEnhancerChain() {
		TokenEnhancerChain tokenEnhancerChain = new TokenEnhancerChain();
		List<TokenEnhancer> tokenEnhancers = new LinkedList<TokenEnhancer>();
		tokenEnhancers.add(new TokenEnhancer() {

			@Override
			public OAuth2AccessToken enhance(OAuth2AccessToken token,
					OAuth2Authentication auth) {
				UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
				Map<String,Object> additionalInformation = new HashMap<String,Object>();
				additionalInformation.putAll(token.getAdditionalInformation());
				additionalInformation.put("email", userDetails.getEmail());
				((DefaultOAuth2AccessToken) token).setAdditionalInformation(additionalInformation);
				return token;
			}
			
		});
		tokenEnhancers.add(jwtAccessTokenConverter());
		tokenEnhancerChain.setTokenEnhancers(tokenEnhancers);
		return tokenEnhancerChain;
	}
	
	@Bean
	public DefaultTokenServices defaultTokenServices() {
		
		DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
		
		defaultTokenServices.setClientDetailsService(clientDetailsService);
		defaultTokenServices.setSupportRefreshToken(true);
		defaultTokenServices.setTokenEnhancer(tokenEnhancerChain());
		defaultTokenServices.setTokenStore(new JwtTokenStore(jwtAccessTokenConverter()));
		
		return defaultTokenServices;
	}

	@Override
	public void configure(AuthorizationServerSecurityConfigurer security)
			throws Exception {
		super.configure(security);
	}

	@Override
	public void configure(ClientDetailsServiceConfigurer clients)
			throws Exception {
		final String RESOURCE_ID = OAuthResourceServer.RESOURCE_ID;
		InMemoryClientDetailsServiceBuilder clientBuilder = clients.inMemory();
		for (RegisteredClient c : settings.getRegisteredClients()) {
			log.info("Added "+c.getClientName()+" to the list of registered OAuth clients.");
			clientBuilder.withClient(c.getClientId())
				.authorizedGrantTypes(c.getAllowedGrantTypes())
				.authorities("ROLE_CLIENT")
				.scopes(OAuthScopes.HPC_ACCOUNT_ACCESS.name())
				.resourceIds(RESOURCE_ID)
				.redirectUris(c.getAllowedRedirects())
				.secret(c.getClientSecret());
		}
	}

}
