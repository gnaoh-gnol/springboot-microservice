package com.longhd.apigateway.configuration;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final RouteValidator routeValidator;
    private final RestTemplate restTemplate;

    public AuthenticationFilter(RouteValidator routeValidator, RestTemplate restTemplate) {
        super(Config.class);
        this.routeValidator = routeValidator;
        this.restTemplate = restTemplate;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            if (routeValidator.isSecured.test(exchange.getRequest())) {
                // header contains tokens or not
                if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    throw new IllegalArgumentException("Missing authorization header");
                }
//                String token;
//                List<String> authHeaders = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);
//                if (authHeaders != null && authHeaders.get(0).startsWith("Bearer ")) {
//                    token = authHeaders.get(0).substring(7);
//                }
                try {
                    restTemplate.getForObject("http://authentication-service/validate", Boolean.class);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Unauthorized");
                }
            }
            return chain.filter(exchange);
        });
    }

    public static class Config {
        // Put the configuration properties
    }
}
