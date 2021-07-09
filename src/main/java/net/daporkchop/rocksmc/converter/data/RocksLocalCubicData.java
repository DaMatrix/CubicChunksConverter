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

package net.daporkchop.rocksmc.converter.data;

import cubicchunks.converter.lib.Dimension;
import cubicchunks.converter.lib.util.Vector2i;
import cubicchunks.regionlib.impl.EntryLocation2D;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;

/**
 * @author DaPorkchop_
 */
public class RocksLocalCubicData implements IRocksLocalData {
    private final Dimension dimension;
    private final EntryLocation2D position;
    private final ByteBuffer columnData;
    private final Map<Integer, ByteBuffer> cubeData;

    public RocksLocalCubicData(Dimension dimension, EntryLocation2D position, ByteBuffer columnData, Map<Integer, ByteBuffer> cubeData) {
        this.dimension = dimension;
        this.position = position;
        this.columnData = columnData;
        this.cubeData = cubeData;
    }

    @Override
    public Dimension getDimension() {
        return this.dimension;
    }

    public EntryLocation2D getPosition() {
        return this.position;
    }

    @Override
    public Vector2i getPositionAsVector() {
        return new Vector2i(this.position.getEntryX(), this.position.getEntryZ());
    }

    @Override
    public ByteBuffer getColumnData() {
        return this.columnData;
    }

    @Override
    public Map<Integer, ByteBuffer> getCubeData() {
        return this.cubeData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RocksLocalCubicData that = (RocksLocalCubicData) o;
        return dimension.equals(that.dimension) &&
               Objects.equals(columnData, that.columnData) &&
               cubeData.equals(that.cubeData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimension, position, columnData, cubeData);
    }

    @Override
    public String toString() {
        return "RocksLocalCubicData{" +
               "dimension='" + dimension + '\'' +
               ", position=" + position +
               ", columnData=" + columnData +
               ", cubeData=" + cubeData +
               '}';
    }
}
