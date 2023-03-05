package com.qbasic.streamingservice.rtmp.handlers;

import com.qbasic.streamingservice.rtmp.model.messages.RtmpConstants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Random;

@Slf4j
public class HandshakeHandler extends ByteToMessageDecoder {

    private boolean C0C1;
    private boolean completed;
    private int timestamp;

    private byte[] clientBytes = new byte[RtmpConstants.RTMP_HANDSHAKE_SIZE - 8];

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        if (completed) {
            // Continue pipeline
            channelHandlerContext.fireChannelRead(byteBuf);
            return;
        }

        // client 0 client 1
        if (!C0C1) {
            // C0
            // read version
            byte version = byteBuf.readByte();
            if (!(version == RtmpConstants.RTMP_VERSION)) {
                log.info("Client requests unsupported version: " + version);
            }
            // C1
            // read timestamp
            timestamp = byteBuf.readInt();
            // read zero
            byteBuf.readInt();
            // read random bytes
            byteBuf.readBytes(clientBytes);

            generateS0S1S2(channelHandlerContext);
            C0C1 = true;

        } else /* Read C2 */ {
            // C2
            // read timestamp
            byteBuf.readInt();
            // read zero
            byteBuf.readInt();
            // read random bytes
            byteBuf.readBytes(clientBytes);

            // Clear buffer
            clientBytes = null;
            completed = true;
            // Handshake is completed. Remove this handler from the pipeline
            channelHandlerContext.channel().pipeline().remove(this);
        }
    }

    private void generateS0S1S2(ChannelHandlerContext channelHandlerContext) {
        ByteBuf resp = Unpooled.buffer(RtmpConstants.RTMP_HANDSHAKE_VERSION_LENGTH
                                                + RtmpConstants.RTMP_HANDSHAKE_SIZE
                                                + RtmpConstants.RTMP_HANDSHAKE_SIZE);

        // S0
        resp.writeByte(RtmpConstants.RTMP_VERSION);

        // S1
        // Write timestamp, always zero
        resp.writeInt(0);
        // Write zero
        resp.writeInt(0);
        // Write random 1528 bytes
        resp.writeBytes(randomBytes(RtmpConstants.RTMP_HANDSHAKE_SIZE - 8));

        // S2
        // Write timestamp
        resp.writeInt(timestamp);
        // Write zero
        resp.writeInt(0);
        // Write random 1528 bytes
        resp.writeBytes(randomBytes(RtmpConstants.RTMP_HANDSHAKE_SIZE - 8));

        channelHandlerContext.writeAndFlush(resp);
    }


    // Placeholder method. For production use java.security.SecureRandom
    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        new Random().nextBytes(bytes);
        return bytes;
    }
}
