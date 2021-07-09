package net.daporkchop.rocksmc.util;

import cubicchunks.converter.lib.util.Vector2i;
import cubicchunks.converter.lib.util.Vector3i;

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
        int[] coords = readInterleavedInts(src);
        return new Vector2i(coords[0], coords[1]);
    }

    public static byte[] writeVec2i(Vector2i pos) {
        return writeInterleavedInts(pos.getX(), pos.getY());
    }

    public static Vector3i readVec3i(byte[] src) {
        int[] coords = readInterleavedInts(src);
        return new Vector3i(coords[0], coords[1], coords[2]);
    }

    public static byte[] writeVec3i(Vector3i pos) {
        return writeInterleavedInts(pos.getX(), pos.getY(), pos.getZ());
    }
}
