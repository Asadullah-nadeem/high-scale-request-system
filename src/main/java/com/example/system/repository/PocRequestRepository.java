package com.example.system.repository;

import com.example.system.entity.PocRequest;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface PocRequestRepository extends ReactiveCrudRepository<PocRequest, Long> {

    @Query("SELECT * FROM \"poc_request\" ORDER BY \"ID\" DESC LIMIT 100")
    Flux<PocRequest> findRecent();
}
