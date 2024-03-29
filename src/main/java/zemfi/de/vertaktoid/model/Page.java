package zemfi.de.vertaktoid.model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.support.v4.provider.DocumentFile;

import com.davemorrissey.labs.subscaleview.ImageSource;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import zemfi.de.vertaktoid.MainActivity;
import zemfi.de.vertaktoid.Vertaktoid;
import zemfi.de.vertaktoid.helpers.Geometry;
import zemfi.de.vertaktoid.helpers.Point2D;

/**
 * Represents the single facsimile page. Contains reference to the image file.
 * Contains a set of related movements, that are arranged on this page.
 */

public class Page implements Parcelable {
    // Related movements.
    public ArrayList<Measure> measures;
    // Image file.
    public DocumentFile imageFile;
    private DocumentFile dir;
    private String imageFileName;
    // Sequence number of the page.
    public int number;

    public String surfaceUuid;
    public String graphicUuid;

    // Image dimensions.
    public int imageWidth;
    public int imageHeight;

    private int inSampleSize = 1;

    /**
     * The constructor.
     * @param imageFileName The image file.
     * @param number The sequence number.
     */
    public Page(DocumentFile dir, String imageFileName, int number) {
        //Universally Unique Identifier
        surfaceUuid = Vertaktoid.MEI_SURFACE_ID_PREFIX + UUID.randomUUID().toString();
        graphicUuid = Vertaktoid.MEI_GRAPHIC_ID_PREFIX + UUID.randomUUID().toString();
        this.dir = dir;
        this.imageFileName = imageFileName;
        measures = new ArrayList<>();
        this.number = number;
    }
    public Page(DocumentFile imageFile, int number) {
        //Universally Unique Identifier
        surfaceUuid = Vertaktoid.MEI_SURFACE_ID_PREFIX + UUID.randomUUID().toString();
        graphicUuid = Vertaktoid.MEI_GRAPHIC_ID_PREFIX + UUID.randomUUID().toString();
        this.imageFile = imageFile;
        measures = new ArrayList<>();
        this.number = number;
    }

    public Page() {
        //Universally Unique Identifier
        surfaceUuid = Vertaktoid.MEI_SURFACE_ID_PREFIX + UUID.randomUUID().toString();
        graphicUuid = Vertaktoid.MEI_GRAPHIC_ID_PREFIX + UUID.randomUUID().toString();
        measures = new ArrayList<>();
    }

    protected Page(Parcel in) {
        measures = in.createTypedArrayList(Measure.CREATOR);
        number = in.readInt();
        surfaceUuid = in.readString();
        graphicUuid = in.readString();
        imageWidth = in.readInt();
        imageHeight = in.readInt();
        inSampleSize = in.readInt();
    }

    public int getInSampleSize() {
        return inSampleSize;
    }

    public void setInSampleSize(int inSampleSize) {
        this.inSampleSize = inSampleSize;
    }

    public static final Creator<Page> CREATOR = new Creator<Page>() {
        @Override
        public Page createFromParcel(Parcel in) {
            return new Page(in);
        }

        @Override
        public Page[] newArray(int size) {
            return new Page[size];
        }
    };

    /**
     * Sorts the measures on the page by their sequence numbers.
     */
    public void sortMeasures() {
        Collections.sort(measures, Measure.MEASURE_NUMBER_COMPARATOR);
    }

    /**
     * Gets a first measure at giving position (means the measure, that contains the giving point).
     * @param point The target point.
     * @return The measure.
     */
    public Measure getMeasureAt(Point2D point) {
        for (Measure measure : measures) {
            if (Geometry.polygonContainsPoint(measure.zone.getVertices(), point)) {
                return measure;
            }
        }
        return null;
    }

    /**
     * Gets all measures at giving position.
     * @param point The target point.
     * @return The list of measures.
     */
    public ArrayList<Measure> getMeasuresAt(Point2D point) {
        ArrayList<Measure> result = new ArrayList<>();
        for (Measure measure : measures) {
            if (Geometry.polygonContainsPoint(measure.zone.getVertices(), point)) {
                result.add(measure);
            }

        }
        return result;
    }

