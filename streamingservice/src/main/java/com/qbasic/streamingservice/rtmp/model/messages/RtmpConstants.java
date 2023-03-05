package com.qbasic.streamingservice.rtmp.model.messages;

public class RtmpConstants {

    public static final byte RTMP_VERSION = 3;
    public static final int RTMP_HANDSHAKE_SIZE = 1536;
    public static final int RTMP_HANDSHAKE_VERSION_LENGTH = 1;
    public static final int RTMP_MAX_TIMESTAMP = 0XFFFFFF;
    public static final int RTMP_DEFAULT_CHUNK_SIZE = 128;


    public static final int RTMP_DEFAULT_OUTPUT_ACK_SIZE = 5_000_000;
    public static final int RTMP_DEFAULT_OUTPUT_CHUNK_SIZE = 5000;
    /*
    *   The message stream ID can be any arbitrary value.
    *   Different message streams multiplexed onto the same chunk stream are demultiplexed based on their message stream IDs.
    *   Beyond that, as far as RTMP Chunk Stream is concerned, this is an opaque value.
    *   This field occupies 4 bytes in the chunk header in little endian format.
    * */
    public static final int RTMP_DEFAULT_MESSAGE_STREAM_ID_VALUE = 42;

    public static final int RTMP_CHUNK_TYPE_0 = 0; // 11-bytes: timestamp(3) + length(3) + stream type(1) + stream id(4)
    public static final int RTMP_CHUNK_TYPE_1 = 1; // 7-bytes: delta(3) + length(3) + stream type(1)
    public static final int RTMP_CHUNK_TYPE_2 = 2; // 3-bytes: delta(3)
    public static final int RTMP_CHUNK_TYPE_3 = 3; // 0-byte

    /* Protocol Control Messages */
    public static final int RTMP_MSG_CONTROL_TYPE_SET_CHUNK_SIZE = 1;
    public static final int RTMP_MSG_CONTROL_TYPE_ABORT = 2;
    public static final int RTMP_MSG_CONTROL_TYPE_ACKNOWLEDGEMENT = 3; // bytes read report
    public static final int RTMP_MSG_CONTROL_TYPE_WINDOW_ACKNOWLEDGEMENT_SIZE = 5; // server bandwidth
    public static final int RTMP_MSG_CONTROL_TYPE_SET_PEER_BANDWIDTH = 6; // client bandwidth

    /* User Control Messages Event (4) */
    public static final int RTMP_MSG_USER_CONTROL_TYPE_EVENT = 4;

    public static final int RTMP_MSG_USER_CONTROL_TYPE_AUDIO = 8;
    public static final int RTMP_MSG_USER_CONTROL_TYPE_VIDEO = 9;

    /* Data Message */
    public static final int RTMP_MSG_DATA_TYPE_AMF3 = 15; // AMF3
    public static final int RTMP_MSG_DATA_TYPE_AMF0 = 18; // AMF0

    /* Shared Object Message */
    public static final int RTMP_MSG_SHARED_OBJ_TYPE_AMF3 = 16; // AMF3
    public static final int RTMP_MSG_SHARED_OBJ_TYPE_AMF0 = 19; // AMF0

    /* Command Message */
    public static final int RTMP_MSG_COMMAND_TYPE_AMF3 = 17; // AMF3
    public static final int RTMP_MSG_COMMAND_TYPE_AMF0 = 20; // AMF0

    /* Stream status */
    public static final int STREAM_BEGIN = 0x00;
    public static final int STREAM_EOF = 0x01;
    public static final int STREAM_DRY = 0x02;
    public static final int STREAM_EMPTY = 0x1f;
    public static final int STREAM_READY = 0x20;


}
