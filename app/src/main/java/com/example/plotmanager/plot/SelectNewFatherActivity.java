package com.example.plotmanager.plot;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.example.plotmanager.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectNewFatherActivity extends AppCompatActivity {

    private TextView tvStoryTitle;
    private PlotView plotView;
    private String storyId,childId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_new_father);

        storyId = getIntent().getStringExtra("story_id");
        childId = getIntent().getStringExtra("child_id");

        tvStoryTitle = findViewById(R.id.tv_story_title);
        plotView = findViewById(R.id.plot_view);
        plotView.loadStoryData(storyId);

        tvStoryTitle.setText("选择新前驱");

        List<PlotNode> nodeList = PlotStorageHelper.loadNodesFromFile(SelectNewFatherActivity.this,storyId);
        Map<String, PlotNode> idMap = new HashMap<>();
        for (PlotNode n : nodeList) idMap.put(n.id, n);
        String oldFatherId = idMap.get(childId).fatherId;

        plotView.setOnNodeClickListener(nodeId -> {
            String title = getNodeTitle(nodeList,nodeId);
            new AlertDialog.Builder(this)
                    .setTitle("选择新前驱")
                    .setMessage("确定要将节点 \"" + title + "\" 设为新的前驱吗？")
                    .setPositiveButton("确定", (dialog, which) -> setFather(nodeList,childId,nodeId,oldFatherId,idMap,judgeNewFather(childId,nodeId,idMap)))
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    private boolean judgeNewFather(String childId,String FatherId,Map<String,PlotNode> idMap){
        PlotNode father = idMap.get(FatherId);
        if (FatherId.startsWith("ghost_root")) {
            return false;
        }
        else if (FatherId.equals(childId)) {
            return true;
        }
        return judgeNewFather(childId,father.fatherId,idMap);
    }

    private void setFather(List<PlotNode> nodeList,String childId,String newFatherId,String oldFatherId,Map<String,PlotNode> idMap,boolean isOriginalSenior){
        if (newFatherId.equals(childId)) {
            Toast.makeText(this, "不能选择自身作为前驱", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newFatherId.equals(oldFatherId)) {
            Toast.makeText(this, "前驱未改变", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (isOriginalSenior) {
            new AlertDialog.Builder(this)
                    .setTitle("选择新前驱")
                    .setMessage("你将如何处理其子节点？")
                    .setPositiveButton("保留位置", (dialog, which) -> moveSubtreeStay(nodeList,childId,newFatherId,oldFatherId,idMap))
                    .setNeutralButton("向前嫁接",(dialog,which) -> moveSubtreeInherit(nodeList,childId,newFatherId,oldFatherId,idMap))
                    .show();
        }
        else {
            new AlertDialog.Builder(this)
                    .setTitle("选择新前驱")
                    .setMessage("你将如何处理其子节点？")
                    .setPositiveButton("保留位置", (dialog, which) -> moveSubtreeStay(nodeList,childId,newFatherId,oldFatherId,idMap))
                    .setNeutralButton("向前嫁接",(dialog,which) -> moveSubtreeInherit(nodeList,childId,newFatherId,oldFatherId,idMap))
                    .setNegativeButton("随本节点", (dialog,which) -> moveSubtreeMove(nodeList,childId,newFatherId,oldFatherId,idMap))
                    .show();
        }
    }

    //创建幽灵节点替代原节点
    private void moveSubtreeStay(List<PlotNode> nodeList,String childId,String newFatherId,String oldFatherId,Map<String,PlotNode> idMap){
        moveEditingNode(childId,newFatherId,idMap);

        PlotNode child = idMap.get(childId);
        PlotNode newFather = idMap.get(newFatherId);
        PlotNode oldFather = idMap.get(oldFatherId);

        child.depth = newFather.depth + 1;
        child.type = "middle";

        PlotNode ghost = new PlotNode();
        ghost.id = PlotStorageHelper.generateNodeId(nodeList);
        ghost.depth = oldFather.depth + 1;
        ghost.y = 0;
        ghost.fatherId = oldFatherId;
        ghost.childrenId = new ArrayList<>();
        ghost.type = "ghost";

        oldFather.childrenId.set(oldFather.childrenId.indexOf(childId),ghost.id);
        for (String grandchildId:child.childrenId) {
            PlotNode grandchild = idMap.get(grandchildId);
            grandchild.fatherId = ghost.id;
            ghost.childrenId.add(grandchildId);
        }

        child.childrenId.clear();
        nodeList.add(ghost);

        finishMoving(nodeList);
    }

    private void moveSubtreeInherit(List<PlotNode> nodeList,String childId,String newFatherId,String oldFatherId,Map<String,PlotNode> idMap){
        moveEditingNode(childId,newFatherId,idMap);

        PlotNode child = idMap.get(childId);
        PlotNode oldFather = idMap.get(oldFatherId);

        int index = oldFather.childrenId.indexOf(childId);
        for (String grandchild:child.childrenId) {
            PlotNode grandchildren = idMap.get(grandchild);
            grandchildren.fatherId = oldFatherId;
            oldFather.childrenId.add(index,grandchild);
            index += 1;
            PlotStorageHelper.renewChildDepth(grandchild,idMap,oldFather.depth);
        }

        oldFather.childrenId.remove(childId);

        child.childrenId.clear();

        finishMoving(nodeList);
    }

    private void moveSubtreeMove(List<PlotNode> nodeList,String childId,String newFatherId,String oldFatherId,Map<String,PlotNode> idMap){
        moveEditingNode(childId,newFatherId,idMap);

        PlotNode child = idMap.get(childId);
        PlotNode oldFather = idMap.get(oldFatherId);

        for (String grandchildId:child.childrenId) {
            PlotStorageHelper.renewChildDepth(grandchildId,idMap,child.depth);
        }

        oldFather.childrenId.remove(childId);

        finishMoving(nodeList);
    }

    private void moveEditingNode(String childId,String newFatherId,Map<String,PlotNode> idMap){
        PlotNode child = idMap.get(childId);
        child.fatherId = newFatherId;

        PlotNode newFather = idMap.get(newFatherId);
        newFather.childrenId.add(childId);

        child.depth = newFather.depth + 1;
    }

    private void finishMoving(List<PlotNode> nodeList){
        PlotStorageHelper.recalculateYCoordinates(nodeList);
        PlotStorageHelper.saveNodesToFile(SelectNewFatherActivity.this, storyId, nodeList);
        Toast.makeText(this, "前驱已更新", Toast.LENGTH_SHORT).show();
        finish();
    }

    private String getNodeTitle(List<PlotNode> nodeList,String nodeId) {
        for (PlotNode n : nodeList) {
            if (n.id.equals(nodeId)) return n.title;
        }
        return "未命名";
    }
}
