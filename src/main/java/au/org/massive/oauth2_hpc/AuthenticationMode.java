package au.org.massive.oauth2_hpc;


/**
 * Created by jason on 30/11/16.
 */
public enum AuthenticationMode {
    HTTP_HEADERS,
    OIDC;

    public static AuthenticationMode getMethod(String methodName) {
        return AuthenticationMode.valueOf(methodName.toUpperCase());
    }
}
