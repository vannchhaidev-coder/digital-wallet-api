package com.example.apigateway.filter;

import com.example.apigateway.security.JwtValidator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

  private static final String HEADER = "Authorization";
  private static final String PREFIX = "Bearer ";

  private static final List<String> PUBLIC_PATHS =
      List.of(
          "/api/v1/auth/register",
          "/api/v1/auth/login",
          "/api/v1/auth/refresh",
          "/actuator/health");

  private final JwtValidator jwtValidator;

  @Override
  public int getOrder() {
    return -1;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String path = exchange.getRequest().getURI().getPath();

    ServerHttpRequest strippedRequest =
        exchange
            .getRequest()
            .mutate()
            .headers(headers -> {
              headers.remove("X-User-Id");
              headers.remove("X-User-Role");
            })
            .build();
    exchange = exchange.mutate().request(strippedRequest).build();

    if (isPublic(path)) {
      return chain.filter(exchange);
    }

    String header = exchange.getRequest().getHeaders().getFirst(HEADER);
    if (header == null || !header.startsWith(PREFIX)) {
      return reject(exchange, "Missing bearer token");
    }

    String token = header.substring(PREFIX.length());

    try {
      Claims claims = jwtValidator.validate(token);

      ServerHttpRequest mutatedRequest =
          exchange
              .getRequest()
              .mutate()
              .header("X-User-Id", claims.getSubject())
              .header("X-User-Role", claims.get("role", String.class))
              .build();

      return chain.filter(exchange.mutate().request(mutatedRequest).build());

    } catch (JwtException | IllegalArgumentException ex) {
      return reject(exchange, "Invalid or expired token");
    }
  }

  private boolean isPublic(String path) {
    return PUBLIC_PATHS.stream().anyMatch(path::equals);
  }

  private Mono<Void> reject(ServerWebExchange exchange, String detail) {
    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(HttpStatus.UNAUTHORIZED);
    response.getHeaders().add("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE);

    String body =
        """
        {"type":"about:blank","title":"Unauthorized","status":401,"detail":"%s"}\
        """
            .formatted(detail);

    var buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
    return response.writeWith(Mono.just(buffer));
  }
}
