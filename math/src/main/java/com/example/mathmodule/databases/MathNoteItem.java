package com.example.mathmodule.databases;

public class MathNoteItem {
    private int id;
    private String title;
    private String formula;
    private String category;
    private int isPinned; // 0: Chưa ghim, 1: Đã ghim

    public MathNoteItem(int id, String title, String formula, String category, int isPinned) {
        this.id = id;
        this.title = title;
        this.formula = formula;
        this.category = category;
        this.isPinned = isPinned;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getFormula() { return formula; }
    public String getCategory() { return category; }
    public int getIsPinned() { return isPinned; }
    public void setIsPinned(int isPinned) { this.isPinned = isPinned; }
}