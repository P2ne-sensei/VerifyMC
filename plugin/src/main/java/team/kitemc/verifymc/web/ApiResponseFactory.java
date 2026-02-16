package team.kitemc.verifymc.web;

import org.json.JSONObject;

public final class ApiResponseFactory {
    public static final String ERROR_CODE_NONE = "NONE";
    public static final String ERROR_CODE_BUSINESS = "BUSINESS_ERROR";
    public static final String ERROR_CODE_SYSTEM = "SYSTEM_ERROR";

    private ApiResponseFactory() {
    }

    public static JSONObject success(String message) {
        return success(message, null, null);
    }

    public static JSONObject failure(String message) {
        return businessFailure(message, ERROR_CODE_BUSINESS, null);
    }

    public static JSONObject create(boolean success, String message) {
        return create(success, message, success ? ERROR_CODE_NONE : ERROR_CODE_BUSINESS, null);
    }

    public static JSONObject success(String message, JSONObject data, String requestId) {
        return create(true, message, ERROR_CODE_NONE, requestId).put("data", data == null ? JSONObject.NULL : data);
    }

    public static JSONObject businessFailure(String message, String errorCode, String requestId) {
        return create(false, message, errorCode == null || errorCode.isBlank() ? ERROR_CODE_BUSINESS : errorCode, requestId);
    }

    public static JSONObject systemError(String message, String errorCode, String requestId) {
        return create(false, message, errorCode == null || errorCode.isBlank() ? ERROR_CODE_SYSTEM : errorCode, requestId)
                .put("systemError", true);
    }

    public static JSONObject create(boolean success, String message, String errorCode, String requestId) {
        JSONObject response = new JSONObject();
        response.put("success", success);
        response.put("msg", message);
        response.put("message", message);
        response.put("errorCode", errorCode == null ? ERROR_CODE_NONE : errorCode);
        response.put("requestId", requestId == null ? JSONObject.NULL : requestId);
        response.put("type", success ? "SUCCESS" : "FAILURE");
        return response;
    }
}
