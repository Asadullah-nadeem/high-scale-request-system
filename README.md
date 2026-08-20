# High Scale Request System (1 Million Concurrent Requests POC)

This project is a Proof of Concept (POC) demonstrating how a Spring Boot application can scale to handle **1 million concurrent requests/connections** using a reactive SQL database (H2 + Spring Data R2DBC) and a high-performance Java load testing client.

---

## 1. Architectural Approach

Traditional Spring MVC applications use a **thread-per-request** model. At a scale of 1 million requests, this model causes the system to run out of memory (OOM) because OS threads are heavy (1MB stack memory per thread).

### Our Solution: Spring WebFlux + Netty + R2DBC

To achieve massive concurrency:

* **Reactive Event Loop**: Netty uses a tiny pool of worker threads. Connections are handled via non-blocking sockets. Netty registers a callback on the socket and immediately releases the thread to accept more requests.
* **Reactive SQL Database**: Uses **Spring Data R2DBC** and the **r2dbc-h2** driver. Unlike traditional JDBC, R2DBC is fully non-blocking and handles database queries without blocking Netty threads.
* **Minimal Memory Overhead**: The reactive model holds 1 million active connections with standard RAM sizes (typically 8GB to 16GB of heap).

---

## 2. API Endpoints

The following endpoints are exposed on port `8080`:

* `GET /poc/fast`: Immediate response returning "OK".
* `GET /poc/delay?ms=N`: Holds the connection open for `N` milliseconds without blocking threads using `Mono.delay()`.
* `POST /poc/db`: Saves incoming requests in H2. Body format: `{"message": "..."}`.
* `GET /poc/db/recent`: Returns the last 100 entries.
* `GET /poc/db/count`: Returns the total row count in H2.

---

## 3. High-Performance Java Load Tester

We use a lightweight, dependency-free Java load tester ([`LoadTestClient.java`](file:///c:/xampp/htdocs/Spring_boot/high-scale-request-system/LoadTestClient.java)) to trigger requests. It uses Java 11's asynchronous `HttpClient` and throttles concurrent connections with a Semaphore.

### Running the Load Tester

To run the load tester directly (without compiling) using Java 11+:

```bash
# POST Load Test (1,000,000 requests, 2,000 concurrency)
java LoadTestClient.java post 1000000 2000

# GET Load Test (100,000 requests, 2,000 concurrency)
java LoadTestClient.java get 100000 2000
```

---

## 4. Bottlenecks & Tuning Requirements

Running at this scale requires configuration changes across the OS and JVM:

### 1. Operating System Configuration (Linux Recommended)

* **File Descriptor Limits**: Set limits to at least `1,050,000` in `/etc/security/limits.conf`:
  ```text
  * soft nofile 1050000
  * hard nofile 1050000
  ```
* **TCP Backlog Queue (`somaxconn`)**:
  ```bash
  sysctl -w net.core.somaxconn=65535
  ```
* **TCP Memory & Buffers**: Reduce buffer sizes to conserve memory per connection:
  ```bash
  sysctl -w net.ipv4.tcp_rmem="4096 87380 16777216"
  sysctl -w net.ipv4.tcp_wmem="4096 65536 16777216"
  ```

### 2. JVM Configuration

Start the application using:

```bash
.\mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xms4g -Xmx4g -XX:+UseZGC -XX:MaxDirectMemorySize=2g"
```

* Uses **ZGC** (Z Garbage Collector) for sub-millisecond GC pauses.
* Sets heap bounds (`-Xms`/`-Xmx`) to `4g`.
* Configures off-heap direct memory buffer limits to `2g` for Netty.
