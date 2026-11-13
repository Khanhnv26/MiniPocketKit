package com.example.pocketkit.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

import com.example.pocketkit.R;
import com.example.pocketkit.model.Note;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String FILTER_ALL = "ALL";
    public static final String FILTER_PINNED = "PINNED";

    private static final String DATABASE_NAME = "CampusKit.db";
    private static final int DATABASE_VERSION = 4;

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

    private final Context appContext;

    public DatabaseHelper(@NonNull Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
        this.appContext = context.getApplicationContext();
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

        seedInitialData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NOTES + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TITLE + " TEXT NOT NULL, " +
                COL_CONTENT + " TEXT, " +
                COL_SUBJECT + " TEXT, " +
                COL_IS_PINNED + " INTEGER DEFAULT 0, " +
                COL_IS_DONE + " INTEGER DEFAULT 0, " +
                COL_UPDATED_AT + " TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CATEGORIES + " (" +
                COL_CAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_CAT_NAME + " TEXT UNIQUE NOT NULL)");

        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NOTES, null)) {
            if (cursor.moveToFirst() && cursor.getInt(0) == 0) {
                seedInitialData(db);
            }
        }
    }

    private void seedInitialData(SQLiteDatabase db) {
        String cat1 = appContext.getString(R.string.note_seed_category_1);
        String cat2 = appContext.getString(R.string.note_seed_category_2);

        insertCategoryInternal(db, cat1);
        insertCategoryInternal(db, cat2);

        insertNoteInternal(db, Note.builder()
                .title(appContext.getString(R.string.note_seed_title_1))
                .content(appContext.getString(R.string.note_seed_content_1))
                .subject(cat1)
                .isPinned(1)
                .isDone(0)
                .updatedAt(appContext.getString(R.string.note_seed_time_1))
                .build());

        insertNoteInternal(db, Note.builder()
                .title(appContext.getString(R.string.note_seed_title_2))
                .content(appContext.getString(R.string.note_seed_content_2))
                .subject(cat2)
                .isPinned(0)
                .isDone(1)
                .updatedAt(appContext.getString(R.string.note_seed_time_2))
                .build());
    }

    private void insertCategoryInternal(SQLiteDatabase db, String name) {
        ContentValues values = new ContentValues();
        values.put(COL_CAT_NAME, name.trim());
        db.insertWithOnConflict(TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private void insertNoteInternal(SQLiteDatabase db, Note note) {
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, note.getTitle());
        values.put(COL_CONTENT, note.getContent());
        values.put(COL_SUBJECT, note.getSubject());
        values.put(COL_IS_PINNED, note.getIsPinned());
        values.put(COL_IS_DONE, note.getIsDone());
        values.put(COL_UPDATED_AT, note.getUpdatedAt());
        db.insert(TABLE_NOTES, null, values);
    }

    public long insertCategory(String name) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CAT_NAME, name.trim());
        return db.insertWithOnConflict(TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void deleteCategory(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return;
        }
        String fallback = appContext.getString(R.string.note_default_category);
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues updateValues = new ContentValues();
            updateValues.put(COL_SUBJECT, fallback);
            db.update(TABLE_NOTES, updateValues, COL_SUBJECT + " = ?", new String[]{categoryName});
            db.delete(TABLE_CATEGORIES, COL_CAT_NAME + " = ?", new String[]{categoryName});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<String> getAllCategories() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_CATEGORIES, new String[]{COL_CAT_NAME}, null, null, null, null, COL_CAT_ID + " ASC")) {
            if (cursor.moveToFirst()) {
                do {
                    list.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
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
                    .append(COL_TITLE).append(" LIKE ? OR ")
                    .append(COL_CONTENT).append(" LIKE ? OR ")
                    .append(COL_SUBJECT).append(" LIKE ?)");

            String query = "%" + keyword.trim() + "%";
            args.add(query);
            args.add(query);
            args.add(query);
        }

        if (category != null && !FILTER_ALL.equals(category)) {
            if (FILTER_PINNED.equals(category)) {
                where.append(" AND ").append(COL_IS_PINNED).append(" = 1");
            } else {
                where.append(" AND ").append(COL_SUBJECT).append(" = ?");
                args.add(category);
            }
        }

        String orderBy = COL_IS_DONE + " ASC, " + COL_IS_PINNED + " DESC, " + COL_ID + " DESC";
        try (Cursor cursor = db.query(TABLE_NOTES, null, where.toString(), args.toArray(new String[0]), null, null, orderBy)) {
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
        return db.update(TABLE_NOTES, values, COL_ID + " = ?", new String[]{String.valueOf(note.getId())});
    }

    public int deleteNote(int id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_NOTES, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }
}
