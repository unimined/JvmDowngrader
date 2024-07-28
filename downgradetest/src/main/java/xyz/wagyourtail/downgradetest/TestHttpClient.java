package xyz.wagyourtail.downgradetest;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;

public class TestHttpClient {

    public static int servermain() throws IOException, InterruptedException {
        HttpServer server = HttpServer.create();
        server.bind(new InetSocketAddress(0), 0);
        server.setExecutor(ForkJoinPool.commonPool());
        server.createContext("/", (e) -> {
            e.sendResponseHeaders(200, 0);
            // read body
            if (!Objects.equals(e.getRequestMethod(), "GET")) {
                byte[] b = e.getRequestBody().readAllBytes();
                System.out.println(new String(b));
                OutputStream os = e.getResponseBody();
                os.write("response: ".getBytes());
                os.write(b);
            } else {
                e.getResponseBody().write("response".getBytes());
            }
            e.close();
        });
        Thread t = new Thread(server::start);
        t.setDaemon(true);
        t.start();
        Thread.sleep(250);
        return server.getAddress().getPort();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = servermain();
        System.out.println(Filter.afterHandler("test", (e) -> System.out.println("test")).description());

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                .header("User-Agent", "JVMDG Test 1.0")
                .uri(URI.create("http://localhost:" + port))
                .build();

            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(resp.body());

            HttpRequest request2 = HttpRequest.newBuilder()
                .header("User-Agent", "JVMDG Test 1.0")
                .uri(URI.create("http://localhost:" + port))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("test body"))
                .build();

            HttpResponse<String> resp2 = client.send(request2, HttpResponse.BodyHandlers.ofString());
            System.out.println(resp2.body());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
