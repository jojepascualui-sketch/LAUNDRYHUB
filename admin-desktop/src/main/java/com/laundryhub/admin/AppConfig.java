package com.laundryhub.admin;

import java.util.Map;

/**
 * Where the backend is. Read from the {@code LAUNDRYHUB_API_URL} environment variable, else the
 * {@code laundryhub.apiUrl} system property (set by the Windows installer, see
 * {@code package-windows.ps1}), else {@value #DEFAULT_API_URL}. No secrets live in the console: the A2
 * map page, with its Maps key, is served by the backend.
 */
public final class AppConfig {

    /** The backend on this computer. */
    public static final String DEFAULT_API_URL = "http://localhost:8080/api/";

    static final String API_URL_VARIABLE = "LAUNDRYHUB_API_URL";
    static final String API_URL_PROPERTY = "laundryhub.apiUrl";
    private static final String MAP_PAGE = "maps/order-pin";

    // OOP: Encapsulation — immutable settings, validated once.
    private final String apiBaseUrl;

    /**
     * @param apiBaseUrl API root ending in "/"
     * @throws IllegalArgumentException if the URL is blank or does not end with "/"
     */
    public AppConfig(String apiBaseUrl) {
        if (apiBaseUrl == null || apiBaseUrl.isBlank() || !apiBaseUrl.endsWith("/")) {
            throw new IllegalArgumentException("API base URL must end with '/'");
        }
        this.apiBaseUrl = apiBaseUrl;
    }

    /**
     * @param environment usually {@code System.getenv()}
     * @param properties  usually {@code System.getProperties()} as strings
     * @return the settings: environment first, then the installer's property, then the default
     */
    public static AppConfig from(Map<String, String> environment, Map<String, String> properties) {
        String url = environment.getOrDefault(API_URL_VARIABLE, "");
        if (url.isBlank()) {
            url = properties.getOrDefault(API_URL_PROPERTY, "");
        }
        if (url.isBlank()) {
            url = DEFAULT_API_URL;
        } else if (!url.endsWith("/")) {
            url = url + "/";
        }
        return new AppConfig(url.trim());
    }

    /** @return the settings of this process */
    public static AppConfig fromSystem() {
        Map<String, String> properties = new java.util.HashMap<>();
        System.getProperties().forEach((k, v) -> properties.put(String.valueOf(k), String.valueOf(v)));
        return from(System.getenv(), properties);
    }

    /** @return API root, e.g. {@code http://localhost:8080/api/} */
    public String apiBaseUrl() {
        return apiBaseUrl;
    }

    /**
     * @param latitude  pin latitude
     * @param longitude pin longitude
     * @return the backend page with the Google map for A2 "Pinned location"
     */
    public String mapPageUrl(String latitude, String longitude) {
        return apiBaseUrl + MAP_PAGE + "?lat=" + latitude + "&lng=" + longitude;
    }
}
