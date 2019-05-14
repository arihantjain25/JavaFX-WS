/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autowebservices.tree;

import autowebservices.database.DB;
import autowebservices.database.ForeignKey;
import autowebservices.datapull.SQLPull;
import autowebservices.joingraph.Graph;
import autowebservices.joingraph.Path;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


/**
 * This class represents a pattern tree. A pattern tree is constructed when a
 * JSON pattern is parsed. The tree consists of different kinds of nodes: array,
 * object, and key/value pair.
 *
 * @author Curtis Dyreson
 */
public class PatternTree {

    private List<PatternTree> children;
    private String label;
    private String value;
    private String table;
    private PatternTree parent;
    private static DB db;  // Possibly could be static, but not important?

    public PatternTree(DB db) {
        label = "root";
        value = null;
        parent = null;
        PatternTree.db = db;
        table = null;
        children = null;
    }

    public PatternTree(DB db, PatternTree p, String s) {
        label = s;
        value = null;
        parent = p;
        PatternTree.db = db;
        table = null;
        children = null;
    }

    private void setValue(String s) {
        value = s;
    }

    private boolean hasChildren() {
        return children != null;
    }

    private boolean hasChildren(PatternTree patternTree) {
        return patternTree.children != null;
    }

    private boolean isNotRoot() {
        return parent != null;
    }

    private boolean isNotRoot(PatternTree tree) {
        return !tree.label.equals("root");
    }

    public void addChild(PatternTree t) {
        if (children == null) {
            children = new ArrayList<>(3);
        }
        children.add(t);
    }

    private PatternTree getRoot() {
        PatternTree rootNode = children.get(0);
        while (isNotRoot(rootNode)) {
            rootNode = rootNode.parent;
        }
        return rootNode;
    }

    private List<String> listTables() {
        PatternTree root = getRoot();
        HashSet<String> tables = new HashSet<>();
        listOfTablesInTree(root, tables);
        return new ArrayList<>(tables);
    }

    private void listOfTablesInTree(PatternTree tree, HashSet<String> result) {
        if (isNotRoot()) {
            if (table != null) {
                result.add(table);
            }
        }
        if (hasChildren()) {
            for (PatternTree child : tree.children) {
                child.listOfTablesInTree(child, result);
            }
        }
    }

    private List<String> listColumns() {
        List<String> columns = new ArrayList<>();
        PatternTree root = getRoot();
        listOfColumnsInTree(root, columns);
        return new ArrayList<>(columns);
    }

    private void listOfColumnsInTree(PatternTree tree, List<String> result) {
        if (isNotRoot()) {
            if (table != null) {
                result.add(table + ".\"" + value + "\"");
            }
        }
        if (hasChildren()) {
            for (PatternTree child : tree.children) {
                child.listOfColumnsInTree(child, result);
            }
        }
    }

    public void buildPotentialLabels(String s) {
        setValue(s);
        Set<String> tables = db.stringLookup(s);
        if (tables != null) {
            for (String tab : tables) {
                table = tab;
            }
        }
    }

    public void computeTreePaths(Graph joinGraph) throws IOException {
        SQLPull sqlPull = new SQLPull();
        HashMap<Integer, Set<ForeignKey>> allPaths = new HashMap<>();
        HashMap<String, Integer> queryAndNumberRows = new HashMap<>();
        boolean containsDuplicates = containsDuplicate();

        if (hasChildren()) {
            PatternTree rootNode = getRoot();
            allPaths = savePaths(joinGraph, rootNode, allPaths);
            if (rootNode.children.size() == 1 || allPaths.isEmpty()) {
                String query = sqlPull.generateRowsEstimaiton(new HashSet<>(), listColumns().toString(), listTables());
                queryAndNumberRows.put(query.split("EXPLAIN ")[1] + "!!!" + listTables().get(0),
                        getRowsNumberFromOutput(query));
            }
        }

        String queryorderby = "";
        boolean flag = true;
        for (Integer i : allPaths.keySet()) {
            Set<ForeignKey> set = new HashSet<>(allPaths.get(i));
            String addPath = createAddPaths(new ArrayList<>(allPaths.get(i)));
            String query = sqlPull.generateRowsEstimaiton(set, listColumns().toString(), listTables());
            if (containsDuplicates) {
                String tempQuery = sqlPull.generateQuery(set, new HashSet<>(listColumns()).toString(), listTables());
                query = changeQueryToAddSecondTable(tempQuery, listColumns());
                queryAndNumberRows.put(query + "!!!" + addPath, getRowsNumberFromOutput("EXPLAIN " + query));
            } else {
                if (flag) {
                    queryorderby = query.split("EXPLAIN ")[1].split("ORDER BY")[1];
                    flag = false;
                }
                queryAndNumberRows.put(query.split("EXPLAIN ")[1] + "!!!" + addPath,
                        getRowsNumberFromOutput(query));
            }
        }
        HashMap<String, Integer> temp = sortByValue(queryAndNumberRows);
        writeToFile(queryorderby, temp, allPaths);
    }

    private static String createAddPaths(List<ForeignKey> arrayList) {
        StringBuilder addPath = new StringBuilder();
        for (ForeignKey foreignKey : arrayList)
            addPath.append(foreignKey.getFromTable()).append(",").append(foreignKey.getToTable()).
                    append(",").append(foreignKey.getColumnJoin()).append("@");
        return addPath.toString();
    }

