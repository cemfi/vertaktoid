package zemfi.de.vertaktoid.commands;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import zemfi.de.vertaktoid.model.Facsimile;
import zemfi.de.vertaktoid.model.Measure;


public class AdjustMeasureCommand implements ICommand, Parcelable {

    private Facsimile facsimile;
    private Measure measure;
    private String manualSequenceNumber;
    private String rest;
    private String oldManualSequenceNumber;
    private int oldRest;

    public AdjustMeasureCommand(Facsimile facsimile, Measure measure, String manualSequenceNumber, String rest) {
        this.facsimile = facsimile;
        this.measure = measure;
        this.manualSequenceNumber = manualSequenceNumber;
        this.rest = rest;
    }

    public AdjustMeasureCommand() {

    }

    protected AdjustMeasureCommand(Parcel in) {
        facsimile = in.readParcelable(Facsimile.class.getClassLoader());
        measure = in.readParcelable(Measure.class.getClassLoader());
        manualSequenceNumber = in.readString();
        rest = in.readString();
        oldManualSequenceNumber = in.readString();
        oldRest = in.readInt();
    }

    public static final Creator<AdjustMeasureCommand> CREATOR = new Creator<AdjustMeasureCommand>() {
        @Override
        public AdjustMeasureCommand createFromParcel(Parcel in) {
            return new AdjustMeasureCommand(in);
        }

        @Override
        public AdjustMeasureCommand[] newArray(int size) {
            return new AdjustMeasureCommand[size];
        }
    };

    public Facsimile getFacsimile() {
        return facsimile;
    }

    public void setFacsimile(Facsimile facsimile) {
        this.facsimile = facsimile;
    }

    public Measure getMeasure() {
        return measure;
    }

    public void setMeasure(Measure measure) {
        this.measure = measure;
    }

    public String getManualSequenceNumber() {
        return manualSequenceNumber;
    }

    public void setManualSequenceNumber(String manualSequenceNumber) {
        this.manualSequenceNumber = manualSequenceNumber;
    }

    public String getRest() {
        return rest;
    }

    public void setRest(String rest) {
        this.rest = rest;
    }

    @Override
    public int execute() {
        if(measure != null) {
            oldRest = measure.rest;
            oldManualSequenceNumber = measure.manualSequenceNumber;
            measure.manualSequenceNumber = manualSequenceNumber.equals("") ? null : manualSequenceNumber;
            try {

                measure.rest = Integer.parseInt(rest);
            } catch (NumberFormatException e) {
                measure.rest = 0;
            }
            measure.movement.calculateSequenceNumbers();
            measure.page.sortMeasures();
            return facsimile.pages.indexOf(measure.page);
        }
        return -1;
    }

    @Override
    public int unexecute() {
        if(measure != null) {
            measure.manualSequenceNumber = oldManualSequenceNumber;
            measure.rest = oldRest;
            measure.movement.calculateSequenceNumbers();
            measure.page.sortMeasures();
            return facsimile.pages.indexOf(measure.page);
        }
        return -1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(facsimile, i);
        parcel.writeParcelable(measure, i);
        parcel.writeString(manualSequenceNumber);
        parcel.writeString(rest);
        parcel.writeString(oldManualSequenceNumber);
        parcel.writeInt(oldRest);
    }
}
