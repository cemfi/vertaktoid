package zemfi.de.vertaktoid;

import android.app.Application;

import java.io.Serializable;
import android.content.res.Resources;

/**
 * Application class containing some default strings.
 */

public class Vertaktoid extends Application implements Serializable {
    // Default MEI file name with extension.
    //public final static String DEFAULT_MEI_FILENAME = "mei.mei";
    public final static String DEFAULT_MEI_EXTENSION = ".mei";
    // Not found stub image file name.
    public final static String NOT_FOUND_STUBIMG = "facsimile404.png";
    // Default subfolder name.
    public final static String APP_SUBFOLDER = "vertaktoid";
    // Default MEI name space.
    public final static String MEI_NS = "http://www.music-encoding.org/ns/mei";

    public final static String MEI_ZONE_ID_PREFIX = "zone_";
    public final static String MEI_MEASURE_ID_PREFIX = "measure_";
    public final static String MEI_MDIV_ID_PREFIX = "page_";
    public final static String MEI_GRAPHIC_ID_PREFIX = "graphic_";
    public final static String MEI_SURFACE_ID_PREFIX = "surface_";

    public final static int DEFAULT_UNDOREDO_STACK_SIZE = 50;

    public void onCreate() {
        super.onCreate();
        defWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        defHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    public static int defWidth = 2560;
    public static int defHeight = 1600;
    public static final int MIN_DPI = 80;
    public static final int MIN_GESTURE_LENGTH = 20;
    public static int defBitmapResScaleFactor = 1;
}
