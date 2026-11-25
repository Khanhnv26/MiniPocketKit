package com.example.pocketkit.network.api;

import com.example.pocketkit.network.dto.ApiModels;

import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface PocketKitApi {

    @POST("api/auth/register")
    Call<ApiModels.ApiResponse<ApiModels.AuthResponse>> register(@Body ApiModels.RegisterRequest request);

    @POST("api/auth/login")
    Call<ApiModels.ApiResponse<ApiModels.AuthResponse>> login(@Body ApiModels.LoginRequest request);

    @POST("api/auth/oauth/google")
    Call<ApiModels.ApiResponse<ApiModels.AuthResponse>> loginWithGoogle(@Body ApiModels.GoogleLoginRequest request);

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

    @POST("api/schedules/share")
    Call<ApiModels.ApiResponse<ApiModels.CreateShareResponse>> createScheduleShare(@Body ApiModels.CreateShareRequest request);

    @GET("api/schedules/share/{code}")
    Call<ApiModels.ApiResponse<ApiModels.ShareDetailResponse>> getScheduleShare(@Path("code") String code);

    @GET("api/schedules/share")
    Call<ApiModels.ApiResponse<List<ApiModels.ShareSummaryResponse>>> listMyScheduleShares();

    @DELETE("api/schedules/share/{code}")
    Call<ApiModels.ApiResponse<Void>> deleteScheduleShare(@Path("code") String code);

    @Multipart
    @POST("api/tools/schedules/parse-image")
    Call<ApiModels.ApiResponse<ApiModels.ScheduleScanResponse>> parseScheduleImage(@Part MultipartBody.Part file);
}
