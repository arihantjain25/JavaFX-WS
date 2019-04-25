/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autowebservices.grammar;

import autowebservices.database.DB;
import autowebservices.joingraph.Graph;
import autowebservices.tree.PatternTree;

import java.io.IOException;
import java.util.*;

/**
 * @author Curtis Dyreson
 */
public class Visitor {

    private static PatternTree tree;
    private static DB db;
    private static Stack<PatternTree> treeStack;
    private static Graph joinGraph;

    public static void enterJson(DB db) {
        treeStack = new Stack();
        Visitor.db = db;
        tree = new PatternTree(db);
        joinGraph = new Graph(db);
    }

    public static void exitJson() throws IOException {
        tree.computeTreePaths(joinGraph, null);
    }

    public static void enterArray() {
        PatternTree child = new PatternTree(db, tree, PatternTree.arrayType);
        treeStack.push(tree);
        tree = child;
    }

    public static void exitArray() {
        tree = treeStack.pop();
    }

    public static void enterObj() {
        treeStack.push(tree);
        PatternTree child = new PatternTree(db, tree, PatternTree.objType);
        treeStack.push(tree);
        tree = child;
    }

    public static void exitObj() {
        tree = treeStack.pop();
    }

    public static void enterPair(String key) {
        key = key.replace("\"", "");
        tree = treeStack.peek();
        PatternTree child = new PatternTree(db, tree, key, PatternTree.pairType);
        tree.addChild(child);
        tree = child;
        treeStack.push(child);
    }

    public static void parsedString(String value) {
        value = value.replace("\"", "");
        tree = treeStack.pop();
        tree.buildPotentialLabels(value);
    }

    public static void exitPair() {
        tree = treeStack.peek();
    }
}
