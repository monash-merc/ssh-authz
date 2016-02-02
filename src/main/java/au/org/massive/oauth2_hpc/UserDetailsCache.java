package au.org.massive.oauth2_hpc;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Stores and retrieves user details in a disk-based cache
 * to avoid hitting LDAP servers unnecessarily
 */
public class UserDetailsCache extends DiskCache {
    private Map<String, UserDetails> userData;
    private final static Settings settings = Settings.getInstance();

    public UserDetailsCache() {
        userData = getCache("user-details",
                new DiskCache.Expiry(settings.getCacheExpiryHours(), TimeUnit.HOURS));
    }

    /**
     * Retrieves user details from the cache based on a key
     * @param key lookup key, e.g. email.
     * @return the UserDetails object or null if no such user
     */
    public UserDetails getUserDetails(String key) {
        if (!userData.containsKey(key)) {
            return null;
        }
        return userData.get(key);
    }

    /**
     * Saves a user in the cache
     * @param key e.g. email
     * @param userDetails the UserDetails object
     */
    public void saveUserDetails(String key, UserDetails userDetails) {
        userData.put(key, userDetails);
        commit();
    }

}
