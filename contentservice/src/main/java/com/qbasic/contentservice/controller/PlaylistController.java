package com.qbasic.contentservice.controller;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@RestController
@Slf4j
public class PlaylistController {

    @Value("${stream.directory}")
    private String path;

    @GetMapping(value = "/streams/**")
    public Mono<Void> downloadM3u8(ServerHttpRequest request,
                                   ServerHttpResponse response) {
        Path file = Paths.get(path);
        String requestUrl = request.getPath().toString();
        String fileName = requestUrl.split("/streams/")[1];

        ZeroCopyHttpOutputMessage zeroCopyResponse =
                (ZeroCopyHttpOutputMessage) response;
        HttpHeaders headers = response.getHeaders();
        headers.setContentDispositionFormData(fileName, fileName);
        headers.setAccessControlAllowOrigin("*");
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setCacheControl(CacheControl.noCache());

        Path ans = file.resolve(fileName);
        log.info(ans.toAbsolutePath().toString());
        if (!Files.exists(ans)) {
            response.setStatusCode(HttpStatus.NOT_FOUND);
            return zeroCopyResponse.setComplete();
        }

        return zeroCopyResponse
                .writeWith(ans, 0, ans.toFile().length());
    }
}
