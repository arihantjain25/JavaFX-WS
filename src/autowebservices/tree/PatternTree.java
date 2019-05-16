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

    public void computeTreePaths(Graph joinGraph) throws IOException, SQLException {
        SQLPull sqlPull = new SQLPull();
        HashMap<Integer, Set<ForeignKey>> allPaths = new HashMap<>();
        HashMap<String, Integer> queryAndNumberRows = new HashMap<>();
        boolean containsDuplicates = containsDuplicate();
        List<ForeignKey> fks = db.buildFKs();

        if (hasChildren()) {
            PatternTree rootNode = getRoot();
            allPaths = savePaths(joinGraph, rootNode, allPaths);
            if (rootNode.children.size() == 1 || allPaths.isEmpty()) {
                String query = sqlPull.generateRowsEstimation(new HashSet<>(), listColumns().toString(), listTables());
                if (containsDuplicates) {
                    String colJoin = "";
                    boolean flag = true;
                    String duplicateTableJoin = getDuplicateTableJoin();
                    String table = Objects.requireNonNull(duplicateTableJoin).split("\\.")[0];
                    List<String> list = new ArrayList<>(listColumns());
                    List<String> list1 = new ArrayList<>();
                    Set<ForeignKey> set = new HashSet<>();
                    for (ForeignKey fk : fks) {
                        if (fk.getFromTable().equals(table)) {
                            if (flag) {
                                flag = false;
                                colJoin = db.getPrimaryKey(fk.getToTable());
                                set.add(fk);
                                if (fk.generateJoinCondition().split(" = ")[0].split("\\.")[0].equals(fk.getToTable()))
                                    list.add(fk.generateJoinCondition().split(" = ")[0].split("\\.")[0] + ".\"" + colJoin + "\"");
                                else
                                    list.add(fk.generateJoinCondition().split(" = ")[1].split("\\.")[0] + ".\"" + colJoin + "\"");
//                                list1.add(fk.generateJoinCondition().split(" = ")[0].split("\\.")[1].replace("\"", ""));
//                                list1.add(fk.generateJoinCondition().split(" = ")[1].split("\\.")[1].replace("\"", ""));
                            }
                        }
                    }
                    if (fks.size() == 0)
                        colJoin = nonDuplicateColumn();

                    String tempQuery = sqlPull.generateQuery(set, new HashSet<>(list).toString(), listTables());
                    query = sqlPull.changeQueryToAddSecondTable(tempQuery, listColumns(), list1, colJoin);
                    queryAndNumberRows.put(query + "!!!" + listColumns().get(0), getRowsNumberFromOutput("EXPLAIN " + query));
                } else {
                    queryAndNumberRows.put(query.split("EXPLAIN ")[1] + "!!!" + listTables().get(0),
                            getRowsNumberFromOutput(query));
                }
            }
        }

        String queryOrderBy = "";
        boolean flag = true;
        for (Integer i : allPaths.keySet()) {
            Set<ForeignKey> set = new HashSet<>(allPaths.get(i));
            String addPath = sqlPull.createAddPaths(new ArrayList<>(allPaths.get(i)));
            String query = sqlPull.generateRowsEstimation(set, listColumns().toString(), listTables());
            if (containsDuplicates) {
                String colJoin = "";
                boolean flag1 = true;
                String duplicateTableJoin = getDuplicateTableJoin();
                String table = Objects.requireNonNull(duplicateTableJoin).split("\\.")[0];
                List<String> list = new ArrayList<>(listColumns());
                List<String> list1 = new ArrayList<>();
                Set<ForeignKey> set1 = new HashSet<>();
                for (ForeignKey fk : fks) {
                    if (fk.getFromTable().equals(table)) {
                        if (flag1) {
                            flag1 = false;
                            colJoin = db.getPrimaryKey(fk.getToTable());
                            set.add(fk);
                            if (fk.generateJoinCondition().split(" = ")[0].split("\\.")[0].equals(fk.getToTable()))
                                list.add(fk.generateJoinCondition().split(" = ")[0].split("\\.")[0] + ".\"" + colJoin + "\"");
                            else
                                list.add(fk.generateJoinCondition().split(" = ")[1].split("\\.")[0] + ".\"" + colJoin + "\"");
//                            list1.add(fk.generateJoinCondition().split(" = ")[0].split("\\.")[1].replace("\"", ""));
//                            list1.add(fk.generateJoinCondition().split(" = ")[1].split("\\.")[1].replace("\"", ""));
                        }
                    }
                }

                String tempQuery = sqlPull.generateQuery(set1, new HashSet<>(list).toString(), listTables());
                query = sqlPull.changeQueryToAddSecondTable(tempQuery, listColumns(), list1, colJoin);
                queryAndNumberRows.put(query + "!!!" + listColumns().get(0), getRowsNumberFromOutput("EXPLAIN " + query));
            } else {
                if (flag) {
                    queryOrderBy = query.split("EXPLAIN ")[1].split("ORDER BY")[1];
                    flag = false;
                }
                queryAndNumberRows.put(query.split("EXPLAIN ")[1] + "!!!" + addPath,
                        getRowsNumberFromOutput(query));
            }
        }
        HashMap<String, Integer> temp = sortByValue(queryAndNumberRows);
        sqlPull.writeToFile(queryOrderBy, temp, allPaths);
    }

    private String nonDuplicateColumn() {
        List<String> list = listColumns();
        for (String s : list) {
            if (Collections.frequency(list, s) == 1) {
                return s.split("\\.")[1].replaceAll("\"", "");
            }
        }
        return null;
    }

    private boolean containsDuplicate() {
        List<String> list = listColumns();
        Set<String> set = new HashSet<>(list);
        return (set.size() < list.size());
    }

    private String getDuplicateTableJoin() {
        List<String> list = listColumns();
        Set<String> strings = new HashSet<>();
        for (String l : list) {
            if (!strings.add(l)) {
                return l.split("\\.")[0];
            }
        }
        return null;
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
                    HashSet<ForeignKey> tempHash = new HashSet<>();
                    tempHash.addAll(allPaths.get(integer));
                    tempHash.addAll(path.getFKs());
                    tempAllPaths.put(count++, tempHash);
                }
            } else {
                HashSet<ForeignKey> tempHash = new HashSet<>(path.getFKs());
                tempAllPaths.put(count++, tempHash);
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
//            System.out.println(query);
            System.err.println("Query not executed");
        }
        return Integer.parseInt("0");
    }

    private int parseQueryOutput(String row) {
        return Integer.parseInt(row.split(" ")[3].split("=")[1]);
    }
}