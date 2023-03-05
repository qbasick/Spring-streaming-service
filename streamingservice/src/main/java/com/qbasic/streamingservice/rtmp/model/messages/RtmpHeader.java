package com.qbasic.streamingservice.rtmp.model.messages;

import lombok.Data;

@Data
public class RtmpHeader {
    int fmt;
    int cid;
    int timestamp;
    int messageLength;
    short type;
    int streamId;
    int timestampDelta;
    long extendedTimestamp;
    int headerLength;
}
