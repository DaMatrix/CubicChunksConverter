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

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * @author DaPorkchop_
 */
public class RocksLocalVanillaColumnData {
    protected final Dimension dimension;
    protected final Vector2i position;
    protected final ByteBuffer data;
    public final int offsetSections;

    public RocksLocalVanillaColumnData(Dimension dimension, Vector2i position, ByteBuffer data, int offsetSections) {
        this.dimension = dimension;
        this.position = position;
        this.data = data;
        this.offsetSections = offsetSections;
    }

    public Dimension getDimension() {
        return dimension;
    }

    public Vector2i getPosition() {
        return position;
    }

    public ByteBuffer getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RocksLocalVanillaColumnData that = (RocksLocalVanillaColumnData) o;
        return dimension.equals(that.dimension) &&
               position.equals(that.position) &&
               data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimension, position, data);
    }

    @Override
    public String toString() {
        return "RocksLocalVanillaColumnData{" +
               "dimension='" + dimension + '\'' +
               ", position=" + position +
               ", data=" + data +
               '}';
    }
}
