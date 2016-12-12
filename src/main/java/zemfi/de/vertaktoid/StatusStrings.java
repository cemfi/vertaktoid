package zemfi.de.vertaktoid;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by yevgen on 12.12.2016.
 */

public class StatusStrings {

    public enum StatusId {
        UNKNOWN, SUCCESS, FAIL
    }

    public enum ActionId {
        UNKNOWN, STARTED, SAVED, TMP_SAVED, LOADED
    }

    static Map<StatusId, String> statusStrs;
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
