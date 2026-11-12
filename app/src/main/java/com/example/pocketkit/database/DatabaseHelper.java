package com.example.pocketkit.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.example.pocketkit.model.Note;

import java.util.ArrayList;
import java.util.List;


public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "CampusKit.db";
    private static final int DATABASE_VERSION = 3;

    private static final String TABLE_NOTES = "notes";
    private static final String COL_ID = "id";
    private static final String COL_TITLE = "title";
    private static final String COL_CONTENT = "content";
    private static final String COL_SUBJECT = "subject";
    private static final String COL_IS_PINNED = "is_pinned";
    private static final String COL_IS_DONE = "is_done";
    private static final String COL_UPDATED_AT = "updated_at";

    private static final String TABLE_CATEGORIES = "categories";
    private static final String COL_CAT_ID = "id";
    private static final String COL_CAT_NAME = "name";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE " + TABLE_NOTES + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TITLE + " TEXT NOT NULL, " +
                COL_CONTENT + " TEXT, " +
                COL_SUBJECT + " TEXT, " +
                COL_IS_PINNED + " INTEGER DEFAULT 0, " +
                COL_IS_DONE + " INTEGER DEFAULT 0, " +
                COL_UPDATED_AT + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_CATEGORIES + " (" +
                COL_CAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_CAT_NAME + " TEXT UNIQUE NOT NULL)");

        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COL_CAT_NAME + ") VALUES ('PRM392')");
        db.execSQL("INSERT INTO " + TABLE_CATEGORIES + " (" + COL_CAT_NAME + ") VALUES ('SWP391')");

        db.execSQL("INSERT INTO " + TABLE_NOTES + " (title, content, subject, is_pinned, is_done, updated_at) " +
                "VALUES ('Ôn thi Practical Exam PRM392', 'Nắm vững SQLiteOpenHelper, RecyclerView Adapter và Intent.', 'PRM392', 1, 0, '10:30 hôm nay')");
        db.execSQL("INSERT INTO " + TABLE_NOTES + " (title, content, subject, is_pinned, is_done, updated_at) " +
                "VALUES ('Nộp báo cáo Sprint 2 SWP391', 'Hoàn thiện tài liệu SRS và sơ đồ kiến trúc.', 'SWP391', 0, 1, 'Hôm qua')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        onCreate(db);
    }

    public long insertCategory(String name) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CAT_NAME, name.trim());
        return db.insertWithOnConflict(TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public List<String> getAllCategories() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_CATEGORIES, new String[]{COL_CAT_NAME}, null, null, null, null, COL_CAT_ID + " ASC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(0));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public long insertNote(Note note) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, note.getTitle());
        values.put(COL_CONTENT, note.getContent());
        values.put(COL_SUBJECT, note.getSubject());
        values.put(COL_IS_PINNED, note.getIsPinned());
        values.put(COL_IS_DONE, note.getIsDone());
        values.put(COL_UPDATED_AT, note.getUpdatedAt());
        return db.insert(TABLE_NOTES, null, values);
    }

    public List<Note> getNotesFiltered(String keyword, String category) {
        List<Note> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        StringBuilder where = new StringBuilder("1=1");
        List<String> args = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            where.append(" AND (")
                    .append(COL_TITLE)
                    .append(" LIKE ? OR ")
                    .append(COL_CONTENT).append(" LIKE ? OR ")
                    .append(COL_SUBJECT).append(" LIKE ?)");

            String q = "%" + keyword.trim() + "%";
            args.add(q);
            args.add(q);
            args.add(q);
        }

        if(category != null && !category.equals("ALL")) {
            if(category.equals("PINNED")) {
                where.append(" AND ").append(COL_IS_PINNED).append(" = 1");
            } else {
                where.append(" AND ").append(COL_SUBJECT).append(" = ?");
                args.add(category);
            }
        }

        String orderBy = COL_IS_PINNED + " DESC, " + COL_ID + " DESC";
        Cursor cursor = db.query(TABLE_NOTES, null, where.toString(), args.toArray(new String[0]), null, null, orderBy);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Note note = Note.builder()
                            .id(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)))
                            .title(cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)))
                            .content(cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTENT)))
                            .subject(cursor.getString(cursor.getColumnIndexOrThrow(COL_SUBJECT)))
                            .isPinned(cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_PINNED)))
                            .isDone(cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_DONE)))
                            .updatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COL_UPDATED_AT)))
                            .build();
                    list.add(note);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return list;
    }

    public int updateNote(Note note) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, note.getTitle());
        values.put(COL_CONTENT, note.getContent());
        values.put(COL_SUBJECT, note.getSubject());
        values.put(COL_IS_DONE, note.getIsDone());
        values.put(COL_IS_PINNED, note.getIsPinned());
        values.put(COL_UPDATED_AT, note.getUpdatedAt());
        return db.update(TABLE_NOTES, values, COL_ID + " = ? ", new String[]{String.valueOf(note.getId())});
    }

    public int deleteNote(int id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_NOTES, COL_ID + " = ? ", new String[]{String.valueOf(id)});
    }
}
