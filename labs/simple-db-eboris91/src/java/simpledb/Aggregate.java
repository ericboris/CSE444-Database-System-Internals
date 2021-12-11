package simpledb;

import java.util.*;

/**
 * The Aggregation operator that computes an aggregate (e.g., sum, avg, max,
 * min). Note that we only support aggregates over a single column, grouped by a
 * single column.
 */
public class Aggregate extends Operator {

    private static final long serialVersionUID = 1L;

    /**
     * The OpIterator that is feeding us tuples.
     */
    private OpIterator child;

    /**
     * The column over which we are computing an aggregate.
     */
    private int afield;

    /**
     * The column over which we are grouping the result, or -1 if
     * there is no grouping
     */
    private int gfield;

    /**
     * The aggregation operator to use
     */
    private Aggregator.Op aop;

    /**
     * the current aggregate
     */
    private Aggregator agg;

    /**
     * an iterator over the aggregate
     */
    private OpIterator aIter;

    /**
     * the description of the child
     */
    private TupleDesc ctd;

    /**
     * the tuple description of this aggregate
     */
    private TupleDesc td;

    /**
     * Constructor.
     *
     * Implementation hint: depending on the type of afield, you will want to
     * construct an {@link IntegerAggregator} or {@link StringAggregator} to help
     * you with your implementation of readNext().
     *
     *
     * @param child
     *            The OpIterator that is feeding us tuples.
     * @param afield
     *            The column over which we are computing an aggregate.
     * @param gfield
     *            The column over which we are grouping the result, or -1 if
     *            there is no grouping
     * @param aop
     *            The aggregation operator to use
     */
    public Aggregate(OpIterator child, int afield, int gfield, Aggregator.Op aop) {
        this.child = child;
        this.afield = afield;
        this.gfield = gfield;
        this.aop = aop;
        this.ctd = child.getTupleDesc();

        Type gFieldType;
        Type[] typeAr;
        String[] fieldAr;

        if (gfield == Aggregator.NO_GROUPING){
            gFieldType = null;
            typeAr = new Type[] {ctd.getFieldType(afield)};
            fieldAr = new String[] {ctd.getFieldName(afield)};
        } else {
            gFieldType = ctd.getFieldType(gfield);
            typeAr = new Type[] {ctd.getFieldType(gfield), Type.INT_TYPE};
            fieldAr = new String[] {ctd.getFieldName(gfield), ctd.getFieldName(afield)};
        }
        this.td = new TupleDesc(typeAr, fieldAr);

        if (ctd.getFieldType(afield) == Type.INT_TYPE){
            this.agg = new IntegerAggregator(gfield, gFieldType, afield, aop);
        } else {
            this.agg = new StringAggregator(gfield, gFieldType, afield, aop);
        }
        this.aIter = agg.iterator();
    }

    /**
     * @return If this aggregate is accompanied by a groupby, return the groupby
     *         field index in the <b>INPUT</b> tuples. If not, return
     *         {@link simpledb.Aggregator#NO_GROUPING}
     * */
    public int groupField() {
        return this.gfield;
    }

    /**
     * @return If this aggregate is accompanied by a group by, return the name
     *         of the groupby field in the <b>OUTPUT</b> tuples. If not, return
     *         null;
     * */
    public String groupFieldName() {
        if (gfield != Aggregator.NO_GROUPING) {
            return ctd.getFieldName(gfield);
        }
        return null;
    }

    /**
     * @return the aggregate field
     * */
    public int aggregateField() {
        return afield;
    }

    /**
     * @return return the name of the aggregate field in the <b>OUTPUT</b>
     *         tuples
     * */
    public String aggregateFieldName() {
        return ctd.getFieldName(afield);
    }

    /**
     * @return return the aggregate operator
     * */
    public Aggregator.Op aggregateOp() {
        return aop;
    }

    public static String nameOfAggregatorOp(Aggregator.Op aop) {
	    return aop.toString();
    }

    public void open() throws NoSuchElementException, DbException, TransactionAbortedException {
        child.open();
        while (child.hasNext()){
            agg.mergeTupleIntoGroup(child.next());
        }
        aIter.open();
        super.open();
    }

    /**
     * Returns the next tuple. If there is a group by field, then the first
     * field is the field by which we are grouping, and the second field is the
     * result of computing the aggregate. If there is no group by field, then
     * the result tuple should contain one field representing the result of the
     * aggregate. Should return null if there are no more tuples.
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        if (aIter.hasNext()){
            return aIter.next();
        }
        return null;
    }

    public void rewind() throws DbException, TransactionAbortedException {
        child.rewind();
        aIter.rewind();
    }

    /**
     * Returns the TupleDesc of this Aggregate. If there is no group by field,
     * this will have one field - the aggregate column. If there is a group by
     * field, the first field will be the group by field, and the second will be
     * the aggregate value column.
     *
     * The name of an aggregate column should be informative. For example:
     * "aggName(aop) (child_td.getFieldName(afield))" where aop and afield are
     * given in the constructor, and child_td is the TupleDesc of the child
     * iterator.
     */
    public TupleDesc getTupleDesc() {
        return this.td;
    }

    public void close() {
        super.close();
        agg.iterator().close();
        child.close();
    }

    @Override
    public OpIterator[] getChildren() {
        return new OpIterator[]{this.child};
    }

    @Override
    public void setChildren(OpIterator[] children) {
        if (children.length != 1){
            throw new IllegalArgumentException("Children length must equal 1");
        }
        this.child = children[0];
    }
}