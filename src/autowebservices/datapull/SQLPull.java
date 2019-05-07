/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autowebservices.datapull;

import autowebservices.database.ForeignKey;
import autowebservices.joingraph.Graph;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.util.*;

/**
 * @author Curtis Dyreson
 */
public class SQLPull {

    public String generateQuery(Graph graph, Set<ForeignKey> result, String columns, List<String> tableList) {
        Set<String> tablesSet = new HashSet();
        if (result != null) {
            for (ForeignKey fk : result) {
                tablesSet.add(fk.getFromTable());
                tablesSet.add(fk.getToTable());
            }
        }

        for (String table : tableList) {
            tablesSet.add(table);
        }

        String tables = tablesSet.toString();
        tables = tables.replace("[", "");
        tables = tables.replace("]", "");
        columns = columns.replace("[", "");
        columns = columns.replace("]", "");
        tables = tables.replaceAll(" ", "");
        String[] tableArr = tables.split(",");
        String[] orderOfTables = columns.replaceAll(" ", "").split(",");
        for (int i = 0; i < orderOfTables.length; i++) orderOfTables[i] = orderOfTables[i].split("\\.")[0];
        HashSet<String> tableCanBeUsed = new HashSet<>();
        tableCanBeUsed.add(tableArr[0]);
        ArrayList<String> listOfUniqueFkConditions = new ArrayList<>();
        HashSet<String> uniqueFkConditions = new HashSet<>();
        String leftjoin = "FROM " + tableArr[0] + " ";

        if (result != null) {
            for (ForeignKey fk : result) {
                ArrayList<String> arrayList = new ArrayList<>();
                arrayList.add(fk.generateJoinCondition().split("=")[0].replaceAll(" ", ""));
                arrayList.add(fk.generateJoinCondition().split("=")[1].replaceAll(" ", ""));
                Collections.sort(arrayList);
                if (uniqueFkConditions.add(arrayList.toString())) {
                    listOfUniqueFkConditions.add(fk.generateJoinCondition());
                }
            }
        }

        while (listOfUniqueFkConditions.size() != 0) {
            leftjoin += "\n";
            String fkCondition = addNextCondition(tableCanBeUsed, listOfUniqueFkConditions);
            if (fkCondition == null) {
                String table = listOfUniqueFkConditions.get(0).replaceAll(" ", "").split("=")[0].split("\\.")[0];
                tableCanBeUsed.add(table);
                leftjoin += ", " + table + " ";
            } else {
                String str1 = fkCondition.replaceAll(" ", "").split("=")[0].split("\\.")[0];
                String str2 = fkCondition.replaceAll(" ", "").split("=")[1].split("\\.")[0];
                if (!tableCanBeUsed.contains(str1)) {
                    leftjoin = leftjoin + "LEFT JOIN " + str1;
                    tableCanBeUsed.add(str1);
                } else {
                    leftjoin = leftjoin + "LEFT JOIN " + str2;
                    tableCanBeUsed.add(str2);
                }
                leftjoin = leftjoin + " ON " + fkCondition;
            }
        }

        String leftOutTables = "";
        for (String str : tableArr) {
            if (!tableCanBeUsed.contains(str)) {
                leftOutTables += str + ", ";
                tableCanBeUsed.add(str);
            }
        }

        if (!leftOutTables.equals(""))
            leftjoin = "FROM " + leftOutTables + leftjoin.split("FROM ")[1];

        String s = "";
        s = "SELECT DISTINCT " + columns + "\n" + s;
        s += leftjoin;
        s += "\nORDER BY " + columns;
        return s;
    }


    public String generateRowsQuery(Graph graph, Set<ForeignKey> result, String columns, List<String> tableList) {
        return "EXPLAIN " + generateQuery(graph, result, columns, tableList);
    }

    public String addNextCondition(HashSet<String> tableCanBeUsed, ArrayList<String> listOfUniqueFkConditions) {
        for (String str : listOfUniqueFkConditions) {
            int count1 = 0;
            int count2 = 0;
            String[] orderOfTables = str.replaceAll(" ", "").split("=");
            for (int i = 0; i < orderOfTables.length; i++) orderOfTables[i] = orderOfTables[i].split("\\.")[0];
            if (tableCanBeUsed.contains(orderOfTables[0])) count1++;
            if (tableCanBeUsed.contains(orderOfTables[1])) count1++;
            if (!tableCanBeUsed.contains(orderOfTables[0])) count2++;
            if (!tableCanBeUsed.contains(orderOfTables[1])) count2++;
            if (count1 == 1 && count2 == 1) {
                listOfUniqueFkConditions.remove(str);
                return str;
            }
        }
        return null;
    }

    public String readAllBytes(String filePath) {
        String content = "";
        try {
            content = new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    public int getCountForValues(String filePath) {
        String str = readAllBytes(filePath);
        str = str.replaceAll("\\s+", "");
        String findStr = "\":\"";
        int lastIndex = 0;
        int count = 0;
        while (lastIndex != -1) {
            lastIndex = str.indexOf(findStr, lastIndex);
            if (lastIndex != -1) {
                count++;
                lastIndex += findStr.length();
            }
        }
        return count;
    }

    public JSONArray convertToJSON(ResultSet resultSet) {
        try {
            JSONArray jsonArray = new JSONArray();
            while (resultSet.next()) {
                int total_rows = resultSet.getMetaData().getColumnCount();
                for (int i = 0; i < total_rows; i++) {
                    JSONObject obj = new JSONObject();
                    obj.put(resultSet.getMetaData().getColumnLabel(i + 1)
                            .toLowerCase(), resultSet.getObject(i + 1));
                    jsonArray.put(obj);
                }
            }
            return jsonArray;
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public String fillNested(String filePath, String[] fillArray, int count) {
        StringBuilder newSchema = generateFillableSchema(filePath);
        int num = 0;
        String[] fillObj = new String[count];
        StringBuilder finalOut = new StringBuilder();
        finalOut.append("[");
        for (int i = 0; i < fillArray.length; i++) {
            fillObj[num++] = fillArray[i];
            if (num == count) {
                finalOut.append(fillObject(newSchema.toString(), fillObj)).append(",");
                num = 0;
            }
        }
        finalOut.append("]");
        finalOut.deleteCharAt(finalOut.length() - 2);
        return finalOut.toString();
    }

    public String fillObject(String schema, String[] fillObject) {
        for (String s : fillObject)
            schema = schema.replaceFirst("\"\"", s);
        return schema;
    }

    public StringBuilder generateFillableSchema(String filePath) {
        String schema = readAllBytes(filePath);
        schema = schema.replaceAll("\\s+", "");
        String[] str = schema.split("");
        for (int i = 0; i < str.length; i++) {
            if (str[i].equals("\"") && str[i - 1].equals(":")) {
                if (str[i - 2].equals("\"")) {
                    i++;
                    while (!str[i].equals("\"")) {
                        str[i++] = "";
                    }
                }
            }
        }
        StringBuilder newSchema = new StringBuilder();
        for (String s : str) newSchema.append(s);
        return newSchema;
    }
}
