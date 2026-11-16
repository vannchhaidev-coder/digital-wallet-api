package com.example.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

  @Value("${app.routes.auth-service-url}")
  private String authServiceUrl;

  @Value("${app.routes.order-service-url}")
  private String orderServiceUrl;

  @Bean
  public RouteLocator routes(RouteLocatorBuilder builder) {
    return builder
        .routes()
        .route("auth-service", r -> r.path("/api/v1/auth/**").uri(authServiceUrl))
        .route("order-service", r -> r.path("/api/v1/orders/**").uri(orderServiceUrl))
        .build();
  }
}
