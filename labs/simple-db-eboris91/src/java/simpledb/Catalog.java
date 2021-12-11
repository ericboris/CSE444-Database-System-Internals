package simpledb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Catalog keeps track of all available tables in the database and their
 * associated schemas.
 * For now, this is a stub catalog that must be populated with tables by a
 * user program before it can be used -- eventually, this should be converted
 * to a catalog that reads a catalog table from disk.
 *
 * @Threadsafe
 */
public class Catalog {
    /**
     * DbTable maintains the state of individual tables in the Catalog class.
     */
    private class DbTable {
        /**
         * The contents of the table.
         */
        final DbFile file;

        /**
         * The name of the table.
         */
        final String name;

        /**
         * The name of the primary key field.
         */
        final String pkeyField;

        public DbTable(DbFile file, String name, String pkeyField) {
            this.file = file;
            this.name = name;
            this.pkeyField = pkeyField;
        }

        public DbFile getFile() {
            return this.file;
        }

        public String getName() {
            return this.name;
        }

        public String getPkeyField() {
            return this.pkeyField;
        }
    }

    /**
     * A mapping of fileID -> table.
     */
    private ConcurrentHashMap<Integer, DbTable> tableMap;

    /**
     * A mapping of tableName -> fileID.
     */
    private ConcurrentHashMap<String, Integer> nameMap;

    /**
     * Constructor.
     * Creates a new, empty catalog.
     */
    public Catalog() {
        this.tableMap = new ConcurrentHashMap<>();
        this.nameMap = new ConcurrentHashMap<>();
    }

    /**
     * Add a new table to the catalog.
     * This table's contents are stored in anthe specified DbFile.
     * @param file the contents of the table to add;  file.getId() is the identfier of
     *    this file/tupledesc param for the calls getTupleDesc and getFile
     * @param name the name of the table -- may be an empty string.  May not be null.  If a name
     * conflict exists, use the last table to be added as the table for a given name.
     * @param pkeyField the name of the primary key field
     */
    public void addTable(DbFile file, String name, String pkeyField) {
        if (name == null) {
            throw new IllegalArgumentException("name may not be null");
        }
        int id = file.getId();
        DbTable dbTable = new DbTable(file, name, pkeyField);
        this.tableMap.put(id, dbTable);
        this.nameMap.put(name, id);
    }

    public void addTable(DbFile file, String name) {
        addTable(file, name, "");
    }

    /**
     * Add a new table to the catalog.
     * This table has tuples formatted using the specified TupleDesc and its
     * contents are stored in the specified DbFile.
     * @param file the contents of the table to add;  file.getId() is the identfier of
     *    this file/tupledesc param for the calls getTupleDesc and getFile
     */
    public void addTable(DbFile file) {
        addTable(file, (UUID.randomUUID()).toString());
    }

    /**
     * Return the id of the table with a specified name,
     * @throws NoSuchElementException if the table doesn't exist
     */
    public int getTableId(String name) throws NoSuchElementException {
        if (name != null) {
            Integer id = nameMap.get(name);
            if (id != null) {
                return id;
            }
        }
        throw new NoSuchElementException("The table " + name + " doesn't exist");
    }

    /**
     * Returns the tuple descriptor (schema) of the specified table
     * @param tableid The id of the table, as specified by the DbFile.getId()
     *     function passed to addTable
     * @throws NoSuchElementException if the table doesn't exist
     */
    public TupleDesc getTupleDesc(int tableid) throws NoSuchElementException {
        if (!tableMap.containsKey(tableid)) {
            throw new NoSuchElementException("There is not table with that tableid = " + tableid);
        }
        return getDatabaseFile(tableid).getTupleDesc();
    }

    /**
     * Returns the DbFile that can be used to read the contents of the
     * specified table.
     * @param tableid The id of the table, as specified by the DbFile.getId()
     *     function passed to addTable
     */
    public DbFile getDatabaseFile(int tableid) throws NoSuchElementException {
        if (!tableMap.containsKey(tableid)) {
            throw new NoSuchElementException("There is not Database File with that tableid = " + tableid);
        }
        return this.tableMap.get(tableid).getFile();
    }

    public String getPrimaryKey(int tableid) {
        if (!tableMap.containsKey(tableid)) {
            throw new NoSuchElementException("There is no Primary Key with tableid = " + tableid);
        }
        return this.tableMap.get(tableid).getPkeyField();
    }

    public Iterator<Integer> tableIdIterator() {
        return this.tableMap.keySet().iterator();
    }

    public String getTableName(int id) {
        if (!tableMap.containsKey(id)) {
            throw new NoSuchElementException("There is no Table with id = " + id);
        }
        return tableMap.get(id).getName();
    }

    /** Delete all tables from the catalog */
    public void clear() {
        tableMap.clear();
        nameMap.clear();
    }

    /**
     * Reads the schema from a file and creates the appropriate tables in the database.
     * @param catalogFile
     */
    public void loadSchema(String catalogFile) {
        String line = "";
        String baseFolder=new File(new File(catalogFile).getAbsolutePath()).getParent();
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(catalogFile)));

            while ((line = br.readLine()) != null) {
                //assume line is of the format name (field type, field type, ...)
                String name = line.substring(0, line.indexOf("(")).trim();
                //System.out.println("TABLE NAME: " + name);
                String fields = line.substring(line.indexOf("(") + 1, line.indexOf(")")).trim();
                String[] els = fields.split(",");
                ArrayList<String> names = new ArrayList<String>();
                ArrayList<Type> types = new ArrayList<Type>();
                String primaryKey = "";
                for (String e : els) {
                    String[] els2 = e.trim().split(" ");
                    names.add(els2[0].trim());
                    if (els2[1].trim().toLowerCase().equals("int"))
                        types.add(Type.INT_TYPE);
                    else if (els2[1].trim().toLowerCase().equals("string"))
                        types.add(Type.STRING_TYPE);
                    else {
                        System.out.println("Unknown type " + els2[1]);
                        System.exit(0);
                    }
                    if (els2.length == 3) {
                        if (els2[2].trim().equals("pk"))
                            primaryKey = els2[0].trim();
                        else {
                            System.out.println("Unknown annotation " + els2[2]);
                            System.exit(0);
                        }
                    }
                }
                Type[] typeAr = types.toArray(new Type[0]);
                String[] namesAr = names.toArray(new String[0]);
                TupleDesc t = new TupleDesc(typeAr, namesAr);
                HeapFile tabHf = new HeapFile(new File(baseFolder+"/"+name + ".dat"), t);
                addTable(tabHf,name,primaryKey);
                System.out.println("Added table : " + name + " with schema " + t);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (IndexOutOfBoundsException e) {
            System.out.println ("Invalid catalog entry : " + line);
            System.exit(0);
        }
    }
}