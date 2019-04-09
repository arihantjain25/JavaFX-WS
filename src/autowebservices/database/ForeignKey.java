/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autowebservices.database;

import java.util.List;

/**
 *
 * @author Curt
 */
public class ForeignKey {

    String fromTable;
    String toTable;
    List<String> fromColumns;
    List<String> toColumns;
    int id;
    static int count = 0;

    public ForeignKey(String fromTable, String toTable, List<String> fromColumns, List<String> toColumns) {
        this.fromTable = fromTable;
        this.toTable = toTable;
        this.fromColumns = fromColumns;
        this.toColumns = toColumns;
        id = count++;
    }

    public ForeignKey(int id, String fromTable, String toTable, List<String> fromColumns, List<String> toColumns) {
        this.fromTable = fromTable;
        this.toTable = toTable;
        this.fromColumns = fromColumns;
        this.toColumns = toColumns;
        this.id = id;
    }

    public void addFromColumn(String col) {
        fromColumns.add(col);
    }

    public void addToColumn(String col) {
        toColumns.add(col);
    }

    public int getId() {
        return id;
    }
    
    public String getFromTable() {
        return fromTable;
    }

    public String getToTable() {
        return toTable;
    }

    public String generateJoinCondition() {
        String result = "";
        int size = fromColumns.size();
        while (size-- > 0) {
            result += fromTable + ".\"" + fromColumns.get(size) + "\" = " + toTable + ".\"" + toColumns.get(size) + "\"";
            if (size > 0) {
                result += " AND ";
            }
        }
        return result;
    }
}
