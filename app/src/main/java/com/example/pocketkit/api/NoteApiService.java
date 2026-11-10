package com.example.pocketkit.api;

import com.example.pocketkit.dto.NoteRequestDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface NoteApiService {

    @GET("notes")
    Call<List<NoteRequestDto>> getAllNotes();

    @POST("notes")
    Call<NoteRequestDto> createNote(@Body NoteRequestDto noteRequestDto);

    @PUT("notes/{id}")
    Call<NoteRequestDto> updateNote(@Path("id") int id, @Body NoteRequestDto noteRequestDto);

    @DELETE("notes/{id}")
    Call<Void> deleteNote(@Path("id") int id);
}
