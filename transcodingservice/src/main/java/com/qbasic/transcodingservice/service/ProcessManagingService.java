package com.qbasic.transcodingservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ProcessManagingService {
    @Value("${ffmpeg.command}")
    private String template;

    @Value("${rtmp.server}")
    private String address;

    @Value("${stream.directory}")
    private String path;

    private final ConcurrentHashMap<String, Process> processMap = new ConcurrentHashMap<>();

    public Mono<Long> startProcess(String owner) {
        if (processMap.containsKey(owner) && processMap.get(owner).isAlive()) {
            processMap.get(owner).destroyForcibly();
            processMap.remove(owner);
        }
        String command = String.format(template, address + "/" + owner, owner + "_%v/data%d.ts", owner + "_%v.m3u8");

        ProcessBuilder processBuilder = new ProcessBuilder();
        log.info(command);

        List<String> splitCommand = new ArrayList<>();
        Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        Matcher regexMatcher = regex.matcher(command);
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                // Add double-quoted string without the quotes
                splitCommand.add(regexMatcher.group(1));
            } else if (regexMatcher.group(2) != null) {
                // Add single-quoted string without the quotes
                splitCommand.add(regexMatcher.group(2));
            } else {
                // Add unquoted word
                splitCommand.add(regexMatcher.group());
            }
        }

        processBuilder.command(splitCommand);
        processBuilder.redirectErrorStream(true);
        processBuilder.inheritIO();

        return Mono
                .fromCallable(() -> {
                    Path directory = Paths.get(path).resolve(owner);
                    if (!Files.isDirectory(directory)) {
                        try {
                            Files.createDirectory(directory);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    processBuilder.directory(new File(directory.toAbsolutePath().toString()));
                    return processBuilder.start();
                })
                .flatMap(process -> {
                    log.info(process.info().toString());
                    log.info(String.valueOf(process.pid()));
                    process.onExit().thenAccept((c) -> {
                        log.info(owner + " exited with code " + c.exitValue());
                        if (!processMap.get(owner).isAlive()) {
                            processMap.remove(owner);
                        }
                    });
                    processMap.put(owner, process);
                    return Mono.just(process.pid());
                }).subscribeOn(Schedulers.boundedElastic());
    }
}
