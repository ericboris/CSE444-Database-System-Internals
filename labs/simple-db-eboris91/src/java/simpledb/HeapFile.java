package simpledb;

import java.io.*;
import java.util.*;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 *
 * @see simpledb.HeapPage#HeapPage
 * @author Sam Madden
 */
public class HeapFile implements DbFile {

    /**
     * The file that stores the on-disk backing store for this heap file.
     */
    private File f;

    /**
     * The TupleDesc of the table stored in the DbFile.
     */
    private TupleDesc td;

    /**
     * Constructs a heap file backed by the specified file.
     *
     * @param f
     *            the file that stores the on-disk backing store for this heap
     *            file.
     * @param td
     *            the TupleDesc of the table stored in the DbFile.
     */
    public HeapFile(File f, TupleDesc td) {
        this.f = f;
        this.td = td;
    }

    /**
     * Returns the File backing this HeapFile on disk.
     *
     * @return the File backing this HeapFile on disk.
     */

    public File getFile() {
        return f;
    }

    /**
     * Returns an ID uniquely identifying this HeapFile. Implementation note:
     * you will need to generate this tableid somewhere to ensure that each
     * HeapFile has a "unique id," and that you always return the same value for
     * a particular HeapFile. We suggest hashing the absolute file name of the
     * file underlying the heapfile, i.e. f.getAbsoluteFile().hashCode().
     *
     * @return an ID uniquely identifying this HeapFile.
     */
    public int getId() {
        return f.getAbsoluteFile().hashCode();
    }

    /**
     * Returns the TupleDesc of the table stored in this DbFile.
     *
     * @return TupleDesc of this DbFile.
     */
    public TupleDesc getTupleDesc() {
        return td;
    }

    // see DbFile.java for javadocs
    public Page readPage(PageId pid) {
        Page page = null;
        byte[] data = new byte[BufferPool.getPageSize()];
        try (RandomAccessFile raf = new RandomAccessFile(getFile(), "r")) {
            raf.seek(pid.getPageNumber() * BufferPool.getPageSize());
            raf.read(data, 0, data.length);
            page = new HeapPage((HeapPageId) pid, data);
        } catch (IOException e) {
            throw new IllegalArgumentException("PageId " + pid.toString() + " does not exist in this file");
        }
        return page;
    }

    // see DbFile.java for javadocs
    public void writePage(Page page) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(f, "rws");
        raf.seek(page.getId().getPageNumber() * BufferPool.getPageSize());
        raf.write(page.getPageData());
        raf.close();
    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
        return (int) Math.ceil(f.length() / BufferPool.getPageSize());
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        int i = 0;
        HeapPage hp;

        // Find the first HeapPage with empty slots.
        do {
            hp = (HeapPage) Database.getBufferPool().getPage(tid, new HeapPageId(getId(),i), Permissions.READ_WRITE);
            i++;
        } while (i < numPages() && hp.getNumEmptySlots() <= 0);

        // Insert tuple t if an empty slot exists.
        if (hp.getNumEmptySlots() > 0) {
            hp.insertTuple(t);
        // Otherwise create a new HeapPage and insert t.
        } else {
            hp = new HeapPage(new HeapPageId(getId(), numPages()), new byte[BufferPool.getPageSize()]);
            hp.insertTuple(t);
            writePage(hp);
        }

        // Return the result.
        ArrayList<Page> modifiedPages = new ArrayList<>();
        modifiedPages.add(hp);
        return modifiedPages;
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException,
            TransactionAbortedException {
        HeapPage hp = (HeapPage) Database.getBufferPool().getPage(tid, t.getRecordId().getPageId(), Permissions.READ_WRITE);
        hp.deleteTuple(t);

        ArrayList<Page> modifiedPages = new ArrayList<>();
        modifiedPages.add(hp);
        return modifiedPages;
    }

    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
        class HeapFileIterator implements DbFileIterator {
            /**
             * The transaction id.
             */
            private TransactionId tid;

            /**
             * An iterator of tuples.
             */
            private Iterator<Tuple> tupleIterator;

            /**
             * The index of the current page in this Iterator.
             */
            private int pageIndex;

            public HeapFileIterator(TransactionId tid) {
                this.tid = tid;
            }

            private Iterator<Tuple> getTupleIterator(HeapPageId pid) throws TransactionAbortedException, DbException {
                return ((HeapPage) Database.getBufferPool().getPage(tid, pid, Permissions.READ_ONLY)).iterator();
            }

            @Override
            public void open() throws TransactionAbortedException, DbException {
                pageIndex = 0;
                HeapPageId pid = new HeapPageId(getId(), pageIndex);
                tupleIterator = getTupleIterator(pid);
            }

            @Override
            public Tuple next() throws TransactionAbortedException, DbException {
                if (!hasNext()) {
                    throw new NoSuchElementException("must call open() first and tuples must remain");
                }
                return tupleIterator.next();
            }

            @Override
            public boolean hasNext() throws TransactionAbortedException, DbException {
                // open() not called.
                if (tupleIterator == null) {
                    return false;
                }
                // Page has next tuple.
                if (tupleIterator.hasNext()) {
                    return true;
                }
                // Get next page's tuples.
                if (pageIndex < numPages() - 1) {
                    pageIndex++;
                    HeapPageId pid = new HeapPageId(getId(), pageIndex);
                    tupleIterator = getTupleIterator(pid);
                    return hasNext();
                }
                // No more next pages.
                // No more next pages.
                return false;
            }

            @Override
            public void rewind() throws TransactionAbortedException, DbException {
                open();
            }

            @Override
            public void close() {
                pageIndex = 0;
                tupleIterator = null;
            }
        }
        return new HeapFileIterator(tid);
    }
}