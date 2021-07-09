package net.daporkchop.rocksmc.converter.dataconverter;

import cubicchunks.converter.lib.convert.ChunkDataConverter;
import cubicchunks.converter.lib.convert.data.AnvilChunkData;
import cubicchunks.converter.lib.util.Vector2i;
import net.daporkchop.rocksmc.converter.data.RocksLocalVanillaColumnData;
import net.daporkchop.rocksmc.util.ChunkCompressUtils;

import java.util.Collections;
import java.util.Set;

/**
 * @author DaPorkchop_
 */
public class Anvil2RocksLocalVanillaDataConverter implements ChunkDataConverter<AnvilChunkData, RocksLocalVanillaColumnData> {
    @Override
    public Set<RocksLocalVanillaColumnData> convert(AnvilChunkData input) {
        return Collections.singleton(new RocksLocalVanillaColumnData(
                input.getDimension(),
                new Vector2i(input.getPosition().getEntryX(), input.getPosition().getEntryZ()),
                ChunkCompressUtils.decompressFromAnvil(input.getData()),
                input.offsetSections));
    }
}
