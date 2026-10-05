package com.debugathon.pricing;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestContextFilter extends OncePerRequestFilter {
    private final String instance;
    public RequestContextFilter(@Value("${pricing.instance}") String instance) { this.instance = instance; }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                             FilterChain chain) throws IOException, ServletException {
        String id = request.getHeader("X-Request-ID");
        if (id == null || !id.matches("[A-Za-z0-9_.:-]{1,100}")) id = UUID.randomUUID().toString();
        MDC.put("requestId", id);
        MDC.put("instance", instance);
        response.setHeader("X-Request-ID", id);
        response.setHeader("X-Pricing-Instance", instance);
        response.setHeader("Cache-Control", "no-store");
        try { chain.doFilter(request, response); }
        finally { MDC.clear(); }
    }
}
