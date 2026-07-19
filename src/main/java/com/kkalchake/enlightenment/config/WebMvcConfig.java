package com.kkalchake.enlightenment.config;

import com.kkalchake.enlightenment.interceptor.RequestLoggingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Kept separate from SecurityConfig: that class is @EnableWebSecurity and owns the
// authentication/authorization filter chain, which is a different concern from MVC
// interceptor registration (request logging here has nothing to do with security rules).
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RequestLoggingInterceptor requestLoggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestLoggingInterceptor);
    }
}
