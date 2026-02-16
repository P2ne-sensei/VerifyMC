package team.kitemc.verifymc.application.dto;

import org.json.JSONObject;

public record DiscordAuthRequestDto(String username) {
    public static DiscordAuthRequestDto fromJson(String body) {
        JSONObject req = new JSONObject(body == null ? "{}" : body);
        return new DiscordAuthRequestDto(req.optString("username", ""));
    }
}
