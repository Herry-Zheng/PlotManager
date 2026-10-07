package com.example.plotmanager.story;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.plotmanager.R;
import com.example.plotmanager.core.storage.StorageUtil;
import com.example.plotmanager.setting.*;
import com.example.plotmanager.plot.*;

import java.util.List;

public class MainActivity extends AppCompatActivity implements CreateStoryFragment.OnPlotCreatedListener {

    private RecyclerView recyclerView;
    private StoryAdapter adapter;
    private List<Story> storyList;
    private ImageView ivAdd,ivSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recycler_view);
        ivAdd = findViewById(R.id.iv_add);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        storyList = StorageUtil.loadStorys(this);

        adapter = new StoryAdapter();
        recyclerView.setAdapter(adapter);

        ivAdd.setOnClickListener(v -> {
            CreateStoryFragment fragment = new CreateStoryFragment();
            fragment.show(getSupportFragmentManager(), "create_story");
        });

        ivSettings = findViewById(R.id.iv_settings);
        ivSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshData();
    }

    @Override
    public void onPlotCreated() {
        refreshData();
    }

    private void refreshData() {
        storyList = StorageUtil.loadStorys(this);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

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
                    Intent intent = new Intent(MainActivity.this, ReadStoryActivity.class);
                    intent.putExtra("story_id", story.getId());
                    intent.putExtra("story_title", story.getTitle());
                    startActivity(intent);
            });

            holder.itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("删除大纲")
                        .setMessage("确定要删除 \"" + story.getTitle() + "\" 吗？")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("删除", (dialog, which) -> {
                            StorageUtil.deleteStory(MainActivity.this,story.getId());
                            refreshData();
                        })
                        .show();
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return storyList.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvCreateTime,tvEditTime;
            public MyViewHolder(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_story_title);
                tvCreateTime = itemView.findViewById(R.id.tv_story_create_time);
                tvEditTime = itemView.findViewById(R.id.tv_story_edit_time);
            }
        }
    }
}