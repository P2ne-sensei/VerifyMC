package team.kitemc.verifymc.application.dto;

import org.json.JSONObject;

public record DiscordStatusResponseDto(boolean success, String msg, boolean linked, JSONObject user) {
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("success", success);
        if (msg != null) {
            json.put("msg", msg);
        }
        json.put("linked", linked);
        if (user != null) {
            json.put("user", user);
        }
        return json;
    }
}
