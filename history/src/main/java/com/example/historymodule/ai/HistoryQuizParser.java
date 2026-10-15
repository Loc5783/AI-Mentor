package com.example.historymodule.ai;

public final class HistoryQuizParser {
    private HistoryQuizParser(){}
    public static String cleanJson(String response){
        if(response==null) return "[]";
        String text=response.replace("```json","").replace("```","").trim();
        int start=text.indexOf("[");
        int end=text.lastIndexOf("]");
        return start>=0 && end>start ? text.substring(start,end+1) : "[]";
    }
}
