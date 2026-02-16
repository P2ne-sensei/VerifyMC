package team.kitemc.verifymc.application.usecase;

import org.json.JSONObject;

public class UseCaseFailureException extends RuntimeException {
    private final String errorCode;
    private final String messageKey;
    private final JSONObject responseFields;
    private final boolean systemError;

    public UseCaseFailureException(String errorCode, String messageKey, JSONObject responseFields, boolean systemError) {
        super(messageKey);
        this.errorCode = errorCode;
        this.messageKey = messageKey;
        this.responseFields = responseFields;
        this.systemError = systemError;
    }

    public static UseCaseFailureException business(String errorCode, String messageKey) {
        return new UseCaseFailureException(errorCode, messageKey, null, false);
    }

    public static UseCaseFailureException business(String errorCode, String messageKey, JSONObject responseFields) {
        return new UseCaseFailureException(errorCode, messageKey, responseFields, false);
    }

    public static UseCaseFailureException system(String errorCode, String messageKey) {
        return new UseCaseFailureException(errorCode, messageKey, null, true);
    }

    public String errorCode() {
        return errorCode;
    }

    public String messageKey() {
        return messageKey;
    }

    public JSONObject responseFields() {
        return responseFields;
    }

    public boolean systemError() {
        return systemError;
    }
}
