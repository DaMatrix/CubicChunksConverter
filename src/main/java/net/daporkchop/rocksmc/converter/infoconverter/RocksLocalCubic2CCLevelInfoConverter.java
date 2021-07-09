package net.daporkchop.rocksmc.converter.infoconverter;

import cubicchunks.converter.lib.convert.data.CubicChunksColumnData;
import net.daporkchop.rocksmc.converter.data.RocksLocalCubicData;

import java.nio.file.Path;

/**
 * @author DaPorkchop_
 */
public class RocksLocalCubic2CCLevelInfoConverter extends CCFormatChangingCopyEverythingExceptLevelInfoConverter<RocksLocalCubicData, CubicChunksColumnData> {
    public RocksLocalCubic2CCLevelInfoConverter(Path srcDir, Path dstDir) {
        super(srcDir, dstDir, "cubicchunks:anvil3d", "rocksmc_local");
    }
}
