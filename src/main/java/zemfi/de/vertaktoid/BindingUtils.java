package zemfi.de.vertaktoid;

import android.content.res.Resources;

/**
 * Created by yevgen on 24.11.2016.
 */

public class BindingUtils {
    public static String string(int resourceId) {
        try {
            return Vertaktoid.getAppContext().getString(resourceId);
        }
        catch (Resources.NotFoundException e)
        {
            return "not found";
        }

    }
}
