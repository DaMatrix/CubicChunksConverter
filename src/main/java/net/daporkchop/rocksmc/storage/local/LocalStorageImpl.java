package net.daporkchop.rocksmc.storage.local;

import cubicchunks.converter.lib.util.Vector2i;
import cubicchunks.converter.lib.util.Vector3i;
import cubicchunks.regionlib.util.Utils;
import net.daporkchop.rocksmc.storage.IBinaryCubeStorage;
import net.daporkchop.rocksmc.util.UncheckedRocksDBException;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.FlushOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.WriteBatch;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static net.daporkchop.rocksmc.util.PositionSerializerUtils.*;
import static net.daporkchop.rocksmc.util.RocksOptions.*;

/**
 * @author DaPorkchop_
 */
public class LocalStorageImpl implements IBinaryCubeStorage {
    protected static final byte[] COLUMN_NAME_COLUMNS = "columns".getBytes(StandardCharsets.UTF_8);
    protected static final byte[] COLUMN_NAME_CUBES = "cubes".getBytes(StandardCharsets.UTF_8);

    protected static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    protected static byte[] getReadOnlyArray(ByteBuffer data) {
        if (data.hasArray() && data.arrayOffset() == 0 && data.position() == data.limit()) {
            return data.array();
        } else {
            byte[] arr = new byte[data.remaining()];
            int position = data.position();
            data.get(arr, 0, arr.length).position(position);
            return arr;
        }
    }

    protected final Path path;
    protected final RocksDB db;

    protected final List<ColumnFamilyHandle> cfHandles;
    protected final ColumnFamilyHandle cfHandleColumns;
    protected final ColumnFamilyHandle cfHandleCubes;

