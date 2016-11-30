package zemfi.de.vertaktoid;

import android.app.Application;
import android.content.Context;

/**
 * Created by yevgen on 24.11.2016.
 */

public class Vertaktoid extends Application {
    private static Context context;

    public void onCreate() {
        super.onCreate();
        Vertaktoid.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return Vertaktoid.context;
    }
}
