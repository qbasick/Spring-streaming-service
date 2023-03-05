package com.qbasic.authservice.repository;

import com.qbasic.authservice.model.UserAccount;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;


@Repository
public interface UserRepository extends ReactiveCrudRepository<UserAccount, Long> {
    Mono<Boolean> existsByUsernameAndStreamKey(String username, String streamKey);
}
