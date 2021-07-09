package net.daporkchop.rocksmc.util;

import org.rocksdb.RocksDBException;

/**
 * @author DaPorkchop_
 */
public class UncheckedRocksDBException extends RuntimeException {
    public UncheckedRocksDBException(RocksDBException cause) {
        super(cause);
    }

    @Override
    public RocksDBException getCause() {
        return (RocksDBException) super.getCause();
    }
}
