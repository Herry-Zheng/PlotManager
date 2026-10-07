package com.example.plotmanager.setting;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.plotmanager.R;
import com.example.plotmanager.core.storage.StorageUtil;
import com.example.plotmanager.story.Story;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;

public class SelectExportActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private StoryAdapter adapter;
    private List<Story> storyList;
    private ActivityResultLauncher<String> exportLauncher;
    private String selectedStoryId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_export);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 注册导出文件创建器
        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/json"),
                uri -> {
                    if (uri != null && selectedStoryId != null) {
                        exportSingleStory(uri, selectedStoryId);
                    } else if (uri == null) {
                        Toast.makeText(this, "取消导出", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        storyList = StorageUtil.loadStorys(this);
        if (adapter == null) {
            adapter = new StoryAdapter();
            recyclerView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    private void exportSingleStory(Uri uri, String storyId) {
        try {
            // 读取该大纲的节点数据
            String nodeJson = readStoryFile(storyId);
            JSONObject storyObj;
            if (nodeJson != null) {
                storyObj = new JSONObject(nodeJson); // 包含 "nodes" 数组
            } else {
                // 如果节点文件不存在，则创建空 nodes
                storyObj = new JSONObject();
                storyObj.put("nodes", new JSONArray());
            }

            // 补充元数据
            Story story = findStoryById(storyId);
            if (story != null) {
                storyObj.put("id", story.getId());
                storyObj.put("title", story.getTitle());
                storyObj.put("createTime", story.getCreateTime());
                storyObj.put("editTime", story.getEditTime());
            } else {
                // 若 story 列表异常，用默认值
                storyObj.put("id", storyId);
                storyObj.put("title", "未命名");
                storyObj.put("createTime", getCurrentTime());
                storyObj.put("editTime", getCurrentTime());
            }

            String json = storyObj.toString(2);

            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                if (os != null) {
                    os.write(json.getBytes());
                    os.flush();
                    Toast.makeText(this, "导出成功：" + uri.getLastPathSegment(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "导出失败：无法写入文件", Toast.LENGTH_SHORT).show();
                }
            }
            finish(); // 导出完成后关闭此界面
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "导出异常：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String readStoryFile(String storyId) {
        try {
            FileInputStream fis = openFileInput(storyId + ".json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            fis.close();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private Story findStoryById(String id) {
        for (Story s : storyList) {
            if (s.getId().equals(id)) return s;
        }
        return null;
    }

    private String getCurrentTime() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(new java.util.Date());
    }

    // ---------- Adapter ----------
    class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.MyViewHolder> {

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_story, parent, false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            Story story = storyList.get(position);
            holder.tvTitle.setText(story.getTitle());
            holder.tvCreateTime.setText("创建时间：" + story.getCreateTime());
            holder.tvEditTime.setText("最后修改：" + story.getEditTime());

            holder.itemView.setOnClickListener(v -> {
                selectedStoryId = story.getId();
                String defaultName = story.getTitle() + ".json";
                exportLauncher.launch(defaultName);
            });
        }

        @Override
        public int getItemCount() {
            return storyList.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvCreateTime, tvEditTime;
            public MyViewHolder(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_story_title);
                tvCreateTime = itemView.findViewById(R.id.tv_story_create_time);
                tvEditTime = itemView.findViewById(R.id.tv_story_edit_time);
            }
        }
    }
}