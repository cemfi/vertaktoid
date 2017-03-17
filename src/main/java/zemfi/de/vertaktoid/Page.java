package zemfi.de.vertaktoid;

import java.io.Serializable;
import java.util.ArrayList;
import java.io.File;
import java.util.Collections;
import java.util.UUID;

import android.graphics.BitmapFactory;

/**
 * Represents the single facsimile page. Contains reference to the image file.
 * Contains a set of related movements, that are arranged on this page.
 */

public class Page implements Serializable {
    // Related movements.
    ArrayList<Measure> measures;
    // Image file.
    File imageFile;
    // Sequence number of the page.
    int number;

    String surfaceUuid;
    String graphicUuid;

    // Image dimensions.
    int imageWidth;
    int imageHeight;

    /**
     * The constructor.
     * @param imageFile The image file.
     * @param number The sequence number.
     */
    public Page(File imageFile, int number) {
        surfaceUuid = Vertaktoid.MEI_SURFACE_ID_PREFIX + UUID.randomUUID().toString();
        graphicUuid = Vertaktoid.MEI_GRAPHIC_ID_PREFIX + UUID.randomUUID().toString();
        this.imageFile = imageFile;
        measures = new ArrayList<>();
        this.number = number;
        calculateDimensions();
    }

    public Page() {
        surfaceUuid = Vertaktoid.MEI_SURFACE_ID_PREFIX + UUID.randomUUID().toString();
        graphicUuid = Vertaktoid.MEI_GRAPHIC_ID_PREFIX + UUID.randomUUID().toString();
        measures = new ArrayList<>();
    }

    /**
     * Sorts the measures on the page by their sequence numbers.
     */
    void sortMeasures() {
        Collections.sort(measures, Measure.MEASURE_NUMBER_COMPARATOR);
    }

    /**
     * Gets a first measure at giving position (means the measure, that contains the giving point).
     * @param x The x coordinate of point.
     * @param y The y coordinate of point.
     * @return The measure.
     */
    Measure getMeasureAt(float x, float y) {
        for (Measure measure : measures) {
            if (measure.containsPoint(x, y)) {
                return measure;
            }

        }
        return null;
    }

    /**
     * Gets all measures at giving position.
     * @param x The x coordinate of point.
     * @param y The y coordinate of point.
     * @return The list of measures.
     */
    ArrayList<Measure> getMeasuresAt(float x, float y) {
        ArrayList<Measure> result = new ArrayList<>();
        for (Measure measure : measures) {
            if (measure.containsPoint(x, y)) {
                result.add(measure);
            }

        }
        return result;
    }

    ArrayList<Measure> getMeasuresAtSegment(float startx, float starty, float endx, float endy) {
        ArrayList<Measure> toRemove = new ArrayList<>();
        for(Measure measure : measures) {
            if(measure.containsSegment(startx, starty, endx, endy)) {
                toRemove.add(measure);
            }
        }
        return toRemove;
    }

    /**
     * Removes measure from page.
     * @param measure
     */
    void removeMeasure(Measure measure) {
        measures.remove(measure);
        //measure.page = null;
    }

    /**
     * Removes a list of measures from page.
     * @param measures
     */
    void removeMeasures(ArrayList<Measure> measures) {
        for(Measure measure : measures) {
            removeMeasure(measure);
        }
    }

    /**
     * Read the dimensions from image file.
     */
    private void calculateDimensions() {
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
