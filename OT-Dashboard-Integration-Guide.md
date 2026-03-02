# OT-Dashboard API Integration Guide

This guide outlines the complete A-Z process for integrating any Minecraft plugin (or backend application) with the OT-Dashboard API. By following these steps, your plugin will automatically support Live Instance tracking, Version Control enforcement, and Remote Instance Blocking.

---

## 1. Dynamic API URL Resolution

To ensure the API domain can be updated without hardcoding new versions of the plugin, you must dynamically fetch the API URL on startup.

**Endpoint:** `GET https://raw.githubusercontent.com/Open-Trident/info/refs/heads/main/ot-dashboard.json`

**Expected JSON Response:**

```json
{
  "links": {
    "dashboard": "https://dashboard.dlm.lol",
    "api": "https://api.dlm.lol"
  }
}
```

**Implementation Step:**
Read the `links.api` field and cache it in memory. If the request fails or times out, safely fall back to `https://api.dlm.lol`.

---

## 2. Server Identity Tracking (Instance & Hardware ID)

Your plugin must generate a persistent Unique Identifier (UUID) to uniquely identify the server instance running the plugin.

**Implementation Step:**

1. On `onEnable()`, check your plugin's `config.yml` for a `server-id` key.
2. If it does not exist, generate a new `UUID.randomUUID().toString()`, save it to the config, and write the file to disk.
3. Cache this `server-id` in memory. This will be used as **both** your `instanceId` and `hardwareId`.

---

## 3. The Startup Validation Check

When the plugin boots up, it must perform a version and license check against the dashboard. This should run asynchronously after a slight delay (e.g., 3-5 seconds) so it doesn't freeze the server startup thread.

**Endpoint:** `GET {API_URL}/v1/config/{PUBLIC_ID}?env=prod&version={PLUGIN_VERSION}&instanceId={SERVER_ID}&hardwareId={SERVER_ID}`
*Note: Replace `{PUBLIC_ID}` with your project's unique Client ID (e.g., `pub_e4134f633fb8e156`).*

### Handling the Response

* **HTTP 403 Forbidden:** The instance has been blocked by the dashboard. You must log a `SEVERE` message and immediately disable the plugin via Bukkit's PluginManager.
* **HTTP 200 OK:** Parse the resulting JSON payload.

**JSON Payload Example:**

```json
{
  "project": {
    "latestVersion": "1.4.0",
    "updateUrl": "https://github.com/Open-Trident/...",
    "updateRequired": true
  }
}
```

**Logic:**

1. Check `project.updateRequired`. If `true`, the server is running a version below the dashboard's Minimum Version threshold. You **must** log a critical alert containing the `project.updateUrl` and forcefully disable the plugin.
2. Check `project.latestVersion`. If it is higher than `{PLUGIN_VERSION}` but `updateRequired` is `false`, print a friendly console warning that a new update is available.

---

## 4. The Live Heartbeat Task

To keep the "Live Instances" page populated on the dashboard and allow real-time remote blocking, the plugin must continuously ping the dashboard.

**Endpoint:** `POST {API_URL}/v1/heartbeat/{PUBLIC_ID}`

**Task Setup:**
Create an asynchronous repeating task (e.g., a `BukkitRunnable`) that fires every **10 minutes** (12000 ticks).
*Note: Make sure to fire the first heartbeat immediately after the startup check, even if the plugin was blocked, so the dashboard logs the blocked instance.*

**JSON Request Body Payload:**

```json
{
  "instanceId": "your-saved-server-uuid",
  "hardwareId": "your-saved-server-uuid",
  "platform": "Windows 11",
  "version": "1.0.0"
}
```

*(You can get the platform via `System.getProperty("os.name")`)*

### Handling the Response

* **HTTP 403 Forbidden:** The server administrator just blocked this instance ID or IP address from the dashboard. Log a severe warning that the plugin was disabled remotely, and forcibly disable the plugin immediately. Terminate the heartbeat loop.
* **HTTP 200 OK:** Ignore (Heartbeat successful).

---

## Reference Checklist for Developers

- [ ] Implement `ApiClient` to fetch the base URL from GitHub `.json`
* [ ] Auto-generate and save a `UUID` to `config.yml` on first boot
* [ ] Fire `GET /v1/config/` asynchronously on startup
* [ ] Parse `updateRequired` and disable the plugin if forced
* [ ] Parse `403 Forbidden` and disable the plugin if blocked
* [ ] Create Async Heartbeat Scheduler (POST every 10 minutes)
* [ ] Handle `403 Forbidden` on the heartbeat to block live instances
