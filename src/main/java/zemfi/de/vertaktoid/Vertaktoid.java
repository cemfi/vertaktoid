package zemfi.de.vertaktoid;

import android.app.Application;

import java.io.Serializable;

/**
 * Created by yevgen on 12.12.2016.
 */

public class Vertaktoid extends Application implements Serializable {
    public final static String DEFAULT_MEI_FILENAME = "mei.mei";
    public void onCreate() {
        super.onCreate();
    }
}
