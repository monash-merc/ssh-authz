package au.org.massive.oauth2_hpc;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jason on 21/03/2016.
 */
@Controller
public class AccessDeniedErrorController {
    private static final String PATH = "/403";
    private static final Logger log = Logger.getLogger(AccessDeniedErrorController.class.getName());

    @RequestMapping(value = PATH)
    public ModelAndView error(HttpServletRequest request, HttpServletResponse response) {
        Map<String,Object> model = new HashMap<String,Object>();
        return new ModelAndView("login_error", model);
    }

}
