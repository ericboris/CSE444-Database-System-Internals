package simpledb;

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Tuple maintains information about the contents of a tuple. Tuples have a
 * specified schema specified by a TupleDesc object and contain Field objects
 * with the data for each field.
 */
public class Tuple implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The schema of this tuple.
     */
    private TupleDesc tupleDesc;

    /**
     * The fields in this tuple. Must have length > 0.
     */
    private Field[] fields;

    /**
     * The location of this tuple on disk, may be null.
     */
    private RecordId recordId;

    /**
     * Create a new tuple with the specified schema (type).
     *
     * @param td
     *            the schema of this tuple. It must be a valid TupleDesc
     *            instance with at least one field.
     */
    public Tuple(TupleDesc td) {
        if (td.numFields() <= 0) {
            throw new IllegalArgumentException("Instance must have at least one field.");
        }

        tupleDesc = td;
        fields = new Field[td.numFields()];
    }

    /**
     * @return The TupleDesc representing the schema of this tuple.
     */
    public TupleDesc getTupleDesc() {
        return tupleDesc;
    }

    /**
     * @return The RecordId representing the location of this tuple on disk. May
     *         be null.
     */
    public RecordId getRecordId() {
        return this.recordId;
    }

    /**
     * TODO
     */
    public int getNumFields()  { return this.fields.length; }

    /**
     * Set the RecordId information for this tuple.
     *
     * @param rid
     *            the new RecordId for this tuple.
     */
    public void setRecordId(RecordId rid) {
        recordId = rid;
    }

    /**
     * Change the value of the ith field of this tuple.
     *
     * @param i
     *            index of the field to change. It must be a valid index.
     * @param f
     *            new value for the field.
     */
    public void setField(int i, Field f) {
        if (!isValidFieldsIndex(i)) {
            throw new IndexOutOfBoundsException("Index " + i + " is out of bounds.");
        }
        fields[i] = f;
    }

    /**
     * @return the value of the ith field, or null if it has not been set.
     *
     * @param i
     *            field index to return. Must be a valid index.
     */
    public Field getField(int i) {
        if (!isValidFieldsIndex(i)) {
            throw new IndexOutOfBoundsException("Index " + i + " is out of bounds.");
        }
        return fields[i];
    }

    /**
     * @return boolean; true if the index is valid and false otherwise.
     *
     * @param i
     *          index to validate.
     */
    private boolean isValidFieldsIndex(int i) {
        return i >= 0 && i < fields.length;
    }

    /**
     * Returns the contents of this Tuple as a string. Note that to pass the
     * system tests, the format needs to be as follows:
     *
     * column1\tcolumn2\tcolumn3\t...\tcolumnN
     *
     * where \t is any whitespace (except a newline)
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < fields.length; i++) {
            sb.append(fields[i].toString());
            if (i < fields.length - 1) {
                sb.append("\t");
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    /**
     * @return
     *        An iterator which iterates over all the fields of this tuple
     * */
    public Iterator<Field> fields()
    {
        class FieldIterator implements Iterator<Field> {
            /**
             * The current field index.
             */
            private int i = 0;

            /**
             * @return Boolean; true if fields has another field
             *          and false otherwise.
             */
            @Override
            public boolean hasNext() {
                return i < fields.length;
            }

            /**
             * @return Field; the next field from fields.
             */
            @Override
            public Field next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return fields[i++];
            }
        }

        return new FieldIterator();
    }

    /**
     * reset the TupleDesc of this tuple (only affecting the TupleDesc)
     * */
    public void resetTupleDesc(TupleDesc td)
    {
        tupleDesc = td;
    }
}
