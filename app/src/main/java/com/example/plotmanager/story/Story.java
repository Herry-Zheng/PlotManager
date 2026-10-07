package com.example.plotmanager.story;

//大纲类，在大纲列表MainActivity展示的信息

import java.io.Serializable;

public class Story implements Serializable {
    private String id;//内部类
    private String title;
    private String createTime;
    private String editTime;

    public Story(String id, String title, String createTime, String editTime) {
        this.id = id;
        this.title = title;
        this.createTime = createTime;
        this.editTime = editTime;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getCreateTime() { return createTime; }
    public String getEditTime() { return editTime; }

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
    public void setEditTime(String editTime) { this.editTime = editTime; }
}