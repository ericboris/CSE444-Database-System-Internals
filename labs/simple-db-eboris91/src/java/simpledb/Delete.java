package simpledb;

import java.io.IOException;
import java.io.Serial;
import java.util.NoSuchElementException;

/**
 * The delete operator. Delete reads tuples from its child operator and removes
 * them from the table they belong to.
 */
public class Delete extends Operator {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The transaction this delete runs in
     */
    private final TransactionId tid;

    /**
     * The child operator from which to read tuples for deletion
     */
    private OpIterator child;

    /**
     * Convenience field for a new Int Type[] tuple description.
     */
    private final TupleDesc td;

    /**
     * Tracks whether fetchNext has been called.
     */
    private boolean called;

    /**
     * Constructor specifying the transaction that this delete belongs to as
     * well as the child to read from.
     *
     * @param t
     *            The transaction this delete runs in
     * @param child
     *            The child operator from which to read tuples for deletion
     */
    public Delete(TransactionId t, OpIterator child) {
        this.tid = t;
        this.child = child;
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
        child.close();
        called = false;
    }

    public void rewind() throws DbException, TransactionAbortedException {
        child.rewind();
        called = false;
    }

    /**
     * Deletes tuples as they are read from the child operator. Deletes are
     * processed via the buffer pool (which can be accessed via the
     * Database.getBufferPool() method.
     * v
     * @return A 1-field tuple containing the number of deleted records.
     * @see Database#getBufferPool
     * @see BufferPool#deleteTuple
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        if (called) {
            return null;
        }
        called = true;
        int numDeleted = 0;
        while (child.hasNext()) {
            Tuple tupleToInsert = child.next();
            try {
                Database.getBufferPool().deleteTuple(tid, tupleToInsert);
            } catch (IOException | NoSuchElementException e){
                break;
            }
            numDeleted += 1;
        }
        Tuple countTuple = new Tuple(td);
        countTuple.setField(0, new IntField(numDeleted));
        return countTuple;
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