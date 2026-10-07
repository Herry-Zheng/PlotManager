package com.example.plotmanager.plot;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.plotmanager.R;
import com.example.plotmanager.core.storage.StorageUtil;

import java.util.*;

public class EditPlotActivity extends AppCompatActivity {

    // UI 组件
    private EditText etTitle, etSeq, etCharacter, etFaction, etPlot, etColor, etCausalLine;
    private TextView tvTitle;
    private View llSuccessor,llPredecessor, tilCausalLine;
    private Button btnAddSuccessor,btnChangePredecessor;
    private TextView tvCancel, tvConfirm;

    // 数据
    private String selectedFatherId = null;
    private String storyId,originalType;
    private List<PlotNode> allNodes = new ArrayList<>(); // 从文件读取的所有节点

    // 编辑模式
    private String editingNodeId,presetFatherId;
    private boolean isEditMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_plot);

        storyId = getIntent().getStringExtra("story_id");
        //如果传了编辑节点id说明是为了编辑已有节点来的
        editingNodeId = getIntent().getStringExtra("node_id");
        isEditMode = (editingNodeId != null);
        //如果传了父亲节点id说明是从编辑模式的创建后继节点来的
        presetFatherId = getIntent().getStringExtra("preset_father_id");
        selectedFatherId = presetFatherId;
        if (presetFatherId != null) {
            etSeq.setEnabled(false);
        }

        if (storyId == null) {
            Toast.makeText(this, "错误：未传story_id", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupListeners();
        try {
            allNodes = PlotStorageHelper.loadNodesFromFile(this, storyId);
        } catch (IllegalStateException e) {
            PlotStorageHelper.handleInvalidStoryFile(this, storyId);
            return;
        }

        if (isEditMode) {
            loadNodeForEditing(allNodes);
            tvTitle.setText("节点信息");
        } else {
            tvTitle.setText("创建新节点");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isEditMode) {
            allNodes = PlotStorageHelper.loadNodesFromFile(this, storyId);
            loadNodeForEditing(allNodes);
        }
    }

    private void initViews() {
        etTitle = findViewById(R.id.et_title);
        etSeq = findViewById(R.id.et_seq);
        etCharacter = findViewById(R.id.et_characters);
        etFaction = findViewById(R.id.et_factions);
        etPlot = findViewById(R.id.et_plot);
        etColor = findViewById(R.id.et_color);
        etCausalLine = findViewById(R.id.et_causal_line);
        btnAddSuccessor = findViewById(R.id.btn_add_successor);
        btnChangePredecessor = findViewById(R.id.btn_change_predecessor);
        llSuccessor = findViewById(R.id.ll_successor);
        llPredecessor = findViewById(R.id.ll_predecessor);
        tilCausalLine = findViewById(R.id.til_causal_line);
        tvCancel = findViewById(R.id.tv_cancel);
        tvConfirm = findViewById(R.id.tv_confirm);
        tvTitle = findViewById(R.id.tv_title);
    }

    private void setupListeners() {
        btnAddSuccessor.setOnClickListener(v -> {
            Intent intent = new Intent(EditPlotActivity.this, EditPlotActivity.class);
            intent.putExtra("story_id", storyId);
            intent.putExtra("preset_father_id", editingNodeId);
            startActivity(intent);
        });

        btnChangePredecessor.setOnClickListener(v -> {
            Intent intent = new Intent(EditPlotActivity.this, SelectNewFatherActivity.class);
            intent.putExtra("story_id", storyId);
            intent.putExtra("child_id", editingNodeId);

            startActivity(intent);
        });

        tvCancel.setOnClickListener(v -> finish());
        tvConfirm.setOnClickListener(v -> savePlot(allNodes));
    }

    // 编辑模式：加载节点数据到界面
    private void loadNodeForEditing(List<PlotNode> nodeList) {
        if (!isEditMode) return;
        PlotNode target = null;
        for (PlotNode n : nodeList) {
            if (n.id.equals(editingNodeId)) {
                target = n;
                break;
            }
        }
        if (target == null) {
            Toast.makeText(this, "未找到该节点", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        etTitle.setText(target.title);
        etSeq.setText(String.valueOf(target.depth));
        etSeq.setEnabled(false);
        etPlot.setText(target.plot);
        etColor.setText(target.color);
        etCharacter.setText(target.characters);
        etFaction.setText(target.factions);

        selectedFatherId = target.fatherId;
        originalType = target.type;

        llSuccessor.setVisibility(View.VISIBLE);
        llPredecessor.setVisibility(View.VISIBLE);
        if (target.type.equals("middle")) {
            etCausalLine.setText(target.causalLineName);
            tilCausalLine.setVisibility(View.VISIBLE);
        } else {
            tilCausalLine.setVisibility(View.GONE);
        }
    }

    // 保存（新建或更新）
    private void savePlot(List<PlotNode> nodeList) {
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty() || title.length() > 10) {
            Toast.makeText(this, "标题不能为空且不超过10字", Toast.LENGTH_SHORT).show();
            return;
        }
        String seqStr = etSeq.getText().toString().trim();
        if (seqStr.isEmpty()) {
            Toast.makeText(this, "请输入时间序号", Toast.LENGTH_SHORT).show();
            return;
        }
        int seq;
        try {
            seq = Integer.parseInt(seqStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "序号必须为数字", Toast.LENGTH_SHORT).show();
            return;
        }
        if (seq <= 0) {
            Toast.makeText(this, "序号必须为正整数", Toast.LENGTH_SHORT).show();
            return;
        }
        String characters = etCharacter.getText().toString().trim();
        if (characters.isEmpty()) {
            characters = "";
        }
        String factions = etFaction.getText().toString().trim();
        if (factions.isEmpty()) {
            factions = "";
        }
        String plot = etPlot.getText().toString().trim();
        if (plot.isEmpty()) {
            Toast.makeText(this, "请输入情节简介", Toast.LENGTH_SHORT).show();
            return;
        }
        String color = etColor.getText().toString().trim();
        if (color.isEmpty() || !color.startsWith("#")) {
            Toast.makeText(this, "请输入有效颜色如 #FF5722", Toast.LENGTH_SHORT).show();
            return;
        }
        String causalLineName = etCausalLine.getText().toString().trim();

        PlotNode newNode = new PlotNode();
        newNode.id = isEditMode ? editingNodeId : PlotStorageHelper.generateNodeId(nodeList);
        newNode.depth = seq;
        newNode.y = 0; // 之后再计算
        newNode.fatherId = selectedFatherId;

        newNode.title = title;
        newNode.characters = characters;
        newNode.factions = factions;
        newNode.plot = plot;
        newNode.color = color;
        newNode.causalLineName = causalLineName;

        if (presetFatherId != null && !presetFatherId.isEmpty()) {
            newNode.type = "middle";
            for (PlotNode n : nodeList) {
                if (n.id.equals(selectedFatherId)) {
                    n.childrenId.add(newNode.id);
                    break;
                }
            }
        } else if (!isEditMode && (presetFatherId == null || presetFatherId.isEmpty())) {
            newNode.type = "origin";
            newNode.fatherId = generateGhostChain(seq,nodeList,newNode.id);
        } else if (isEditMode) {
            newNode.type = originalType;
        }

        if (isEditMode) {
            boolean found = false;
            for (int i = 0; i < nodeList.size(); i++) {
                PlotNode editingNode = nodeList.get(i);
                if (editingNode.id.equals(editingNodeId)) {
                    newNode.childrenId = editingNode.childrenId;
                    newNode.brotherId = editingNode.brotherId;
                    nodeList.set(i, newNode);
                    found = true;
                    break;
                }
            }
            if (!found) {
                Toast.makeText(this, "未找到要更新的节点", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            nodeList.add(newNode);
        }

        PlotStorageHelper.recalculateYCoordinates(nodeList);
        PlotStorageHelper.saveNodesToFile(EditPlotActivity.this,storyId,nodeList);
        Toast.makeText(this, isEditMode ? "节点更新成功" : "节点创建成功", Toast.LENGTH_SHORT).show();
        StorageUtil.updateStoryEditTime(EditPlotActivity.this,storyId);
        finish();
    }

    private String generateGhostChain(int seq,List<PlotNode> nodeList,String childId){
        if (seq == 1) {
            nodeList.get(0).childrenId.add(childId);
            return "ghost_root_" + storyId;
        }
        PlotNode ghost = new PlotNode();
        ghost.id = PlotStorageHelper.generateNodeId(nodeList);
        ghost.depth = seq-1;
        ghost.y = 0;
        ghost.type = "ghost";
        ghost.fatherId = generateGhostChain(ghost.depth,nodeList,ghost.id);
        ghost.childrenId.add(childId);

        nodeList.add(ghost);
        return ghost.id;
    }
}