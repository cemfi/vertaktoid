package zemfi.de.vertaktoid.helpers;

/**
 * Status values binding helper.
 */

public class BindingUtils {
    /**
     * Gets the value of action related to the giving action id.
     * @param action The action id.
     * @return The action value.
     */
    public static String action(StatusStrings.ActionId action) {

        String result = StatusStrings.actionStrs.get(action);
        if(result == null) {
            return StatusStrings.actionStrs.get(StatusStrings.ActionId.UNKNOWN);
        }
        return result;

    }

    /**
     * Gets the status value related to the giving status id.
     * @param status The status id.
     * @return The status value.
     */
    public static String status(StatusStrings.StatusId status) {

        String result = StatusStrings.statusStrs.get(status);
        if(result == null) {
            return StatusStrings.statusStrs.get(StatusStrings.StatusId.UNKNOWN);
        }
        return result;

    }
}
