/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autowebservices.joingraph;

/**
 *
 * @author Curt
 */
public class Node {
    String table;
    
    public Node(String table) {
        this.table = table;
    }
    
    public String getTable() {
        return table;
    }
    
    @Override
    public int hashCode() {
        return table.hashCode();
    }

    public boolean equals(Node obj) {
        return table.equals(obj.table);
    }
}
