package com.kkalchake.enlightenment.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class RequestLoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Only log requests that resolve to an actual controller method (HandlerMethod);
        // static resource / error-dispatch handlers are excluded to avoid noise.
        if (handler instanceof HandlerMethod hm) {
            // Username is intentionally not read from MDC here and not passed as a message
            // parameter: the Logback pattern's %X{username:-anonymous} token renders it directly
            // from MDC. Passing it here too would double-render it and diverge from the
            // anonymous-request fallback ("null" vs "anonymous") that the pattern default handles.
            log.debug("API request {} {} -> called {}", request.getMethod(), request.getRequestURI(), hm.getMethod().getName());
        }
        return true;
    }
}
