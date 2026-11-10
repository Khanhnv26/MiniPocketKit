package com.example.pocketkit.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String BASE_URL = "https://672f00000000000000000000.mockapi.io/api/v1/";
    private static Retrofit retrofit = null;

    public static NoteApiService getNoteApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(NoteApiService.class);
    }
}
