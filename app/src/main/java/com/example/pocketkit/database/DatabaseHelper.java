package com.example.pocketkit.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.pocketkit.model.Note;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;


public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "CampusKit.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_NOTES = "notes";
    public static final String COL_ID = "id";
    public static final String COL_TITLE = "title";
    public static final String COL_CONTENT = "content";
    public static final String COL_SUBJECT = "subject";
    public static final String COL_IS_PINNED = "is_pinned";
    public static final String COL_IS_DONE = "is_done";
    public static final String COL_UPDATED_AT = "updated_at";


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String createTable = "CREATE TABLE " + TABLE_NOTES + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TITLE + " TEXT NOT NULL, " +
                COL_CONTENT + " TEXT, " +
                COL_SUBJECT + " TEXT, " +
                COL_IS_PINNED + " INTEGER DEFAULT 0, " +
                COL_IS_DONE + " INTEGER DEFAULT 0, " +
                COL_UPDATED_AT + " TEXT)";
        db.execSQL(createTable);

        //mock data
        db.execSQL("INSERT INTO " + TABLE_NOTES + " (title, content, subject, is_pinned, is_done, updated_at) " +
                "VALUES ('Ôn thi Practical Exam PRM392', 'Nắm vững SQLiteOpenHelper, RecyclerView Adapter và Intent truyền dữ liệu.', 'PRM392', 1, 0, '10/11 09:30')");
        db.execSQL("INSERT INTO " + TABLE_NOTES + " (title, content, subject, is_pinned, is_done, updated_at) " +
                "VALUES ('Chuẩn bị slide thuyết trình nhóm', 'Hoàn thành phần kiến trúc hệ thống và demo app.', 'SWP391', 0, 1, '09/11 20:15')"););


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        onCreate(db);
    }

    public long insertNote(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, note.getTitle());
        values.put(COL_CONTENT, note.getContent());
        values.put(COL_SUBJECT, note.getSubject());
        values.put(COL_IS_PINNED, note.getIsPinned());
        values.put(COL_IS_DONE, note.getIsDone());
        values.put(COL_UPDATED_AT, note.getUpdateAt());

        return db.insert(TABLE_NOTES, null, values);
    }

    public List<Note> getAllNotes(String keyword) {
        List<Note> list = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();

        String selection = null;
        String [] selectionArgs = null;

        if(keyword != null && !keyword.trim().isEmpty()) {
            selection = COL_TITLE + " LIKE ? OR " + COL_CONTENT + " LIKE ? OR " + COL_SUBJECT + " LIKE ?";
            String query = "%" + keyword.trim() + "%";
            selectionArgs = new String[] {query, query, query};

            String orderBy = COL_IS_PINNED + " DESC, " + COL_ID + " DESC";
            Cursor cursor = db.query(TABLE_NOTES, null, selection, selectionArgs, null, null, orderBy);

            if(cursor != null && cursor.moveToFirst()) {
                do {
                    Note note = Note.builder()
                            .id(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)))
                            .title(cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)))
                            .content(cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTENT)))
                            .subject(cursor.getString(cursor.getColumnIndexOrThrow(COL_SUBJECT)))
                            .isPinned(cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_PINNED)))
                            .isDone(cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_DONE)))
                            .updateAt(cursor.getString(cursor.getColumnIndexOrThrow(COL_UPDATED_AT)))
                            .build();

                    list.add(note);
                } while(cursor.moveToNext()) {
                    cursor.close();
                }
            }
        }
        return list;
    }

    public int updateNote(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, note.getTitle());
        values.put(COL_CONTENT, note.getContent());
        values.put(COL_SUBJECT, note.getSubject());
        values.put(COL_IS_PINNED, note.getIsPinned());
        values.put(COL_IS_DONE, note.getIsDone());

        return db.update(TABLE_NOTES, values, COL_ID + " = ?", new String[] {String.valueOf(note.getId())});
    }

}
