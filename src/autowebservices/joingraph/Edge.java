/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autowebservices.joingraph;

import autowebservices.database.ForeignKey;

/**
 * An Edge is a FK relationship in the Graph
 *
 * @author Curt
 */
public class  Edge {
    ForeignKey foreignKey;

    public Edge(ForeignKey fk) {
        foreignKey = fk;
    }
    
    public ForeignKey getForeignKey() {
        return foreignKey;
    }

}
