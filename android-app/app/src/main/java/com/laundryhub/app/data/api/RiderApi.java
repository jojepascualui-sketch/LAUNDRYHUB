package com.laundryhub.app.data.api;

import com.laundryhub.app.data.api.dto.DutyRequestDto;
import com.laundryhub.app.data.api.dto.RiderJobDto;
import com.laundryhub.app.data.api.dto.RiderLocationDto;
import com.laundryhub.app.data.api.dto.RiderSummaryDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

/** Rider endpoints (role RIDER), docs/SCREEN_API_MAP.md R1–R5. */
// OOP: Abstraction — Retrofit generates the HTTP code behind this interface.
public interface RiderApi {

    /** @return header and counters */
    @GET("rider/summary")
    Call<RiderSummaryDto> summary();

    /**
     * @param body switch position
     * @return updated header
     */
    @PATCH("rider/duty")
    Call<RiderSummaryDto> duty(@Body DutyRequestDto body);

    /**
     * @param body GPS fix
     * @return nothing (204)
     */
    @POST("rider/location")
    Call<Void> location(@Body RiderLocationDto body);

    /** @return open jobs, nearest first */
    @GET("rider/jobs/available")
    Call<List<RiderJobDto>> available();

    /** @return accepted and in-progress jobs */
    @GET("rider/jobs/mine")
    Call<List<RiderJobDto>> mine();

    /**
     * @param id job id
     * @return the job
     */
    @GET("rider/jobs/{id}")
    Call<RiderJobDto> job(@Path("id") long id);

    /**
     * @param id job id
     * @return the accepted job; 409 if another rider was first
     */
    @POST("rider/jobs/{id}/accept")
    Call<RiderJobDto> accept(@Path("id") long id);

    /**
     * @param id job id
     * @return nothing (204)
     */
    @POST("rider/jobs/{id}/decline")
    Call<Void> decline(@Path("id") long id);

    /**
     * @param id job id
     * @return the job after the step
     */
    @POST("rider/jobs/{id}/advance")
    Call<RiderJobDto> advance(@Path("id") long id);

    /**
     * @param id job id
     * @return the job after "Cash received"
     */
    @POST("rider/jobs/{id}/cash-collected")
    Call<RiderJobDto> cashCollected(@Path("id") long id);
}
