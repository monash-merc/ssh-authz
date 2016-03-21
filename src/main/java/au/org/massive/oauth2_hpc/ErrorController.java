package au.org.massive.oauth2_hpc;

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
public class ErrorController implements org.springframework.boot.autoconfigure.web.ErrorController {
    private static final String PATH = "/error";

    @RequestMapping(value = PATH)
    public ModelAndView error(HttpServletRequest request, HttpServletResponse response) {
        Map<String,Object> model = new HashMap<String,Object>();
        if (response.getStatus() == 403) {
            return new ModelAndView("login_error", model);
        } else {
            model.put("status", response.getStatus());
            return new ModelAndView("error", model);
        }
    }

    @Override
    public String getErrorPath() {
        return PATH;
    }
}