    private static String changeQueryToAddSecondTable(String query, List<String> listColumns) {
        String colJoin = null;
        boolean flag = true;
        HashSet<String> hashSet = new HashSet<>();
        List<String> list = new ArrayList<>();
        for (String col : listColumns) {
            if (hashSet.add(col)) {
                list.add("t1." + col.split("\\.")[1]);
                if (flag) {
                    flag = false;
                    colJoin = col.split("\\.")[1];
                }
            } else {
                list.add("t2." + col.split("\\.")[1]);
            }
        }
        String columns = list.toString().split("\\[")[1].split("]")[0];
        return "SELECT DISTINCT " + columns + " \nFROM (" + query + ") t1 \n" +
                "LEFT JOIN " + "(" + query + ") t2 \n" +
                "ON " + "t1." + colJoin + " = " + "t2." + colJoin + " \nORDER BY " + columns;
    }

    private static void writeToFile(String queryorderby, HashMap<String, Integer> temp, HashMap<Integer,
            Set<ForeignKey>> allPaths) throws IOException {
        FileWriter fileWriter = new FileWriter("generatedfiles/queries.txt");
        Iterator it = temp.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            fileWriter.write((pair.getKey() + "!!!" + pair.getValue()) + "!!!");
            it.remove();
        }

        if (!allPaths.isEmpty() && !queryorderby.equals("")) {
            String[] tempOrderBy = queryorderby.split("\"");
            StringBuilder orderby = new StringBuilder("ORDER BY ");
            for (int i = 1; i < tempOrderBy.length - 1; i = i + 2)
                orderby.append("\"").append(tempOrderBy[i]).append("\"").append(", ");
            orderby.append("\"").append(tempOrderBy[tempOrderBy.length - 1]).append("\"");
            fileWriter.write(orderby.toString());
        }
        fileWriter.close();
    }

    private boolean containsDuplicate() {
        List<String> list = listColumns();
        Set<String> set = new HashSet<>(list);
        return (set.size() < list.size());
    }

    private static HashMap<String, Integer> sortByValue(HashMap<String, Integer> hm) {
        List<Map.Entry<String, Integer>> list = new LinkedList<>(hm.entrySet());
        list.sort((val1, val2) -> (val2.getValue()).compareTo(val1.getValue()));
        HashMap<String, Integer> temp = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> l : list) temp.put(l.getKey(), l.getValue());
        return temp;
    }

    private HashMap<Integer, Set<ForeignKey>> savePaths(Graph joinGraph, PatternTree objRoot,
                                                        HashMap<Integer, Set<ForeignKey>> allPaths) {
        if (hasChildren(objRoot)) {
            for (PatternTree child : objRoot.children) {
                if (child.table != null) {
                    if (isNotRoot(objRoot)) {
                        List<Path> paths;
                        if (parent.children.get(0).value != null) {
                            if (!parent.children.get(0).table.equals(child.table)) {
                                paths = joinGraph.getPaths(parent.children.get(0).table, child.table);
                                if (paths.size() > 0) {
                                    allPaths = listBestPaths(paths, allPaths);
                                }
                            }
                        }
                    }
                    allPaths = child.savePaths(joinGraph, child, allPaths);

                } else {
                    allPaths = child.savePaths(joinGraph, child, allPaths);
                }
            }

            if (objRoot.children.size() > 1) {
                PatternTree previousChild = null;
                for (PatternTree child : objRoot.children) {
                    if (previousChild != null) {
                        if (child.table != null) {
                            if (!child.table.equals(previousChild.table)) {
                                List<Path> paths = joinGraph.getPaths(previousChild.table, child.table);
                                if (paths != null) {
                                    if (parent != null) {
                                        if (!parent.children.get(0).table.equals(child.table)) {
                                            paths = joinGraph.getPaths(previousChild.table, child.table);
                                            if (paths.size() > 0) {
                                                allPaths = listBestPaths(paths, allPaths);
                                            }
                                        }
                                    } else {
                                        if (paths.size() > 0) {
                                            allPaths = listBestPaths(paths, allPaths);
                                        }
                                    }
                                }
                                previousChild = child;
                            }
                        }
                    } else {
                        previousChild = child;
                    }
                }
            }
        }
        return allPaths;
    }

    private HashMap<Integer, Set<ForeignKey>> listBestPaths(List<Path> paths,
                                                            HashMap<Integer, Set<ForeignKey>> allPaths) {
        HashMap<Integer, Set<ForeignKey>> tempAllPaths = new HashMap<>();
        int count = 0;
        for (Path path : paths) {
            if (allPaths.size() > 0) {
                for (Integer integer : allPaths.keySet()) {
                    HashSet<ForeignKey> temphash = new HashSet<>();
                    temphash.addAll(allPaths.get(integer));
                    temphash.addAll(path.getFKs());
                    tempAllPaths.put(count++, temphash);
                }
            } else {
                HashSet<ForeignKey> temphash = new HashSet<>(path.getFKs());
                tempAllPaths.put(count++, temphash);
            }
        }
        return tempAllPaths;
    }

    private int getRowsNumberFromOutput(String query) {
        try (ResultSet resultSet = db.executeQuery(query)) {
            resultSet.next();
            String toParse = resultSet.getString("QUERY PLAN");
            return parseQueryOutput(toParse);
        } catch (SQLException e) {
            System.out.println(query);
            System.err.println("Query not executed");
        }
        return Integer.parseInt("0");
    }

    private int parseQueryOutput(String row) {
        return Integer.parseInt(row.split(" ")[3].split("=")[1]);
    }
}


//    public <K, V extends Comparable<? super V>> List<Map.Entry<K, V>> hashMapValueSort(Map<K, V> map) {
//        List<Map.Entry<K, V>> sortedEntries = new ArrayList<>(map.entrySet());
//        sortedEntries.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
//        return sortedEntries;
//    }