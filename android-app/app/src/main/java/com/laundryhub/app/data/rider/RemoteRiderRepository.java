package com.laundryhub.app.data.rider;

import androidx.annotation.NonNull;

import com.laundryhub.app.data.ApiCallback;
import com.laundryhub.app.data.ApiCalls;
import com.laundryhub.app.data.ApiException;
import com.laundryhub.app.data.api.RiderApi;
import com.laundryhub.app.data.api.dto.DutyRequestDto;
import com.laundryhub.app.data.api.dto.RiderJobDto;
import com.laundryhub.app.data.api.dto.RiderLocationDto;
import com.laundryhub.app.data.api.dto.RiderSummaryDto;
import com.laundryhub.app.model.Location;
import com.laundryhub.app.model.PaymentOption;
import com.laundryhub.app.model.rider.JobStep;
import com.laundryhub.app.model.rider.JobType;
import com.laundryhub.app.model.rider.RiderJob;
import com.laundryhub.app.model.rider.RiderSummary;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Calls {@code /api/rider/**} and maps the JSON to app models. */
// OOP: Inheritance — implements the RiderRepository contract.
public class RemoteRiderRepository implements RiderRepository {

    private static final String ADDRESS_SEPARATOR = " · ";

    // OOP: Encapsulation — the Retrofit interface is fixed at construction.
    private final RiderApi api;

    /**
     * @param api rider endpoints
     */
    public RemoteRiderRepository(@NonNull RiderApi api) {
        this.api = api;
    }

    // OOP: Polymorphism — the Retrofit-backed implementation of each RiderRepository method.
    @Override
    public void summary(@NonNull ApiCallback<RiderSummary> callback) {
        ApiCalls.enqueue(api.summary(), mapped(callback, RemoteRiderRepository::toSummary));
    }

    @Override
    public void setDuty(boolean onDuty, @NonNull ApiCallback<RiderSummary> callback) {
        ApiCalls.enqueue(api.duty(new DutyRequestDto(onDuty)), mapped(callback, RemoteRiderRepository::toSummary));
    }

    @Override
    public void available(@NonNull ApiCallback<List<RiderJob>> callback) {
        ApiCalls.enqueue(api.available(), mapped(callback, RemoteRiderRepository::toJobs));
    }

    @Override
    public void mine(@NonNull ApiCallback<List<RiderJob>> callback) {
        ApiCalls.enqueue(api.mine(), mapped(callback, RemoteRiderRepository::toJobs));
    }

    @Override
    public void job(long jobId, @NonNull ApiCallback<RiderJob> callback) {
        ApiCalls.enqueue(api.job(jobId), mapped(callback, RemoteRiderRepository::toJob));
    }

    @Override
    public void accept(long jobId, @NonNull ApiCallback<RiderJob> callback) {
        ApiCalls.enqueue(api.accept(jobId), mapped(callback, RemoteRiderRepository::toJob));
    }

    @Override
    public void decline(long jobId, @NonNull ApiCallback<Boolean> callback) {
        noBody(api.decline(jobId), callback);
    }

    @Override
    public void completeStep(@NonNull RiderJob job, @NonNull ApiCallback<RiderJob> callback) {
        Call<RiderJobDto> call = job.isAwaitingCash() ? api.cashCollected(job.getId()) : api.advance(job.getId());
        ApiCalls.enqueue(call, mapped(callback, RemoteRiderRepository::toJob));
    }

    @Override
    public void sendLocation(@NonNull Location location, BigDecimal accuracyMeters,
                             @NonNull ApiCallback<Boolean> callback) {
        noBody(api.location(new RiderLocationDto(location.getLatitude(), location.getLongitude(), accuracyMeters)),
                callback);
    }

    /** 204 responses have no body, which ApiCalls treats as an error. */
    private static void noBody(Call<Void> call, ApiCallback<Boolean> callback) {
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> c, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(true);
                } else {
                    callback.onError(new ApiException(response.code(), null, null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> c, @NonNull Throwable t) {
                callback.onError(ApiException.network(t));
            }
        });
    }

    static RiderSummary toSummary(RiderSummaryDto dto) {
        return new RiderSummary(dto.getRiderName() == null ? "" : dto.getRiderName(), dto.isOnDuty(),
                dto.getAvailableCount(), dto.getAcceptedCount(), dto.getDoneTodayCount());
    }

    static List<RiderJob> toJobs(List<RiderJobDto> dtos) {
        List<RiderJob> jobs = new ArrayList<>();
        for (RiderJobDto dto : dtos) {
            jobs.add(toJob(dto));
        }
        return jobs;
    }

    /**
     * @param dto server job
     * @return the app model
     */
    static RiderJob toJob(RiderJobDto dto) {
        List<JobStep> steps = new ArrayList<>();
        for (RiderJobDto.Step step : dto.getSteps()) {
            steps.add(new JobStep(step.getTitle(), step.getActionLabel()));
        }
        RiderJobDto.Pin pin = dto.getCustomerPin();
        return new RiderJob(dto.getId(), dto.getOrderNo(), JobType.valueOf(dto.getType()), status(dto.getStatus()),
                dto.getDistanceKm(), dto.getCustomerName(), dto.getCustomerInitials(), dto.getCustomerMobile(),
                address(pin), point(pin), point(dto.getShop()), dto.getScheduleText(), dto.getBillableKg(),
                dto.getTotal(), dto.getPayment() == null ? PaymentOption.CASH : PaymentOption.valueOf(dto.getPayment().getMethod()),
                steps, Math.min(dto.getCurrentStepIndex(), steps.size()), dto.isAwaitingCash());
    }

    private static RiderJob.Status status(String value) {
        try {
            return RiderJob.Status.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            return RiderJob.Status.OTHER;
        }
    }

    /** "Brgy. Poblacion · [STREET]": the customer's details, then Google's address if different. */
    static String address(RiderJobDto.Pin pin) {
        if (pin == null) {
            return "";
        }
        String details = pin.getAddressDetails() == null ? "" : pin.getAddressDetails().trim();
        String geocoded = pin.getGeocodedAddress() == null ? "" : pin.getGeocodedAddress().trim();
        if (details.isEmpty()) {
            return geocoded;
        }
        if (geocoded.isEmpty() || geocoded.equals(details)) {
            return details;
        }
        return details + ADDRESS_SEPARATOR + geocoded;
    }

    private static Location point(RiderJobDto.Point point) {
        return point == null || point.getLatitude() == null || point.getLongitude() == null
                ? null : new Location(point.getLatitude(), point.getLongitude());
    }

    private static <A, B> ApiCallback<A> mapped(ApiCallback<B> callback, Function<A, B> mapper) {
        return new ApiCallback<>() {
            @Override
            public void onSuccess(@NonNull A value) {
                B result;
                try {
                    result = mapper.apply(value);
                } catch (RuntimeException e) {
                    callback.onError(new ApiException(ApiException.NO_RESPONSE, null, null));
                    return;
                }
                callback.onSuccess(result);
            }

            @Override
            public void onError(@NonNull ApiException error) {
                callback.onError(error);
            }
        };
    }
}
