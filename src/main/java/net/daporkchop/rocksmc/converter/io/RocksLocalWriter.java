package net.daporkchop.rocksmc.converter.io;

import cubicchunks.converter.lib.Dimension;
import cubicchunks.converter.lib.convert.ChunkDataWriter;
import cubicchunks.converter.lib.util.Utils;
import cubicchunks.converter.lib.util.Vector2i;
import cubicchunks.converter.lib.util.Vector3i;
import net.daporkchop.rocksmc.converter.data.IRocksLocalData;
import net.daporkchop.rocksmc.storage.IBinaryCubeStorage;
import net.daporkchop.rocksmc.storage.local.LocalStorageImpl;

import java.io.Flushable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author DaPorkchop_
 */
public class RocksLocalWriter<OUT extends IRocksLocalData> implements ChunkDataWriter<OUT> {
    private final Path dstPath;
    private final Map<Dimension, Save> saves = new ConcurrentHashMap<>();

    public RocksLocalWriter(Path dstPath) {
        this.dstPath = dstPath;
    }

    @Override
    public void accept(OUT data) throws IOException {
        this.saves.computeIfAbsent(data.getDimension(), dim -> {
            try {
                return new Save(new LocalStorageImpl(this.dstPath.resolve(dim.getDirectory())));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).queue().append(data);
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

    private class Save implements Flushable, AutoCloseable {
        protected final IBinaryCubeStorage storage;
        protected final Map<Thread, WriteQueue> queues = new ConcurrentHashMap<>();

        public Save(LocalStorageImpl storage) {
            this.storage = storage;
        }

        public WriteQueue queue() {
            return this.queues.computeIfAbsent(Thread.currentThread(), t -> new WriteQueue());
        }

        @Override
        public void flush() throws IOException {
            try {
                this.queues.keySet().parallelStream().forEach(t -> this.queues.computeIfPresent(t, (tt, q) -> {
                    try {
                        q.flush();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    return null;
                }));
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }
        }

        @Override
        public void close() throws IOException {
            this.flush();
            this.storage.close();
        }

        protected class WriteQueue implements Flushable, AutoCloseable {
            protected final Map<Vector2i, ByteBuffer> columnQueue = new HashMap<>();
            protected final Map<Vector3i, ByteBuffer> cubeQueue = new HashMap<>();
            protected int queueSize = 0;

            public synchronized void append(OUT data) throws IOException {
                Vector2i pos = data.getPositionAsVector();
                int x = pos.getX();
                int z = pos.getY();

                if (data.getColumnData() != null) {
                    this.columnQueue.put(pos, data.getColumnData());
                    this.queueSize += data.getColumnData().remaining();
                }

                data.getCubeData().forEach((y, cubeData) -> {
                    this.cubeQueue.put(new Vector3i(x, y, z), cubeData);
                    this.queueSize += cubeData.remaining();
                });

                if (this.queueSize >= (64 << 20)) { //more than 64MiB of data queued, let's flush it
                    this.flush();
                }
            }

            @Override
            public synchronized void flush() throws IOException {
                if (!this.columnQueue.isEmpty() || !this.cubeQueue.isEmpty()) {
                    Save.this.storage.writeBatch(new IBinaryCubeStorage.BinaryBatch(this.columnQueue, this.cubeQueue));

                    //reset write queue
                    this.columnQueue.clear();
                    this.cubeQueue.clear();
                    this.queueSize = 0;
                }
            }

            @Override
            public void close() throws IOException {
                this.flush();
            }
        }
    }
}
