package com.example.plotmanager.core.storage;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import com.example.plotmanager.story.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StorageUtil {
    private static final String FILE_NAME = "storys.json";

    public static List<Story> loadStorys(Context context) {
        List<Story> list = new ArrayList<>();
        try {
            FileInputStream fis = context.openFileInput(FILE_NAME);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            fis.close();
            String json = sb.toString();
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String id = obj.getString("id");
                String title = obj.getString("title");
                String createTime = obj.getString("createTime");
                String editTime = obj.getString("editTime");
                list.add(new Story(id, title, createTime, editTime));
            }
        } catch (Exception e) {
            Log.d("StorageUtil", "读取文件失败，可能首次运行尚无文件");
        }
        return list;
    }

    public static void saveStorys(Context context, List<Story> list) {
        try {
            JSONArray array = new JSONArray();
            for (Story o : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", o.getId());
                obj.put("title", o.getTitle());
                obj.put("createTime", o.getCreateTime());
                obj.put("editTime", o.getEditTime());
                array.put(obj);
            }
            FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
            fos.write(array.toString().getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteStory(Context context, String storyId) {
        context.deleteFile(storyId + ".json");
        List<Story> list = loadStorys(context);
        list.removeIf(s -> s.getId().equals(storyId));
        saveStorys(context, list);
    }

    public static void updateStoryEditTime(Context context, String storyId) {
        List<Story> list = loadStorys(context);
        for (Story story : list) {
            if (story.getId().equals(storyId)) {
                String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(new Date());
                story.setEditTime(now);
                saveStorys(context, list);
                break;
            }
        }
    }
}