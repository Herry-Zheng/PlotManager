package com.example.plotmanager.plot;

import java.util.ArrayList;
import java.util.List;

public class PlotNode {
    //内部计算
    public String id;
    public int depth;
    public float y;
    public String fatherId; //前驱节点，null表示根
    public List<String> childrenId = new ArrayList<>();
    public String brotherId;

    //用户信息
    public String title;
    public String characters;
    public String factions;
    public String plot;
    public String type;
    public String color;
    public String causalLineName;
}