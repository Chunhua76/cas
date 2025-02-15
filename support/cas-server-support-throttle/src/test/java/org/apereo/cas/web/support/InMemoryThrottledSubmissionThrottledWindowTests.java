package org.apereo.cas.web.support;

import org.apereo.cas.authentication.AcceptUsersAuthenticationHandler;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlanConfigurer;
import org.apereo.cas.util.CollectionUtils;

import lombok.Getter;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ThrottledSubmissionHandlerInterceptor}.
 *
 * @author Misagh Moayyed
 * @since 6.6.0
 */
@EnableScheduling
@SpringBootTest(classes = {
    InMemoryThrottledSubmissionThrottledWindowTests.TestAuthenticationConfiguration.class,
    BaseThrottledSubmissionHandlerInterceptorAdapterTests.SharedTestConfiguration.class
},
    properties = {
        "cas.authn.throttle.failure.range-seconds=5",
        "cas.authn.throttle.failure.throttle-window-seconds=PT2S"
    }
)
@Getter
@Tag("AuthenticationThrottling")
public class InMemoryThrottledSubmissionThrottledWindowTests
    extends BaseThrottledSubmissionHandlerInterceptorAdapterTests {

    @Autowired
    @Qualifier(ThrottledSubmissionHandlerInterceptor.BEAN_NAME)
    private ThrottledSubmissionHandlerInterceptor throttle;

    @Override
    @Test
    public void verifyThrottle() {
        var success = login("casuser", "Mellon", IP_ADDRESS);
        assertEquals(HttpStatus.SC_OK, success.getStatus());

        var result = login("casuser", "bad", IP_ADDRESS);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, result.getStatus());

        result = login("casuser", "bad", IP_ADDRESS);
        assertEquals(HttpStatus.SC_LOCKED, result.getStatus());

        result = login("casuser", "Mellon", IP_ADDRESS);
        assertEquals(HttpStatus.SC_LOCKED, result.getStatus());
    }

    @TestConfiguration(value = "TestAuthenticationConfiguration", proxyBeanMethods = false)
    public static class TestAuthenticationConfiguration {
        @Bean
        public AuthenticationEventExecutionPlanConfigurer surrogateAuthenticationEventExecutionPlanConfigurer() {
            return plan -> plan.registerAuthenticationHandler(new AcceptUsersAuthenticationHandler(CollectionUtils.wrap("casuser", "Mellon")));
        }
    }
}
