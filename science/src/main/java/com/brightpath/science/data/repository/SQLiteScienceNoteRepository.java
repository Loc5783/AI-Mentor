package com.brightpath.science.data.repository;

import android.content.ContentValues;
import android.database.Cursor;
import com.brightpath.science.data.local.ScienceDbContract;
import com.brightpath.science.data.local.ScienceDbHelper;
import java.util.ArrayList;
import java.util.List;

public final class SQLiteScienceNoteRepository {
    public static final class Note {
        public final long id; public final String title; public final String content; public final String subject; public final long updatedAt;
        public Note(long id,String title,String content,String subject,long updatedAt){this.id=id;this.title=title;this.content=content;this.subject=subject;this.updatedAt=updatedAt;}
    }
    private final ScienceDbHelper helper;
    public SQLiteScienceNoteRepository(ScienceDbHelper helper){this.helper=helper;}
    public long insert(int userId,String title,String content,String subject){
        long now=System.currentTimeMillis(); ContentValues v=new ContentValues();
        v.put(ScienceDbContract.Note.USER_ID,userId); v.put(ScienceDbContract.Note.TITLE,title.trim());
        v.put(ScienceDbContract.Note.CONTENT,content.trim()); v.put(ScienceDbContract.Note.SUBJECT,subject);
        v.put(ScienceDbContract.Note.CREATED_AT,now); v.put(ScienceDbContract.Note.UPDATED_AT,now);
        return helper.getWritableDatabase().insert(ScienceDbContract.Note.TABLE,null,v);
    }
    public boolean update(long id,int userId,String title,String content,String subject){
        ContentValues v=new ContentValues(); v.put(ScienceDbContract.Note.TITLE,title.trim()); v.put(ScienceDbContract.Note.CONTENT,content.trim());
        v.put(ScienceDbContract.Note.SUBJECT,subject); v.put(ScienceDbContract.Note.UPDATED_AT,System.currentTimeMillis());
        return helper.getWritableDatabase().update(ScienceDbContract.Note.TABLE,v,ScienceDbContract.Note.ID+"=? AND "+ScienceDbContract.Note.USER_ID+"=?",new String[]{String.valueOf(id),String.valueOf(userId)})>0;
    }
    public boolean delete(long id,int userId){ return helper.getWritableDatabase().delete(ScienceDbContract.Note.TABLE,ScienceDbContract.Note.ID+"=? AND "+ScienceDbContract.Note.USER_ID+"=?",new String[]{String.valueOf(id),String.valueOf(userId)})>0; }
    public List<Note> getAll(int userId){
        List<Note> out=new ArrayList<>(); String sql="SELECT "+ScienceDbContract.Note.ID+","+ScienceDbContract.Note.TITLE+","+ScienceDbContract.Note.CONTENT+","+ScienceDbContract.Note.SUBJECT+","+ScienceDbContract.Note.UPDATED_AT+" FROM "+ScienceDbContract.Note.TABLE+" WHERE "+ScienceDbContract.Note.USER_ID+"=? ORDER BY "+ScienceDbContract.Note.UPDATED_AT+" DESC";
        try(Cursor c=helper.getReadableDatabase().rawQuery(sql,new String[]{String.valueOf(userId)})){ while(c.moveToNext()) out.add(new Note(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getLong(4))); }
        return out;
    }
}
