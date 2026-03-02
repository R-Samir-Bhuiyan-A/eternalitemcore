package com.otitem.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class ApiClient {
    private static final String GITHUB_URL = "https://raw.githubusercontent.com/Open-Trident/info/refs/heads/main/ot-dashboard.json";
    private static final String DEFAULT_API_URL = "https://api.dlm.lol";
    private static String cachedApiUrl = null;

    /**
     * Resolves the API URL by fetching it from the open-trident github repository.
     */
    public static String getApiUrl(Logger logger) {
        if (cachedApiUrl != null) {
            return cachedApiUrl;
        }

        try {
            URL url = new URL(GITHUB_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    if (json.has("links") && json.getAsJsonObject("links").has("api")) {
                        cachedApiUrl = json.getAsJsonObject("links").get("api").getAsString();
                        return cachedApiUrl;
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("[OT-Item] Failed to fetch dynamic API URL from GitHub, using default: " + DEFAULT_API_URL);
        }

        cachedApiUrl = DEFAULT_API_URL;
        return cachedApiUrl;
    }
}
