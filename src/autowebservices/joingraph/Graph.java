/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autowebservices.joingraph;

import autowebservices.database.DB;
import autowebservices.database.ForeignKey;

import java.sql.SQLException;
import java.util.*;

/**
 * The Graph is the graph of join relationships established by foreign keys. Use
 * this package to create joins.
 *
 * @author Curt
 */
public class Graph {

    Map<String, List<Edge>> fromEdges;
    Map<String, List<Edge>> toEdges;
    PathsTable shortestPaths = null;
//    HashSet<String> stringPaths = new HashSet<>();
//    ArrayList<ArrayList<Integer>> paths = new ArrayList<>();
//    String[] allNodesArray;
//    PathsTable tempPaths = null;

    /*
     * Create an empty join graph
     */
    public Graph() {
        fromEdges = new HashMap();
        toEdges = new HashMap();
    }

    public Graph(DB db) {
        fromEdges = new HashMap();
        toEdges = new HashMap();
        try {
            List<String> tables = db.getTableNames();

            for (String s : tables) {
                this.addTable(s);
            }
            List<ForeignKey> fks = db.buildFKs(tables);
            for (ForeignKey fk : fks) {
                this.addFK(fk);
            }
            this.computeShortestPaths();
        } catch (SQLException e) {
            System.err.println("There was an error getting the metadata: "
                    + e.getMessage());
            e.printStackTrace();
            //} catch (ClassNotFoundException e) {
            //    e.printStackTrace();
        }
    }


    /*
     * Add a node (a Table) to the graph
     */
    public void addTable(String table) {
        if (fromEdges.containsKey(table)) {
            System.err.println("JoinGraph: Adding Table that already exists " + table);
            return;
        }
        fromEdges.put(table, new ArrayList(3));
        toEdges.put(table, new ArrayList(3));
    }

    /*
     * Add a foreign key (an Edge) to the graph
     */
    public void addFK(ForeignKey fk) {
        String fromTable = fk.getFromTable();
        if (!fromEdges.containsKey(fromTable)) {
            addTable(fromTable);
        }
        Edge edge = new Edge(fk);
        fromEdges.get(fromTable).add(edge);
        toEdges.get(fk.getToTable()).add(edge);
    }

    public List<Path> getPaths(String fromTable, String toTable) {
        List<Path> paths = shortestPaths.getPaths(fromTable, toTable);
        if (paths == null) {
            System.err.println("No path between " + fromTable + " and " + toTable);
        }
        return paths;
    }


    /*
     * Compute a table of the shortest paths in the graph
     */
    public void computeShortestPaths() {
        List<Path> newPaths = new ArrayList();

        // Initialize path matrix if needed
        if (shortestPaths == null) {
            // Initialize with paths of length one
            shortestPaths = new PathsTable();

            // Get all the nodes in the graph
            Set<String> allNodes = new HashSet();

            allNodes.addAll(fromEdges.keySet());
            allNodes.addAll(toEdges.keySet());

            // Iterate through all of the nodes
            for (String table : allNodes) {
                // Get all of the edges to and from this node
                List<Edge> edges = new ArrayList();
                if (fromEdges.containsKey(table)) {
                    edges.addAll(fromEdges.get(table));
                }

                // Iterate through all of the edges
                for (Edge edge : edges) {
                    ForeignKey fk = edge.getForeignKey();
                    String toTable = fk.getToTable();
                    Path path = new Path();
                    path.push(fk);
                    shortestPaths.addPath(table, toTable, path);
                    newPaths.add(path);
                }
            }
        }

        // Compute the shortest paths
        computeShortestPaths(newPaths);
    }



public void computeShortestPaths(List<Path> newPaths) {

        // Done if there are no new paths
        if (newPaths.size() == 0) {
            return;
        }

        // Paths that we computer in this round
        List<Path> computedPaths = new ArrayList();

        for (Path path : newPaths) {
            String pathStartTable = path.getStart();
            String pathEndTable = path.getEnd();

            //Try the From edges first
            if (fromEdges.containsKey(pathEndTable)) {
                List<Edge> edges = fromEdges.get(pathEndTable);
                for (Edge edge : edges) {
                    String possibleEndTable = edge.getForeignKey().getToTable();
                    if (!shortestPaths.contains(pathStartTable, possibleEndTable)) {

                        // Found a new edge
                        Path tempPath = new Path(path);
                        tempPath.push(edge.getForeignKey());
                        computedPaths.add(tempPath);
                    }
                }
            }
        }
        for (Path path : computedPaths) {
            shortestPaths.addPath(path);
        }

        computeShortestPaths(computedPaths);

    }

}

