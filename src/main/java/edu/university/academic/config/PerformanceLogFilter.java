package edu.university.academic.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class PerformanceLogFilter extends OncePerRequestFilter {

    private static final Path LOG_DIR = Path.of("build", "performance-logs");
    private static final Path LOG_FILE = LOG_DIR.resolve("backend-api-metrics.csv");
    private static final Object LOCK = new Object();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith("/api");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startNs = System.nanoTime();
        long memoryBefore = usedMemoryBytes();
        Throwable error = null;

        try {
            filterChain.doFilter(request, response);
        } catch (Throwable ex) {
            error = ex;
            throw ex;
        } finally {
            long durationMs = (System.nanoTime() - startNs) / 1_000_000;
            long memoryAfter = usedMemoryBytes();
            appendMetric(request, response, durationMs, memoryBefore, memoryAfter, error);
        }
    }

    private void appendMetric(HttpServletRequest request,
                              HttpServletResponse response,
                              long durationMs,
                              long memoryBefore,
                              long memoryAfter,
                              Throwable error) {
        try {
            Files.createDirectories(LOG_DIR);
            synchronized (LOCK) {
                if (!Files.exists(LOG_FILE)) {
                    Files.writeString(
                            LOG_FILE,
                            "\uFEFFtime,method,uri,query,status,duration_ms,content_length,content_type,used_memory_before_mb,used_memory_after_mb,memory_delta_mb,error\n",
                            StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.APPEND
                    );
                }

                String line = String.join(",",
                        csv(LocalDateTime.now().format(FORMATTER)),
                        csv(request.getMethod()),
                        csv(request.getRequestURI()),
                        csv(request.getQueryString()),
                        csv(String.valueOf(response.getStatus())),
                        csv(String.valueOf(durationMs)),
                        csv(String.valueOf(request.getContentLengthLong())),
                        csv(request.getContentType()),
                        csv(String.format("%.2f", memoryBefore / 1024.0 / 1024.0)),
                        csv(String.format("%.2f", memoryAfter / 1024.0 / 1024.0)),
                        csv(String.format("%.2f", (memoryAfter - memoryBefore) / 1024.0 / 1024.0)),
                        csv(error == null ? "" : error.getClass().getSimpleName())
                ) + "\n";

                Files.writeString(LOG_FILE, line, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            }
        } catch (Exception ignored) {
            // Metrics logging must never affect business requests.
        }
    }

    private long usedMemoryBytes() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private String csv(String value) {
        if (value == null) {
            value = "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
