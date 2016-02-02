package au.org.massive.oauth2_hpc;

import org.apache.log4j.Logger;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Abstracts a file-based cache of key/values
 */
public abstract class DiskCache {
    private static final Logger log = Logger.getLogger(DiskCache.class.getName());
    private static final Settings settings = Settings.getInstance();
    private static DB db;

    public DiskCache() {
        if (db == null) {
            File f = null;
            try {
                f = new File(settings.getCacheFileLocation());
                if (!f.exists()) {
                    f.createNewFile();
                }
                log.info("File-based cache file: "+f.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
                log.warn("Could not create cache file: " + f.getAbsolutePath());
                f = null;
            }

            if (f == null) {
                try {
                    f = File.createTempFile("ssh-authz-cache", ".tmp");
                    log.info("File-based cache will use a temporary file that is deleted on exit");
                } catch (IOException e) {
                    e.printStackTrace();
                    log.warn("Could not create temporary cache file.");
                }
            }

            if (f == null) {
                db = DBMaker.memoryDB()
                        .closeOnJvmShutdown()
                        .make();
                log.info("Using in-memory cache.");
            } else {
                db = DBMaker.fileDB(f)
                        .closeOnJvmShutdown()
                        .make();
                log.info("Using file-based cache: " + f.getAbsolutePath());
            }
        }
    }

    protected <K,V> Map<K,V> getCache(String name) {
        return db.treeMap(name);
    }

    public void commit() {
        db.commit();
    }

    public void rollback() {
        db.rollback();
    }
}
