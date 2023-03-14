package zemfi.de.vertaktoid.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Comparator;
import java.util.UUID;

import zemfi.de.vertaktoid.Vertaktoid;

/**
 * Represents the measure in music notation.
 * Contains the coordinates for graphical presentation and semantic properties.
 */

public class Measure implements Comparable<Measure>, Parcelable {

    public final Zone zone;
    // Automatically calculated sequence number of measure in "Movement"
    public int sequenceNumber = -1;
    // Manually created name of measure. Is a string.
    // If the string contains number, the number will be used as sequence number.
    public String manualSequenceNumber = null;
    // Rest value (musical pause).
    public int rest = 0;
    // Id of referenced zone element in MEI.
    //public String zoneUuid = null;
    // Id of referenced measure element in MEI.
    public String measureUuid = null;
    // Reference to the parent movement.
    public Movement movement;
    // Reference to the parent page.
    public Page page;
    public boolean metcon;

    public boolean lastAtSystem = false;
    public boolean lastAtPage = false;

    /**
     * The constructor.
     */
    public Measure() {
        zone = new Zone();
        zone.zoneUuid = Vertaktoid.MEI_ZONE_ID_PREFIX +  UUID.randomUUID().toString();
        measureUuid = Vertaktoid.MEI_MEASURE_ID_PREFIX + UUID.randomUUID().toString();
        this.metcon = true;
        }

    public Measure(int size) {

        zone = new Zone();
        sequenceNumber = size;
        zone.zoneUuid = Vertaktoid.MEI_ZONE_ID_PREFIX +  UUID.randomUUID().toString();
        measureUuid = Vertaktoid.MEI_MEASURE_ID_PREFIX + UUID.randomUUID().toString();
        this.metcon = true;
    }


    protected Measure(Parcel in) {
        zone = in.readParcelable(Zone.class.getClassLoader());
        sequenceNumber = in.readInt();
        manualSequenceNumber = in.readString();
        rest = in.readInt();
        measureUuid = in.readString();
        lastAtSystem = in.readByte() != 0;
        lastAtPage = in.readByte() != 0;
        this.metcon = true;
    }


    public static final Creator<Measure> CREATOR = new Creator<Measure>() {
        @Override
        public Measure createFromParcel(Parcel in) {
            return new Measure(in);
        }

        @Override
        public Measure[] newArray(int size) {
            return new Measure[size];
        }
    };

    public Measure(boolean metcon) {
        zone = new Zone();
        zone.zoneUuid = Vertaktoid.MEI_ZONE_ID_PREFIX +  UUID.randomUUID().toString();
        measureUuid = Vertaktoid.MEI_MEASURE_ID_PREFIX + UUID.randomUUID().toString();
        this.metcon = metcon;
    }

    /**
     * Change the parent movement to another. Removes the measure from old movement and adds it to new.
     * The references will be adjusted.
     * @param newMovement new movement.
     */
    public void changeMovement(Movement newMovement) {
        if (newMovement == null) {
            return;
        }
        if(this.movement != null) {
            this.movement.removeMeasure(this);
        }
        if(!newMovement.measures.contains(this)) {
            newMovement.measures.add(this);
        }
        this.movement = newMovement;
    }

    /**
     * Compare the measures by their sequence number.
     */
    public static final Comparator<Measure> MEASURE_NUMBER_COMPARATOR = new Comparator<Measure>() {
        @Override
        public int compare(Measure m1, Measure m2) {
            if(m1.movement == m2.movement) {
                return m1.compareTo(m2);
            } else {
                return m1.movement.number - m2.movement.number;
            }
        }
    };

    /**
     * Compare the measures by their position at the facsimile.
     */
    public static final Comparator<Measure> MEASURE_POSITION_COMPARATOR = new Comparator<Measure>() {
        @Override
        public int compare(Measure m1, Measure m2) {
            if(m1.page.number == m2.page.number) {
               return m1.zone.compareTo(m2.zone);
            }
            else return m1.page.number - m2.page.number;
        }
    };

    /**
     * Gets the name of measure. It means the manualSequenceNumber if exists and the sequenceNumber else.
     * @return
     */
    public String getName() {
        if(manualSequenceNumber != null)
            return manualSequenceNumber;
        else return "" + sequenceNumber;
    }


    /**
     * Compare the measures by their sequence number by default.
     * @param measure
     * @return
     */
    @Override
    public int compareTo(Measure measure) {
        return this.sequenceNumber - measure.sequenceNumber;

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(zone, i);
        parcel.writeInt(sequenceNumber);
        parcel.writeString(manualSequenceNumber);
        parcel.writeInt(rest);
        parcel.writeString(measureUuid);
        parcel.writeByte((byte) (lastAtSystem ? 1 : 0));
        parcel.writeByte((byte) (lastAtPage ? 1 : 0));
    }
}
