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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

//import autowebservices.output.json.ArrayFormatter;
//import autowebservices.output.json.ObjFormatter;
//import autowebservices.output.json.PairFormatter;

/**
 * This class represents a pattern tree. A pattern tree is constructed when a
 * JSON pattern is parsed. The tree consists of different kinds of nodes: array,
 * object, and key/value pair.
 *
 * @author Curtis Dyreson
 */
public class PatternTree {

    List<PatternTree> children;
    String label;
    String value;
    String table;
    List<String> potentialLabels;
    PatternTree parent;
    static DB db;  // Possibly could be static, but not important?
    int treeType;
    public final static int arrayType = 0;
    public final static int objType = 1;
    public final static int pairType = 2;

    public PatternTree(DB db) {
        treeType = objType;
        label = "root";
        value = null;
        parent = null;
        potentialLabels = null;
        this.db = db;
        table = null;
        children = null;
    }

    public PatternTree(DB db, PatternTree p, int type) {
        treeType = type;
        label = "none";
        value = null;
        parent = null;
        potentialLabels = null;
        this.db = db;
        table = null;
        children = null;
    }

    public PatternTree(DB db, PatternTree p, String s, int type) {
        treeType = type;
        label = s;
        value = null;
        parent = p;
        this.db = db;
        potentialLabels = null;
        table = null;
        children = null;
    }

    public void buildPotentialLabels(String s) {
        setValue(s);
        Set<String> tables = db.stringLookup(s);
        potentialLabels = new ArrayList<>();
        if (tables != null) {
            for (String tab : tables) {
                table = tab;
            }
        }
    }

    public void setValue(String s) {
        value = s;
    }

    public boolean hasChildren() {
        return children != null;
    }