    public ArrayList<Measure> getMeasuresAtSegment(Point2D[] segment) {
        ArrayList<Measure> result = new ArrayList<>();
        for(Measure measure : measures) {
            if(Geometry.segmentIntersectsPolygon(measure.zone.getVertices(), segment)) {
                result.add(measure);
            }
        }
        return result;
    }

    /**
     * Removes measure from page.
     * @param measure
     */
    public void removeMeasure(Measure measure) {
        measures.remove(measure);
        //measure.page = null;
    }

    /**
     * Removes a list of measures from page.
     * @param measures
     */
    public void removeMeasures(ArrayList<Measure> measures) {
        for(Measure measure : measures) {
            removeMeasure(measure);
        }
    }

    public int calculateInSampleSize(int imgWidth, int imgHeight) {
        int inSampleSize = 1;

        int reqWidth = Vertaktoid.defWidth * Vertaktoid.defBitmapResScaleFactor;
        int reqHeight = Vertaktoid.defHeight * Vertaktoid.defBitmapResScaleFactor;

        if(imgHeight > reqHeight || imgWidth > reqWidth) {
            while ((imgHeight / inSampleSize) >= reqHeight &&
                    (imgWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }

        }

        return inSampleSize;
    }

    /**
     * Calculates factor to downscale the image
     * Example: inSampleSize == 4 for 1/4 of the width/height of the original
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int inSampleSize = 1;

        final int height = options.outHeight;
        final int width = options.outWidth;

        if(height > reqHeight || width > reqWidth) {
            while ((height / inSampleSize) >= reqHeight &&
                    (width / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }

        }

        return inSampleSize;
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

            try {
                ParcelFileDescriptor parcelFileDescriptor =
                        MainActivity.context.getContentResolver().openFileDescriptor(imageFile.getUri(), "r");
                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                Bitmap bmp = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
                parcelFileDescriptor.close();

                imageHeight = options.outHeight;
                imageWidth = options.outWidth;

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                imageHeight = 0;
                imageWidth = 0;
            } catch (IOException e) {
                e.printStackTrace();
                imageHeight = 0;
                imageWidth = 0;
            }
        }
        else {
            imageHeight = 0;
            imageWidth = 0;
        }
        inSampleSize = calculateInSampleSize(options,
                Vertaktoid.defWidth * Vertaktoid.defBitmapResScaleFactor,
                Vertaktoid.defHeight * Vertaktoid.defBitmapResScaleFactor);
    }
    public boolean imageExists(){
        if(imageFile == null) {
            imageFile = dir.findFile(imageFileName);
            if (imageFile == null) {
                imageFile = dir.createFile("image/" + imageFileName.substring(imageFileName.lastIndexOf(".")).toLowerCase(), imageFileName);
            }
        }
        calculateDimensions();
        return imageFile.exists();

    }
    public String getImageFileName(){
        if(imageFile == null){
            return imageFileName;
        }
        return imageFile.getName();
    }
    public ImageSource getImage() {
        if(imageFile == null) {
            imageFile = dir.findFile(imageFileName);
            if (imageFile == null) {
                imageFile = dir.createFile("image/" + imageFileName.substring(imageFileName.lastIndexOf(".")).toLowerCase(), imageFileName);
            }
        }
        calculateDimensions();

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inSampleSize = inSampleSize;

        ImageSource bitmap = null;

        try {
            ParcelFileDescriptor parcelFileDescriptor =
                    MainActivity.context.getContentResolver().openFileDescriptor(imageFile.getUri(), "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            bitmap = ImageSource.bitmap(BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options));
            parcelFileDescriptor.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedList(measures);
        parcel.writeInt(number);
        parcel.writeString(surfaceUuid);
        parcel.writeString(graphicUuid);
        parcel.writeInt(imageWidth);
        parcel.writeInt(imageHeight);
        parcel.writeInt(inSampleSize);
    }

    public void setimageHeight(int imageHeight) {
        this.imageHeight = imageHeight;
    }

    public void setimageWidth(int imageWidth) {
        this.imageWidth = imageWidth;
    }
    public int getimageHeight() {
        return this.imageHeight;
    }

    public int getimageWidth() {
        return this.imageWidth;
    }
}
