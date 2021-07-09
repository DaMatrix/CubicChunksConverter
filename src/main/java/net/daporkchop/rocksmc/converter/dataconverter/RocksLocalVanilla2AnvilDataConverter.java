package net.daporkchop.rocksmc.converter.dataconverter;

import cubicchunks.converter.lib.convert.ChunkDataConverter;
import cubicchunks.converter.lib.convert.data.AnvilChunkData;
import cubicchunks.regionlib.impl.MinecraftChunkLocation;
import net.daporkchop.rocksmc.converter.data.RocksLocalVanillaColumnData;
import net.daporkchop.rocksmc.util.ChunkCompressUtils;

import java.util.Collections;
import java.util.Set;

/**
 * @author DaPorkchop_
 */
public class RocksLocalVanilla2AnvilDataConverter implements ChunkDataConverter<RocksLocalVanillaColumnData, AnvilChunkData> {
    @Override
    public Set<AnvilChunkData> convert(RocksLocalVanillaColumnData input) {
        return Collections.singleton(new AnvilChunkData(
                input.getDimension(),
                new MinecraftChunkLocation(input.getPosition().getX(), input.getPosition().getY(), "mca"),
                ChunkCompressUtils.compressAnvil(input.getColumnData()),
                input.offsetSections));
    }
}
