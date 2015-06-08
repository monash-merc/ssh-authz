package au.org.massive.oauth2_hpc;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class UserDetailsImpl extends User {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2619118679158679269L;
	private final String email;

	public UserDetailsImpl(String username, String password, String email, boolean enabled,
			boolean accountNonExpired, boolean credentialsNonExpired,
			boolean accountNonLocked,
			Collection<? extends GrantedAuthority> authorities) {
		super(username, password, enabled, accountNonExpired, credentialsNonExpired,
				accountNonLocked, authorities);
		this.email = email;
	}

	public UserDetailsImpl(String username, String password, String email,
			Collection<? extends GrantedAuthority> authorities) {
		super(username, password, authorities);
		this.email = email;
	}

	public String getEmail() {
		return email;
	}
}
