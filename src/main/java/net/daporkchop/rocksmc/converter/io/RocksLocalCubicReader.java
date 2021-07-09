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

import com.carrotsearch.hppc.LongObjectMap;
import com.carrotsearch.hppc.LongObjectScatterMap;
import cubicchunks.converter.lib.Dimension;
import cubicchunks.converter.lib.convert.io.BaseMinecraftReader;
import cubicchunks.converter.lib.util.UncheckedInterruptedException;
import cubicchunks.converter.lib.util.Utils;
import cubicchunks.converter.lib.util.Vector2i;
import cubicchunks.converter.lib.util.Vector3i;
import cubicchunks.regionlib.impl.EntryLocation2D;
import cubicchunks.regionlib.util.CheckedFunction;
import net.daporkchop.rocksmc.converter.data.RocksLocalCubicData;
import net.daporkchop.rocksmc.storage.IBinaryCubeStorage;
import net.daporkchop.rocksmc.storage.local.LocalStorageImpl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * @author DaPorkchop_
 */
public class RocksLocalCubicReader extends BaseMinecraftReader<RocksLocalCubicData, IBinaryCubeStorage> {
    private static Path getDimensionPath(Dimension d, Path worldDir) {
        if (!d.getDirectory().isEmpty()) {
            worldDir = worldDir.resolve(d.getDirectory());
        }
        return worldDir;
    }

    private static EntryLocation2D unpack2d(long pos) {
        return new EntryLocation2D((int) pos, (int) (pos >>> 32L));
    }

    private static long pack2d(int x, int z) {
        return (x & 0xFFFFFFFFL) | ((z & 0xFFFFFFFFL) << 32L);
    }

    private volatile boolean running = true;
    private final CompletableFuture<Map<Dimension, LongObjectMap<int[]>>> countFuture = new CompletableFuture<>();
    private final CompletableFuture<Void> loadFuture = new CompletableFuture<>();

    public RocksLocalCubicReader(Path srcDir) {
        super(srcDir, (dim, path) -> Files.exists(getDimensionPath(dim, path))
                ? Utils.propagateExceptions((CheckedFunction<Path, IBinaryCubeStorage, IOException>) LocalStorageImpl::new).apply(getDimensionPath(dim, path))
                : null);
    }

    @Override
    public void countInputChunks(Runnable increment) throws IOException, InterruptedException {
        try {
            Map<Dimension, LongObjectMap<int[]>> dimensions = new ConcurrentHashMap<>();
            this.saves.entrySet().parallelStream().forEach(entry -> {
                if (!this.running) {
                    throw new UncheckedInterruptedException();
                }

                LongObjectMap<int[]> columns = dimensions.computeIfAbsent(entry.getKey(), dim -> new LongObjectScatterMap<>());
                try {
                    entry.getValue().forEachColumn(pos -> {
                        if (!this.running) {
                            throw new UncheckedInterruptedException();
                        }

                        if (columns.put(pack2d(pos.getX(), pos.getY()), new int[0]) == null) {
                            increment.run();
                        }
                    });
                    entry.getValue().forEachCube(pos -> {
                        if (!this.running) {
                            throw new UncheckedInterruptedException();
                        }

                        long key = pack2d(pos.getX(), pos.getZ());
                        int[] arr = columns.get(key);
                        arr = arr != null ? Arrays.copyOf(arr, arr.length + 1) : new int[1];
                        arr[arr.length - 1] = pos.getY();
                        if (columns.put(key, arr) == null) {
                            increment.run();
                        }
                    });
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });

            this.countFuture.complete(dimensions);
        } catch (UncheckedInterruptedException ignored) {
            //do nothing
        } catch (UncheckedIOException e) {
            throw e.getCause();
        } finally {
            this.countFuture.complete(null);
        }
    }

    @Override
    public void loadChunks(Consumer<? super RocksLocalCubicData> consumer, Predicate<Throwable> errorHandler) throws IOException, InterruptedException {
        try {
            Map<Dimension, LongObjectMap<int[]>> dimensions = this.countFuture.join();

            this.saves.entrySet().parallelStream().forEach(entry -> {
                if (!this.running) {
                    throw new UncheckedInterruptedException();
                }

                Dimension dim = entry.getKey();
                IBinaryCubeStorage storage = entry.getValue();
                LongObjectMap<int[]> columns = dimensions.get(dim);

                StreamSupport.stream(columns.spliterator(), true).forEach(cursor -> {
                    if (!this.running) {
                        throw new UncheckedInterruptedException();
                    }

                    try {
                        EntryLocation2D pos = unpack2d(cursor.key);
                        int x = pos.getEntryX();
                        int z = pos.getEntryZ();

                        IBinaryCubeStorage.BinaryBatch batch = storage.readBatch(new IBinaryCubeStorage.PosBatch(
                                Collections.singleton(new Vector2i(x, z)),
                                IntStream.of(cursor.value).mapToObj(y -> new Vector3i(x, y, z)).collect(Collectors.toSet())));

                        consumer.accept(new RocksLocalCubicData(
                                dim, pos,
                                batch.columns.get(new Vector2i(x, z)),
                                batch.cubes.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getY(), Map.Entry::getValue))));
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (!errorHandler.test(e)) {
                            throw new UncheckedInterruptedException();
                        }
                    }
                });
            });
        } catch (UncheckedInterruptedException ex) {
            // interrupted, do nothing
        } catch (UncheckedIOException e) {
            throw e.getCause();
        } finally {
            this.loadFuture.complete(null);
        }
    }

    @Override
    public void stop() {
        this.running = false;
        CompletableFuture.allOf(this.countFuture, this.loadFuture).join();
    }
}
