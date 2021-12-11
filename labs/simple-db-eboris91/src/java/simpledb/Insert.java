package simpledb;

import java.io.IOException;
import java.io.Serial;
import java.util.NoSuchElementException;

/**
 * Inserts tuples read from the child operator into the tableId specified in the
 * constructor
 */
public class Insert extends Operator {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The transaction running the insert.
     */
    private final TransactionId tid;

    /**
     * The child operator from which to read tuples to be inserted.
     */
    private OpIterator child;

    /**
     * The table in which to insert tuples.
     */
    private final int tableId;

    /**
     * Convenience field for a new Int Type[] tuple description
     */
    private final TupleDesc td;

    /**
     * Tracks whether fetchNext has been called.
     */
    private boolean called;

    /**
     * Constructor.
     *
     * @param t
     *            The transaction running the insert.
     * @param child
     *            The child operator from which to read tuples to be inserted.
     * @param tableId
     *            The table in which to insert tuples.
     * @throws DbException
     *             if TupleDesc of child differs from table into which we are to
     *             insert.
     */
    public Insert(TransactionId t, OpIterator child, int tableId)
            throws DbException {
        this.tid = t;
        this.child = child;
        this.tableId = tableId;
        this.td = new TupleDesc(new Type[]{Type.INT_TYPE});
    }

    public TupleDesc getTupleDesc() {
        return this.td;
    }

    public void open() throws DbException, TransactionAbortedException {
        child.open();
        super.open();
        called = false;
    }

    public void close() {
        super.close();
        called = false;
        child.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        child.rewind();
        called = false;
    }

    /**
     * Inserts tuples read from child into the tableId specified by the
     * constructor. It returns a one field tuple containing the number of
     * inserted records. Inserts should be passed through BufferPool. An
     * instances of BufferPool is available via Database.getBufferPool(). Note
     * that insert DOES NOT need check to see if a particular tuple is a
     * duplicate before inserting it.
     *
     * @return A 1-field tuple containing the number of inserted records, or
     *         null if called more than once.
     * @see Database#getBufferPool
     * @see BufferPool#insertTuple
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        if (called) {
            return null;
        }
        called = true;
        int numInserted = 0;
        while (child.hasNext()) {
            try{
                Database.getBufferPool().insertTuple(tid, tableId, child.next());
            } catch (IOException | NoSuchElementException e) {
                return null;
            }
            numInserted++;
        }
        Tuple t = new Tuple(td);
        t.setField(0, new IntField(numInserted));
        return t;
    }

    @Override
    public OpIterator[] getChildren() {
        return new OpIterator[]{child};
    }

    @Override
    public void setChildren(OpIterator[] children) {
        this.child = children[0];
    }
}