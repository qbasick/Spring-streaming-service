package com.qbasic.streamingservice.rtmp.amf0;



import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

import java.io.Serial;
import java.time.Instant;
import java.util.*;

/**
 * This is an implementation of a binary format Action Message Format (AMF).
 * AMF3 version is not implemented in this application, only AMF0 is supported
 * */

@Slf4j
public class Amf0Rules {

    // Use LinkedHashMap to preserve insertion order
    public static class Amf0Object extends LinkedHashMap<String, Object> {
        @Serial
        private static final long serialVersionUID = 42L;
    }
    public enum Type {

        NUMBER(0x00),
        BOOLEAN(0x01),
        STRING(0x02),
        // Java map
        OBJECT(0x03),
        NULL(0x05),
        UNDEFINED(0x06),
        // Java map
        ECMA_ARRAY(0x08),
        // Java object array
        STRICT_ARRAY(0x0A),
        DATE(0x0B),
        LONG_STRING(0x0C),
        UNSUPPORTED(0x0D),

        /* Not supported */
        RECORDSET(0x0E),
        XML_DOCUMENT(0x0F),
        TYPED_OBJECT(0x10);

        // 0x09 end marker
        private static final byte[] OBJECT_END_MARKER = new byte[] { 0x00, 0x00, 0x09 };
        private final int value;
        Type(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

        public static Type getType(Object value) {
            if (value == null)                  return NULL;
            if (value instanceof Number)        return NUMBER;
            if (value instanceof Boolean)       return BOOLEAN;
            if (value instanceof String)        return STRING;
            if (value instanceof Amf0Object)    return OBJECT;
            if (value instanceof Map)           return ECMA_ARRAY;
            if (value instanceof Object[])      return STRICT_ARRAY;
            if (value instanceof Instant)       return DATE;

            throw new RuntimeException("Unsupported type: " + value.getClass());
        }

        public static Type getFromHexValue(int value) {
            return switch (value) {
                case 0x00 -> NUMBER;
                case 0x01 -> BOOLEAN;
                case 0x02 -> STRING;
                case 0x03 -> OBJECT;
                case 0x05 -> NULL;
                case 0x06 -> UNDEFINED;
                case 0x08 -> ECMA_ARRAY;
                case 0x0A -> STRICT_ARRAY;
                case 0x0B -> DATE;
                case 0x0C -> LONG_STRING;
                case 0x0D -> UNSUPPORTED;

                default -> throw new RuntimeException("Unsupported type: " + value);
            };
        }
    }
    /**
     * These methods are critical for performance,
     * so we use netty ByteBuf class
     * */

    public static void encode(ByteBuf buf, Object value) {
        Type type = Type.getType(value);

        buf.writeByte((byte) type.getValue());

        switch (type) {
            case NUMBER         -> encodeNumber(buf, (Number) value);
            case BOOLEAN        -> encodeBoolean(buf, (Boolean) value);
            case STRING         -> encodeString(buf, (String) value);
            case ECMA_ARRAY     -> encodeEcmaArray(buf, (Map<String, Object>) value);
            case OBJECT         -> encodeAmf0Object(buf, (Map<String, Object>) value);
            case STRICT_ARRAY   -> encodeArray(buf, (Object[]) value);
            case DATE           -> encodeDate(buf, (Instant) value);
            case NULL           -> {}
            default             -> throw new RuntimeException("Unsupported type " + value);
        }

    }

    /**The data following a
     * Number type marker is always an
     * 8 byte IEEE-754 double precision floating point value
     * in network byte order
     * **/
    private static void encodeNumber(ByteBuf buf, Number number) {
        // case double
        if (number instanceof Double) {
            buf.writeLong(Double.doubleToLongBits((Double) number));
        // other cases
        } else {
            buf.writeLong(Double.doubleToLongBits(Double.parseDouble(number.toString())));
        }
    }

