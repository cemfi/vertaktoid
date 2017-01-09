package zemfi.de.vertaktoid;

import android.app.Application;

import java.io.Serializable;

/**
 * Created by yevgen on 12.12.2016.
 */

public class Vertaktoid extends Application implements Serializable {
    public final static String DEFAULT_MEI_FILENAME = "mei.mei";
    public final static String NOT_FOUND_STUBIMG = "facsimile404.png";
    public final static String APP_SUBFOLDER = "vertaktoid";
    public void onCreate() {
        super.onCreate();
    }
}
