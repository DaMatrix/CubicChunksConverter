/*
 *  This file is part of CubicChunksConverter, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2017 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package cubicchunks.converter.lib.convert.io;

import cubicchunks.converter.lib.Dimension;
import cubicchunks.converter.lib.convert.data.AnvilChunkData;
import cubicchunks.converter.lib.convert.data.NukkitChunkData;
import cubicchunks.converter.lib.util.UncheckedInterruptedException;
import cubicchunks.regionlib.impl.save.MinecraftSaveSection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static cubicchunks.converter.lib.util.Utils.*;
import static cubicchunks.regionlib.impl.save.MinecraftSaveSection.MinecraftRegionType.*;
import static java.nio.file.Files.*;

public class NukkitChunkReader extends BaseMinecraftReader<NukkitChunkData, MinecraftSaveSection> {
    private final Thread loadThread;

    public NukkitChunkReader(Path srcDir) {
        super(srcDir, (dim, path) -> exists(getDimensionPath(dim, path)) ? MinecraftSaveSection.createAt(getDimensionPath(dim, path), MCA) : null);
        loadThread = Thread.currentThread();
    }

    private static Path getDimensionPath(Dimension d, Path worldDir) {
        if (!d.getDirectory().isEmpty()) {
            worldDir = worldDir.resolve(d.getDirectory());
        }
        return worldDir.resolve("region");
    }

    @Override
    public void countInputChunks(Runnable increment) throws IOException {
        try {
            doCountChunks(increment);
        } catch (UncheckedInterruptedException ex) {
            // return
        }
    }

    private void doCountChunks(Runnable increment) throws IOException, UncheckedInterruptedException {
        for (MinecraftSaveSection save : saves.values()) {
            save.forAllKeys(interruptibleConsumer(loc -> increment.run()));
        }
    }

    @Override
    public void loadChunks(Consumer<? super NukkitChunkData> consumer, Predicate<Throwable> errorHandler) throws IOException {
        try {
            doLoadChunks(consumer, errorHandler);
        } catch (UncheckedInterruptedException ex) {
            // return
        }
    }

    private void doLoadChunks(Consumer<? super NukkitChunkData> consumer, Predicate<Throwable> errorHandler) throws IOException, UncheckedInterruptedException {
        for (Map.Entry<Dimension, MinecraftSaveSection> entry : saves.entrySet()) {
            if (Thread.interrupted()) {
                return;
            }
            MinecraftSaveSection vanillaSave = entry.getValue();
            Dimension d = entry.getKey();
            vanillaSave.forAllKeys(interruptibleConsumer(mcPos -> {
                try {
                    Optional<ByteBuffer> load = vanillaSave.load(mcPos, true);
                    consumer.accept(new NukkitChunkData(d, mcPos, load.orElse(null)));
                } catch (Exception e) {
                    e.printStackTrace();
                    if (!errorHandler.test(e)) {
                        throw new UncheckedInterruptedException();
                    }
                }
            }));
        }
    }

    @Override
    public void stop() {
        loadThread.interrupt();
    }
}
