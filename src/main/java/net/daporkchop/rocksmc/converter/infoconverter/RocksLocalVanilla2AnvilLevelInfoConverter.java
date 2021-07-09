package net.daporkchop.rocksmc.converter.infoconverter;

import cubicchunks.converter.lib.convert.data.AnvilChunkData;
import net.daporkchop.rocksmc.converter.data.RocksLocalVanillaColumnData;

import java.nio.file.Path;

/**
 * @author DaPorkchop_
 */
public class RocksLocalVanilla2AnvilLevelInfoConverter extends CopyEverythingExceptLevelInfoConverter<RocksLocalVanillaColumnData, AnvilChunkData> {
    public RocksLocalVanilla2AnvilLevelInfoConverter(Path srcDir, Path dstDir) {
        super(srcDir, dstDir, "rocksmc_local");
    }
}
