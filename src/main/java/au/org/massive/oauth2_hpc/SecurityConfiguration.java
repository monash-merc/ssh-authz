package au.org.massive.oauth2_hpc;

import org.apache.log4j.Logger;
import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.model.RegisteredClient;
import org.mitre.openid.connect.client.OIDCAuthenticationFilter;
import org.mitre.openid.connect.client.OIDCAuthenticationProvider;
import org.mitre.openid.connect.client.service.impl.*;
import org.mitre.openid.connect.config.ServerConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
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

    private static final Logger log = Logger.getLogger(SecurityConfiguration.class.getName());

    public SecurityConfiguration() {
        super();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth)
            throws Exception {

        switch (Settings.getInstance().getAuthenticaionMode()) {
            case OIDC:
                auth.authenticationProvider(new OIDCAuthenticationProvider());
                break;
            case HTTP_HEADERS:
            default:
                UserDetailsService userDetailsService = new LdapUserDetailsService();
                UserDetailsByNameServiceWrapper<PreAuthenticatedAuthenticationToken> wrapper = new UserDetailsByNameServiceWrapper<PreAuthenticatedAuthenticationToken>(
                        userDetailsService);
                PreAuthenticatedAuthenticationProvider preAuthenticatedProvider = new PreAuthenticatedAuthenticationProvider();
                preAuthenticatedProvider.setPreAuthenticatedUserDetailsService(wrapper);
                auth.authenticationProvider(preAuthenticatedProvider);
        }

    }

    private Filter getAuthFilter() throws Exception {
        switch (Settings.getInstance().getAuthenticaionMode()) {
            case OIDC: {
                return openIdConnectAuthenticationFilter();
            }
            case HTTP_HEADERS:
            default: {
                log.info("Using HTTP_HEADERS authentication");
                HeaderAuthenticationFilter authFilter = new HeaderAuthenticationFilter();
                authFilter.setAuthenticationManager(authenticationManager());
                return authFilter;
            }
        }
    }

    @Bean
    public OIDCAuthenticationFilter openIdConnectAuthenticationFilter() throws Exception {
        log.info("Using OIDC authentication");
        OIDCAuthenticationFilter authFilter = new OIDCAuthenticationFilter();
        StaticSingleIssuerService issuer = new StaticSingleIssuerService();
        issuer.setIssuer(Settings.getInstance().getOIDCIssuer());
        authFilter.setIssuerService(issuer);
        authFilter.setAuthRequestOptionsService(new StaticAuthRequestOptionsService());
        authFilter.setAuthRequestUrlBuilder(new PlainAuthRequestUrlBuilder());
        authFilter.setServerConfigurationService(new DynamicServerConfigurationService());
        authFilter.setClientConfigurationService(new StaticClientConfigurationService() {
            @Override
            public RegisteredClient getClientConfiguration(ServerConfiguration issuer) {
                RegisteredClient client = new RegisteredClient();
                client.setClientId(Settings.getInstance().getOIDCClientId());
                client.setClientSecret(Settings.getInstance().getOIDCClientSecret());

                client.setScope(new HashSet<String>(Arrays.asList("openid",
                        "email", "offline_access", "profile")));
                HashSet<String> redirect = new HashSet<String>();
                redirect.add(Settings.getInstance().getOIDCRedirectURI());
                client.setRedirectUris(redirect);

                client.setTokenEndpointAuthMethod(ClientDetailsEntity.AuthMethod.SECRET_BASIC);

                return client;
            }
        });
        authFilter.setAuthenticationManager(authenticationManager());
        return authFilter;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.addFilterBefore(getAuthFilter(), AbstractPreAuthenticatedProcessingFilter.class)
                .authorizeRequests()
                .antMatchers("/oauth/authorize").fullyAuthenticated()
                .and()
                .csrf().disable();

        if (Settings.getInstance().getAuthenticaionMode() != AuthenticationMode.OIDC) {
            http.exceptionHandling().accessDeniedPage("/403");
        } else {
            http.exceptionHandling().authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/openid_connect_login"));
        }
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        super.configure(web);
        web.ignoring().antMatchers("/oauth/token_key");
    }


}
