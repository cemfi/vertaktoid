package zemfi.de.vertaktoid;

import android.app.Application;

import java.io.Serializable;

/**
 * Application class containing some default strings.
 */

public class Vertaktoid extends Application implements Serializable {
    // Default MEI file name with extension.
    public final static String DEFAULT_MEI_FILENAME = "mei.mei";
    // Not found stub image file name.
    public final static String NOT_FOUND_STUBIMG = "facsimile404.png";
    // Default subfolder name.
    public final static String APP_SUBFOLDER = "vertaktoid";
    // Default MEI name space.
    public final static String MEI_NS = "http://www.music-encoding.org/ns/mei";

    public void onCreate() {
        super.onCreate();
    }
}
