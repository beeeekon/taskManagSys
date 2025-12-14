package ru.astalavista.taskManagSys.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.astalavista.taskManagSys.config.AuditContextProvider;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuditSourceFilter extends OncePerRequestFilter {

    private final AuditContextProvider auditContextProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String source = determineSource(request);
        auditContextProvider.setContext(source, null);

        try {
            filterChain.doFilter(request, response);
        } finally {
            auditContextProvider.clearContext();
        }
    }

    private String determineSource(HttpServletRequest request) {
        String path = request.getRequestURI();

        if (path.contains("/telegram/")) return "TELEGRAM_BOT";
        if (path.contains("/api/")) return "API";
        if (path.contains("/internal/")) return "INTERNAL_SERVICE";

        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null) {
            userAgent = userAgent.toLowerCase();
            if (userAgent.contains("mobile") || userAgent.contains("android") || userAgent.contains("iphone")) {
                return "MOBILE_APP";
            }
        }

        return "WEB";
    }
}