package zemfi.de.vertaktoid;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps and enums for status and action values.
 */

public class StatusStrings {

    // Status id enum.
    public enum StatusId {
        UNKNOWN, SUCCESS, FAIL
    }

    // Action id enum.
    public enum ActionId {
        UNKNOWN, STARTED, SAVED, TMP_SAVED, LOADED
    }

    // Status map in form id -> value
    static Map<StatusId, String> statusStrs;
    // Action map in form id -> value
    static Map<ActionId, String> actionStrs;

    static {
        statusStrs = new HashMap<>();
        statusStrs.put(StatusId.UNKNOWN, "Unknown StatusId");
        statusStrs.put(StatusId.SUCCESS, "Successful");
        statusStrs.put(StatusId.FAIL, "Failure");

        actionStrs = new HashMap<>();
        actionStrs.put(ActionId.UNKNOWN, "Unknown ActionId");
        actionStrs.put(ActionId.STARTED, "Application started");
        actionStrs.put(ActionId.SAVED, "Saved");
        actionStrs.put(ActionId.TMP_SAVED, "Temporary saved");
        actionStrs.put(ActionId.LOADED, "Loaded");
    }

}
