package fr.zbar.starter.restclient;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.client.RestClient;

/** Answers 200 to every request and records the last one received. */
final class RecordingServer implements AutoCloseable {

  private final HttpServer server;
  private final AtomicReference<String> lastPath = new AtomicReference<>();
  private final AtomicReference<Headers> lastHeaders = new AtomicReference<>();

  RecordingServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/",
        exchange -> {
          lastPath.set(exchange.getRequestURI().getPath());
          lastHeaders.set(exchange.getRequestHeaders());
          exchange.sendResponseHeaders(200, -1);
          exchange.close();
        });
    server.start();
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
