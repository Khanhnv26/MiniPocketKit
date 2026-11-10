package com.example.pocketkit.repository;

import android.content.Context;
import android.util.Log;

import com.example.pocketkit.api.ApiClient;
import com.example.pocketkit.api.NoteApiService;
import com.example.pocketkit.database.DatabaseHelper;
import com.example.pocketkit.dto.NoteRequestDto;
import com.example.pocketkit.model.Note;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NoteRepository {

    private static final String TAG = "NoteRepository";
    private final DatabaseHelper dbHelper;
    private final NoteApiService apiService;

    public interface DataCallback<T> {
        void onResult(T data, boolean isFromCloud);
    }

    public NoteRepository(Context context) {
        this.dbHelper = new DatabaseHelper(context);
        this.apiService = ApiClient.getNoteApiService();
    }

    public void getNotes(String keyword, DataCallback<List<Note>> callback) {
        List<Note> localNotes = dbHelper.getAllNotes(keyword);
        if (callback != null) {
            callback.onResult(localNotes, false);
        }

        if (keyword == null || keyword.trim().isEmpty()) {
            apiService.getAllNotes().enqueue(new Callback<List<NoteRequestDto>>() {
                @Override
                public void onResponse(Call<List<NoteRequestDto>> call, Response<List<NoteRequestDto>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d(TAG, "Synced " + response.body().size() + " notes from Cloud API");
                    }
                }

                @Override
                public void onFailure(Call<List<NoteRequestDto>> call, Throwable t) {
                    Log.w(TAG, "Offline fallback: " + t.getMessage());
                }
            });
        }
    }

    public long createNote(NoteRequestDto dto) {
        Note entity = dto.toEntity();
        long localId = dbHelper.insertNote(entity);

        apiService.createNote(dto).enqueue(new Callback<NoteRequestDto>() {
            @Override
            public void onResponse(Call<NoteRequestDto> call, Response<NoteRequestDto> response) {
                Log.d(TAG, "Created note on Cloud API");
            }

            @Override
            public void onFailure(Call<NoteRequestDto> call, Throwable t) {
                Log.w(TAG, "Offline create note: " + t.getMessage());
            }
        });

        return localId;
    }

    public int updateNote(int id, NoteRequestDto dto) {
        Note entity = dto.toEntity();
        entity.setId(id);
        int rows = dbHelper.updateNote(entity);

        apiService.updateNote(id, dto).enqueue(new Callback<NoteRequestDto>() {
            @Override
            public void onResponse(Call<NoteRequestDto> call, Response<NoteRequestDto> response) {
                Log.d(TAG, "Updated note #" + id + " on Cloud API");
            }

            @Override
            public void onFailure(Call<NoteRequestDto> call, Throwable t) {
                Log.w(TAG, "Offline update note: " + t.getMessage());
            }
        });

        return rows;
    }

    public int deleteNote(int id) {
        int rows = dbHelper.deleteNote(id);

        apiService.deleteNote(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d(TAG, "Deleted note #" + id + " on Cloud API");
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.w(TAG, "Offline delete note: " + t.getMessage());
            }
        });

        return rows;
    }
}
