package zemfi.de.vertaktoid.commands;

import android.os.Parcel;
import android.os.Parcelable;

import zemfi.de.vertaktoid.model.Facsimile;
import zemfi.de.vertaktoid.model.Measure;

/**
 * Created by eugen on 16.03.17.
 */

public class CutMeasureCommand implements ICommand, Parcelable {
    private Facsimile facsimile;
    private Measure oldMeasure;
    private Measure measure1;
    private Measure measure2;

    public CutMeasureCommand(Facsimile facsimile, Measure oldMeasure,
                             Measure measure1, Measure measure2) {
        this.facsimile = facsimile;
        this.oldMeasure = oldMeasure;
        this.measure1 = measure1;
        this.measure2 = measure2;
    }

    public CutMeasureCommand() {

    }

    protected CutMeasureCommand(Parcel in) {
        facsimile = in.readParcelable(Facsimile.class.getClassLoader());
        oldMeasure = in.readParcelable(Measure.class.getClassLoader());
        measure1 = in.readParcelable(Measure.class.getClassLoader());
        measure2 = in.readParcelable(Measure.class.getClassLoader());
    }

    public static final Creator<CutMeasureCommand> CREATOR = new Creator<CutMeasureCommand>() {
        @Override
        public CutMeasureCommand createFromParcel(Parcel in) {
            return new CutMeasureCommand(in);
        }

        @Override
        public CutMeasureCommand[] newArray(int size) {
            return new CutMeasureCommand[size];
        }
    };

    public Facsimile getFacsimile() {
        return facsimile;
    }

    public void setFacsimile(Facsimile facsimile) {
        this.facsimile = facsimile;
    }

    public Measure getOldMeasure() {
        return oldMeasure;
    }

    public void setOldMeasure(Measure oldMeasure) {
        this.oldMeasure = oldMeasure;
    }

    public Measure getMeasure1() {
        return measure1;
    }

    public void setMeasure1(Measure measure1) {
        this.measure1 = measure1;
    }

    public Measure getMeasure2() {
        return measure2;
    }

    public void setMeasure2(Measure measure2) {
        this.measure2 = measure2;
    }

    @Override
    public int execute() {
        if(oldMeasure != null) {
            facsimile.removeMeasure(oldMeasure);
            facsimile.addMeasure(measure1, oldMeasure.movement, oldMeasure.page);
            facsimile.addMeasure(measure2, oldMeasure.movement, oldMeasure.page);
            facsimile.resort(oldMeasure.movement, oldMeasure.page);
            return facsimile.pages.indexOf(oldMeasure.page);
        }
        return -1;
    }

    @Override
    public int unexecute() {
        if(oldMeasure != null) {
            facsimile.removeMeasure(measure1);
            facsimile.removeMeasure(measure2);
            facsimile.addMeasure(oldMeasure, oldMeasure.movement, oldMeasure.page);
            facsimile.resort(oldMeasure.movement, oldMeasure.page);
            return facsimile.pages.indexOf(oldMeasure.page);
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
        parcel.writeParcelable(oldMeasure, i);
        parcel.writeParcelable(measure1, i);
        parcel.writeParcelable(measure2, i);
    }
}