    public boolean hasChildren(PatternTree patternTree) {
        return patternTree.children != null;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public boolean isRoot(PatternTree tree) {
        return tree.label.equals("root");
    }

    public void addChild(PatternTree t) {
        if (children == null) {
            children = new ArrayList<>(3);
        }
        children.add(t);
    }

    public String getValue() {
        return value;
    }

    public List<String> listTables() {
        PatternTree root = getRoot();
        HashSet<String> tables = new HashSet<>();
        listOfTablesInTree(root, tables);
        return new ArrayList<>(tables);
    }


    public void listOfTablesInTree(PatternTree tree, HashSet<String> result) {
        if (!isRoot()) {
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

    public List<String> listColumns() {
        List<String> columns = new ArrayList<>();
        PatternTree root = getRoot();
        listOfColumnsInTree(root, columns);
        return new ArrayList<>(columns);
    }

    public void listOfColumnsInTree(PatternTree tree, List<String> result) {
        if (!isRoot()) {
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

    public void computeTreePaths(Graph joinGraph, String parentTable) throws IOException {
        SQLPull sqlPull = new SQLPull();
        HashMap<Integer, Set<ForeignKey>> allPaths = new HashMap<>();
        if (hasChildren()) {
            PatternTree rootNode = getRoot();
            allPaths = savePaths(joinGraph, rootNode, null, allPaths);
        }
        HashMap<String, Integer> queryAndNumberRows = new HashMap<>();
        for (Integer i : allPaths.keySet()) {
            Set<ForeignKey> set = new HashSet<>(allPaths.get(i));
            List<ForeignKey> arrayList1 = new ArrayList<>(allPaths.get(i));
            StringBuilder addPath = new StringBuilder();
            for (ForeignKey foreignKey : arrayList1) {
                addPath.append(foreignKey.getFromTable()).append(",").append(foreignKey.getToTable()).append(",").append(foreignKey.getColumnJoin()).append("@");
            }
            String query = sqlPull.generateRowsQuery(joinGraph, set,
                    listColumns().toString(), listTables());
            queryAndNumberRows.put(query.split("EXPLAIN ")[1] + "!!!" + addPath.toString(), getRowsNumber(query));
        }
        HashMap<String, Integer> temp = sortByValue(queryAndNumberRows);
        FileWriter fileWriter = new FileWriter("queries.txt");
        Iterator it = temp.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            fileWriter.write((pair.getKey() + "!!!" + pair.getValue()) + "!!!");
            it.remove();
        }
        fileWriter.close();
//        String[] temp2 = usingBufferedReader().split("!!!");
//        for (String s : temp2)
//            System.out.println(s);
    }

    public static String usingBufferedReader() {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader("queries.txt"))) {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                contentBuilder.append(sCurrentLine).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }

    public static HashMap<String, Integer> sortByValue(HashMap<String, Integer> hm) {
        List<Map.Entry<String, Integer>> list = new LinkedList<>(hm.entrySet());
        list.sort((val1, val2) -> (val2.getValue()).compareTo(val1.getValue()));
        HashMap<String, Integer> temp = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> l : list)
            temp.put(l.getKey(), l.getValue());
        return temp;
    }

    public PatternTree getRoot() {
        PatternTree rootNode = children.get(0);
        while (!isRoot(rootNode)) {
            rootNode = rootNode.parent;
        }
        return rootNode;
    }

    public HashMap<Integer, Set<ForeignKey>> savePaths(Graph joinGraph, PatternTree objRoot, String parentTable, HashMap<Integer, Set<ForeignKey>> allPaths) {
        if (hasChildren(objRoot)) {
            for (PatternTree child : objRoot.children) {
                if (child.table != null) {
                    if (!isRoot(objRoot)) {
                        List<Path> paths = null;
                        if (parent.children.get(0).value != null) {
                            if (!parent.children.get(0).table.equals(child.table)) {
                                paths = joinGraph.getPaths(parent.children.get(0).table, child.table);
                                if (paths.size() > 0) {
                                    allPaths = listBestPaths(joinGraph, paths, allPaths);
                                }
                            }
                        }
                    }
                    allPaths = child.savePaths(joinGraph, child, child.table, allPaths);

                } else {
                    allPaths = child.savePaths(joinGraph, child, parentTable, allPaths);
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
                                                allPaths = listBestPaths(joinGraph, paths, allPaths);
                                            }
                                        }
                                    } else {
                                        if (paths.size() > 0) {
                                            allPaths = listBestPaths(joinGraph, paths, allPaths);
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

    public HashMap<Integer, Set<ForeignKey>> listBestPaths(Graph joinGraph, List<Path> paths, HashMap<Integer, Set<ForeignKey>> allPaths) {
        HashMap<Integer, Set<ForeignKey>> tempAllPaths = new HashMap<>();
        int count = 0;
        for (int i = 0; i < paths.size(); i++) {
            if (allPaths.size() > 0) {
                for (Integer integer : allPaths.keySet()) {
                    HashSet<ForeignKey> temphash = new HashSet<>();
                    temphash.addAll(allPaths.get(integer));
                    temphash.addAll(paths.get(i).getFKs());
                    tempAllPaths.put(count++, temphash);
                }
            } else {
                HashSet<ForeignKey> temphash = new HashSet<>();
                temphash.addAll(paths.get(i).getFKs());
                tempAllPaths.put(count++, temphash);
            }
        }
        return tempAllPaths;
    }

    public ArrayList<Integer> getRanking(HashMap<Integer, Integer> hashMap) {
        List<Map.Entry<Integer, Integer>> hm = hashMapValueSort(hashMap);
        ArrayList<Integer> arrayList = new ArrayList<>();
        String string = Arrays.toString(hm.toArray());
        string = string.replace("[", "");
        string = string.replace("]", "");
        string = string.replace(" ", "");
        List<String> myList = new ArrayList<>(Arrays.asList(string.split(",")));
        for (String s : myList)
            arrayList.add(Integer.parseInt(s.split("=")[0]));
        return arrayList;
    }

    public <K, V extends Comparable<? super V>> List<Map.Entry<K, V>> hashMapValueSort(Map<K, V> map) {
        List<Map.Entry<K, V>> sortedEntries = new ArrayList<>(map.entrySet());
        sortedEntries.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        return sortedEntries;
    }

    public int getRowsNumber(String query) {
        try (ResultSet resultSet = db.executeQuery(query)) {
            resultSet.next();
            String toParse = resultSet.getString("QUERY PLAN");
            return parseOpString(toParse);
        } catch (SQLException e) {
            System.out.println(query);
            System.out.println();
            System.err.println("Query not executed");
        }
        return Integer.parseInt("0");
    }

    public int parseOpString(String row) {
        return Integer.parseInt(row.split(" ")[3].split("=")[1]);
    }

    public List<PatternTree> getChildren() {
        return children;
    }

    public String formatResult() {
        String result = "";
        if (children != null) {
        }
        return result;
    }

    public void testRanking() {
        HashMap<Integer, Integer> hashMap = new HashMap<>();
        hashMap.put(0, 120);
        hashMap.put(1, 7);
        hashMap.put(2, 4000);
        hashMap.put(3, 55);
        ArrayList<Integer> arrayList = getRanking(hashMap);
        System.out.println(arrayList.get(0));
        for (Integer i : arrayList) {
            System.err.println(hashMap.get(i) + "       " + i);
        }
    }
}

//    public List<Formatter> buildFormatter(Formatter parent) {
//        List<Formatter> result = new ArrayList<>();
//        if (treeType == pairType) {
//            result.add(new PairFormatter(parent, label));
//        } else if (treeType == objType) {
//            result.add(new ObjFormatter(parent));
//        } else {
//            result.add(new ArrayFormatter(parent));
//        }
//        if (hasChildren()) {
//            for (PatternTree child : children) {
//                result.addAll(child.buildFormatter(result.get(result.size())));
//            }
//        }
//
//        return result;
//    }