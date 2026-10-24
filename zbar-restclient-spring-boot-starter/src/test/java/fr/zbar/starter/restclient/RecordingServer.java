package fr.zbar.starter.restclient;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.client.RestClient;

/** Answers 200 (or the configured response) to every request and records the last one received. */
final class RecordingServer implements AutoCloseable {

  private final HttpServer server;
  private final AtomicReference<String> lastPath = new AtomicReference<>();
  private final AtomicReference<Headers> lastHeaders = new AtomicReference<>();
  private volatile int status = 200;
  private volatile String contentType = "text/plain";
  private volatile String body = "";

  RecordingServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/",
        exchange -> {
          lastPath.set(exchange.getRequestURI().getPath());
          lastHeaders.set(exchange.getRequestHeaders());
          if (body.isEmpty()) {
            exchange.sendResponseHeaders(status, -1);
          } else {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", contentType);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
          }
          exchange.close();
        });
    server.start();
  }

  void respondWith(int status, String contentType, String body) {
    this.status = status;
    this.contentType = contentType;
    this.body = body;
  }

  String baseUrl() {
    return "http://localhost:" + server.getAddress().getPort();
  }

  String lastPath() {
    return lastPath.get();
  }

  Headers lastHeaders() {
    return lastHeaders.get();
  }

  static void call(ConfigurableApplicationContext context, String uri) {
    context.getBean(RestClient.Builder.class).build().get().uri(uri).retrieve().toBodilessEntity();
  }

  @Override
  public void close() {
    server.stop(0);
  }
}
