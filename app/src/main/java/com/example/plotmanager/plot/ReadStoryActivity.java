package com.example.plotmanager.plot;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.plotmanager.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReadStoryActivity extends AppCompatActivity {

    private TextView tvStoryTitle;
    private TextView tvModeOverview, tvModeHistory;
    private PlotView plotView;
    private RecyclerView recyclerViewList;
    private ImageButton btnAddPlot;
    private ImageView ivSearch;

    private String storyId,storyTitle,keyWord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_story);

        storyId = getIntent().getStringExtra("story_id");
        storyTitle = getIntent().getStringExtra("story_title");
        keyWord = getIntent().getStringExtra("key_word");

        if (keyWord != null) {
            searchNode(keyWord);
        }

        //story曾经叫outline
        if (storyId == null) storyId = getIntent().getStringExtra("outline_id");
        if (storyTitle == null) storyTitle = getIntent().getStringExtra("outline_title");

        tvStoryTitle = findViewById(R.id.tv_story_title);
        tvModeOverview = findViewById(R.id.tv_mode_overview);
        tvModeHistory = findViewById(R.id.tv_mode_history);
        plotView = findViewById(R.id.plot_view);
        btnAddPlot = findViewById(R.id.btn_add_plot);
        ivSearch = findViewById(R.id.iv_search);
        recyclerViewList = findViewById(R.id.recycler_view_list);

        tvStoryTitle.setText(keyWord != null ? "搜索结果：" + keyWord : storyTitle);

        // 短按：进入编辑
        plotView.setOnNodeClickListener(nodeId -> {
            Intent intent = new Intent(ReadStoryActivity.this, EditPlotActivity.class);
            intent.putExtra("story_id", storyId);
            intent.putExtra("node_id", nodeId);
            startActivity(intent);
        });

        // 长按：弹出删除确认
        plotView.setOnNodeLongClickListener(nodeId -> {
            // 获取节点标题
            String title = getNodeTitle(nodeId);
            new AlertDialog.Builder(this)
                    .setTitle("删除节点 \"" + title)
                    .setMessage("你将如何处理其子节点？")
                    .setPositiveButton("保留位置", (dialog, which) -> deleteNode(nodeId,false))
                    .setNegativeButton("向前嫁接",(dialog,which) -> deleteNode(nodeId,true))
                    .setNeutralButton("取消", null)
                    .show();
        });

        // 加载数据
        if (storyId != null) {
            plotView.loadStoryData(storyId);
        } else {
            Toast.makeText(this, "错误：未传story_id", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ivSearch.setOnClickListener(v -> {
            SearchFragment fragment = new SearchFragment();
            fragment.show(getSupportFragmentManager(), "search_plot");
        });

        // 模式切换
        tvModeOverview.setSelected(true);
        tvModeHistory.setSelected(false);
        plotView.setVisibility(View.VISIBLE);
        recyclerViewList.setVisibility(View.GONE);

        tvModeOverview.setOnClickListener(v -> {
            tvModeOverview.setSelected(true);
            tvModeHistory.setSelected(false);
            plotView.setVisibility(View.VISIBLE);
            recyclerViewList.setVisibility(View.GONE);
            if (storyId != null) plotView.loadStoryData(storyId);
        });

        tvModeHistory.setOnClickListener(v -> {
            tvModeHistory.setSelected(true);
            tvModeOverview.setSelected(false);
            plotView.setVisibility(View.GONE);
            recyclerViewList.setVisibility(View.VISIBLE);
            // TODO: 实现列表模式
        });

        // 添加节点按钮
        btnAddPlot.setOnClickListener(v -> {
            Intent intent = new Intent(ReadStoryActivity.this, EditPlotActivity.class);
            intent.putExtra("story_id", storyId);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (storyId != null && plotView.getVisibility() == View.VISIBLE) {
            if (keyWord == null) {
                plotView.setNodeVisibility(null);
            }
            plotView.loadStoryData(storyId);
        }
    }

    private void deleteNode(String nodeId, boolean moveChild) {
        List<PlotNode> nodeList;
        try {
            nodeList = PlotStorageHelper.loadNodesFromFile(this, storyId);
        } catch (IllegalStateException e) {
            PlotStorageHelper.handleInvalidStoryFile(this, storyId);
            return;
        }

        if (nodeList.isEmpty()) return;

        Map<String, PlotNode> idMap = new HashMap<>() ;
        for (PlotNode n : nodeList) idMap.put(n.id, n);
        List<PlotNode> toRemove = new ArrayList<>();
        PlotNode delettingNode = idMap.get(nodeId);
        if (moveChild || (delettingNode.childrenId == null || delettingNode.childrenId.isEmpty())) {
            toRemove.add(delettingNode);
            PlotNode father = idMap.get(delettingNode.fatherId);
            if (moveChild && !(delettingNode.childrenId == null || delettingNode.childrenId.isEmpty())) {
                int index = father.childrenId.indexOf(nodeId);
                for (String childId:delettingNode.childrenId) {
                    PlotNode child = idMap.get(childId);
                    father.childrenId.add(index,childId);
                    index += 1;
                    child.fatherId = father.id;
                    PlotStorageHelper.renewChildDepth(childId,idMap, father.depth);
                }
            }
            father.childrenId.remove(delettingNode.id);
            PlotStorageHelper.cleanGhostChain(toRemove,father.id,idMap);
        }
        else {
            delettingNode.type = "ghost";
            delettingNode.characters = null;
            delettingNode.factions = null;
            delettingNode.plot = null;
            delettingNode.color = null;
            delettingNode.title = null;
        }
        if (!toRemove.isEmpty()) {
            for (PlotNode n:toRemove) {
                nodeList.remove(n);
            }
        }

        PlotStorageHelper.saveNodesToFile(this, storyId, nodeList);
        plotView.loadStoryData(storyId);
        Toast.makeText(this, "节点已删除", Toast.LENGTH_SHORT).show();
    }

    private String getNodeTitle(String nodeId) {
        List<PlotNode> nodeList = PlotStorageHelper.loadNodesFromFile(this, storyId);
        for (PlotNode n : nodeList) {
            if (n.id.equals(nodeId)) return n.title;
        }
        return "未命名";
    }

    private void searchNode(String keyWord){
        List<PlotNode> nodeList = PlotStorageHelper.loadNodesFromFile(this,storyId);
        Set<String> resultId = new HashSet<>();
        for (PlotNode n : nodeList) {
            if (n.type.equals("ghost")) {
                continue;
            }
            if (n.title.contains(keyWord)) {
                resultId.add(n.id);
                continue;
            }
            if (n.characters.contains(keyWord)) {
                resultId.add(n.id);
                continue;
            }
            if(n.factions.contains(keyWord)) {
                resultId.add(n.id);
                continue;
            }
            if (n.plot.contains(keyWord)) {
                resultId.add(n.id);
            }
        }
        plotView.setNodeVisibility(resultId);
    }
}