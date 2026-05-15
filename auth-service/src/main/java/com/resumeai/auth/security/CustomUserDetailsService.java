package com.resumeai.auth.security;

import com.resumeai.auth.entity.User;
import com.resumeai.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Security component supporting authentication workflows in auth-service. */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Resolves the user and converts role to Spring Security authorities.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (user.isDeleted()) {
            throw new UsernameNotFoundException("User deleted: " + username);
        }

        String password = user.getPassword() == null ? "" : user.getPassword();

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(password)
                .authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                .disabled(!user.isActive())
                .build();
    }
}
