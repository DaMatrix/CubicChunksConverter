/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2021-2021 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.rocksmc.converter.io;

import cubicchunks.converter.lib.Dimension;
import cubicchunks.converter.lib.convert.io.BaseMinecraftReader;
import cubicchunks.converter.lib.util.UncheckedInterruptedException;
import cubicchunks.converter.lib.util.Utils;
import cubicchunks.regionlib.util.CheckedFunction;
import net.daporkchop.rocksmc.converter.data.RocksLocalVanillaColumnData;
import net.daporkchop.rocksmc.storage.IBinaryCubeStorage;
import net.daporkchop.rocksmc.storage.local.LocalStorageImpl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author DaPorkchop_
 */
public class RocksLocalVanillaReader extends BaseMinecraftReader<RocksLocalVanillaColumnData, IBinaryCubeStorage> {
    private static Path getDimensionPath(Dimension d, Path worldDir) {
        if (!d.getDirectory().isEmpty()) {
            worldDir = worldDir.resolve(d.getDirectory());
        }
        return worldDir;
    }

    private volatile boolean running = true;
    private final CompletableFuture<Void> countFuture = new CompletableFuture<>();
    private final CompletableFuture<Void> loadFuture = new CompletableFuture<>();

    private final int offset;

    public RocksLocalVanillaReader(Path srcDir) {
        super(srcDir, (dim, path) -> Files.exists(getDimensionPath(dim, path))
                ? Utils.propagateExceptions((CheckedFunction<Path, IBinaryCubeStorage, IOException>) LocalStorageImpl::new).apply(getDimensionPath(dim, path))
                : null);

        try {
            Path offsetFile = srcDir.resolve("offset.txt");
            this.offset = Files.exists(offsetFile) ? Integer.parseInt(Files.readAllLines(offsetFile, StandardCharsets.UTF_8).get(0)) : 0;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void countInputChunks(Runnable increment) throws IOException, InterruptedException {
        try {
            CompletableFuture.allOf(this.saves.values().stream()
                    .map(storage -> CompletableFuture.runAsync(() -> {
                        try {
                            storage.forEachColumn(pos -> {
                                if (!this.running) {
                                    throw new UncheckedInterruptedException();
                                }

                                increment.run();
                            });
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }))
                    .toArray(CompletableFuture[]::new))
                    .join();

            this.countFuture.complete(null);
        } catch (UncheckedInterruptedException ignored) {
            //do nothing
        } catch (UncheckedIOException e) {
            throw e.getCause();
        } finally {
            this.countFuture.completeExceptionally(new IllegalStateException());
        }
    }

    @Override
    public void loadChunks(Consumer<? super RocksLocalVanillaColumnData> consumer, Predicate<Throwable> errorHandler) throws IOException, InterruptedException {

        try {
            this.countFuture.join();

            CompletableFuture.allOf(this.saves.entrySet().stream()
                    .map(entry -> {
                        Dimension dim = entry.getKey();
                        IBinaryCubeStorage storage = entry.getValue();

                        return CompletableFuture.runAsync(() -> {
                            try {
                                storage.forEachColumn((pos, data) -> {
                                    if (!this.running) {
                                        throw new UncheckedInterruptedException();
                                    }

                                    try {
                                        consumer.accept(new RocksLocalVanillaColumnData(dim, pos, data, this.offset));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        if (!errorHandler.test(e)) {
                                            throw new UncheckedInterruptedException();
                                        }
                                    }
                                });
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
                    })
                    .toArray(CompletableFuture[]::new))
                    .join();

            this.loadFuture.complete(null);
        } catch (UncheckedInterruptedException ex) {
            // interrupted, do nothing
        } catch (UncheckedIOException e) {
            throw e.getCause();
        } finally {
            this.loadFuture.completeExceptionally(new IllegalStateException());
        }
    }

    @Override
    public void stop() {
        this.running = false;
        CompletableFuture.allOf(this.countFuture, this.loadFuture).join();
    }
}
