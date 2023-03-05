package com.qbasic.transcodingservice.controller;

import com.qbasic.transcodingservice.service.ProcessManagingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/ffmpeg")
public class ProcessController {

    private final ProcessManagingService service;

    @Autowired
    public ProcessController(ProcessManagingService service) {
        this.service = service;
    }


    @GetMapping("/{owner}")
    Mono<Long> initiateFfmpeg(@PathVariable("owner") String owner) {
        return service.startProcess(owner);
    }
}
