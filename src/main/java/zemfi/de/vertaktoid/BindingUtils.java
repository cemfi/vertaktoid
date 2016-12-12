package zemfi.de.vertaktoid;

/**
 * Created by yevgen on 24.11.2016.
 */

public class BindingUtils {
    public static String action(StatusStrings.ActionId action) {

        String result = StatusStrings.actionStrs.get(action);
        if(result == null) {
            return StatusStrings.actionStrs.get(StatusStrings.ActionId.UNKNOWN);
        }
        return result;

    }

    public static String status(StatusStrings.StatusId status) {

        String result = StatusStrings.statusStrs.get(status);
        if(result == null) {
            return StatusStrings.statusStrs.get(StatusStrings.StatusId.UNKNOWN);
        }
        return result;

    }
}
