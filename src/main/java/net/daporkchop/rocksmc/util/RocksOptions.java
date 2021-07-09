package net.daporkchop.rocksmc.util;

import org.rocksdb.BlockBasedTableConfig;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.CompressionType;
import org.rocksdb.DBOptions;
import org.rocksdb.Env;
import org.rocksdb.FlushOptions;
import org.rocksdb.LRUCache;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.WriteOptions;

/**
 * @author DaPorkchop_
 */
public class RocksOptions {
    public static final DBOptions DB_OPTIONS;
    public static final ColumnFamilyOptions COLUMN_FAMILY_OPTIONS;

    public static final ReadOptions READ_OPTIONS;
    public static final WriteOptions WRITE_OPTIONS;
    public static final FlushOptions FLUSH_OPTIONS;

    static {
        RocksDB.loadLibrary();

        DB_OPTIONS = new DBOptions()
                .setCreateIfMissing(true)
                .setCreateMissingColumnFamilies(true)
                .setEnv(Env.getDefault().setBackgroundThreads(Runtime.getRuntime().availableProcessors()))
                .setIncreaseParallelism(Runtime.getRuntime().availableProcessors())
                .setParanoidChecks(false)
                .setMaxFileOpeningThreads(Runtime.getRuntime().availableProcessors())
                .setUseFsync(false)
                .setMaxSubcompactions(1)
                .setUseDirectReads(false)
                .setUseDirectIoForFlushAndCompaction(false)
                .setAllowMmapReads(true)
                .setAllowMmapWrites(true)
                .setAdviseRandomOnOpen(true)
                .setDbWriteBufferSize(0L << 10L)
                .setAllowConcurrentMemtableWrite(true)
                .setSkipStatsUpdateOnDbOpen(true)
                .setManualWalFlush(false)
                .setMaxBackgroundJobs(Runtime.getRuntime().availableProcessors())
                .setMaxOpenFiles(256);

        COLUMN_FAMILY_OPTIONS = new ColumnFamilyOptions()
                .setMaxWriteBufferNumber(Runtime.getRuntime().availableProcessors())
                .setMinWriteBufferNumberToMerge(Runtime.getRuntime().availableProcessors())
                .setCompressionType(CompressionType.ZSTD_COMPRESSION)
                .setTargetFileSizeBase(65536L << 10L)
                .setTableFormatConfig(new BlockBasedTableConfig()
                        .setBlockSize(1024L << 10L)
                        .setBlockCache(new LRUCache(
                                (1024L << 11L) * (1L << 6),
                                6)))
                .setTargetFileSizeMultiplier(1);

        READ_OPTIONS = new ReadOptions();
        WRITE_OPTIONS = new WriteOptions();
        FLUSH_OPTIONS = new FlushOptions();
    }
}
