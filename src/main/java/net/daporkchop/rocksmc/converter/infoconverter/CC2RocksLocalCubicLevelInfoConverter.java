package net.daporkchop.rocksmc.converter.infoconverter;

import cubicchunks.converter.lib.convert.data.CubicChunksColumnData;
import net.daporkchop.rocksmc.converter.data.RocksLocalCubicData;

import java.nio.file.Path;

/**
 * @author DaPorkchop_
 */
public class CC2RocksLocalCubicLevelInfoConverter extends CCFormatChangingCopyEverythingExceptLevelInfoConverter<CubicChunksColumnData, RocksLocalCubicData> {
    public CC2RocksLocalCubicLevelInfoConverter(Path srcDir, Path dstDir) {
        super(srcDir, dstDir, "rocksmc:local", "region2d", "region3d");
    }
}
