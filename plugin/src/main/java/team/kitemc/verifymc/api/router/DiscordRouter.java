package team.kitemc.verifymc.api.router;

import com.sun.net.httpserver.HttpServer;
import team.kitemc.verifymc.api.handler.DiscordAuthApiHandler;
import team.kitemc.verifymc.api.handler.DiscordCallbackApiHandler;
import team.kitemc.verifymc.api.handler.DiscordStatusApiHandler;
import team.kitemc.verifymc.web.WebServer;

public class DiscordRouter {
    public void register(HttpServer server, WebServer webServer) {
        server.createContext("/api/discord/auth", new DiscordAuthApiHandler(webServer));
        server.createContext("/api/discord/callback", new DiscordCallbackApiHandler(webServer));
        server.createContext("/api/discord/status", new DiscordStatusApiHandler(webServer));
    }
}
