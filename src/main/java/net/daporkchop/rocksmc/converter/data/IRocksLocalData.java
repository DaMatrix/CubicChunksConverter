package net.daporkchop.rocksmc.converter.data;

import cubicchunks.converter.lib.Dimension;
import cubicchunks.converter.lib.util.Vector2i;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * @author DaPorkchop_
 */
public interface IRocksLocalData {
    Dimension getDimension();

    Vector2i getPositionAsVector();

    ByteBuffer getColumnData();

    Map<Integer, ByteBuffer> getCubeData();
}
