package com.aja.internshipportal.security;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.aja.internshipportal.entity.User;
import com.aja.internshipportal.repository.UserRepository;


//Spring Security calls this to load user from DB during authentication
//It uses email as the "username"

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

	private final UserRepository userRepository;

	public UserDetailsServiceImpl(UserRepository userRepository) {
		super();
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		// find user by email — throw if not found
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                    new UsernameNotFoundException(
                        "User not found with email: " + email
                    )
                );
        
        // Spring Security needs roles prefixed with ROLE_
        // e.g. ADMIN becomes ROLE_ADMIN
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.isEnabled(),
                true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
	}
	
//	true, true, true
//
//	These 3 flags are:
//
//	accountNonExpired,
//	credentialsNonExpired,
//	accountNonLocked

}
