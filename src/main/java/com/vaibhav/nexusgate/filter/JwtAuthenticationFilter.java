package com.vaibhav.nexusgate.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Allow public auth endpoints
            if (request.getURI().getPath().contains("/auth/login") || request.getURI().getPath().contains("/actuator")) {
                return chain.filter(exchange);
            }

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "Missing Authorization Header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid Bearer Token Format", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);
            // Stateless signature check simulation
            if (token.isEmpty() || token.equalsIgnoreCase("invalid")) {
                return onError(exchange, "Expired or Invalid Token", HttpStatus.UNAUTHORIZED);
            }

            // Mutate request injecting verified caller identity
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-Caller-Id", "tenant-" + token.hashCode())
                    .header("X-Caller-Role", "ENTERPRISE_CLIENT")
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    public static class Config {}
}
