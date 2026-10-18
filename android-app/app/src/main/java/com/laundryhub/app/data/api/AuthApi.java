package com.laundryhub.app.data.api;

import com.laundryhub.app.data.api.dto.AuthResponse;
import com.laundryhub.app.data.api.dto.GoogleLoginRequest;
import com.laundryhub.app.data.api.dto.LoginRequest;
import com.laundryhub.app.data.api.dto.RegisterRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/** Auth endpoints (docs/SCREEN_API_MAP.md, screens 00a–00c). Paths are relative to API_BASE_URL. */
// OOP: Abstraction — Retrofit generates the HTTP code behind this interface.
public interface AuthApi {

    /**
     * @param body e-mail (or rider mobile), password, tab
     * @return token and profile
     */
    @POST("auth/login")
    Call<AuthResponse> login(@Body LoginRequest body);

    /**
     * @param body sign-up form
     * @return token and profile of the new customer
     */
    @POST("auth/register")
    Call<AuthResponse> register(@Body RegisterRequest body);

    /**
     * @param body Google ID token
     * @return token and profile
     */
    @POST("auth/google")
    Call<AuthResponse> google(@Body GoogleLoginRequest body);
}
