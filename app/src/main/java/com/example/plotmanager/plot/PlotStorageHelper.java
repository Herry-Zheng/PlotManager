package com.example.plotmanager.plot;

import android.content.Context;
import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import com.example.plotmanager.story.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import java.util.*;

public class PlotStorageHelper {

    public static List<PlotNode> loadNodesFromFile(Context context, String storyId) {
        List<PlotNode> list = new ArrayList<>();
        try {
            FileInputStream fis = context.openFileInput(storyId + ".json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            fis.close();

            String json = sb.toString();
            if (json == null || json.isEmpty()) {
                return createFile(context, storyId);
            }

            JSONObject root = new JSONObject(sb.toString());
            JSONArray array = root.getJSONArray("nodes");
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                PlotNode node = new PlotNode();
                node.id = obj.getString("id");
                node.depth = obj.getInt("depth");
                node.y = (float) obj.optDouble("y", 0);
                if (obj.isNull("fatherId")) {
                    node.fatherId = null;
                } else {
                    node.fatherId = obj.getString("fatherId");
                }

                node.childrenId = jsonArrayToList(obj.getJSONArray("childrenId"));

                if (obj.isNull("brotherId")) {
                    node.brotherId = null;
                } else {
                    node.brotherId = obj.getString("brotherId");
                }
                node.title = obj.optString("title", "");
                node.characters = obj.optString("characters","");
                node.factions = obj.optString("factions","");
                node.plot = obj.optString("plot", "");
                node.type = obj.optString("type", "");
                node.color = obj.optString("color", "#808080");
                node.causalLineName = obj.optString("causalLineName", "");
                list.add(node);

                boolean hasGhostRoot = !list.isEmpty()
                        && "ghost".equals(list.get(0).type)
                        && (list.get(0).fatherId == null || list.get(0).fatherId.isEmpty());
                if (!hasGhostRoot) {
                    // 没有幽灵根节点 → 该文件不被支持 → 抛出异常
                    throw new IllegalStateException("Invalid story file: missing ghost root node. File may be corrupted or imported from an unsupported source.");
                }
            }
        } catch (FileNotFoundException e) {
            return createFile(context, storyId);

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // 保存节点列表到文件
    public static void saveNodesToFile(Context context, String storyId, List<PlotNode> nodeList) {
        try {
            JSONArray array = new JSONArray();
            for (PlotNode n : nodeList) {
                JSONObject obj = new JSONObject();
                obj.put("id", n.id);
                obj.put("depth", n.depth);
                obj.put("y", n.y);
                obj.put("fatherId", n.fatherId != null ? n.fatherId : JSONObject.NULL);
                obj.put("childrenId",new JSONArray(n.childrenId));
                obj.put("brotherId", n.brotherId != null ? n.brotherId : JSONObject.NULL);
                obj.put("title", n.title);
                obj.put("characters", n.characters);
                obj.put("factions", n.factions);
                obj.put("plot", n.plot);
                obj.put("type", n.type);
                obj.put("color", n.color);
                obj.put("causalLineName", n.causalLineName != null ? n.causalLineName : JSONObject.NULL);
                array.put(obj);
            }
            JSONObject root = new JSONObject();
            root.put("nodes", array);
            FileOutputStream fos = context.openFileOutput(storyId + ".json", Context.MODE_PRIVATE);
            fos.write(root.toString().getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int countLeaf(String nodeId,
                                        Map<String, PlotNode> idMap,
                                        Map<String, Integer> leafCountMap) {
        PlotNode node = idMap.get(nodeId);
        if (node == null) return 0;

        if (node.childrenId == null || node.childrenId.isEmpty()) {
            leafCountMap.put(nodeId, 1);
            return 1;
        }

        int totalLeaves = 0;
        for (String childId : node.childrenId) {
            totalLeaves += countLeaf(childId, idMap, leafCountMap);
        }

        leafCountMap.put(nodeId, totalLeaves);
        return totalLeaves;
    }

    private static void assignYRecursive(String nodeId,
                                         Map<String, PlotNode> idMap,
                                         Map<String, Integer> leafCountMap) {
        PlotNode node = idMap.get(nodeId);
        if (node == null) return;

        if (node.childrenId == null || node.childrenId.isEmpty()) {
            return;
        }

        float y = node.y + (leafCountMap.get(nodeId)-1) / 2.0f;
        for (String childId : node.childrenId) {
            PlotNode child = idMap.get(childId);
            float width = (leafCountMap.get(childId)-1) / 2.0f;
            child.y = y - width;
            y = child.y - width -1;
            assignYRecursive(childId,idMap,leafCountMap);
        }
    }

    public static void recalculateYCoordinates(List<PlotNode> nodeList) {
        if (nodeList.isEmpty()) return;
        Map<String, PlotNode> idMap = new HashMap<>();
        for (PlotNode n : nodeList) idMap.put(n.id, n);

        PlotNode root = nodeList.get(0);
        root.y = 0;

        Map<String, Integer> leafCountMap = new HashMap<>();
        countLeaf(root.id,idMap,leafCountMap);
        assignYRecursive(root.id,idMap,leafCountMap);
    }

    private static List<String> jsonArrayToList(JSONArray array) {
        List<String> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) list.add(array.optString(i));
        return list;
    }

    private static List<PlotNode> createFile(Context context, String storyId) {
        PlotNode ghost = new PlotNode();
        ghost.id = "ghost_root_" + storyId;
        ghost.depth = 0;
        ghost.y = 0;
        ghost.fatherId = null;
        ghost.childrenId = new ArrayList<>();
        ghost.type = "ghost";

        List<PlotNode> list = new ArrayList<>();
        list.add(ghost);
        saveNodesToFile(context, storyId, list);
        return list;
    }

    public static void handleInvalidStoryFile(AppCompatActivity activity, String storyId) {
        new AlertDialog.Builder(activity)
                .setTitle("文件格式错误")
                .setMessage("该大纲文件格式不支持,即将返回主页面。")
                .setCancelable(false)
                .setPositiveButton("确定", (dialog, which) -> {
                    Intent intent = new Intent(activity, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(intent);
                    activity.finish();
                })
                .show();
    }

    public static void cleanGhostChain(List<PlotNode> toRemove,String nodeId,Map<String,PlotNode> idMap){
        if (nodeId.startsWith("ghost_root_")){
            return;
        }
        PlotNode father = idMap.get(nodeId);
        if (father.type.equals("ghost") && (father.childrenId == null || father.childrenId.isEmpty())) {
            toRemove.add(father);
            PlotNode grandfather = idMap.get(father.fatherId);
            grandfather.childrenId.remove(father.id);
            cleanGhostChain(toRemove,father.fatherId,idMap);
        }
    }

    public static void renewChildDepth(String nodeId,Map<String,PlotNode> idMap,Integer fatherDepth){
        PlotNode node = idMap.get(nodeId);
        node.depth = fatherDepth + 1;
        if (node.childrenId != null && !node.childrenId.isEmpty()) {
            for (String childId:node.childrenId) {
                renewChildDepth(childId,idMap,node.depth);
            }
        }
    }

    public static String generateNodeId(List<PlotNode> nodeList) {
        int maxNum = 0;
        for (PlotNode n : nodeList) {
            if (n.id.startsWith("node_")) {
                try {
                    int num = Integer.parseInt(n.id.substring(5));
                    if (num > maxNum) maxNum = num;
                } catch (Exception e) { /* ignore */ }
            }
        }
        return String.format("node_%d", maxNum + 1);
    }
}