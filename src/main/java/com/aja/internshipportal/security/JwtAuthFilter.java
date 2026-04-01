//package com.aja.internshipportal.security;
//
//import java.io.IOException;
//
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//
//@Component
//@RequiredArgsConstructor
//public class JwtAuthFilter extends OncePerRequestFilter {
//
//
//	private final JwtUtil jwtUtil;
//	private final UserDetailsServiceImpl userDetailsService;
//
//	@Override
//	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
//			throws ServletException, IOException {
//		// TODO Auto-generated method stub
//
//
//	    String path = request.getServletPath();
//
//	    // ✅ Skip JWT filter for PUBLIC URLs
//	    if (path.startsWith("/api/auth") ||
//	        path.startsWith("/api/packages") ||
//	        path.startsWith("/api/questions/samples") ||
//	        path.startsWith("/swagger-ui") ||
//	        path.startsWith("/v3/api-docs")) {
//
//	        filterChain.doFilter(request, response);
//	        return;
//	    }
//        // Step 1 — read Authorization header
//        // Expected format: "Bearer eyJhbGci..."
//        final String authHeader = request.getHeader("Authorization");
//
//        // Step 2 — if no header or not Bearer, skip this filter
//        // the request will be blocked by SecurityConfig if route is protected
//        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//            filterChain.doFilter(request, response);
//            return;
//        }
//
//        // Step 3 — extract the token (remove "Bearer " prefix)
//        final String token = authHeader.substring(7);
//
//        // Step 4 — extract email from token
//        final String email = jwtUtil.extractEmail(token);
//
//        // Step 5 — if email exists and user is not already authenticated
//        if (email != null &&
//            SecurityContextHolder.getContext().getAuthentication() == null) {
//
//            // Step 6 — load user from DB
//            UserDetails userDetails =
//                userDetailsService.loadUserByUsername(email);
//
//            // Step 7 — validate token against loaded user
//            if (jwtUtil.validateToken(token, userDetails.getUsername())) {
//
//                // Step 8 — create authentication object
//                // this tells Spring Security "this user is authenticated"
//                UsernamePasswordAuthenticationToken authToken =
//                    new UsernamePasswordAuthenticationToken(
//                        userDetails,
//                        null,
//                        userDetails.getAuthorities()
//                    );
//
//                authToken.setDetails(
//                    new WebAuthenticationDetailsSource()
//                        .buildDetails(request)
//                );
//
//                // Step 9 — store in SecurityContext
//                // from this point forward, Spring knows who the user is
//                SecurityContextHolder.getContext()
//                    .setAuthentication(authToken);
//            }
//        }
//
//        // Step 10 — continue to next filter / controller
//        filterChain.doFilter(request, response);
//	}
//
//}

package com.aja.internshipportal.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // ✅ REMOVED: The "Skip filter for /api/auth" block.
        // We now allow the filter to run for all requests. 
        // If no token is found, it will just pass through, and SecurityConfig will decide if it's okay.

        // Step 1 — read Authorization header
        final String authHeader = request.getHeader("Authorization");

        // Step 2 — if no header or not Bearer, skip this filter
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 3 — extract the token (remove "Bearer " prefix)
        final String token = authHeader.substring(7);

        // Step 4 — extract email from token
        final String email = jwtUtil.extractEmail(token);

        // Step 5 — if email exists and user is not already authenticated
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Step 6 — load user from DB
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // Step 7 — validate token against loaded user
            if (jwtUtil.validateToken(token, userDetails.getUsername())) {

                // Step 8 — create authentication object
                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );

                authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Step 9 — store in SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Step 10 — continue to next filter
        filterChain.doFilter(request, response);
    }
}

