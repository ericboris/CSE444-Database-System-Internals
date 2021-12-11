package simpledb;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.*;

/**
 * TupleDesc describes the schema of a tuple.
 */
public class TupleDesc implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The number of fields in the tuple.
     */
    private int numFields;

    /**
     * The array of TDItem items.
     */
    private TDItem[] items;

    /**
     * A help class to facilitate organizing the information of each field
     * */
    public static class TDItem implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * The type of the field
         * */
        public final Type fieldType;

        /**
         * The name of the field
         * */
        public final String fieldName;

        public TDItem(Type t, String n) {
            this.fieldName = n;
            this.fieldType = t;
        }

        public String toString() {
            return fieldName + "(" + fieldType + ")";
        }
    }

    /**
     * @return
     *        An iterator which iterates over all the field TDItems
     *        that are included in this TupleDesc
     * */
    public Iterator<TDItem> iterator() {
        class TDItemIterator implements Iterator<TDItem> {
            /**
             * The current items index.
             */
            private int i = 0;

            /**
             * @return Boolean; true if items has another item
             *          and false otherwise.
             */
            @Override
            public boolean hasNext() {
                return i < numFields;
            }

            /**
             * @return TDItem; the next item from items.
             */
            @Override
            public TDItem next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return items[i++];
            }
        }
        return new TDItemIterator();
    }

    /**
     * Create a new TupleDesc with typeAr.length fields with fields of the
     * specified types, with associated named fields.
     *
     * @param typeAr
     *            array specifying the number of and types of fields in this
     *            TupleDesc. It must contain at least one entry.
     * @param fieldAr
     *            array specifying the names of the fields. Note that names may
     *            be null.
     */
    public TupleDesc(Type[] typeAr, String[] fieldAr) {
        if (typeAr.length <= 0) {
            throw new IllegalArgumentException("typeAr must contain at least one entry.");
        }
        if (typeAr.length != fieldAr.length) {
            throw new IllegalArgumentException("typeAr and fieldAr lengths must match.");
        }

        this.numFields = typeAr.length;

        this.items = new TDItem[numFields];
        for (int i = 0; i < numFields; i++) {
            items[i] = new TDItem(typeAr[i], fieldAr[i]);
        }
    }

    /**
     * Constructor. Create a new tuple desc with typeAr.length fields with
     * fields of the specified types, with anonymous (unnamed) fields.
     *
     * @param typeAr
     *            array specifying the number of and types of fields in this
     *            TupleDesc. It must contain at least one entry.
     */
    public TupleDesc(Type[] typeAr) {
        this(typeAr, new String[typeAr.length]);
    }

    /**
     * Constructor. Create a new tuple desc.
     *
     * @param items
     *            array items in this TupleDesc. It must contain at least one
     *            entry.
     */
    public TupleDesc(TDItem[] items) {
        if (items == null || items.length <= 0) {
            throw new IllegalArgumentException("items must contain at least one entry.");
        }
        this.items = items;
        this.numFields = items.length;
    }

    /**
     * @return the number of fields in this TupleDesc
     */
    public int numFields() {
        return this.numFields;
    }

    /**
     * Gets the (possibly null) field name of the ith field of this TupleDesc.
     *
     * @param i
     *            index of the field name to return. It must be a valid index.
     * @return the name of the ith field
     * @throws NoSuchElementException
     *             if i is not a valid field reference.
     */
    public String getFieldName(int i) throws NoSuchElementException {
        if (!isValidIndex(i)) {
            throw new NoSuchElementException("Index " + i + " is out of bounds.");
        }
        return this.items[i].fieldName;
    }

    /**
     * Gets the type of the ith field of this TupleDesc.
     *
     * @param i
     *            The index of the field to get the type of. It must be a valid
     *            index.
     * @return the type of the ith field
     * @throws NoSuchElementException
     *             if i is not a valid field reference.
     */
    public Type getFieldType(int i) throws NoSuchElementException {
        if (!isValidIndex(i)) {
            throw new NoSuchElementException("Index " + i + " is out of bounds.");
        }
        return this.items[i].fieldType;
    }

    /**
     * @return boolean; true if the index is valid and false otherwise.
     *
     * @param i
     *          index to validate.
     */
    private boolean isValidIndex(int i) {
        return i >= 0 && i < numFields;
    }

    /**
     * Find the index of the field with a given name.
     *
     * @param name
     *            name of the field.
     * @return the index of the field that is first to have the given name.
     * @throws NoSuchElementException
     *             if no field with a matching name is found.
     */
    public int fieldNameToIndex(String name) throws NoSuchElementException {
        if (name == null) {
            throw new NoSuchElementException("name must not be null.");
        }

        for (int i = 0; i < numFields; i++) {
            if (getFieldName(i) != null && getFieldName(i).equals(name)) {
                return i;
            }
        }

        throw new NoSuchElementException("No field with a matching name is found.");
    }

    /**
     * @return The size (in bytes) of tuples corresponding to this TupleDesc.
     *         Note that tuples from a given TupleDesc are of a fixed size.
     */
    public int getSize() {
        int size = 0;
        for (TDItem item : items) {
            size += item.fieldType.getLen();
        }
        return size;
    }

    /**
     * Merge two TupleDescs into one, with td1.numFields + td2.numFields fields,
     * with the first td1.numFields coming from td1 and the remaining from td2.
     *
     * @param td1
     *            The TupleDesc with the first fields of the new TupleDesc
     * @param td2
     *            The TupleDesc with the last fields of the TupleDesc
     * @return the new TupleDesc
     */
    public static TupleDesc merge(TupleDesc td1, TupleDesc td2) {
        int td1n = td1.numFields();
        int td2n = td2.numFields();
        TDItem[] result = new TDItem[td1n + td2n];
        System.arraycopy(td1.items, 0, result, 0, td1n);
        System.arraycopy(td2.items, 0, result, td1n, td2n);
        return new TupleDesc(result);
    }

    /**
     * Compares the specified object with this TupleDesc for equality. Two
     * TupleDescs are considered equal if they have the same number of items
     * and if the i-th type in this TupleDesc is equal to the i-th type in o
     * for every i.
     *
     * @param o
     *            the Object to be compared for equality with this TupleDesc.
     * @return true if the object is equal to this TupleDesc.
     */

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TupleDesc)) {
            return false;
        }
        TupleDesc other = (TupleDesc) o;
        return Arrays.deepToString(items).equals(Arrays.deepToString(other.items));
    }

    public int hashCode() {
        // If you want to use TupleDesc as keys for HashMap, implement this so
        // that equal objects have equals hashCode() results
        return Arrays.hashCode(items);
    }

    /**
     * Returns a String describing this descriptor. It should be of the form
     * "fieldType[0](fieldName[0]), ..., fieldType[M](fieldName[M])", although
     * the exact format does not matter.
     *
     * @return String describing this descriptor.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (TDItem item : items) {
            sb.append(item.toString() + ", ");
        }
        return sb.toString();
    }
}
