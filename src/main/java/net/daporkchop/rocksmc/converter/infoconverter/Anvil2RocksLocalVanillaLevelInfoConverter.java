package net.daporkchop.rocksmc.converter.infoconverter;

import cubicchunks.converter.lib.convert.data.AnvilChunkData;
import net.daporkchop.rocksmc.converter.data.RocksLocalVanillaColumnData;

import java.nio.file.Path;

/**
 * @author DaPorkchop_
 */
public class Anvil2RocksLocalVanillaLevelInfoConverter extends CopyEverythingExceptLevelInfoConverter<AnvilChunkData, RocksLocalVanillaColumnData> {
    public Anvil2RocksLocalVanillaLevelInfoConverter(Path srcDir, Path dstDir) {
        super(srcDir, dstDir, "region");
    }
}
