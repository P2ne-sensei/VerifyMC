package team.kitemc.verifymc.application.dto;

import org.json.JSONObject;

public record DiscordCallbackResponseDto(boolean success, String msg) {
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("success", success);
        json.put("msg", msg);
        return json;
    }
}
