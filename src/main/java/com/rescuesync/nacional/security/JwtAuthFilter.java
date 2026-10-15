package com.rescuesync.nacional.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        //hay que sacar del header la parte de "bearer"
        if (header != null && header.startsWith("Bearer ")) {
            jwtService.validar(header.substring(7).trim()).ifPresent(p ->
            {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    p, // 1. principal: QUIÉN es → el AuthPrincipal (username, rol, ongId)
                    null, // 2. credentials: con QUÉ se autenticó → null. Esto ya se hizo en jwtService.validar por lo que ya confiamos
                    List.of(new SimpleGrantedAuthority("ROLE_" + p.getRol().name()))); // 3. authorities: QUÉ PUEDE hacer
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }
        chain.doFilter(request, response);
    }
}
