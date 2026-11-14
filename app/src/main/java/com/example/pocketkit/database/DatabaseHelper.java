package com.example.pocketkit.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

import com.example.pocketkit.R;
import com.example.pocketkit.model.Note;
import com.example.pocketkit.model.ScheduleSlot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String FILTER_ALL = "ALL";
    public static final String FILTER_PINNED = "PINNED";

    private static final String DATABASE_NAME = "CampusKit.db";
    private static final int DATABASE_VERSION = 5;

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

    private static final String TABLE_SCHEDULES = "schedules";
    private static final String COL_SCH_ID = "id";
    private static final String COL_SCH_DAY = "day_of_week";
    private static final String COL_SCH_START = "start_time";
    private static final String COL_SCH_END = "end_time";
    private static final String COL_SCH_SUBJECT = "subject";
    private static final String COL_SCH_ROOM = "room";
    private static final String COL_SCH_LECTURER = "lecturer";
    private static final String COL_SCH_NOTE = "note";

    private final Context appContext;

    public static final class ImportSummary {
        public final int insertedCount;
        public final int skippedDuplicates;

        public ImportSummary(int insertedCount, int skippedDuplicates) {
            this.insertedCount = insertedCount;
            this.skippedDuplicates = skippedDuplicates;
        }
    }

    public DatabaseHelper(@NonNull Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
        this.appContext = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTablesIfNeeded(db);
        seedInitialNotes(db);
        seedInitialSchedules(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        createTablesIfNeeded(db);

        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NOTES, null)) {
            if (cursor.moveToFirst() && cursor.getInt(0) == 0) {
                seedInitialNotes(db);
            }
        }

        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SCHEDULES, null)) {
            if (cursor.moveToFirst() && cursor.getInt(0) == 0) {
                seedInitialSchedules(db);
            }
        }
    }

    private void createTablesIfNeeded(SQLiteDatabase db) {
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

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SCHEDULES + " (" +
                COL_SCH_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_SCH_DAY + " INTEGER NOT NULL, " +
                COL_SCH_START + " TEXT NOT NULL, " +
                COL_SCH_END + " TEXT NOT NULL, " +
                COL_SCH_SUBJECT + " TEXT NOT NULL, " +
                COL_SCH_ROOM + " TEXT, " +
                COL_SCH_LECTURER + " TEXT, " +
                COL_SCH_NOTE + " TEXT)");
    }

    private void seedInitialNotes(SQLiteDatabase db) {
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

    private void seedInitialSchedules(SQLiteDatabase db) {
        String[] seedRows = appContext.getResources().getStringArray(R.array.schedule_seed_rows);
        for (String row : seedRows) {
            String[] parts = row.split("\\|", -1);
            if (parts.length < 7) {
                continue;
            }
            try {
                int day = Integer.parseInt(parts[0].trim());
                ScheduleSlot slot = ScheduleSlot.builder()
                        .dayOfWeek(day)
                        .startTime(parts[1].trim())
                        .endTime(parts[2].trim())
                        .subject(parts[3].trim())
                        .room(parts[4].trim())
                        .lecturer(parts[5].trim())
                        .note(parts[6].trim())
                        .build();
                insertScheduleInternal(db, slot);
            } catch (NumberFormatException ignored) {
            }
        }
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

    private long insertScheduleInternal(SQLiteDatabase db, ScheduleSlot slot) {
        return db.insert(TABLE_SCHEDULES, null, toScheduleValues(slot));
    }

    private ContentValues toScheduleValues(ScheduleSlot slot) {
        ContentValues values = new ContentValues();
        values.put(COL_SCH_DAY, slot.getDayOfWeek());
        values.put(COL_SCH_START, safeTrim(slot.getStartTime()));
        values.put(COL_SCH_END, safeTrim(slot.getEndTime()));
        values.put(COL_SCH_SUBJECT, safeTrim(slot.getSubject()));
        values.put(COL_SCH_ROOM, safeTrim(slot.getRoom()));
        values.put(COL_SCH_LECTURER, safeTrim(slot.getLecturer()));
        values.put(COL_SCH_NOTE, safeTrim(slot.getNote()));
        return values;
    }

    private String safeTrim(String text) {
        return text == null ? "" : text.trim();
    }

    private ScheduleSlot readScheduleCursor(Cursor cursor) {
        return ScheduleSlot.builder()
                .id(cursor.getInt(cursor.getColumnIndexOrThrow(COL_SCH_ID)))
                .dayOfWeek(cursor.getInt(cursor.getColumnIndexOrThrow(COL_SCH_DAY)))
                .startTime(cursor.getString(cursor.getColumnIndexOrThrow(COL_SCH_START)))
                .endTime(cursor.getString(cursor.getColumnIndexOrThrow(COL_SCH_END)))
                .subject(cursor.getString(cursor.getColumnIndexOrThrow(COL_SCH_SUBJECT)))
                .room(cursor.getString(cursor.getColumnIndexOrThrow(COL_SCH_ROOM)))
                .lecturer(cursor.getString(cursor.getColumnIndexOrThrow(COL_SCH_LECTURER)))
                .note(cursor.getString(cursor.getColumnIndexOrThrow(COL_SCH_NOTE)))
                .build();
    }

    private String buildScheduleKey(ScheduleSlot slot) {
        return slot.getDayOfWeek()
                + "|" + safeTrim(slot.getStartTime())
                + "|" + safeTrim(slot.getEndTime())
                + "|" + safeTrim(slot.getSubject()).toLowerCase(Locale.ROOT);
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

    public long insertSchedule(@NonNull ScheduleSlot slot) {
        SQLiteDatabase db = getWritableDatabase();
        return insertScheduleInternal(db, slot);
    }

    public int updateSchedule(@NonNull ScheduleSlot slot) {
        SQLiteDatabase db = getWritableDatabase();
        return db.update(TABLE_SCHEDULES, toScheduleValues(slot), COL_SCH_ID + " = ?", new String[]{String.valueOf(slot.getId())});
    }

    public int deleteSchedule(int id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_SCHEDULES, COL_SCH_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public List<ScheduleSlot> getSchedulesByDay(int dayOfWeek) {
        List<ScheduleSlot> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String orderBy = COL_SCH_START + " ASC, " + COL_SCH_END + " ASC, " + COL_SCH_ID + " ASC";
        try (Cursor cursor = db.query(
                TABLE_SCHEDULES,
                null,
                COL_SCH_DAY + " = ?",
                new String[]{String.valueOf(dayOfWeek)},
                null,
                null,
                orderBy
        )) {
            if (cursor.moveToFirst()) {
                do {
                    list.add(readScheduleCursor(cursor));
                } while (cursor.moveToNext());
            }
        }
        return list;
    }

    public List<ScheduleSlot> getAllSchedules() {
        List<ScheduleSlot> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String orderBy = COL_SCH_DAY + " ASC, " + COL_SCH_START + " ASC, " + COL_SCH_END + " ASC, " + COL_SCH_ID + " ASC";
        try (Cursor cursor = db.query(TABLE_SCHEDULES, null, null, null, null, null, orderBy)) {
            if (cursor.moveToFirst()) {
                do {
                    list.add(readScheduleCursor(cursor));
                } while (cursor.moveToNext());
            }
        }
        return list;
    }

    public int getTotalScheduleCount() {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SCHEDULES, null)) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }
        return 0;
    }

    public ImportSummary importSchedules(@NonNull List<ScheduleSlot> incomingSlots, boolean replaceExisting) {
        SQLiteDatabase db = getWritableDatabase();
        Set<String> existingKeys = new HashSet<>();
        int inserted = 0;
        int skipped = 0;

        db.beginTransaction();
        try {
            if (replaceExisting) {
                db.delete(TABLE_SCHEDULES, null, null);
            } else {
                try (Cursor cursor = db.query(
                        TABLE_SCHEDULES,
                        new String[]{COL_SCH_DAY, COL_SCH_START, COL_SCH_END, COL_SCH_SUBJECT},
                        null,
                        null,
                        null,
                        null,
                        null
                )) {
                    if (cursor.moveToFirst()) {
                        do {
                            ScheduleSlot existing = ScheduleSlot.builder()
                                    .dayOfWeek(cursor.getInt(0))
                                    .startTime(cursor.getString(1))
                                    .endTime(cursor.getString(2))
                                    .subject(cursor.getString(3))
                                    .build();
                            existingKeys.add(buildScheduleKey(existing));
                        } while (cursor.moveToNext());
                    }
                }
            }

            for (ScheduleSlot slot : incomingSlots) {
                String key = buildScheduleKey(slot);
                if (existingKeys.contains(key)) {
                    skipped++;
                    continue;
                }
                if (insertScheduleInternal(db, slot) > 0) {
                    existingKeys.add(key);
                    inserted++;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return new ImportSummary(inserted, skipped);
    }
}
