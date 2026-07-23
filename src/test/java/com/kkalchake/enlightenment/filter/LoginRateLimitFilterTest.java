package com.kkalchake.enlightenment.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class LoginRateLimitFilterTest {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String RATE_LIMIT_BODY =
            "{\"error\":\"Too many login attempts. Please try again later.\"}";

    private LoginRateLimitFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new LoginRateLimitFilter();
        chain = mock(FilterChain.class);
    }

    private MockHttpServletRequest loginRequest(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", LOGIN_PATH);
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    @Test
    void underCapacity_passesThrough() throws Exception {
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = loginRequest("10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, chain);

            assertNotEquals(429, response.getStatus());
        }
        verify(chain, times(5)).doFilter(any(), any());
    }

    @Test
    void overCapacity_returns429() throws Exception {
        String ip = "10.0.0.2";
        for (int i = 0; i < 5; i++) {
            filter.doFilter(loginRequest(ip), new MockHttpServletResponse(), chain);
        }

        MockHttpServletRequest sixthRequest = loginRequest(ip);
        MockHttpServletResponse sixthResponse = new MockHttpServletResponse();
        filter.doFilter(sixthRequest, sixthResponse, chain);

        assertEquals(429, sixthResponse.getStatus());
        assertEquals("application/json", sixthResponse.getContentType());
        assertEquals(RATE_LIMIT_BODY, sixthResponse.getContentAsString());
        verify(chain, times(5)).doFilter(any(), any());
    }

    @Test
    void distinctIps_independentBuckets() throws Exception {
        String ipA = "10.0.0.3";
        for (int i = 0; i < 5; i++) {
            filter.doFilter(loginRequest(ipA), new MockHttpServletResponse(), chain);
        }
        MockHttpServletResponse exhaustedResponse = new MockHttpServletResponse();
        filter.doFilter(loginRequest(ipA), exhaustedResponse, chain);
        assertEquals(429, exhaustedResponse.getStatus());

        MockHttpServletResponse ipBResponse = new MockHttpServletResponse();
        filter.doFilter(loginRequest("10.0.0.4"), ipBResponse, chain);

        assertNotEquals(429, ipBResponse.getStatus());
        verify(chain, times(6)).doFilter(any(), any());
    }

    @Test
    void xForwardedFor_firstHopUsed() throws Exception {
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = loginRequest("192.168.0." + i);
            request.addHeader("X-Forwarded-For", "1.2.3.4, 5.6.7.8");
            filter.doFilter(request, new MockHttpServletResponse(), chain);
        }

        MockHttpServletRequest sixthRequest = loginRequest("192.168.0.99");
        sixthRequest.addHeader("X-Forwarded-For", "1.2.3.4, 5.6.7.8");
        MockHttpServletResponse sixthResponse = new MockHttpServletResponse();
        filter.doFilter(sixthRequest, sixthResponse, chain);

        assertEquals(429, sixthResponse.getStatus());
        verify(chain, never()).doFilter(
                argThat(req -> req instanceof MockHttpServletRequest r
                        && "192.168.0.99".equals(r.getRemoteAddr())),
                any());
    }
}
