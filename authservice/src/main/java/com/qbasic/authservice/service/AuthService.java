package com.qbasic.authservice.service;


import com.qbasic.authservice.model.UserAccount;
import com.qbasic.authservice.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthService {

    @Autowired
    private UserRepository repository;
    public Mono<Boolean> checkCredentials(String username, String streamKey) {
        return repository.existsByUsernameAndStreamKey(username, streamKey);
    }

    public Mono<UserAccount> saveUser(String username, String password, String streamKey) {
        return repository.save(UserAccount.builder()
                .username(username)
                .password(password)
                .streamKey(streamKey)
                .build());
    }

}
