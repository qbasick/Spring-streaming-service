package com.qbasic.authservice.controller;


import com.qbasic.authservice.dto.RegisterRequest;
import com.qbasic.authservice.dto.UsernameAndStreamKeyRequest;
import com.qbasic.authservice.model.UserAccount;
import com.qbasic.authservice.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "auth")
@Slf4j
public class AuthController {


    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Mono<UserAccount> register(@RequestBody RegisterRequest registerRequest) {
        log.info(registerRequest.toString());
        return authService.saveUser(
                registerRequest.getUsername(),
                registerRequest.getPassword(),
                registerRequest.getStreamKey());
    }

    @PostMapping("/check")
    public Mono<Boolean> checkCredentials(@RequestBody UsernameAndStreamKeyRequest request) {
        log.info(request.toString());
        return authService.checkCredentials(request.getUsername(), request.getStreamKey());
    }


}
