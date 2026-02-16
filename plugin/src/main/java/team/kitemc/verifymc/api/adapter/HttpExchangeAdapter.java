package team.kitemc.verifymc.api.adapter;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.json.JSONObject;
import team.kitemc.verifymc.application.usecase.UseCaseFailureException;
import team.kitemc.verifymc.web.ApiResponseFactory;
import team.kitemc.verifymc.web.RegistrationRequest;
import team.kitemc.verifymc.web.WebResponseHelper;

public class HttpExchangeAdapter {
    public String resolveRequestId(HttpExchange exchange) {
        String requestId = exchange.getRequestHeaders().getFirst("X-Request-ID");
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
    }

    public RegistrationRequest readRegistrationRequest(HttpExchange exchange, BiFunction<String, String, String> usernameNormalizer) throws IOException {
        JSONObject req = WebResponseHelper.readJson(exchange);
        return RegistrationRequest.fromJson(req, usernameNormalizer);
    }

    public void writeUseCaseFailure(HttpExchange exchange, UseCaseFailureException exception, String language,
                                    String requestId, Function<String, String> messageResolver) throws IOException {
        String message = messageResolver.apply(exception.messageKey());
        JSONObject response = exception.systemError()
                ? ApiResponseFactory.systemError(message, exception.errorCode(), requestId)
                : ApiResponseFactory.businessFailure(message, exception.errorCode(), requestId);

        JSONObject fields = exception.responseFields();
        if (fields != null) {
            if (fields.has("regex")) {
                message = message.replace("{regex}", fields.optString("regex", ""));
                response.put("msg", message).put("message", message);
            }
            for (String key : fields.keySet()) {
                if (!"regex".equals(key)) {
                    response.put(key, fields.get(key));
                }
            }
        }
        response.put("language", language);
        WebResponseHelper.sendJson(exchange, response);
    }

    public void writeSuccess(HttpExchange exchange, JSONObject response) throws IOException {
        WebResponseHelper.sendJson(exchange, response);
    }
}
