package com.example.plotmanager.setting;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import com.example.plotmanager.R;
import com.example.plotmanager.core.storage.StorageUtil;
import com.example.plotmanager.story.Story;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

public class SettingActivity extends AppCompatActivity {

    private Button btnExport, btnImport;
    private ActivityResultLauncher<String[]> importLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        btnExport = findViewById(R.id.btn_export);
        btnImport = findViewById(R.id.btn_import);

        // 导入文件选择器
        importLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        importSingleStory(uri);
                    }
                }
        );

        btnExport.setOnClickListener(v -> {
            // 跳转到 SelectExportActivity
            startActivity(new Intent(SettingActivity.this, SelectExportActivity.class));
        });

        btnImport.setOnClickListener(v -> {
            importLauncher.launch(new String[]{"application/json"});
        });
    }

    private void importSingleStory(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) {
                Toast.makeText(this, "无法打开文件", Toast.LENGTH_SHORT).show();
                return;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            is.close();

            JSONObject root = new JSONObject(sb.toString());
            String id = root.optString("id");
            if (id.isEmpty()) {
                String fileName = uri.getLastPathSegment();
                if (fileName != null && fileName.contains(".")) {
                    id = fileName.substring(0, fileName.lastIndexOf('.'));
                } else {
                    id = UUID.randomUUID().toString();
                }
            }
            String title = root.optString("title", "导入大纲");
            String createTime = root.optString("createTime", getCurrentTime());
            String editTime = root.optString("editTime", getCurrentTime());
            JSONArray nodesArray = root.optJSONArray("nodes");
            if (nodesArray == null) nodesArray = new JSONArray();

            List<Story> existingStorys = StorageUtil.loadStorys(this);
            Story found = null;
            for (Story s : existingStorys) {
                if (s.getId().equals(id)) {
                    found = s;
                    break;
                }
            }
            final Story existing = found;   // 关键：转为 final

            final String finalid = id;
            final JSONArray finalnodesArray = nodesArray;
            if (existing != null) {
                new AlertDialog.Builder(this)
                        .setTitle("覆盖确认")
                        .setMessage("已存在同名大纲 \"" + existing.getTitle() + "\"，是否用导入的数据覆盖？")
                        .setPositiveButton("覆盖", (dialog, which) -> {
                            existing.setTitle(title);
                            existing.setCreateTime(createTime);
                            existing.setEditTime(getCurrentTime());
                            saveStoryFile(finalid, finalnodesArray);
                            StorageUtil.saveStorys(this, existingStorys);
                            Toast.makeText(this, "已覆盖：" + title, Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("取消", null)
                        .show();
            } else {
                Story newStory = new Story(id, title, createTime, getCurrentTime());
                existingStorys.add(newStory);
                saveStoryFile(id, nodesArray);
                StorageUtil.saveStorys(this, existingStorys);
                Toast.makeText(this, "导入成功，新增大纲：" + title, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "导入异常：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 保存节点文件（只存 nodes 数组）
    private void saveStoryFile(String storyId, JSONArray nodesArray) {
        try {
            JSONObject root = new JSONObject();
            root.put("nodes", nodesArray);
            java.io.FileOutputStream fos = openFileOutput(storyId + ".json", MODE_PRIVATE);
            fos.write(root.toString().getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getCurrentTime() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(new java.util.Date());
    }
}