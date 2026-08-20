package com.example.system.controller;

import com.example.system.entity.PocRequest;
import com.example.system.repository.PocRequestRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
public class RequestController {

    private final PocRequestRepository repository;

    public RequestController(PocRequestRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/poc/fast")
    public Mono<String> getFastResponse() {
        return Mono.just("OK");
    }

    @GetMapping("/poc/delay")
    public Mono<String> getDelayedResponse(@RequestParam(value = "ms", defaultValue = "5000") long ms) {
        return Mono.delay(Duration.ofMillis(ms))
                .map(d -> "Delayed OK (delayed for " + ms + " ms)");
    }

    @PostMapping("/poc/db")
    public Mono<PocRequest> saveRequest(@RequestBody PocRequest request) {
        request.setId(null); // Ensure database auto-increment is used
        request.setTimestamp(System.currentTimeMillis());
        return repository.save(request);
    }

    @GetMapping("/poc/db/recent")
    public Flux<PocRequest> getRecentRequests() {
        return repository.findRecent();
    }

    @GetMapping("/poc/db/count")
    public Mono<Long> getRequestCount() {
        return repository.count();
    }
}
