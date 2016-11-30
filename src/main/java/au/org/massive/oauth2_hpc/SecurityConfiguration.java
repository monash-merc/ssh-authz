package au.org.massive.oauth2_hpc;

import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.model.RegisteredClient;
import org.mitre.openid.connect.client.OIDCAuthenticationFilter;
import org.mitre.openid.connect.client.service.impl.DynamicServerConfigurationService;
import org.mitre.openid.connect.client.service.impl.StaticClientConfigurationService;
import org.mitre.openid.connect.client.service.impl.StaticSingleIssuerService;
import org.mitre.openid.connect.config.ServerConfiguration;
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

import javax.servlet.Filter;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Service security configuration
 *
 * @author jrigby
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final PreAuthenticatedAuthenticationProvider preAuthenticatedProvider;

    public SecurityConfiguration() {
        super();

        UserDetailsService userDetailsService = new LdapUserDetailsService();
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

    private Filter getAuthFilter() throws Exception {
        switch (Settings.getInstance().getAuthenticaionMethod()) {
            case OIDC: {
                OIDCAuthenticationFilter authFilter = new OIDCAuthenticationFilter();
                StaticSingleIssuerService issuer = new StaticSingleIssuerService();
                issuer.setIssuer(Settings.getInstance().getOIDCIssuer());
                authFilter.setIssuerService(issuer);
                authFilter.setServerConfigurationService(new DynamicServerConfigurationService());
                authFilter.setClientConfigurationService(new StaticClientConfigurationService() {
                    @Override
                    public RegisteredClient getClientConfiguration(ServerConfiguration issuer) {
                        RegisteredClient client = new RegisteredClient();
                        client.setClientId(Settings.getInstance().getOIDCClientId());
                        client.setClientSecret(Settings.getInstance().getOIDCClientSecret());

                        client.setScope(new HashSet<String>(Arrays.asList("openid",
                                "email", "phone", "address", "offline_access",
                                "profile")));
                        HashSet<String> redirect = new HashSet<String>();
                        redirect.add(Settings.getInstance().getOIDCRedirectURI());
                        client.setRedirectUris(redirect);

                        client.setTokenEndpointAuthMethod(ClientDetailsEntity.AuthMethod.SECRET_POST);

                        return client;
                    }
                });
                authFilter.setAuthenticationManager(authenticationManager());
                return authFilter;
            }
            case HTTP_HEADERS:
            default: {
                HeaderAuthenticationFilter authFilter = new HeaderAuthenticationFilter();
                authFilter.setAuthenticationManager(authenticationManager());
                return authFilter;
            }
        }
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.addFilter(getAuthFilter())
                .authorizeRequests()
                .antMatchers("/oauth/authorize").fullyAuthenticated()
                .and()
                .csrf().disable();
        http.exceptionHandling().accessDeniedPage("/oauth/authorize");
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        super.configure(web);
        web.ignoring().antMatchers("/oauth/token_key");
    }


}
