package com.mecklon.core.security;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.function.Function;


@Component
public class JwtUtil {

    private final String SECRET = "my-super-secret-key-my-super-secret-key-12345";
    private static final long EXPIRATION = 1000L * 60 * 60 * 10;

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    public String generateToken(String email, String userId, String username) {
        return Jwts.builder()
                .setSubject(email)                 // email
                .claim("userId", userId)
                .claim("displayUsername", username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    public String generateToken(JwtPrincipal jwtPrincipal){
        return Jwts.builder()
                .setSubject(jwtPrincipal.getUsername())                 // email
                .claim("userId", jwtPrincipal.getUserId())
                .claim("displayUsername", jwtPrincipal.getDisplayUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }


    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }


    public boolean validateToken(String token) {
        try {
            return extractUsername(token) != null
                    && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }


    public JwtPrincipal extractUserDetails(String token) {

        Claims claims = extractAllClaims(token);

        JwtPrincipal principal = new JwtPrincipal(
                claims.get("userId", String.class),
                claims.getSubject(),
                claims.get("displayUsername", String.class),
                List.of()
        );


        return new JwtPrincipal();
    }


    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }



    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUserId(String token) {
        return extractAllClaims(token)
                .get("userId", String.class);
    }


    public String extractDisplayUsername(String token) {
        return extractAllClaims(token)
                .get("displayUsername", String.class);
    }
}