package simpledb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * BufferPool manages the reading and writing of pages into memory from
 * disk. Access methods call into it to retrieve pages, and it fetches
 * pages from the appropriate location.
 * <p>
 * The BufferPool is also responsible for locking;  when a transaction fetches
 * a page, BufferPool checks that the transaction has the appropriate
 * locks to read/write the page.
 * 
 * @Threadsafe, all fields are final
 */
public class BufferPool {
    /** Bytes per page, including header. */
    private static final int DEFAULT_PAGE_SIZE = 4096;

    private static int pageSize = DEFAULT_PAGE_SIZE;
    
    /** Default number of pages passed to the constructor. This is used by
    other classes. BufferPool should use the numPages argument to the
    constructor instead. */
    public static final int DEFAULT_PAGES = 50;

    /**
     * Maximum number of pages held by this buffer pool.
     */
    private static int maxPages;

    /**
     * Map representing the buffer pool.
     */
    private static HashMap<PageId, Page> pageMap;

    /**
     * TODO
     */
    private LockManager lockManager;

    /**
     * Creates a BufferPool that caches up to numPages pages.
     *
     * @param numPages maximum number of pages in this buffer pool.
     */
    public BufferPool(int numPages) {
        this.maxPages = numPages;
        this.pageMap = new HashMap<>();
        this.lockManager = new LockManager();
    }
    
    public static int getPageSize() {
      return pageSize;
    }
    
    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void setPageSize(int pageSize) {
    	BufferPool.pageSize = pageSize;
    }
    
    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void resetPageSize() {
    	BufferPool.pageSize = DEFAULT_PAGE_SIZE;
    }

    /**
     * Retrieve the specified page with the associated permissions.
     * Will acquire a lock and may block if that lock is held by another
     * transaction.
     * <p>
     * The retrieved page should be looked up in the buffer pool.  If it
     * is present, it should be returned.  If it is not present, it should
     * be added to the buffer pool and returned.  If there is insufficient
     * space in the buffer pool, a page should be evicted and the new page
     * should be added in its place.
     *
     * @param tid the ID of the transaction requesting the page
     * @param pid the ID of the requested page
     * @param perm the requested permissions on the page
     */
    public  Page getPage(TransactionId tid, PageId pid, Permissions perm)
        throws TransactionAbortedException, DbException {
        // Return the page if it's present.
        if (pageMap.size() >= maxPages) {
            evictPage();
        }

        this.lockManager.acquire(tid, pid, perm);

        if (pageMap.containsKey(pid)) {
            return pageMap.get(pid);
        } else {
            // Add the new page to the buffer pool and return it.
            if (pageMap.size() < maxPages) {
                Page newPage = ((HeapFile) Database.getCatalog().getDatabaseFile(pid.getTableId())).readPage(pid);
                pageMap.put(pid, newPage);
                return newPage;
            // If there's insufficient space, throw an exception.
            } else {
                throw new DbException("More than " + maxPages + " requests for different pages");
            }
        }
    }

    /**
     * Releases the lock on a page.
     * Calling this is very risky, and may result in wrong behavior. Think hard
     * about who needs to call this and why, and why they can run the risk of
     * calling it.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param pid the ID of the page to unlock
     */
    public  void releasePage(TransactionId tid, PageId pid) {
        lockManager.release(tid, pid);
    }

    /**
     * Release all locks associated with a given transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     */
    public void transactionComplete(TransactionId tid) throws IOException {
        transactionComplete(tid, true);
    }

    /** Return true if the specified transaction has a lock on the specified page */
    public boolean holdsLock(TransactionId tid, PageId pid) {
        return lockManager.isLocked(tid, pid);
    }

    /**
     * Commit or abort a given transaction; release all locks associated to
     * the transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param commit a flag indicating whether we should commit or abort
     */
    public void transactionComplete(TransactionId tid, boolean commit)
        throws IOException {
        for (PageId pid: lockManager.getTransactionMap().get(tid)) {
            if (pageMap.containsKey(pid)){
                Page p = pageMap.get(pid);
                Database.getLogFile().logWrite(tid, p.getBeforeImage(), p);
                Database.getLogFile().force();
                p.setBeforeImage();
                if(commit == false){
                    pageMap.replace(pid, p.getBeforeImage());
                }
            }
        }
        lockManager.releaseLocks(tid);
    }

