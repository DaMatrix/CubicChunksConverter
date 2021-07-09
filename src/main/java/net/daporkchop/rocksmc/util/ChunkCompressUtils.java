package net.daporkchop.rocksmc.util;

import cubicchunks.converter.lib.util.Utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.Function;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * @author DaPorkchop_
 */
public class ChunkCompressUtils {
    protected static final int MAX_COMPRESSED_DATA_SIZE = 1044475;

    protected static final ThreadLocal<Deflater> DEFLATER_CACHE = ThreadLocal.withInitial(Deflater::new);
    protected static final ThreadLocal<Inflater> INFLATER_CACHE = ThreadLocal.withInitial(Inflater::new);
    protected static final ThreadLocal<ByteBuffer> BUFFER_CACHE = ThreadLocal.withInitial(() -> ByteBuffer.allocate(MAX_COMPRESSED_DATA_SIZE));

    public static ByteBuffer compressForAnvil(ByteBuffer src) {
        Deflater deflater = DEFLATER_CACHE.get();
        ByteBuffer dst = BUFFER_CACHE.get();
        dst.clear().position(1);
        dst.put(0, (byte) 2); //zlib marker

        //attempt normal compression
        deflater.reset();
        deflater.setInput(src.array(), src.arrayOffset() + src.position(), src.remaining());
        deflater.finish();
        int cnt = deflater.deflate(dst.array(), dst.arrayOffset() + dst.position(), dst.remaining());
        if (!deflater.finished()) { //compression couldn't be completed - the resulting data was too large!
            //let's try again, but this time using the maximum compression level.
            deflater.setLevel(Deflater.BEST_COMPRESSION);
            deflater.deflate(new byte[0]); //setLevel() demands we call deflate() afterwards

            deflater.reset();
            try {
                deflater.setInput(src.array(), src.arrayOffset() + src.position(), src.remaining());
                deflater.finish();
                cnt = deflater.deflate(dst.array(), dst.arrayOffset() + dst.position(), dst.remaining());
                if (!deflater.finished()) { //if it was still too big, even with max compression, it'll never fit into a region, so we should abort
                    throw new IllegalArgumentException("chunk data is too large!");
                }
            } finally {
                deflater.setLevel(Deflater.DEFAULT_COMPRESSION);
                deflater.deflate(new byte[0]); //setLevel() demands we call deflate() afterwards
            }
        }
        dst.position(dst.position() + cnt).flip();
        if (dst.remaining() >= MAX_COMPRESSED_DATA_SIZE) {
            throw new IllegalStateException();
        }

        return ByteBuffer.wrap(Arrays.copyOfRange(dst.array(), dst.arrayOffset() + dst.position(), dst.remaining()));
    }

    public static ByteBuffer decompressFromAnvil(ByteBuffer src) {
        try {
            switch (src.get() & 0xFF) {
                case 2: //zlib
                    return decompressZlib(src);
                case 1:
                    return decompressGzip(src);
                default:
                    throw new IllegalArgumentException("unsupported compression version: " + (src.get(src.position() - 1) & 0xFF));
            }
        } catch (DataFormatException e) {
            throw new UncheckedIOException(new IOException(e));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected static ByteBuffer decompressZlib(ByteBuffer src) throws IOException, DataFormatException {
        Inflater inflater = INFLATER_CACHE.get();
        ByteBuffer buf = BUFFER_CACHE.get();
        buf.clear();

        inflater.reset();
        inflater.setInput(src.array(), src.arrayOffset() + src.position(), src.remaining());
        int cnt = inflater.inflate(buf.array(), buf.arrayOffset() + buf.position(), buf.remaining());
        if (!inflater.finished()) { //temp buffer doesn't have enough space, use streaming decompression
            //yes, i'm aware that this is much slower, but it'll hardly ever occur so it really shouldn't be much of an issue
            inflater.reset();
            return decompressStream(src, Utils.propagateExceptions(stream -> new InflaterInputStream(stream, inflater)));
        }

        return ByteBuffer.wrap(Arrays.copyOfRange(buf.array(), buf.arrayOffset() + buf.position(), cnt));
    }

    protected static ByteBuffer decompressGzip(ByteBuffer src) throws IOException {
        return decompressStream(src, Utils.propagateExceptions(GZIPInputStream::new));
    }

    protected static ByteBuffer decompressStream(ByteBuffer src, Function<InputStream, InputStream> inflaterFactory) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteBuffer buf = BUFFER_CACHE.get();
        buf.clear();

        try (InputStream in = inflaterFactory.apply(new ByteArrayInputStream(src.array(), src.arrayOffset() + src.position(), src.remaining()))) {
            for (int cnt; (cnt = in.read(buf.array(), buf.arrayOffset() + buf.position(), buf.remaining())) >= 0; ) {
                baos.write(buf.array(), buf.arrayOffset() + buf.position(), cnt);
            }
        }

        return ByteBuffer.wrap(baos.toByteArray());
    }
}
