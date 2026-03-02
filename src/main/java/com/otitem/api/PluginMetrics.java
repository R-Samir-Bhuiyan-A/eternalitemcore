package com.otitem.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.otitem.OTItem;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Logger;

public class PluginMetrics {

    private final OTItem plugin;
    private final Logger logger;
    private String serverId;
    private static final String PUBLIC_ID = "pub_e4134f633fb8e156";

    public PluginMetrics(OTItem plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void init() {
        // Load or generate server ID
        this.serverId = plugin.getConfig().getString("server-id");
        if (this.serverId == null || this.serverId.isEmpty()) {
            this.serverId = UUID.randomUUID().toString();
            plugin.getConfig().set("server-id", this.serverId);
            plugin.saveConfig();
        }

        // Run validation and heartbeat async after a slight delay
        new BukkitRunnable() {
            @Override
            public void run() {
                validateAndStartHeartbeat();
            }
        }.runTaskLaterAsynchronously(plugin, 60L);
    }

    private void validateAndStartHeartbeat() {
        String apiUrl = ApiClient.getApiUrl(logger);
        String rawVersion = plugin.getDescription().getVersion();
        
        // Fix for when maven resource filtering fails
        final String version = rawVersion.contains("${") ? "1.0.0" : rawVersion;
        
        // 1. Send initial heartbeat so the backend registers this instance even if it's about to be blocked
        sendHeartbeat(apiUrl, version);

        // 2. Validate configuration and block if necessary
        try {
            String configUrlStr = String.format("%s/v1/config/%s?env=prod&version=%s&instanceId=%s&hardwareId=%s",
                    apiUrl, PUBLIC_ID, version, serverId, serverId);
            URL configUrl = new URL(configUrlStr);
            HttpURLConnection conn = (HttpURLConnection) configUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 403) {
                disablePlugin("Your server instance has been blocked by the OT-Dashboard. Please contact support.");
                return;
            }

            if (responseCode == 200) {
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    if (json.has("project")) {
                        JsonObject project = json.getAsJsonObject("project");
                        
                        // Check if an update is strictly required
                        if (project.has("updateRequired") && project.get("updateRequired").getAsBoolean()) {
                            String updateUrl = project.has("updateUrl") && !project.get("updateUrl").isJsonNull() && !project.get("updateUrl").getAsString().isEmpty() ? project.get("updateUrl").getAsString() : "your dashboard or download page.";
                            disablePlugin("A critical update is required for OT-item. Please update immediately: " + updateUrl);
                            return;
                        } else if (project.has("latestVersion")) {
                            String latest = project.get("latestVersion").getAsString();
                            if (!latest.equals(version)) {
                                logger.info("[OT-Item] A new version (" + latest + ") is available! Currently running: " + version);
                            } else {
                                logger.info("[OT-Item] Plugin is up-to-date (Version: " + version + ")");
                            }
                        }
                    }
                }
            } else {
                logger.warning("[OT-Item] Dashboard validation returned unexpected status: " + responseCode);
            }

            // 3. Start recurring heartbeat task if validation passed
            startRecurringHeartbeatTask(apiUrl, version);

        } catch (Exception e) {
            logger.warning("[OT-Item] Failed to connect to OT-Dashboard API for configuration. Some features may be unavailable.");
            // We still start the recurring heartbeat just in case config endpoint was temporarily down
            startRecurringHeartbeatTask(apiUrl, version);
        }
    }

    private boolean sendHeartbeat(String apiUrl, String version) {
        try {
            String platform = System.getProperty("os.name");
            URL url = new URL(apiUrl + "/v1/heartbeat/" + PUBLIC_ID);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            JsonObject payload = new JsonObject();
            payload.addProperty("instanceId", serverId);
            payload.addProperty("hardwareId", serverId);
            payload.addProperty("platform", platform);
            payload.addProperty("version", version);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            if (code == 403) {
                disablePlugin("Your server instance has been blocked remotely. Disabling OT-Item.");
                return false;
            }
            return true;
        } catch (Exception e) {
            return true; // Ignore transient heartbeat failures
        }
    }

    private void startRecurringHeartbeatTask(String apiUrl, String version) {
        // 10 minutes heartbeat (ticks = 20 * 60 * 10)
        new BukkitRunnable() {
            @Override
            public void run() {
                boolean active = sendHeartbeat(apiUrl, version);
                if (!active) {
                    this.cancel();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 60 * 10, 20L * 60 * 10);
    }

    private void disablePlugin(String reason) {
        logger.severe("========================================");
        logger.severe("[OT-Item] CRITICAL ALERT");
        logger.severe(reason);
        logger.severe("========================================");
        
        // Schedule turning off on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.getPluginManager().disablePlugin(plugin);
        });
    }
}
