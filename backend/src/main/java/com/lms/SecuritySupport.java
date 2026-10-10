package com.lms;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@interface RequireRole {
    Role[] value();
}

@Component
class ApiAuthenticationInterceptor implements HandlerInterceptor {
    private final UserRepository users;
    private final TokenService tokens;

    ApiAuthenticationInterceptor(UserRepository users, TokenService tokens) {
        this.users = users; this.tokens = tokens;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod method) || !request.getRequestURI().startsWith("/api/")) return true;
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        if (method.getMethodAnnotation(PublicEndpoint.class) != null) return true;

        try {
            HttpSession session = request.getSession(false);
            Long userId = session == null ? null : (Long) session.getAttribute("authenticatedUserId");
            String header = request.getHeader("Authorization");
            if (userId == null && header != null && header.startsWith("Bearer ")) userId = tokens.userId(header.substring("Bearer ".length()));
            if (userId == null) { response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token"); return false; }
            User user = users.findById(userId).orElse(null);
            if (user == null || !user.isActive()) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or inactive token");
                return false;
            }
            request.setAttribute("authenticatedUser", user);

            RequireRole required = method.getMethodAnnotation(RequireRole.class);
            if (required == null) required = method.getBeanType().getAnnotation(RequireRole.class);
            if (required != null && Arrays.stream(required.value()).noneMatch(role -> role == user.getRole())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Insufficient role");
                return false;
            }
            return true;
        } catch (RuntimeException ex) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return false;
        }
    }
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface PublicEndpoint {}

@Component
class ApiWebConfiguration implements WebMvcConfigurer {
    private final ApiAuthenticationInterceptor authenticationInterceptor;

    ApiWebConfiguration(ApiAuthenticationInterceptor authenticationInterceptor) {
        this.authenticationInterceptor = authenticationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authenticationInterceptor);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:4200", "http://127.0.0.1:4200")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Accept")
                .allowCredentials(true);
    }
}
