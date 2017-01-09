package zemfi.de.vertaktoid;

import java.io.Serializable;
import java.util.ArrayList;
import java.io.File;
import java.util.Collections;

import android.graphics.BitmapFactory;

/**
 * Created by yevgen on 16.12.2016.
 */

public class Page implements Serializable {
    ArrayList<Measure> measures;
    File imageFile;
    int number;

    int imageWidth;
    int imageHeight;

    public Page(File imageFile, int number) {
        this.imageFile = imageFile;
        measures = new ArrayList<>();
        this.number = number;
        calculateDimensions();
    }

    void sortMeasures() {
        Collections.sort(measures, Measure.MEASURE_NUMBER_COMPARATOR);
    }

    Measure getMeasureAt(float x, float y) {
        for (Measure measure : measures) {
            if (measure.containsPoint(x, y)) {
                return measure;
            }

        }
        return null;
    }

    ArrayList<Measure> getMeasuresAt(float x, float y) {
        ArrayList<Measure> result = new ArrayList<>();
        for (Measure measure : measures) {
            if (measure.containsPoint(x, y)) {
                result.add(measure);
            }

        }
        return result;
    }

    void removeMeasure(Measure measure) {
        measures.remove(measure);
        measure.page = null;
    }

    void removeMeasures(ArrayList<Measure> measures) {
        for(Measure measure : measures) {
            removeMeasure(measure);
        }
    }

    void calculateDimensions() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        //The option inJustDecodeBounds is very important here.
        //Without this option the Bitmap will be read and stored in the memory, what cause the memory leaks.
        options.inJustDecodeBounds = true;
        if(imageFile != null) {
            BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
            imageHeight = options.outHeight;
            imageWidth = options.outWidth;
        }
        else {
            imageHeight = 0;
            imageWidth = 0;
        }
    }
}
