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
package cubicchunks.converter.lib.util;

import cubicchunks.regionlib.api.region.IRegion;
import cubicchunks.regionlib.api.region.IRegionProvider;
import cubicchunks.regionlib.api.region.key.IKey;
import cubicchunks.regionlib.api.region.key.RegionKey;
import cubicchunks.regionlib.util.CheckedBiConsumer;
import cubicchunks.regionlib.util.CheckedConsumer;
import cubicchunks.regionlib.util.CheckedFunction;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A region caching provider that uses a shared underlying cache for all instances
 */
public class ConcurrentCachedRegionProvider<K extends IKey<K>> implements IRegionProvider<K> {

    private final IRegionProvider<K> sourceProvider;

    private final Semaphore cleaning = new Semaphore(1);

    private final int maxCacheSizeHard = 64;
    private final int maxCacheSizeSoft = this.maxCacheSizeHard >> 1;

    private final ConcurrentHashMap<RegionKey, IRegion<K>> regionLocationToRegion = new ConcurrentHashMap<>(this.maxCacheSizeHard << 4);

    private boolean closed;

    /**
     * Creates a RegionProvider using the given {@code regionFactory} and {@code maxCacheSize}
     *
     * @param sourceProvider provider used as source of regions
     */
    public ConcurrentCachedRegionProvider(IRegionProvider<K> sourceProvider) {
        this.sourceProvider = sourceProvider;
    }

    @Override
    public <R> Optional<R> fromExistingRegion(K key, CheckedFunction<? super IRegion<K>, R, IOException> func) throws IOException {
        if (closed) {
            throw new IllegalStateException("Already closed");
        }
        return fromRegion(key, func, false);
    }

    @Override
    public <R> R fromRegion(K key, CheckedFunction<? super IRegion<K>, R, IOException> func) throws IOException {
        if (closed) {
            throw new IllegalStateException("Already closed");
        }
        return fromRegion(key, func, true).get();
    }

    @Override
    public void forRegion(K key, CheckedConsumer<? super IRegion<K>, IOException> cons) throws IOException {
        if (closed) {
            throw new IllegalStateException("Already closed");
        }
        forRegion(key, cons, true);
    }

    @Override
    public void forExistingRegion(K key, CheckedConsumer<? super IRegion<K>, IOException> cons) throws IOException {
        if (closed) {
            throw new IllegalStateException("Already closed");
        }
        forRegion(key, cons, false);
    }

    @SuppressWarnings("unchecked") @Override public IRegion<K> getRegion(K key) throws IOException {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked") @Override public Optional<IRegion<K>> getExistingRegion(K key) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override public void forAllRegions(CheckedBiConsumer<RegionKey, ? super IRegion<K>, IOException> consumer) throws IOException {
        if (closed) {
            throw new IllegalStateException("Already closed");
        }
        sourceProvider.forAllRegions(consumer);
    }

    @Override public void close() throws IOException {
        synchronized (regionLocationToRegion) {
            if (closed) {
                throw new IllegalStateException("Already closed");
            }
            clearRegions(true);
            this.sourceProvider.close();
            this.closed = true;
        }
    }

    @SuppressWarnings("unchecked")
    private void forRegion(K location, CheckedConsumer<? super IRegion<K>, IOException> cons, boolean canCreate) throws IOException {
        try {
            this.regionLocationToRegion.compute(location.getRegionKey(), (regionKey, region) -> {
                try {
                    if (region == null) {
                        //the region isn't cached

                        if (canCreate) {
                            //getRegion() will create a new region if none exists (i.e. it will never return null)
                            region = this.sourceProvider.getRegion(location);
                        } else if ((region = this.sourceProvider.getExistingRegion(location).orElse(null)) == null) {
                            //the region doesn't exist, abort
                            return null;
                        }
                    }

                    cons.accept(region);
                    return region;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });

            if (this.regionLocationToRegion.size() > this.maxCacheSizeHard) {
                this.clearRegions(false);
            }
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    @SuppressWarnings("unchecked")
    public <R> Optional<R> fromRegion(K location, CheckedFunction<? super IRegion<K>, R, IOException> func, boolean canCreate) throws IOException {
        try {
            AtomicReference<Optional<R>> result = new AtomicReference<>(Optional.empty());
            this.regionLocationToRegion.compute(location.getRegionKey(), (regionKey, region) -> {
                try {
                    if (region == null) {
                        //the region isn't cached

                        if (canCreate) {
                            //getRegion() will create a new region if none exists (i.e. it will never return null)
                            region = this.sourceProvider.getRegion(location);
                        } else if ((region = this.sourceProvider.getExistingRegion(location).orElse(null)) == null) {
                            //the region doesn't exist, abort
                            return null;
                        }
                    }

                    result.set(Optional.of(func.apply(region)));
                    return region;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });

            if (this.regionLocationToRegion.size() > this.maxCacheSizeHard) {
                this.clearRegions(false);
            }

            return result.get();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    public void clearRegions(boolean force) throws IOException {
        if (force) {
            this.cleaning.acquireUninterruptibly();
        } else if (!this.cleaning.tryAcquire()) {
            return;
        }

        try {
            this.regionLocationToRegion.forEach((k, _region) -> this.regionLocationToRegion.computeIfPresent(k, (key, region) -> {
                try {
                    region.close();
                    return null; //remove from map
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }));
        } catch (UncheckedIOException e) {
            throw e.getCause();
        } finally {
            this.cleaning.release();
        }
    }
}
