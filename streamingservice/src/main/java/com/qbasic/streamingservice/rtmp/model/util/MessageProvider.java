package com.qbasic.streamingservice.rtmp.model.util;

import com.qbasic.streamingservice.rtmp.amf0.Amf0Rules;
import com.qbasic.streamingservice.rtmp.model.messages.RtmpHeader;
import com.qbasic.streamingservice.rtmp.model.messages.RtmpMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.List;

import static com.qbasic.streamingservice.rtmp.model.messages.RtmpConstants.RTMP_DEFAULT_MESSAGE_STREAM_ID_VALUE;

public class MessageProvider {

    public static RtmpMessage onStatus(String level, String code, String description) {
        List<Object> result = new ArrayList<>();
        result.add("onStatus");
        result.add(0);
        result.add(null);

        Amf0Rules.Amf0Object status = new Amf0Rules.Amf0Object();
        status.put("level", level);
        status.put("code", code);
        status.put("description", description);

        result.add(status);

        ByteBuf payload = Unpooled.buffer();
        Amf0Rules.encodeList(payload, result);

        RtmpHeader header = HeaderProvider.commandMessageHeader(payload.readableBytes());

        return new RtmpMessage(header, payload);
    }

    public static RtmpMessage userControlMessageEvent(int event) {
        RtmpHeader userCtrlEventHeader = HeaderProvider.userControlMessageEventHeader(event);
        ByteBuf userCtrlEventPayload = Unpooled
                .buffer(6)
                .writeShort((short) event)
                .writeInt(RTMP_DEFAULT_MESSAGE_STREAM_ID_VALUE);
        return new RtmpMessage(userCtrlEventHeader, userCtrlEventPayload);
    }

    public static RtmpMessage commandMessage(List<Object> objects) {
        ByteBuf commandBuf = Unpooled.buffer();
        Amf0Rules.encodeList(commandBuf, objects);
        int size = commandBuf.readableBytes();

        return new RtmpMessage(HeaderProvider.commandMessageHeader(size), commandBuf);
    }

    public static RtmpMessage dataMessage(List<Object> objects) {
        ByteBuf commandBuf = Unpooled.buffer();
        Amf0Rules.encodeList(commandBuf, objects);
        int size = commandBuf.readableBytes();

        return new RtmpMessage(HeaderProvider.dataMessageHeader(size), commandBuf);
    }

    public static RtmpMessage setWindowAcknowledgement(int ackSize) {
        ByteBuf payload = Unpooled.buffer(4).writeInt(ackSize);
        return new RtmpMessage(HeaderProvider.setWindowAcknowledgementHeader(), payload);
    }

    public static RtmpMessage setPeerBandwidth(int bandwidth, int type) {
        ByteBuf payload = Unpooled.buffer(5).writeInt(bandwidth).writeByte(type);
        return new RtmpMessage(HeaderProvider.setPeerBandwidthHeader(), payload);
    }

    public static RtmpMessage setChunkSize(int chunkSize) {
        ByteBuf payload = Unpooled.buffer(4).writeInt(chunkSize);
        return new RtmpMessage(HeaderProvider.setChunkSizeHeader(), payload);
    }

    public static RtmpMessage acknowledgement(int sequence) {
        ByteBuf payload = Unpooled.buffer(4).writeInt(sequence);
        return new RtmpMessage(HeaderProvider.acknowledgementHeader(), payload);
    }
}
