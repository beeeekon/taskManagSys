package ru.astalavista.taskManagSys.config;

import lombok.Getter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Getter
@Component
public class AuditContextProvider {

    private final ThreadLocal<String> changeSource = new ThreadLocal<>();
    private final ThreadLocal<String> changedBy = new ThreadLocal<>();

    public void setContext(String source, String user) {
        changeSource.set(source);
        changedBy.set(user);
    }

    public void clearContext() {
        changeSource.remove();
        changedBy.remove();
    }

    public String getCurrentChangeSource() {
        String source = changeSource.get();
        return source != null ? source : "WEB";
    }

    public String getCurrentChangedBy() {
        String user = changedBy.get();
        if (user != null) return user;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() ? auth.getName() : "SYSTEM";
    }
}