    public LocalStorageImpl(Path path) throws IOException {
        this.path = path.resolve("rocksmc_local");

        try {
            List<ColumnFamilyDescriptor> cfDescriptors = Arrays.asList(
                    new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, COLUMN_FAMILY_OPTIONS),
                    new ColumnFamilyDescriptor(COLUMN_NAME_COLUMNS, COLUMN_FAMILY_OPTIONS),
                    new ColumnFamilyDescriptor(COLUMN_NAME_CUBES, COLUMN_FAMILY_OPTIONS));
            List<ColumnFamilyHandle> cfHandles = new ArrayList<>(cfDescriptors.size());

            Path currentDir = this.path.resolve("db");
            Utils.createDirectories(currentDir);

            this.db = RocksDB.open(DB_OPTIONS, currentDir.toString(), cfDescriptors, cfHandles);

            this.cfHandles = cfHandles;
            this.cfHandleColumns = cfHandles.get(1);
            this.cfHandleCubes = cfHandles.get(2);
        } catch (RocksDBException e) {
            throw new IOException(e); //rethrow
        }
    }

    @Override
    public boolean columnExists(Vector2i pos) throws IOException {
        try {
            return this.db.get(this.cfHandleColumns, READ_OPTIONS, writeVec2i(pos), EMPTY_BYTE_ARRAY) != RocksDB.NOT_FOUND;
        } catch (RocksDBException e) {
            throw new IOException(e); //rethrow
        }
    }

    @Override
    public boolean cubeExists(Vector3i pos) throws IOException {
        try {
            return this.db.get(this.cfHandleCubes, READ_OPTIONS, writeVec3i(pos), EMPTY_BYTE_ARRAY) != RocksDB.NOT_FOUND;
        } catch (RocksDBException e) {
            throw new IOException(e); //rethrow
        }
    }

    @Override
    public ByteBuffer readColumn(Vector2i pos) throws IOException {
        try {
            return Optional.ofNullable(this.db.get(this.cfHandleColumns, writeVec2i(pos))).map(ByteBuffer::wrap).orElse(null);
        } catch (RocksDBException e) {
            throw new IOException(e); //rethrow
        }
    }

    @Override
    public ByteBuffer readCube(Vector3i pos) throws IOException {
        try {
            return Optional.ofNullable(this.db.get(this.cfHandleCubes, writeVec3i(pos))).map(ByteBuffer::wrap).orElse(null);
        } catch (RocksDBException e) {
            throw new IOException(e); //rethrow
        }
    }

    @Override
    public BinaryBatch readBatch(PosBatch positions) throws IOException {
        try {
            List<Vector2i> columnPositions = new ArrayList<>(positions.columns);
            List<Vector3i> cubePositions = new ArrayList<>(positions.cubes);

            List<byte[]> keys = new ArrayList<>(columnPositions.size() + cubePositions.size());
            List<ColumnFamilyHandle> columnFamilies = new ArrayList<>(columnPositions.size() + cubePositions.size());

            for (Vector2i columnPos : columnPositions) {
                keys.add(writeVec2i(columnPos));
                columnFamilies.add(this.cfHandleColumns);
            }
            for (Vector3i cubePos : cubePositions) {
                keys.add(writeVec3i(cubePos));
                columnFamilies.add(this.cfHandleCubes);
            }

            List<byte[]> values = this.db.multiGetAsList(columnFamilies, keys);
            int i = 0;

            Map<Vector2i, ByteBuffer> columnData = new HashMap<>();
            for (Vector2i columnPos : columnPositions) {
                columnData.put(columnPos, Optional.ofNullable(values.get(i++)).map(ByteBuffer::wrap).orElse(null));
            }

            Map<Vector3i, ByteBuffer> cubeData = new HashMap<>();
            for (Vector3i cubePos : cubePositions) {
                cubeData.put(cubePos, Optional.ofNullable(values.get(i++)).map(ByteBuffer::wrap).orElse(null));
            }

            return new BinaryBatch(columnData, cubeData);
        } catch (RocksDBException e) {
            throw new IOException(e); //rethrow
        }
    }

    @Override
    public void writeColumn(Vector2i pos, ByteBuffer data) throws IOException {
        try {
            byte[] key = writeVec2i(pos);
            this.db.put(this.cfHandleColumns, WRITE_OPTIONS,
                    key, 0, key.length,
                    data.array(), data.arrayOffset() + data.position(), data.remaining());
        } catch (RocksDBException e) {
            throw new IOException(e); //rethrow
        }
    }

    @Override
    public void writeCube(Vector3i pos, ByteBuffer data) throws IOException {
        try {
            byte[] key = writeVec3i(pos);
            this.db.put(this.cfHandleCubes, WRITE_OPTIONS,
                    key, 0, key.length,
                    data.array(), data.arrayOffset() + data.position(), data.remaining());
        } catch (RocksDBException e) {
            throw new IOException(e); //rethrow
        }
    }

    @Override
    public void writeBatch(BinaryBatch batch) throws IOException {
        try (WriteBatch writeBatch = new WriteBatch()) {
            try {
                batch.columns.forEach((pos, data) -> {
                    try {
                        writeBatch.put(this.cfHandleColumns, writeVec2i(pos), getReadOnlyArray(data));
                    } catch (RocksDBException e) {
                        throw new UncheckedRocksDBException(e);
                    }
                });
                batch.cubes.forEach((pos, data) -> {
                    try {
                        writeBatch.put(this.cfHandleCubes, writeVec3i(pos), getReadOnlyArray(data));
                    } catch (RocksDBException e) {
                        throw new UncheckedRocksDBException(e);
                    }
                });
            } catch (UncheckedRocksDBException e) {
                throw e.getCause();
            }

            this.db.write(WRITE_OPTIONS, writeBatch);
        } catch (RocksDBException e) {
            throw new IOException(e); //rethrow
        }
    }

    @Override
    public void forEachColumn(Consumer<Vector2i> callback) throws IOException {
        try (RocksIterator itr = this.db.newIterator(this.cfHandleColumns, READ_OPTIONS)) {
            for (itr.seekToFirst(); itr.isValid(); itr.next()) {
                callback.accept(readVec2i(itr.key()));
            }
        }
    }

    @Override
    public void forEachColumn(BiConsumer<Vector2i, ByteBuffer> callback) throws IOException {
        try (RocksIterator itr = this.db.newIterator(this.cfHandleColumns, READ_OPTIONS)) {
            for (itr.seekToFirst(); itr.isValid(); itr.next()) {
                callback.accept(readVec2i(itr.key()), ByteBuffer.wrap(itr.value()));
            }
        }
    }

    @Override
    public void forEachCube(Consumer<Vector3i> callback) throws IOException {
        try (RocksIterator itr = this.db.newIterator(this.cfHandleCubes, READ_OPTIONS)) {
            for (itr.seekToFirst(); itr.isValid(); itr.next()) {
                callback.accept(readVec3i(itr.key()));
            }
        }
    }

    @Override
    public void forEachCube(BiConsumer<Vector3i, ByteBuffer> callback) throws IOException {
        try (RocksIterator itr = this.db.newIterator(this.cfHandleCubes, READ_OPTIONS)) {
            for (itr.seekToFirst(); itr.isValid(); itr.next()) {
                callback.accept(readVec3i(itr.key()), ByteBuffer.wrap(itr.value()));
            }
        }
    }

    @Override
    public void flush() throws IOException {
        try {
            this.db.flush(new FlushOptions().setWaitForFlush(true).setAllowWriteStall(true), this.cfHandles);
            this.db.flushWal(true);
        } catch (RocksDBException e) {
            throw new IOException(e); //rethrow
        }
    }

    @Override
    public void close() throws IOException {
        this.flush();

        this.cfHandles.forEach(ColumnFamilyHandle::close); //close column families before db
        this.db.close();
    }
}
