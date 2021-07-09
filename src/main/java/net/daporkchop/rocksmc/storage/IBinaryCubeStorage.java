package net.daporkchop.rocksmc.storage;

import cubicchunks.converter.lib.util.Vector2i;
import cubicchunks.converter.lib.util.Vector3i;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author DaPorkchop_
 */
public interface IBinaryCubeStorage extends Flushable, Closeable {
    /**
     * Checks whether or not the column at the given position exists.
     *
     * @param pos the column's position
     * @return whether or not the column at the given position exists
     */
    boolean columnExists(Vector2i pos) throws IOException;

    /**
     * Checks whether or not the cube at the given position exists.
     *
     * @param pos the cube's position
     * @return whether or not the cube at the given position exists
     */
    boolean cubeExists(Vector3i pos) throws IOException;

    /**
     * Checks for the existence of multiple cubes+columns at once.
     *
     * @param positions a {@link PosBatch} containing the positions of all the cubes+columns to check for
     * @return a {@link PosBatch} containing the positions of all the cubes+columns that exist
     */
    @Nonnull
    default PosBatch existsBatch(PosBatch positions) throws IOException {
        //default implementation: check positions individually, but in parallel
        try {
            return new PosBatch(
                    positions.columns.parallelStream().filter(pos -> {
                        try {
                            return this.columnExists(pos);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }).collect(Collectors.toSet()),
                    positions.cubes.parallelStream().filter(pos -> {
                        try {
                            return this.cubeExists(pos);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }).collect(Collectors.toSet()));
        } catch (UncheckedIOException e) {
            throw e.getCause(); //rethrow original exception
        }
    }

    /**
     * Reads the raw binary data for the column at the given position.
     *
     * @param pos the column's position
     * @return the column's raw binary data, or {@code null} if the column couldn't be found
     */
    ByteBuffer readColumn(Vector2i pos) throws IOException;

    /**
     * Reads the raw binary data for the cube at the given position.
     *
     * @param pos the cube's position
     * @return the cube's raw binary data, or {@code null} if the cube couldn't be found
     */
    ByteBuffer readCube(Vector3i pos) throws IOException;

    /**
     * Reads the raw binary data for multiple cubes+columns at once.
     *
     * @param positions a {@link PosBatch} containing the positions of all the cubes+columns to read
     * @return a {@link BinaryBatch} containing all the given cube+column positions mapped to their corresponding raw binary data, or {@code null} for cubes/columns that can't be found
     */
    BinaryBatch readBatch(PosBatch positions) throws IOException;

    /**
     * Writes the raw binary data to the column at the given position.
     *
     * @param pos  the column's position
     * @param data the column's raw binary data
     */
    void writeColumn(Vector2i pos, ByteBuffer data) throws IOException;

    /**
     * Writes the raw binary data to the cube at the given position.
     *
     * @param pos  the cube's position
     * @param data the cube's raw binary data
     */
    void writeCube(Vector3i pos, ByteBuffer data) throws IOException;

    /**
     * Writes the raw binary data for multiple cubes+columns at once.
     *
     * @param batch a {@link BinaryBatch} containing the cube+column positions and the NBT data to write to each
     */
    void writeBatch(BinaryBatch batch) throws IOException;

    /**
     * Iterates over all the columns that exist in the world.
     *
     * @param callback the callback function to run
     */
    void forEachColumn(Consumer<Vector2i> callback) throws IOException;

    /**
     * Iterates over all the columns that exist in the world.
     *
     * @param callback the callback function to run
     */
    void forEachColumn(BiConsumer<Vector2i, ByteBuffer> callback) throws IOException;

    /**
     * Iterates over all the cubes that exist in the world.
     *
     * @param callback the callback function to run
     */
    void forEachCube(Consumer<Vector3i> callback) throws IOException;

    /**
     * Iterates over all the cubes that exist in the world.
     *
     * @param callback the callback function to run
     */
    void forEachCube(BiConsumer<Vector3i, ByteBuffer> callback) throws IOException;

    /**
     * Forces any internally buffered data to be written to disk immediately, blocking until the action is completed.
     * <p>
     * Once this method returns, all writes issued at the time of this method's invocation are guaranteed to be present on disk.
     */
    @Override
    void flush() throws IOException;

    /**
     * Closes this storage.
     * <p>
     * This method may only be called once for a given of {@link IBinaryCubeStorage}. Once called, the instance shall be considered to have been disposed, and the behavior of
     * all other methods is undefined.
     */
    @Override
    void close() throws IOException;

    /**
     * A group of positions for both columns and cubes.
     * <p>
     * Used for bulk I/O operations.
     */
    class PosBatch {
        public final Set<Vector2i> columns;
        public final Set<Vector3i> cubes;

        public PosBatch(Set<Vector2i> columns, Set<Vector3i> cubes) {
            this.columns = Objects.requireNonNull(columns, "columns");
            this.cubes = Objects.requireNonNull(cubes, "cubes");
        }
    }

    /**
     * A group of position+binary data pairs for both column and cube data.
     * <p>
     * Used for bulk I/O operations.
     *
     * @author DaPorkchop_
     */
    class BinaryBatch {
        public final Map<Vector2i, ByteBuffer> columns;
        public final Map<Vector3i, ByteBuffer> cubes;

        public BinaryBatch(Map<Vector2i, ByteBuffer> columns, Map<Vector3i, ByteBuffer> cubes) {
            this.columns = Objects.requireNonNull(columns, "columns");
            this.cubes = Objects.requireNonNull(cubes, "cubes");
        }
    }
}