    /**
     * Add a tuple to the specified table on behalf of transaction tid.  Will
     * acquire a write lock on the page the tuple is added to and any other 
     * pages that are updated (Lock acquisition is not needed for lab2). 
     * May block if the lock(s) cannot be acquired.
     * 
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have 
     * been dirtied to the cache (replacing any existing versions of those pages) so 
     * that future requests see up-to-date pages. 
     *
     * @param tid the transaction adding the tuple
     * @param tableId the table to add the tuple to
     * @param t the tuple to add
     */
    public void insertTuple(TransactionId tid, int tableId, Tuple t)
        throws DbException, IOException, TransactionAbortedException {
        HeapFile hf = (HeapFile) Database.getCatalog().getDatabaseFile(tableId);
        ArrayList<Page> modifiedPages = hf.insertTuple(tid, t);
        for (Page pg: modifiedPages){
            pg.markDirty(true, tid);
            if (pageMap.size() > maxPages){
                evictPage();
            }
            pageMap.put(pg.getId(), pg);
        }
    }

    /**
     * Remove the specified tuple from the buffer pool.
     * Will acquire a write lock on the page the tuple is removed from and any
     * other pages that are updated. May block if the lock(s) cannot be acquired.
     *
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have 
     * been dirtied to the cache (replacing any existing versions of those pages) so 
     * that future requests see up-to-date pages. 
     *
     * @param tid the transaction deleting the tuple.
     * @param t the tuple to delete
     */
    public  void deleteTuple(TransactionId tid, Tuple t)
        throws DbException, IOException, TransactionAbortedException {
        int tableId = t.getRecordId().getPageId().getTableId();
        HeapFile hf = (HeapFile) Database.getCatalog().getDatabaseFile(tableId);
        ArrayList<Page> modifiedPages = hf.deleteTuple(tid, t);
        for(Page pg: modifiedPages){
            pg.markDirty(true, tid);
            pageMap.put(pg.getId(), pg);
        }
    }

    /**
     * Flush all dirty pages to disk.
     * NB: Be careful using this routine -- it writes dirty data to disk so will
     *     break simpledb if running in NO STEAL mode.
     */
    public synchronized void flushAllPages() throws IOException {
        for (PageId pid: pageMap.keySet()){
            flushPage(pid);
        }
    }

    /** Remove the specific page id from the buffer pool.
        Needed by the recovery manager to ensure that the
        buffer pool doesn't keep a rolled back page in its
        cache.
        
        Also used by B+ tree files to ensure that deleted pages
        are removed from the cache so they can be reused safely
    */
    public synchronized void discardPage(PageId pid) {
        pageMap.remove(pid);
    }

    /**
     * Flushes a certain page to disk
     * @param pid an ID indicating the page to flush
     */
    private synchronized void flushPage(PageId pid) throws IOException {
        Page p = pageMap.get(pid);
        if (pageMap.containsKey(pid)){
            TransactionId dirtier = p.isDirty();
            if (dirtier != null) {
                Database.getLogFile().logWrite(dirtier, p.getBeforeImage(), p);
                Database.getLogFile().force();
                HeapFile hf = (HeapFile) Database.getCatalog().getDatabaseFile(pid.getTableId());
                hf.writePage(p);
                p.markDirty(false, null);
            }
        }
    }

    /** Write all pages of the specified transaction to disk.
     */
    public synchronized void flushPages(TransactionId tid) throws IOException {
        for (Page p : pageMap.values()) {
            if (tid.equals(p.isDirty())) {
                flushPage(p.getId());
            }
        }
    }

    /**
     * Discards a page from the buffer pool.
     * Flushes the page to disk to ensure dirty pages are updated on disk.
     */
    private synchronized void evictPage() throws DbException {
        ArrayList<PageId> pageIds = new ArrayList<>(pageMap.keySet());
        // Reverse for LRU eviction policy.
        Collections.reverse(pageIds);
        for (PageId pid: pageIds) {
            if (pageMap.get(pid).isDirty() == null) {
                try {
                    flushPage(pid);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                pageMap.remove(pid);
                return;
            }
        }
    }
}
