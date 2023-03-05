package com.qbasic.streamingservice.rtmp.model.context;

import com.qbasic.streamingservice.rtmp.model.messages.RtmpMediaMessage;
import com.qbasic.streamingservice.rtmp.model.messages.RtmpMessage;
import com.qbasic.streamingservice.rtmp.model.util.MessageProvider;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

import static com.qbasic.streamingservice.rtmp.model.messages.RtmpConstants.*;


@Getter
@Setter
@Slf4j
public class Stream {

    private Map<String, Object> metadata;
    private Channel publisher;

    private final Set<Channel> subscribers;
    private final String streamName;
    private String streamKey;

    // Group of pictures cache to broadcast. Cleared if keyframe is received
    private final BlockingQueue<RtmpMediaMessage> rtmpGopCache;

    private RtmpMediaMessage videoConfig;
    private RtmpMediaMessage audioConfig;

    private CompletableFuture<Boolean> readyToBroadcast;

    public Stream(String streamName) {
        this.streamName = streamName;
        this.subscribers = new LinkedHashSet<>();
        this.rtmpGopCache = new ArrayBlockingQueue<>(1024);
        this.readyToBroadcast = new CompletableFuture<>();
    }

    public void addMedia(RtmpMediaMessage message) {
        short type = message.header().getType();

        if (type == (short) RTMP_MSG_USER_CONTROL_TYPE_AUDIO) {
            if (message.isAudioConfig()) {
                log.info("Audio config is set");
                audioConfig = message;
            }
        } else if (type == (short) RTMP_MSG_USER_CONTROL_TYPE_VIDEO) {
            if (message.isVideoConfig()) {
                log.info("Video config is set");
                videoConfig = message;
            }
            // clear interFrames queue
            if (message.isKeyframe()) {
                log.info("Keyframe added. {} frames cleared", rtmpGopCache.size());
                rtmpGopCache.clear();
            }
        }
        rtmpGopCache.add(message);
        broadcastMessage(message);
    }

    public void broadcastMessage(RtmpMediaMessage message) {
        // Handle concurrent modification exception
        if (!readyToBroadcast.isDone()) {
            readyToBroadcast.complete(Boolean.TRUE);
        }
        Iterator<Channel> channelIterator = subscribers.iterator();
        while (channelIterator.hasNext()) {
            Channel next = channelIterator.next();
            if (next.isActive()) {
                next.writeAndFlush(RtmpMediaMessage.toRtmpMessage(message));
            } else {
                log.info("Inactive channel detected");
                channelIterator.remove();
            }
        }
    }

    public void addSubscriber(Channel channel) {
        log.info("Subscriber {} added to stream {}", channel.remoteAddress(), streamName);
        subscribers.add(channel);

        channel.writeAndFlush(RtmpMediaMessage.toRtmpMessage(videoConfig));
        channel.writeAndFlush(RtmpMediaMessage.toRtmpMessage(audioConfig));

        log.info("Sending group of pictures to client");
        for (RtmpMediaMessage message : rtmpGopCache) {
            channel.writeAndFlush(RtmpMediaMessage.toRtmpMessage(message));
        }
    }

    public void closeStream() {

        log.info("Closing stream");
        RtmpMessage eof = MessageProvider.userControlMessageEvent(STREAM_EOF);

        for (Channel channel : subscribers) {
            channel.writeAndFlush(eof).addListener(ChannelFutureListener.CLOSE);
        }
    }

    public void sendPublishMessage() {
        publisher.writeAndFlush(MessageProvider.onStatus(
                "status",
                "NetStream.Publish.Start",
                "Start publishing"));
    }

}
