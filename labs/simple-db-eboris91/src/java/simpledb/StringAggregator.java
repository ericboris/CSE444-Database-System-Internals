package simpledb;

import java.util.HashMap;
import java.util.NoSuchElementException;

/**
 * Knows how to compute some aggregate over a set of StringFields.
 */
public class StringAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;

    /**
     * the 0-based index of the group-by field in the tuple, or NO_GROUPING if there is no grouping
     */
    private int gbfield;

    /**
     * the type of the group by field (e.g., Type.INT_TYPE), or null if there is no grouping
     */
    private Type gbfieldtype;

    /**
     * the 0-based index of the aggregate field in the tuple
     */
    private int afield;

    /**
     * aggregation operator to use -- only supports COUNT
     */
    private Op what;

    /**
     * Field to partial aggregates
     */
    private HashMap<Field, Integer> gbMap;

    /**
     * current aggregate valueT
     */
    private int aval;

    /**
     * Aggregate constructor
     * @param gbfield the 0-based index of the group-by field in the tuple, or NO_GROUPING if there is no grouping
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null if there is no grouping
     * @param afield the 0-based index of the aggregate field in the tuple
     * @param what aggregation operator to use -- only supports COUNT
     * @throws IllegalArgumentException if what != COUNT
     */
    public StringAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        if (what != Op.COUNT) {
            throw new IllegalArgumentException("what must equal Op.COUNT");
        }
        this.gbfield = gbfield;
        this.gbfieldtype = gbfieldtype;
        this.afield = afield;
        this.what = what;
        this.gbMap = new HashMap<>();
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the constructor
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        if (gbfield == Aggregator.NO_GROUPING){
            aval += 1;
        } else {
            Field f = tup.getField(gbfield);
            // Insert the field.
            if (!gbMap.containsKey(f)){
                gbMap.put(f, 0);
            }
            // Increment it's value.
            gbMap.put(f, gbMap.get(f) + 1);
        }
    }

    /**
     * Create a OpIterator over group aggregate results.
     *
     * @return a OpIterator whose tuples are the pair (groupVal,
     *   aggregateVal) if using group, or a single (aggregateVal) if no
     *   grouping. The aggregateVal is determined by the type of
     *   aggregate specified in the constructor.
     */
    public OpIterator iterator() {
        return new OpIterator() {
            private TupleDesc td;
            private Tuple[] aggs;
            private int index = 0;

            @Override
            public void open() throws DbException, TransactionAbortedException {
                Tuple t;
                if (gbfield == Aggregator.NO_GROUPING) {
                    aggs = new Tuple[1];
                    td = new TupleDesc(new Type[] {Type.INT_TYPE});
                    t = new Tuple(td);
                    t.setField(0, new IntField(aval));
                    aggs[0] = t;
                } else {
                    aggs = new Tuple[gbMap.size()];
                    td = new TupleDesc(new Type[] {gbfieldtype, Type.INT_TYPE});
                    int i = 0;
                    for (Field gbField: gbMap.keySet()){
                        t = new Tuple(td);
                        t.setField(0, gbField);
                        t.setField(1, new IntField(gbMap.get(gbField)));
                        aggs[i] = t;
                        i++;
                    }
                }
            }

            @Override
            public boolean hasNext() throws DbException, TransactionAbortedException {
                return index < aggs.length;
            }

            @Override
            public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
                if (hasNext()){
                    Tuple t = aggs[index++];
                    return t;
                } else {
                    throw new NoSuchElementException();
                }
            }

            @Override
            public void rewind() throws DbException, TransactionAbortedException {
                index = 0;
            }

            @Override
            public TupleDesc getTupleDesc() {
                return td;
            }

            @Override
            public void close() {
                td = null;
                aggs = null;
                index = 0;
            }
        };
    }
}