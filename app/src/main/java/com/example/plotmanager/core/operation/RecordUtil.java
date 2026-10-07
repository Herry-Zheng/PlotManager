package com.example.plotmanager.core.operation;

import android.content.Context;
import android.util.Log;

import com.example.plotmanager.plot.PlotNode;
import com.example.plotmanager.story.Story;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class RecordUtil {
    public static List<Operation> loadOptions(Context context, String storyId) {
        List<Operation> list = new ArrayList<>();
        try {
            FileInputStream fis = context.openFileInput(storyId + ".json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            fis.close();

            JSONObject root = new JSONObject(sb.toString());
            JSONArray array = root.getJSONArray("operations");
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

    public static void saveOptions() {

    }

    private static List<String> jsonArrayToList(JSONArray array) {
        List<String> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) list.add(array.optString(i));
        return list;
    }

}