//    public void computeBestPaths() {
//        List<Path> newPaths = new ArrayList();
//        if (shortestPaths == null) {
//            // Initialize with paths of length one
//            shortestPaths = new PathsTable();
//
//            // Get all the nodes in the graph
//            Set<String> allNodes = new HashSet();
//            allNodes.addAll(fromEdges.keySet());
//            allNodes.addAll(toEdges.keySet());
//            allNodesArray = new String[allNodes.size()];
//            allNodes.toArray(allNodesArray);
//
//            ArrayList<Edge>[] adjacencyList = new ArrayList[allNodes.size()];
//
////            initialize all array lists
//            for (int i = 0; i < allNodes.size(); i++) {
//                adjacencyList[i] = new ArrayList<Edge>();
//            }
//
//            // Iterate through all of the nodes
//            for (String table : allNodes) {
//                if (fromEdges.containsKey(table)) {
//                    if (fromEdges.get(table).size() != 0) {
//                        adjacencyList[getIndex(allNodesArray, fromEdges.get(table).get(0).getForeignKey().getFromTable())].addAll(fromEdges.get(table));
//                    }
//                }
//
//                // Get all of the edges to and from this node
//                List<Edge> edges = new ArrayList<>();
//
//                if (fromEdges.containsKey(table))
//                    edges.addAll(fromEdges.get(table));
//
////                    // Print table connections
////                    if (fromEdges.get(table).size() != 0) {
////                        System.out.print(fromEdges.get(table).get(0).getForeignKey().getFromTable() + "     ");
////                        for (int i = 0; i < fromEdges.get(table).size(); i++) {
////                            System.err.print(fromEdges.get(table).get(i).getForeignKey().getToTable() + ", ");
////                        }
////                    }
////                }
//
//                for (Edge edge : edges) {
//                    ForeignKey fk = edge.getForeignKey();
//                    String toTable = fk.getToTable();
//                    Path path = new Path();
//                    path.push(fk);
//                    shortestPaths.addPath(table, toTable, path);
//                    newPaths.add(path);
//                }
//            }
//
//            for (String node1 : allNodesArray) {
//                for (String node2 : allNodesArray) {
//                    if (!node1.equals(node2))
//                        AllPaths(getIndex(allNodesArray, node1), getIndex(allNodesArray, node2), allNodesArray.length, adjacencyList, allNodesArray);
//                }
//            }
//        }
//
//        Iterator<String> it = stringPaths.iterator();
//        while (it.hasNext()) {
//            String temp = it.next();
//            String replace = temp.replace("[", "");
//            String replace1 = replace.replace("]", "");
//            replace1 = replace1.replace(" ", "");
//            ArrayList<String> stringList = new ArrayList<>(Arrays.asList(replace1.split(",")));
//            ArrayList<Integer> intList = getIntegerArray(stringList);
//            paths.add(intList);
//        }
//
//        generatePaths();
////        System.out.println(paths.size());
////        System.err.println(shortestPaths.getPaths("employees", "customers").get(0).getStart() + "        " +  shortestPaths.getPaths("employees", "customers").get(0).getEnd() + "        " + shortestPaths.getPaths("employees", "customers").get(0).getFKs().get(0).getFromTable() + "        " + shortestPaths.getPaths("employees", "customers").get(0).getFKs().get(0).getToTable() + "        " + shortestPaths.getPaths("employees", "customers").get(0).getFKs().get(1).getFromTable() + "        " +shortestPaths.getPaths("employees", "customers").get(0).getFKs().get(1).getToTable());
//    }
//
//
//    public void generatePaths() {
//        tempPaths = new PathsTable();
//        Path path2 = new Path();
//        for (int i = 0; i < paths.size(); i++) {
//            Path path1 = new Path();
//            ArrayList<Integer> path = paths.get(i);
//            if (path.size() > 2) {
//                for (int j = 1; j < path.size(); j++) {
//                    int t1 = path.get(j - 1);
//                    int t2 = path.get(j);
//                    String table1 = allNodesArray[t1];
//                    String table2 = allNodesArray[t2];
//                    ForeignKey foreignKey = shortestPaths.getPaths(table1, table2).get(0).getFKs().get(0);
//                    path1.push(foreignKey);
//                }
//                tempPaths.addPath(path1);
//                path2 = path1;
//
////            if (path1 == null)
////                System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
////            System.out.println(path1.getStart() + "     " + path1.getEnd());
////            System.out.println();
////            System.out.println(path1.getFKs());
////            System.out.println(path);
////            if (path1.getStart().equals(path1.getEnd()))
////                System.out.println(path1.getStart() + "     " + path1.getEnd() + "      " + path1.getFKs() + "      " + path1.getFKs().get(0).getFromTable() + "      " + path1.getFKs().get(0).getToTable() + "      " + path1.getFKs().get(1).getFromTable() + "      " + path1.getFKs().get(1).getToTable());
////            tempPaths.addPath(path1);
//            }
//        }
//        tempPaths.addPath("employees", "customers", path2);
////        System.err.println(tempPaths.getPaths("employees", "customers").get(1).getEnd());
//    }
//
//    /*
//     * Generating all paths in a graph using Depth First Search
//     */
//    public void AllPaths(int source, int destination, int v, ArrayList<Edge>[] adjList, String[] nodeArray) {
//        if (source != destination) {
//            boolean[] isVisited = new boolean[v];
//            ArrayList<Integer> pathList = new ArrayList<>();
//            pathList.add(source);
////        System.out.println("\nThe paths are -");
//            AllPathsUtil(source, destination, isVisited, pathList, adjList, nodeArray);
//        }
//    }
//
//    private void AllPathsUtil(Integer u, Integer destination,
//                              boolean[] isVisited,
//                              ArrayList<Integer> localPathList,
//                              ArrayList<Edge>[] adjList,
//                              String[] nodeArray) {
//
//
//        isVisited[u] = true;
//        if (u.equals(destination)) {
//            stringPaths.add(localPathList.toString());
////            System.err.println(localPathList);
////            System.out.println(paths);
//            isVisited[u] = false;
//            return;
//        }
//
//        for (Edge e : adjList[u]) {
//            int i = getIndex(nodeArray, e.getForeignKey().getToTable());
//            if (!isVisited[i]) {
//                localPathList.add(i);
//                AllPathsUtil(i, destination, isVisited, localPathList, adjList, nodeArray);
//                for (int a = 0; a < localPathList.size(); a++) {
//                    if (localPathList.get(a) == i)
//                        localPathList.remove(a);
//                }
//            }
//        }
//        isVisited[u] = false;
//    }
//
//
//    public ArrayList<Integer> getIntegerArray(ArrayList<String> stringArray) {
//        ArrayList<Integer> result = new ArrayList<>();
//        for (String stringValue : stringArray) {
//            result.add(Integer.parseInt(stringValue));
//        }
//        return result;
//    }
//
//
//    public int getIndex(String[] array, String table) {
//        for (int i = 0; i < array.length; i++) {
//            if (array[i].equals(table))
//                return i;
//        }
//        return Integer.parseInt(null);
//    }
//
//
//    public void computePathsInTree(PatternTree tree, Set<String> labelsSoFar) {
//        //Recursively descend PatternTree to figure out paths.
//        String label = tree.getValue();
//        labelsSoFar.add(label);
//
//        if (tree.hasChildren()) {
//
//        }
//    }