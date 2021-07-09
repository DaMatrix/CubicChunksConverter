package net.daporkchop.rocksmc.converter.dataconverter;

import cubicchunks.converter.lib.convert.ChunkDataConverter;
import cubicchunks.converter.lib.convert.data.CubicChunksColumnData;
import net.daporkchop.rocksmc.converter.data.RocksLocalCubicData;
import net.daporkchop.rocksmc.util.ChunkCompressUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author DaPorkchop_
 */
public class RocksLocalCubic2CCDataConverter implements ChunkDataConverter<RocksLocalCubicData, CubicChunksColumnData> {
    @Override
    public Set<CubicChunksColumnData> convert(RocksLocalCubicData input) {
        return Collections.singleton(new CubicChunksColumnData(
                input.getDimension(),
                input.getPosition(),
                ChunkCompressUtils.compressCubicChunks(input.getColumnData()),
                input.getCubeData().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> ChunkCompressUtils.compressCubicChunks(e.getValue())))));
    }
}
