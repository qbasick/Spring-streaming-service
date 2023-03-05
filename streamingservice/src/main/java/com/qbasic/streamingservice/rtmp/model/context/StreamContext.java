package com.qbasic.streamingservice.rtmp.model.context;

import java.util.concurrent.ConcurrentHashMap;

public class StreamContext {

    public final ConcurrentHashMap<String, Stream> context;

    public StreamContext() {
        this.context = new ConcurrentHashMap<>();
    }

    public void addStream(Stream stream) {
        context.put(stream.getStreamName(), stream);
    }
    public void deleteStream(String streamName) {
        context.remove(streamName);
    }
    public Stream getStream(String streamName) {
        if (streamName == null) {
            return null;
        }
        return context.getOrDefault(streamName, null);
    }



}