    private static void encodeString(ByteBuf buf, String string) {
        byte[] bytes = string.getBytes();
        // length of the string
        buf.writeShort((short) bytes.length);
        buf.writeBytes(bytes);
    }

    private static void encodeBoolean(ByteBuf buf, Boolean bool) {
        buf.writeByte(bool ? 0x01 : 0x00);
    }

    private static void encodeAmf0Object(ByteBuf buf, Map<String, Object> amf0Object) {
        for (Map.Entry<String, Object> entry : amf0Object.entrySet()) {
            encodeString(buf, entry.getKey());
            encode(buf, entry.getValue());
        }
        buf.writeBytes(Type.OBJECT_END_MARKER);
    }

    private static void encodeEcmaArray(ByteBuf buf, Map<String, Object> map) {
        buf.writeInt(map.size());
        encodeAmf0Object(buf, map);
    }

    private static void encodeArray(ByteBuf buf, Object[] array) {
        buf.writeInt(array.length);
        for (Object o : array) {
            encode(buf, o);
        }
    }

    private static void encodeDate(ByteBuf buf, Instant instant) {
        long epochSecond = instant.getEpochSecond();
        buf.writeLong(Double.doubleToLongBits(epochSecond));
        buf.writeShort(0);
    }

    public static void encodeList(ByteBuf buf, List<Object> list) {
        for (Object obj : list) {
            encode(buf, obj);
        }
    }


    public static Object decode(ByteBuf buf) {
        Type type = Type.getFromHexValue(buf.readByte());

        return switch (type) {
            case NUMBER         -> decodeNumber(buf);
            case BOOLEAN        -> decodeBoolean(buf);
            case STRING         -> decodeString(buf);
            case ECMA_ARRAY     -> decodeEcmaArray(buf);
            case OBJECT         -> decodeAmf0Object(buf);
            case STRICT_ARRAY   -> decodeArray(buf);
            case DATE           -> decodeDate(buf);
            case LONG_STRING    -> decodeLongString(buf);
            case    NULL,
                    UNDEFINED,
                    UNSUPPORTED -> null;
            default             -> throw new RuntimeException("Unsupported type " + type);
        };
    }

    public static List<Object> decodeAll(ByteBuf buf) {
        List<Object> result = new ArrayList<>();
        while (buf.isReadable()) {
            Object obj = decode(buf);
            result.add(obj);
        }
        return result;
    }

    private static Object decodeNumber(ByteBuf buf) {
        return Double.longBitsToDouble(buf.readLong());
    }

    private static Boolean decodeBoolean(ByteBuf buf) {
        return buf.readByte() == 0x01;
    }

    private static String decodeString(ByteBuf buf) {
        short size = buf.readShort();
        byte[] bytes = new byte[size];
        buf.readBytes(bytes);
        return new String(bytes);
    }

    private static Object decodeAmf0Object(ByteBuf buf) {
        Map<String, Object> map = new Amf0Object();
        byte[] endMarker = new byte[3];
        while (buf.isReadable()) {
            buf.getBytes(buf.readerIndex(), endMarker);
            if (Arrays.equals(endMarker, Type.OBJECT_END_MARKER)) {
                buf.skipBytes(3);
                break;
            }
            map.put(decodeString(buf), decode(buf));
        }
        return map;
    }

    private static Object decodeEcmaArray(ByteBuf buf) {
        buf.readInt();
        return decodeAmf0Object(buf);
    }

    private static Object[] decodeArray(ByteBuf buf) {
        int len = buf.readInt();
        final Object[] array = new Object[len];
        for (int i = 0; i < len; i++) {
            array[i] = decode(buf);
        }
        return array;
    }

    private static Instant decodeDate(ByteBuf buf) {
        long epochSecond = buf.readLong();
        return Instant.ofEpochSecond(epochSecond);
    }

    private static String decodeLongString(ByteBuf buf) {
        int size = buf.readInt();
        byte[] bytes = new byte[size];
        buf.readBytes(bytes);
        return new String(bytes);
    }

}
