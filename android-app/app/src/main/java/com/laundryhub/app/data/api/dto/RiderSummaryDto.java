package com.laundryhub.app.data.api.dto;

/** {@code GET /api/rider/summary} and {@code PATCH /api/rider/duty}. Filled by Gson. */
public final class RiderSummaryDto {

    // OOP: Encapsulation — read-only fields set by Gson.
    private String riderName;
    private boolean onDuty;
    private long availableCount;
    private long acceptedCount;
    private long doneTodayCount;

    /** For Gson. */
    public RiderSummaryDto() {
    }

    /** @return rider name */
    public String getRiderName() {
        return riderName;
    }

    /** @return duty switch */
    public boolean isOnDuty() {
        return onDuty;
    }

    /** @return "Available" counter */
    public long getAvailableCount() {
        return availableCount;
    }

    /** @return "Accepted" counter */
    public long getAcceptedCount() {
        return acceptedCount;
    }

    /** @return "Done today" counter */
    public long getDoneTodayCount() {
        return doneTodayCount;
    }
}
