package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.function.Consumer;
import org.json.JSONObject;
import team.kitemc.verifymc.application.config.ConfigProvider;
import team.kitemc.verifymc.service.DiscordService;
import team.kitemc.verifymc.service.QuestionnaireService;

public class ConfigHandler implements HttpHandler {
    private final ConfigProvider configProvider;
    private final QuestionnaireService questionnaireService;
    private final DiscordService discordService;
    private final Consumer<String> debugLog;
    private final JsonResponseWriter responseWriter;

    public ConfigHandler(ConfigProvider configProvider, QuestionnaireService questionnaireService, DiscordService discordService,
                         Consumer<String> debugLog, JsonResponseWriter responseWriter) {
        this.configProvider = configProvider;
        this.questionnaireService = questionnaireService;
        this.discordService = discordService;
        this.debugLog = debugLog;
        this.responseWriter = responseWriter;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        JSONObject resp = new JSONObject();
        org.bukkit.configuration.file.FileConfiguration config = configProvider.raw();
        JSONObject login = new JSONObject();
        login.put("enable_email", config.getStringList("auth_methods").contains("email"));
        login.put("email_smtp", config.getString("smtp.host", ""));
        JSONObject admin = new JSONObject();
        JSONObject frontend = new JSONObject();
        frontend.put("theme", config.getString("frontend.theme", "default"));
        frontend.put("logo_url", config.getString("frontend.logo_url", ""));
        frontend.put("announcement", config.getString("frontend.announcement", ""));
        frontend.put("web_server_prefix", config.getString("web_server_prefix", "[VerifyMC]"));
        frontend.put("current_theme", config.getString("frontend.theme", "default"));
        JSONObject authme = new JSONObject();
        authme.put("enabled", config.getBoolean("authme.enabled", false));
        authme.put("require_password", config.getBoolean("authme.require_password", false));
        authme.put("password_regex", config.getString("authme.password_regex", "^[a-zA-Z0-9_]{8,26}$"));
        frontend.put("username_regex", config.getString("username_regex", "^[a-zA-Z0-9_-]{3,16}$"));

        JSONObject captcha = new JSONObject();
        java.util.List<String> authMethods = config.getStringList("auth_methods");
        debugLog.accept("auth_methods from config: " + authMethods);
        debugLog.accept("captcha enabled: " + authMethods.contains("captcha"));
        captcha.put("enabled", authMethods.contains("captcha"));
        captcha.put("email_enabled", authMethods.contains("email"));
        captcha.put("type", config.getString("captcha.type", "math"));

        JSONObject bedrock = new JSONObject();
        bedrock.put("enabled", config.getBoolean("bedrock.enabled", false));
        bedrock.put("prefix", config.getString("bedrock.prefix", "."));
        bedrock.put("username_regex", config.getString("bedrock.username_regex", "^\\.[a-zA-Z0-9_\\s]{3,16}$"));

        JSONObject questionnaire = new JSONObject();
        questionnaire.put("enabled", questionnaireService.isEnabled());
        questionnaire.put("pass_score", questionnaireService.getPassScore());
        questionnaire.put("has_text_questions", questionnaireService.hasTextQuestions());

        JSONObject discord = new JSONObject();
        discord.put("enabled", discordService.isEnabled());
        discord.put("required", discordService.isRequired());

        resp.put("login", login);
        resp.put("admin", admin);
        resp.put("frontend", frontend);
        resp.put("authme", authme);
        resp.put("captcha", captcha);
        resp.put("bedrock", bedrock);
        resp.put("questionnaire", questionnaire);
        resp.put("discord", discord);
        responseWriter.write(exchange, resp);
    }
}
