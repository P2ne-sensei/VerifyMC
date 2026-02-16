package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import org.json.JSONObject;

@FunctionalInterface
public interface JsonResponseWriter {
    void write(HttpExchange exchange, JSONObject body) throws IOException;
}
