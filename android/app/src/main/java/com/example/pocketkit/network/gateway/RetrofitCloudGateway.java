package com.example.pocketkit.network.gateway;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.pocketkit.network.api.PocketKitApi;
import com.example.pocketkit.network.dto.ApiModels;
import com.example.pocketkit.network.exception.CloudException;
import com.example.pocketkit.network.session.SessionStore;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitCloudGateway implements CloudGateway {

    private final SessionStore sessionStore;
    private final Gson gson;
    private final PocketKitApi api;

    public RetrofitCloudGateway(@NonNull String baseUrl, @NonNull SessionStore sessionStore) {
        this.sessionStore = sessionStore;
        this.gson = new Gson();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    String path = original.url().encodedPath();
                    boolean isAuthEndpoint = path.endsWith("/api/auth/login") || path.endsWith("/api/auth/register");
                    String token = sessionStore.getAccessToken();
                    Request.Builder builder = original.newBuilder()
                            .header("Accept", "application/json");
                    if (!isAuthEndpoint && token != null && !token.isEmpty()) {
                        builder.header("Authorization", "Bearer " + token);
                    }
                    return chain.proceed(builder.build());
                })
                .build();

        String normalizedUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        this.api = retrofit.create(PocketKitApi.class);
    }

    @Override
    public boolean isLoggedIn() {
        return sessionStore.isLoggedIn();
    }

    @NonNull
    @Override
    public String getEmail() {
        return sessionStore.getEmail();
    }

    @NonNull
    @Override
    public String getFullName() {
        return sessionStore.getFullName();
    }

    @Override
    public long getLastBackupEpoch() {
        return sessionStore.getLastBackupEpoch();
    }

    @NonNull
    @Override
    public ApiModels.AuthResponse register(
            @NonNull String fullName,
            @NonNull String email,
            @NonNull String password
    ) throws CloudException {
        ApiModels.RegisterRequest request = ApiModels.RegisterRequest.builder()
                .fullName(fullName.trim())
                .email(email.trim())
                .password(password)
                .build();
        ApiModels.AuthResponse auth = execute(api.register(request));
        sessionStore.saveAuth(auth, System.currentTimeMillis());
        return auth;
    }

    @NonNull
    @Override
    public ApiModels.AuthResponse login(
            @NonNull String email,
            @NonNull String password
    ) throws CloudException {
        ApiModels.LoginRequest request = ApiModels.LoginRequest.builder()
                .email(email.trim())
                .password(password)
                .build();
        ApiModels.AuthResponse auth = execute(api.login(request));
        sessionStore.saveAuth(auth, System.currentTimeMillis());
        return auth;
    }

    @Override
    public void logout() {
        sessionStore.clear();
    }

    @NonNull
    @Override
    public ApiModels.BackupSnapshotResponse saveBackup(@NonNull ApiModels.BackupPayload payload) throws CloudException {
        ApiModels.BackupSnapshotResponse res = execute(api.saveBackup(payload));
        sessionStore.saveLastBackupEpoch(res.getBackedUpAtEpoch());
        return res;
    }

    @NonNull
    @Override
    public ApiModels.BackupSnapshotResponse getLatestBackup() throws CloudException {
        ApiModels.BackupSnapshotResponse res = execute(api.getLatestBackup());
        sessionStore.saveLastBackupEpoch(res.getBackedUpAtEpoch());
        return res;
    }

    @NonNull
    private <T> T execute(@NonNull Call<ApiModels.ApiResponse<T>> call) throws CloudException {
        try {
            Response<ApiModels.ApiResponse<T>> response = call.execute();
            if (response.isSuccessful()) {
                ApiModels.ApiResponse<T> body = response.body();
                if (body != null && body.isSuccess() && body.getData() != null) {
                    return body.getData();
                }
                String msg = body != null && hasText(body.getMessage())
                        ? body.getMessage()
                        : "Máy chủ trả về phản hồi không hợp lệ";
                throw new CloudException(response.code(), msg);
            }

            int code = response.code();
            if (code == 401) {
                sessionStore.clear();
            }
            throw new CloudException(code, parseErrorMessage(response.errorBody(), code));
        } catch (CloudException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new CloudException(0, "Không thể kết nối tới máy chủ, vui lòng kiểm tra mạng hoặc backend");
        }
    }

    @NonNull
    private String parseErrorMessage(@Nullable ResponseBody errorBody, int httpCode) {
        if (errorBody != null) {
            try {
                String raw = errorBody.string();
                if (hasText(raw)) {
                    ApiModels.ApiResponse<?> parsed = gson.fromJson(raw, ApiModels.ApiResponse.class);
                    if (parsed != null && hasText(parsed.getMessage())) {
                        return parsed.getMessage();
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (httpCode == 401) {
            return "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại";
        }
        if (httpCode == 404) {
            return "Chưa có dữ liệu sao lưu trên đám mây";
        }
        return "Yêu cầu thất bại (mã " + httpCode + ")";
    }

    private boolean hasText(@Nullable String value) {
        return value != null && !value.trim().isEmpty();
    }
}