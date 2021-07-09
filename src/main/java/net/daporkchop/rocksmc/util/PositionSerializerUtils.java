package net.daporkchop.rocksmc.util;

import cubicchunks.converter.lib.util.Vector2i;
import cubicchunks.converter.lib.util.Vector3i;

import java.nio.ByteBuffer;

import static com.google.common.base.Preconditions.*;

/**
 * @author DaPorkchop_
 */
public class PositionSerializerUtils {
    public static int[] readInterleavedInts(byte[] src) {
        checkArgument((src.length & 3) == 0, "array must be a multiple of 4 bytes, but is %d", src.length);

        int cnt = src.length >> 2;
        int[] coords = new int[cnt];
        for (int bit = 0; bit < (cnt << 5); ) {
            int b = src[bit >> 3] & 0xFF;
            for (int i = 7; i >= 0; i--, bit++) {
                coords[bit % cnt] |= ((b >>> i) & 1) << (bit / cnt);
            }
        }
        return coords;
    }

    public static byte[] writeInterleavedInts(int... coords) {
        byte[] arr = new byte[coords.length << 2];
        for (int bit = 0; bit < (coords.length << 5); ) {
            byte b = 0;
            for (int i = 7; i >= 0; i--, bit++) {
                b |= ((coords[bit % coords.length] >>> (bit / coords.length)) & 1) << i;
            }
            arr[(bit >> 3) - 1] = b;
        }
        return arr;
    }

    public static Vector2i readVec2i(byte[] src) {
        long l = ByteBuffer.wrap(src).getLong();
        int x = 0;
        int z = 0;
        for (int i = 0; i < 32; i++) {
            int bits = (int) (l >>> (i << 1)) & 0x3;
            x |= (bits >> 1) << i;
            z |= (bits & 1) << i;
        }
        return new Vector2i(x, z);
    }

    public static byte[] writeVec2i(Vector2i pos) {
        byte[] dst = new byte[8];

        int x = pos.getX();
        int z = pos.getY();
        long l = 0L;
        for (int i = 0; i < 32; i++) {
            l |= (long) ((((x >>> i) & 1) << 1) | ((z >>> i) & 1)) << (i << 1);
        }
        ByteBuffer.wrap(dst).putLong(l);
        return dst;
    }

    public static Vector3i readVec3i(byte[] src) {
        int startIndex = src.length - 1;
        int x = readInterleavedIntBits(src, startIndex, 3, 2);
        int y = readInterleavedIntBits(src, startIndex, 3, 1);
        int z = readInterleavedIntBits(src, startIndex, 3, 0);
        return new Vector3i(x, y, z);
    }

    private static int readInterleavedIntBits(byte[] src, int startIndex, int nValues, int bitOffset) {
        int value = 0;
        for (int shift = 0; shift < 32; shift++) {
            int targetBitIndex = bitOffset + shift * nValues;
            int targetByteIndex = startIndex - (targetBitIndex >>> 3);
            int bit = (src[targetByteIndex] >>> (targetBitIndex & 0x7)) & 1;
            value |= bit << shift;
        }
        return value;
    }

    public static byte[] writeVec3i(Vector3i pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        byte[] dst = new byte[12];
        int startIndex = dst.length - 1;
        writeInterleavedIntBits(dst, startIndex, 3, 2, x);
        writeInterleavedIntBits(dst, startIndex, 3, 1, y);
        writeInterleavedIntBits(dst, startIndex, 3, 0, z);
        return dst;
    }

    private static void writeInterleavedIntBits(byte[] dst, int startIndex, int nValues, int bitOffset, int value) {
        for (int shift = 0; shift < 32; shift++) {
            int bit = (value >>> shift) & 1;
            int targetBitIndex = bitOffset + shift * nValues;
            int targetByteIndex = startIndex - (targetBitIndex >>> 3);
            dst[targetByteIndex] |= bit << (targetBitIndex & 0x7);
        }
    }
}
