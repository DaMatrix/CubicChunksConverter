package net.daporkchop.rocksmc.converter.infoconverter;

import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.StringTag;
import cubicchunks.converter.lib.util.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author DaPorkchop_
 */
public abstract class CCFormatChangingCopyEverythingExceptLevelInfoConverter<IN, OUT> extends CopyEverythingExceptLevelInfoConverter<IN, OUT> {
    protected final String newFormatName;

    public CCFormatChangingCopyEverythingExceptLevelInfoConverter(Path srcDir, Path dstDir, String newFormatName, String... blacklist) {
        super(srcDir, dstDir, blacklist);

        this.newFormatName = newFormatName;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void convert() throws IOException {
        super.convert();

        Path levelDat = this.dstDir.resolve("data").resolve("cubicChunksData.dat");
        CompoundTag nbt;
        try (InputStream in = Files.newInputStream(levelDat)) {
            nbt = Utils.readCompressedCC(in);
        }
        nbt.getValue().put(new StringTag("storageFormat", this.newFormatName));
        try (OutputStream out = Files.newOutputStream(levelDat)) {
            out.write(Utils.writeCompressed(nbt, false).array());
        }
    }
}
