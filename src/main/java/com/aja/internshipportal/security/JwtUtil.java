package com.aja.internshipportal.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.aja.internshipportal.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

	// Reads app.jwt.secret from application.properties
	@Value("${app.jwt.secret}")
	private String jwtSecret;

	// Reads app.jwt.expiration-ms - 15minutes

	@Value("${app.jwt.expiration-ms}")
	private long jwtExpirationMs;

	// Reads app.jwt.refresh-expiration-ms — 7 days
	@Value("${app.jwt.refresh-expiration-ms}")
	private long refreshExpirationMs;

	// ── builds a signing key from our secret string ──
	private SecretKey getSigningKey() {
		return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
	}

	// ── called on login — creates the short-lived access token ──
	// stores email as subject, role as a custom claim
	public String generateAccesToken(User user) {
		return Jwts.builder().setSubject(user.getEmail()).claim("role", user.getRole().name())
				.claim("userId", user.getId()).setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs)).signWith(getSigningKey())
				.compact();
	}


    // ── called on login — creates the long-lived refresh token ──
    // only stores email, no role needed
	public String generateRefreshToken(User user) {
		return Jwts.builder().setSubject(user.getEmail()).setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + refreshExpirationMs)).signWith(getSigningKey())
				.compact();
	}
	
	 // ── extracts all claims (data) from a token ──
    // throws exception automatically if token is expired or tampered
    private Claims extractAllClaims(String token) {
    	return Jwts.parserBuilder()
    	        .setSigningKey(getSigningKey())
    	        .build()
    	        .parseClaimsJws(token)
    	        .getBody();
    }

    // ── extracts email (subject) from token ──
    // used by JwtAuthFilter to find the user in DB
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    // ── extracts role from token ──
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    // ── checks if token is expired ──
    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    // ── full validation — used in JwtAuthFilter ──
    public boolean validateToken(String token, String email) {
        final String tokenEmail = extractEmail(token);
        return tokenEmail.equals(email) && !isTokenExpired(token);
    }
}
