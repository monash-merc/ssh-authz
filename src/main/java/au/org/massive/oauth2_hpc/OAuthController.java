package au.org.massive.oauth2_hpc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

    @Autowired
    private ClientDetailsService clientDetailsService;

    @RequestMapping(value="/oauth/confirm_access")
    public ModelAndView getConfirmAccess(Map<String,Object> model, HttpServletRequest request) {
        AuthorizationRequest clientAuth = (AuthorizationRequest) model.remove("authorizationRequest");

        model.put("auth_request", clientAuth);
        model.put("client", clientDetailsService.loadClientByClientId(clientAuth.getClientId()));
        model.put("json_scopes", new Gson().toJson(clientAuth.getScope()));

        HttpSession session = request.getSession(false);
        if (session != null) {
            Object exception = session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
            if (exception != null && exception instanceof AuthenticationException) {
                model.put("exception", exception);
            }
        }

        return new ModelAndView("access_confirmation", model);
    }

    @RequestMapping(value="/error")
    public ModelAndView loginError(Map<String,Object> model, HttpServletRequest request) {
        return new ModelAndView("error", model);
    }

}
