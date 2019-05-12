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

    private Map<String, List<Edge>> fromEdges;
    private Map<String, List<Edge>> toEdges;
    private PathsTable shortestPaths = null;
    private ArrayList<Edge>[] adjacencyList;
    private String[] allNodesArray;


    public Graph(DB db) {
        fromEdges = new HashMap<>();
        toEdges = new HashMap<>();
        try {
            List<String> tables = db.getTableNames();
            for (String s : tables) this.addTable(s);
            List<ForeignKey> fks = db.buildFKs();
            for (ForeignKey fk : fks) this.addFK(fk);
            this.computeShortestPaths();
        } catch (SQLException e) {
            System.err.println("There was an error getting the metadata: "
                    + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addTable(String table) {
        if (fromEdges.containsKey(table)) {
            System.err.println("JoinGraph: Adding Table that already exists " + table);
            return;
        }
        fromEdges.put(table, new ArrayList<>(3));
        toEdges.put(table, new ArrayList<>(3));
    }

    private void addFK(ForeignKey fk) {
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

    private void computeShortestPaths() {
        List<Path> newPaths = new ArrayList<>();
        if (shortestPaths == null) {
            shortestPaths = new PathsTable();
            Set<String> allNodes = new HashSet<>();
            allNodes.addAll(fromEdges.keySet());
            allNodes.addAll(toEdges.keySet());
            for (String table : allNodes) {
                List<Edge> edges = new ArrayList<>();
                if (fromEdges.containsKey(table)) {
                    edges.addAll(fromEdges.get(table));
                }
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
        computeShortestPaths(newPaths);
        computeCyclicPaths(newPaths);
    }

    private void computeShortestPaths(List<Path> newPaths) {
        if (newPaths.size() == 0)
            return;
        List<Path> computedPaths = new ArrayList<>();
        for (Path path : newPaths) {
            String pathStartTable = path.getStart();
            String pathEndTable = path.getEnd();
            if (fromEdges.containsKey(pathEndTable)) {
                List<Edge> edges = fromEdges.get(pathEndTable);
                for (Edge edge : edges) {
                    String possibleEndTable = edge.getForeignKey().getToTable();
                    if (!shortestPaths.contains(pathStartTable, possibleEndTable)) {
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

    private void computeCyclicPaths(List<Path> newPaths) {
        if (newPaths.size() == 0)
            return;
        Set<String> allNodes = new HashSet<>();
        allNodes.addAll(fromEdges.keySet());
        allNodes.addAll(toEdges.keySet());
        allNodesArray = new String[allNodes.size()];
        allNodes.toArray(allNodesArray);
        adjacencyList = new ArrayList[allNodes.size()];
        for (int i = 0; i < allNodes.size(); i++) adjacencyList[i] = new ArrayList<>();

        for (String table : allNodes) {
            if (fromEdges.containsKey(table)) {
                if (fromEdges.get(table).size() != 0) {
                    adjacencyList[getIndex(allNodesArray, fromEdges.get(table).get(0).getForeignKey().getFromTable())].addAll(fromEdges.get(table));
                }
            }
        }

        boolean[] visited = new boolean[allNodesArray.length];
        for (int v = 0; v < allNodesArray.length; ++v) {
            if (!visited[v]) {
                DFSUtil(v, visited);
                System.out.println();
            }
        }
    }

    private void DFSUtil(int v, boolean[] visited) {
        visited[v] = true;
        System.out.print(allNodesArray[v] + " ");
        for (Edge x : adjacencyList[v]) {
            int i = getIndex(allNodesArray, x.getForeignKey().getToTable());
            if (!visited[i]) DFSUtil(i, visited);
        }

    }

    private int getIndex(String[] array, String table) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(table))
                return i;
        }
        return -1;
    }
}