package com.radioacademy.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    // Debe ser una cadena larga y aleatoria (al menos 256 bits para HS256)
    @org.springframework.beans.factory.annotation.Value("${security.jwt.secret-key}")
    private String secretKey;

    @org.springframework.beans.factory.annotation.Value("${security.jwt.expiration-time}")
    private long jwtExpiration;

    // Extraer el nombre de usuario (email) del token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extraer cualquier dato (claim) genérico
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Generar token AUTOMÁTICAMENTE inyectando el rol del usuario
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();

        // 1. Buscamos el rol del usuario (ADMIN, STUDENT, etc.)
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(item -> item.getAuthority())
                .orElse("STUDENT"); // Por si acaso no tuviera, ponemos uno por defecto

        // 2. Lo metemos en el mapa
        extraClaims.put("role", role);

        // 3. Generamos el token con ese dato extra
        return generateToken(extraClaims, userDetails);
    }

    // Generar token con datos extra (claims) y el usuario
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername()) // El "Subject" es el email
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration)) // 24 horas de validez
                .signWith(getSignInKey(), SignatureAlgorithm.HS256) // Firmamos digitalmente
                .compact();
    }

    // Validar si el token pertenece a este usuario y no ha caducado
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}