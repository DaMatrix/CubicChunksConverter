package net.daporkchop.rocksmc.converter.dataconverter;

import cubicchunks.converter.lib.convert.ChunkDataConverter;
import cubicchunks.converter.lib.convert.data.CubicChunksColumnData;
import net.daporkchop.rocksmc.converter.data.RocksLocalCubicData;
import net.daporkchop.rocksmc.util.ChunkCompressUtils;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author DaPorkchop_
 */
public class CC2RocksLocalCubicDataConverter implements ChunkDataConverter<CubicChunksColumnData, RocksLocalCubicData> {
    @Override
    public Set<RocksLocalCubicData> convert(CubicChunksColumnData input) {
        return Collections.singleton(new RocksLocalCubicData(
                input.getDimension(),
                input.getPosition(),
                input.getColumnData() != null ? ChunkCompressUtils.decompressCubicChunks(((ByteBuffer) input.getColumnData().flip())) : null,
                input.getCubeData().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> ChunkCompressUtils.decompressCubicChunks(((ByteBuffer) e.getValue().flip()))))));
    }
}
