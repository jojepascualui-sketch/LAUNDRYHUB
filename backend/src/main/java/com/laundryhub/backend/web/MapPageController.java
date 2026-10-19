package com.laundryhub.backend.web;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

/**
 * The A2 "Pinned location" map for the admin console's WebView. The page is served from this API so the
 * Maps JavaScript API key ({@code MAPS_JS_KEY}) stays on the server and can be restricted to this site's
 * address (HTTP referrer) in Google Cloud. Without a key the page shows the plain design frame.
 */
@RestController
@RequestMapping("/api/maps")
public class MapPageController {

    private static final BigDecimal MAX_LATITUDE = BigDecimal.valueOf(90);
    private static final BigDecimal MAX_LONGITUDE = BigDecimal.valueOf(180);
    private static final int ZOOM = 16;
    /** Design colors (DESIGN_SPEC.md §1): map background, pin, white, muted text. */
    private static final String MAP_BG = "#E3EAE2";
    private static final String MUTED = "#4A5863";
    /** The design pin (40 × 48 view box, #E0662A, white stroke and dot), drawn at 34 × 42 like A2. */
    private static final String PIN_SVG = "<svg xmlns='http://www.w3.org/2000/svg' width='34' height='42' "
            + "viewBox='0 0 40 48'><path d='M20 47s16-15.6 16-27A16 16 0 0 0 4 20c0 11.4 16 27 16 27z' "
            + "fill='%23E0662A' stroke='%23fff' stroke-width='2.5'/><circle cx='20' cy='20' r='6' fill='%23fff'/></svg>";
    private static final int PIN_ANCHOR_X = 17;
    private static final int PIN_ANCHOR_Y = 42;
    private static final String NO_KEY = "Set MAPS_JS_KEY on the server to show the map.";

    // OOP: Encapsulation — the key is read once from configuration and never returned on its own.
    private final String mapsKey;

    /**
     * @param mapsKey Maps JavaScript API key from {@code MAPS_JS_KEY}; blank = no map
     */
    public MapPageController(@Value("${laundryhub.maps.js-key:}") String mapsKey) {
        this.mapsKey = mapsKey == null ? "" : mapsKey.trim();
    }

    /**
     * @param lat pin latitude, -90 to 90
     * @param lng pin longitude, -180 to 180
     * @return a small HTML page with the Google map and the design's orange pin
     * @throws IllegalArgumentException if a coordinate is out of range (400)
     */
    @GetMapping(value = "/order-pin", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "A2 Pinned location map page (admin console WebView)")
    public ResponseEntity<String> orderPin(@RequestParam BigDecimal lat, @RequestParam BigDecimal lng) {
        if (lat.abs().compareTo(MAX_LATITUDE) > 0 || lng.abs().compareTo(MAX_LONGITUDE) > 0) {
            throw new IllegalArgumentException("Coordinates are out of range");
        }
        String body = mapsKey.isEmpty() ? placeholder() : map(lat.toPlainString(), lng.toPlainString());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.TEXT_HTML).body(body);
    }

    private static String placeholder() {
        return "<!doctype html><html><head><meta charset='utf-8'><style>html,body{margin:0;height:100%;background:"
                + MAP_BG + ";font:13px 'DM Sans',sans-serif;color:" + MUTED + ";display:flex;align-items:center;"
                + "justify-content:center}</style></head><body>" + NO_KEY + "</body></html>";
    }

    private String map(String lat, String lng) {
        String key = URLEncoder.encode(mapsKey, StandardCharsets.UTF_8);
        return "<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width'>"
                + "<style>html,body,#map{margin:0;height:100%;background:" + MAP_BG + "}</style></head><body>"
                + "<div id='map'></div><script>function init(){var p={lat:" + lat + ",lng:" + lng + "};"
                + "var m=new google.maps.Map(document.getElementById('map'),{center:p,zoom:" + ZOOM + ","
                + "disableDefaultUI:true,gestureHandling:'cooperative'});"
                + "new google.maps.Marker({position:p,map:m,icon:{url:\"data:image/svg+xml;utf8," + PIN_SVG + "\","
                + "anchor:new google.maps.Point(" + PIN_ANCHOR_X + "," + PIN_ANCHOR_Y + ")}});}</script>"
                + "<script async src='https://maps.googleapis.com/maps/api/js?key=" + key + "&callback=init'></script>"
                + "</body></html>";
    }
}
