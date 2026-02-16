package team.kitemc.verifymc.application.dto;

import org.json.JSONObject;

public record DiscordAuthResponseDto(boolean success, String msg, String authUrl) {
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("success", success);
        if (msg != null) {
            json.put("msg", msg);
        }
        if (authUrl != null) {
            json.put("auth_url", authUrl);
        }
        return json;
    }
}
