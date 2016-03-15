package au.org.massive.oauth2_hpc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Created by jason on 15/03/2016.
 */
@Controller
public class OAuthErrorController {
    @RequestMapping(value="/error")
    public ModelAndView loginError(Map<String,Object> model, HttpServletRequest request) {
        return new ModelAndView("error", model);
    }
}
