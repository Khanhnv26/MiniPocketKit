package com.example.pocketkit.network.api;

import com.example.pocketkit.network.dto.ApiModels;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface PocketKitApi {

    @POST("api/auth/register")
    Call<ApiModels.ApiResponse<ApiModels.AuthResponse>> register(@Body ApiModels.RegisterRequest request);

    @POST("api/auth/login")
    Call<ApiModels.ApiResponse<ApiModels.AuthResponse>> login(@Body ApiModels.LoginRequest request);

    @GET("api/auth/me")
    Call<ApiModels.ApiResponse<ApiModels.UserSummary>> aboutMe();

    @POST("api/backup")
    Call<ApiModels.ApiResponse<ApiModels.BackupSnapshotResponse>> saveBackup(@Body ApiModels.BackupPayload payload);

    @GET("api/backup/latest")
    Call<ApiModels.ApiResponse<ApiModels.BackupSnapshotResponse>> getLatestBackup();

    @Multipart
    @POST("api/tools/ocr")
    Call<ApiModels.ApiResponse<ApiModels.OcrResponse>> scanOcr(@Part MultipartBody.Part file);

    @POST("api/tools/notes/summarize")
    Call<ApiModels.ApiResponse<ApiModels.NoteSummarizeResponse>> summarizeNote(@Body ApiModels.NoteSummarizeRequest request);

    @Multipart
    @POST("api/tools/notes/summarize-image")
    Call<ApiModels.ApiResponse<ApiModels.NoteSummarizeResponse>> summarizeNoteImage(@Part MultipartBody.Part file);
}
