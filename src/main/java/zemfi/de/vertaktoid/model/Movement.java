package zemfi.de.vertaktoid.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import zemfi.de.vertaktoid.Vertaktoid;

/**
 * Represents the movement in musical notation. Movement can be arranged on the multiple facsimile pages.
 * On the one facsimile page can be multiple movements instead.
 */

public class Movement implements Parcelable {
    // The contained measures.
    public ArrayList<Measure> measures;
    // Number of movement on the facsimile.
    public int number;
    // Label for movement.
    public String label = "";
    public String mdivUuid;
    /**
     * The constructor.
     */
    public Movement() {
        mdivUuid = Vertaktoid.MEI_MDIV_ID_PREFIX + UUID.randomUUID().toString();
        measures = new ArrayList<>();
    }

    protected Movement(Parcel in) {
        measures = in.createTypedArrayList(Measure.CREATOR);
        number = in.readInt();
        label = in.readString();
        mdivUuid = in.readString();
    }

    public static final Creator<Movement> CREATOR = new Creator<Movement>() {
        @Override
        public Movement createFromParcel(Parcel in) {
            return new Movement(in);
        }

        @Override
        public Movement[] newArray(int size) {
            return new Movement[size];
        }
    };

    /**
     * Sorts the measures in the movement by their position on the pages.
     */
    public void sortMeasures() {
        try {
            Collections.sort(measures, Measure.MEASURE_POSITION_COMPARATOR);
        } catch (IllegalArgumentException e) {

        }
    }

    /**
     * Calculates the sequence numbers of the measures.
     * The algorithm will try to parse the manualSequenceNumbers to obtain a number (first digit(s) found).
     *
     * If a number is recognized, the sequenceNumber will be those number, it doesn't have to be unique.
     * Else: sequenceNumber will be 1 or "rest" more then the last measure.
     * (The calculation of sequent and unique numbers "n" will take place when writing the file)
     *
     * Sets manualSequenceNumber to null if it doesn't differ from intended sequenceNumber
     */
    public void calculateSequenceNumbers() {
        if(measures.size() == 0) return;
        Measure measure;
        int num = 1;
        for (int i = 0; i < measures.size(); i++) {
            measure = measures.get(i);
            // Calculate the next sequence number by default
            if(i > 0) {
                // sequent number
                num = measures.get(i-1).sequenceNumber + (measures.get(i-1).rest > 1 ? measures.get(i-1).rest : 1);
            }
            // Try to parse manual sequence number if not null.
            // If no number can be parsed from manual sequence number,
            // then use the next number calculated by default.
            if(measure.manualSequenceNumber != null) {
                try {
                    String modified = measure.manualSequenceNumber.replaceAll("[\\D]", " ");
                    modified = modified.trim();
                    String mnumStrs[] = modified.split(" ");
                    // if no number recognized (only non-digit)
                    if(mnumStrs.length == 0) {
                        measure.sequenceNumber = num;
                    }
                    else {
                        // first number found
                        String mnumStr = mnumStrs[0];
                        int mnum = Integer.parseInt(mnumStr);
                        // if unchanged throughout the manipulation and equals intended (sequent) sequenceNumber set manualSequenceNumber to null
                        if (String.valueOf(mnum).equals(measure.manualSequenceNumber) && mnum == num) {
                            measure.manualSequenceNumber = null;
                        }
                        // set sequenceNumber to first number found
                        measure.sequenceNumber = mnum;
                    }
                }
                catch (NumberFormatException e) {
                    // set sequenceNumber to sequent number
                    measure.sequenceNumber = num;
                }
            }
            else {
                // set sequenceNumber to sequent number
                measure.sequenceNumber = num;
            }
        }
    }

    /**
     * Removes a measure from the movement.
     * @param measure The measure.
     */
    public void removeMeasure(Measure measure) {
        measures.remove(measure);
        calculateSequenceNumbers();
    }

    /**
     * Removes measures from the movement.
     * @param measures The measures.
     */
    public void removeMeasures(ArrayList<Measure> measures) {
        int index = measures.size();
        for (Measure measure : measures) {
            measures.remove(measure);

        }
        calculateSequenceNumbers();
    }

    /**
     * Gets a string name for the movement. It gets a label or default string created by its number.
     * @return
     */
    public String getName() {
        if(!label.equals("")) return label;
        else return "Movement " + number;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedList(measures);
        parcel.writeInt(number);
        parcel.writeString(label);
        parcel.writeString(mdivUuid);
    }
}
