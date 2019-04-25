/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autowebservices.joingraph;

import autowebservices.database.ForeignKey;

import java.util.ArrayList;
import java.util.List;

/**
 * A Path is a list of foreign keys
 *
 * @author Curt
 */
public class Path {

    List<ForeignKey> path = null;

    public Path() {
        path = new ArrayList(1);
    }

    public Path(Path clone) {
        this();
        path.addAll(clone.path);
    }

    public String getStart() {
        ForeignKey fk = path.get(0);
        return fk.getFromTable();
    }

    public String getEnd() {
        ForeignKey fk = path.get(path.size() - 1);
        return fk.getToTable();
    }

    public String generateJoinCondition() {
        int count = 0;
        String result = "";
        for (ForeignKey fk : path) {
            if (count++ > 0) { result += " AND "; }
            result += fk.generateJoinCondition();
        }
        return result;
    }

    public List<ForeignKey> getFKs() {
        return path;
    }
    
    public void put(ForeignKey fk) {
        path.add(0, fk);
    }
//

    public void push(ForeignKey fk) {
        path.add(fk);
    }

    public int size() {
        return path.size();
    }
}


//    public boolean equals(Path other) {
//        for (ForeignKey fk : path) {
//
//        }
//        return true;
//    }