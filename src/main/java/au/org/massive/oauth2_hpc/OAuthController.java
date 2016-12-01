package au.org.massive.oauth2_hpc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.configuration.ConfigurationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

@Controller
@SessionAttributes("authorizationRequest")
public class OAuthController {

    private static final Settings settings = Settings.getInstance();

    private RegisteredClient findClientById(String id) throws ConfigurationException {
        for (RegisteredClient client : settings.getRegisteredClients()) {
            if (client.getClientId().equals(id)) {
                return client;
            }
        }
        return null;
    }

    @RequestMapping(value="/oauth/confirm_access")
    public ModelAndView getConfirmAccess(Map<String,Object> model, HttpServletRequest request) throws ConfigurationException {
        AuthorizationRequest clientAuth = (AuthorizationRequest) model.remove("authorizationRequest");

        RegisteredClient client = findClientById(clientAuth.getClientId());

        model.put("auth_request", clientAuth);
        model.put("client", client);
        model.put("json_scopes", new Gson().toJson(clientAuth.getScope()));
        model.put("remote_system_name", settings.getRemoteResourceName());

        HttpSession session = request.getSession(false);
        if (session != null) {
            Object exception = session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
            if (exception != null && exception instanceof AuthenticationException) {
                model.put("exception", exception);
            }
        }

        return new ModelAndView("access_confirmation", model);
    }

}
