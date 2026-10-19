package com.laundryhub.app.data.api.dto;

/** One time chip from {@code GET /api/orders/slots}. Filled by Gson. */
public final class SlotDto {

    // OOP: Encapsulation — read-only fields set by Gson.
    private String date;
    private String label;
    private String text;

    /** For Gson. */
    public SlotDto() {
    }

    /**
     * @param date  ISO day
     * @param label chip text after the day
     * @param text  "Today, 2–4 PM"
     */
    public SlotDto(String date, String label, String text) {
        this.date = date;
        this.label = label;
        this.text = text;
    }

    /** @return ISO day, e.g. 2026-10-19 */
    public String getDate() {
        return date;
    }

    /** @return label sent back when ordering */
    public String getLabel() {
        return label;
    }

    /** @return chip text */
    public String getText() {
        return text;
    }
}
