package com.example.modulelaptrinh;

import java.util.ArrayList;
import java.util.List;

public class ProgrammingMessage {
    private String content;
    private boolean isUser;
    private List<String> imageBase64List;

    public ProgrammingMessage(String content, boolean isUser) {
        this.content = content;
        this.isUser = isUser;
        this.imageBase64List = new ArrayList<>();
    }

    public ProgrammingMessage(String content, boolean isUser, List<String> imageBase64List) {
        this.content = content;
        this.isUser = isUser;
        this.imageBase64List = imageBase64List != null ? imageBase64List : new ArrayList<>();
    }

    public String getContent() {
        return content;
    }

    public boolean isUser() {
        return isUser;
    }

    public List<String> getImageBase64List() {
        return imageBase64List;
    }

    public void setImageBase64List(List<String> imageBase64List) {
        this.imageBase64List = imageBase64List != null ? imageBase64List : new ArrayList<>();
    }

    public void addImage(String imageBase64) {
        if (imageBase64List == null) {
            imageBase64List = new ArrayList<>();
        }
        imageBase64List.add(imageBase64);
    }

    public boolean hasImages() {
        return imageBase64List != null && !imageBase64List.isEmpty();
    }

    public int getImageCount() {
        return imageBase64List != null ? imageBase64List.size() : 0;
    }

    public String getFirstImage() {
        return hasImages() ? imageBase64List.get(0) : null;
    }
}