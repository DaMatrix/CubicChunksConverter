package net.daporkchop.rocksmc.converter.infoconverter;

import cubicchunks.converter.lib.Dimension;
import cubicchunks.converter.lib.Dimensions;
import cubicchunks.converter.lib.convert.LevelInfoConverter;
import cubicchunks.converter.lib.util.Utils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author DaPorkchop_
 */
public abstract class CopyEverythingExceptLevelInfoConverter<IN, OUT> implements LevelInfoConverter<IN, OUT> {
    protected final Path srcDir;
    protected final Path dstDir;
    protected final Set<Path> blacklist;

    public CopyEverythingExceptLevelInfoConverter(Path srcDir, Path dstDir, String... blacklist) {
        this.srcDir = srcDir;
        this.dstDir = dstDir;

        this.blacklist = Dimensions.getDimensions().stream()
                .map(Dimension::getDirectory)
                .map(this.srcDir::resolve)
                .flatMap(path -> Stream.of(blacklist).map(path::resolve))
                .collect(Collectors.toSet());
    }

    @Override
    public void convert() throws IOException {
        Utils.copyEverythingExcept(this.srcDir, this.srcDir, this.dstDir, this.blacklist::contains, path -> {});
    }
}
