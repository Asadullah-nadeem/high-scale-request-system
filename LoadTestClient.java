import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class LoadTestClient {

    private static final String GET_URL = "http://127.0.0.1:8080/poc/db/recent";
    private static final String POST_URL = "http://127.0.0.1:8080/poc/db";
    private static int TOTAL_REQUESTS = 1000000;
    private static int CONCURRENCY = 2000; 

    public static void main(String[] args) throws Exception {
        String mode = "post";
        if (args.length > 0) {
            mode = args[0].toLowerCase();
        }
        if (args.length > 1) {
            TOTAL_REQUESTS = Integer.parseInt(args[1]);
        }
        if (args.length > 2) {
            CONCURRENCY = Integer.parseInt(args[2]);
        }

        String targetUrl = mode.equals("post") ? POST_URL : GET_URL;
        System.out.println("=================================================");
        System.out.println("Starting Java Load Test Client");
        System.out.println("Mode:         " + mode.toUpperCase());
        System.out.println("Target URL:   " + targetUrl);
        System.out.println("Total Reqs:   " + TOTAL_REQUESTS);
        System.out.println("Concurrency:  " + CONCURRENCY);
        System.out.println("=================================================");
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENCY);
        HttpClient client = HttpClient.newBuilder()
                .executor(executor)
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        AtomicLong successCount = new AtomicLong(0);
        AtomicLong failureCount = new AtomicLong(0);
        AtomicLong activeCount = new AtomicLong(0);
        long startTime = System.currentTimeMillis();
        Thread monitor = new Thread(() -> {
            try {
                while (successCount.get() + failureCount.get() < TOTAL_REQUESTS) {
                    Thread.sleep(1000);
                    long elapsed = System.currentTimeMillis() - startTime;
                    long success = successCount.get();
                    long failure = failureCount.get();
                    double rate = success / (elapsed / 1000.0);
                    System.out.printf("Elapsed: %.1fs | Active Connections: %d | Success: %d | Failed: %d | Rate: %.1f req/sec%n",
                            elapsed / 1000.0, activeCount.get(), success, failure, rate);
                }
            } catch (InterruptedException ignored) {}
        });
        monitor.setDaemon(true);
        monitor.start();
        Semaphore semaphore = new Semaphore(CONCURRENCY);
        for (int i = 0; i < TOTAL_REQUESTS; i++) {
            semaphore.acquire();
            activeCount.incrementAndGet();

            HttpRequest request;
            if (mode.equals("post")) {
                String jsonBody = "{\"message\":\"Load test request #" + i + "\"}";
                request = HttpRequest.newBuilder()
                        .uri(URI.create(targetUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
            } else {
                request = HttpRequest.newBuilder()
                        .uri(URI.create(targetUrl))
                        .GET()
                        .build();
            }

            client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .handle((response, ex) -> {
                        activeCount.decrementAndGet();
                        semaphore.release();
                        if (ex == null && response != null && response.statusCode() == 200) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                        return null;
                    });
        }
        while (successCount.get() + failureCount.get() < TOTAL_REQUESTS) {
            Thread.sleep(50);
        }
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);
        long finalElapsed = System.currentTimeMillis() - startTime;
        System.out.println("=================================================");
        System.out.println("Load Test Finished");
        System.out.println("Total Time:   " + (finalElapsed / 1000.0) + " seconds");
        System.out.println("Successful:   " + successCount.get());
        System.out.println("Failed:       " + failureCount.get());
        System.out.printf("Average Rate:  %.1f req/sec%n", successCount.get() / (finalElapsed / 1000.0));
        System.out.println("=================================================");
    }
}
