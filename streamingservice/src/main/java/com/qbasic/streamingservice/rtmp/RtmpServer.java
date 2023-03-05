package com.qbasic.streamingservice.rtmp;

import com.qbasic.streamingservice.rtmp.entity.User;
import com.qbasic.streamingservice.rtmp.handlers.*;
import com.qbasic.streamingservice.rtmp.model.context.Stream;
import io.netty.channel.ChannelOption;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.*;
import reactor.netty.tcp.TcpServer;
import reactor.util.retry.Retry;
import java.time.Duration;


@NoArgsConstructor
@Getter
@Setter
@Slf4j
public abstract class RtmpServer implements CommandLineRunner {

    protected abstract RtmpMessageHandler getRtmpMessageHandler();
    protected abstract InboundConnectionLogger getInboundConnectionLogger();
    protected abstract HandshakeHandler getHandshakeHandler();
    protected abstract ChunkDecoder getChunkDecoder();
    protected abstract ChunkEncoder getChunkEncoder();

    @Autowired
    private WebClient webClient;

    @Value("${transcoding.server}")
    private String transcodingAddress;

    @Value("${auth.server}")
    private String authAddress;

    @Override
    public void run(String... args) {
        DisposableServer server = TcpServer.create()
                //.host("0.0.0.0")
                .port(1935)
                .doOnBound(disposableServer ->
                        log.info("RTMP server started on port {}", disposableServer.port()))
                .doOnConnection(connection -> connection
                        .addHandlerLast(getInboundConnectionLogger())
                        .addHandlerLast(getHandshakeHandler())
                        .addHandlerLast(getChunkDecoder())
                        .addHandlerLast(getChunkEncoder())
                        .addHandlerLast(getRtmpMessageHandler()))
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .handle((in, out) -> in
                        .receiveObject()
                        .cast(Stream.class)
                        .flatMap(stream ->
                                webClient
                                .post()
                                .uri(authAddress + "/auth/check")
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .body(Mono.just(new User(stream.getStreamName(), stream.getStreamKey())), User.class)
                                .retrieve()
                                .bodyToMono(Boolean.class)
                                .retryWhen(Retry.fixedDelay(3, Duration.ofMillis(500)))
                                .doOnError(error -> log.info(error.getMessage()))
                                .onErrorReturn(Boolean.FALSE)
                                .flatMap(ans -> {
                                    log.info("User {} stream key validation", stream.getStreamName());
                                    if (ans) {
                                        stream.sendPublishMessage();
                                        stream.getReadyToBroadcast().thenRun(() -> webClient
                                                .get()
                                                .uri(transcodingAddress + "/ffmpeg/" + stream.getStreamName())
                                                .retrieve()
                                                .bodyToMono(Long.class)
                                                //.delaySubscription(Duration.ofSeconds(10L))
                                                .retryWhen(Retry.fixedDelay(3, Duration.ofMillis(1000)))
                                                .doOnError(error -> {
                                                    log.info("Error occurred on transcoding server " + error.getMessage());
                                                    stream.closeStream();
                                                    stream.getPublisher().disconnect();
                                                })
                                                .onErrorComplete()
                                                .subscribe((s) -> log.info("Transcoding server started ffmpeg with pid " + s.toString())));
                                    } else {
                                        stream.getPublisher().disconnect();
                                    }
                                    return Mono.empty();
                                }))
                        .then())
                .bindNow();
        server.onDispose().block();
    }
}
