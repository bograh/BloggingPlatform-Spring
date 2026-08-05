package org.amalitech.bloggingplatformspring.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.services.RuntimeMetricsService;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RuntimeMetricsFilter extends OncePerRequestFilter {

  private final RuntimeMetricsService runtimeMetricsService;

  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    String path = request.getRequestURI();
    return !path.startsWith("/api/");
  }

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    long startNanos = System.nanoTime();
    try {
      filterChain.doFilter(request, response);
    } finally {
      long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
      String path = request.getRequestURI();
      String method = request.getMethod();
      int status = response.getStatus();

      runtimeMetricsService.recordRequest(method, path, status, latencyMs);

      log.info("[RUNTIME_METRIC] method={} path={} status={} latencyMs={}",
          method,
          path,
          status,
          latencyMs);
    }
  }
}
