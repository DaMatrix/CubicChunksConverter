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
import cubicchunks.converter.lib.convert.ChunkDataWriter;
import cubicchunks.converter.lib.util.Utils;
import cubicchunks.converter.lib.util.Vector2i;
import net.daporkchop.rocksmc.converter.data.RocksLocalVanillaColumnData;
import net.daporkchop.rocksmc.storage.IBinaryCubeStorage;
import net.daporkchop.rocksmc.storage.local.LocalStorageImpl;

import java.io.Flushable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author DaPorkchop_
 */
public class RocksLocalVanillaWriter implements ChunkDataWriter<RocksLocalVanillaColumnData> {
    private final Path dstPath;
    private final Map<Dimension, Save> saves = new ConcurrentHashMap<>();

    public RocksLocalVanillaWriter(Path dstPath) {
        this.dstPath = dstPath;
    }

    @Override
    public void accept(RocksLocalVanillaColumnData data) throws IOException {
        this.saves.computeIfAbsent(data.getDimension(), dim -> {
            try {
                return new Save(new LocalStorageImpl(this.dstPath.resolve(dim.getDirectory())));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).append(data);
    }

    @Override
    public void discardData() throws IOException {
        Utils.rm(this.dstPath);
    }

    @Override
    public void close() throws Exception {
        boolean exception = false;
        for (Save save : this.saves.values()) {
            try {
                save.close();
            } catch (IOException e) {
                e.printStackTrace();
                exception = true;
            }
        }
        if (exception) {
            throw new IOException();
        }
    }

    private static class Save implements Flushable, AutoCloseable {
        protected final IBinaryCubeStorage storage;
        protected final Map<Vector2i, ByteBuffer> queue = new HashMap<>();
        protected int queueSize = 0;

        public Save(LocalStorageImpl storage) {
            this.storage = storage;
        }

        public synchronized Save append(RocksLocalVanillaColumnData data) throws IOException {
            this.queue.put(data.getPosition(), data.getData());
            this.queueSize += data.getData().remaining();

            if (this.queueSize >= (64 << 20)) { //more than 64MiB of data queued, let's flush it
                this.flush();
            }

            return this;
        }

        @Override
        public synchronized void flush() throws IOException {
            if (!this.queue.isEmpty()) {
                this.storage.writeBatch(new IBinaryCubeStorage.BinaryBatch(this.queue, Collections.emptyMap()));

                //reset write queue
                this.queue.clear();
                this.queueSize = 0;
            }
        }

        @Override
        public synchronized void close() throws IOException {
            this.flush();
            this.storage.close();
        }
    }
}
