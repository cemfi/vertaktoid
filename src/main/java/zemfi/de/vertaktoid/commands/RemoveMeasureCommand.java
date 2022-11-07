package zemfi.de.vertaktoid.commands;


import android.os.Parcel;
import android.os.Parcelable;

import zemfi.de.vertaktoid.model.Facsimile;
import zemfi.de.vertaktoid.model.Measure;


public class RemoveMeasureCommand implements ICommand, Parcelable {
    private Measure measure;
    private Facsimile facsimile;

    public RemoveMeasureCommand(Measure measure, Facsimile facsimile) {
        this.measure = measure;
        this.facsimile = facsimile;
    }

    public RemoveMeasureCommand() {

    }

    protected RemoveMeasureCommand(Parcel in) {
        measure = in.readParcelable(Measure.class.getClassLoader());
        facsimile = in.readParcelable(Facsimile.class.getClassLoader());
    }

    public static final Creator<RemoveMeasureCommand> CREATOR = new Creator<RemoveMeasureCommand>() {
        @Override
        public RemoveMeasureCommand createFromParcel(Parcel in) {
            return new RemoveMeasureCommand(in);
        }

        @Override
        public RemoveMeasureCommand[] newArray(int size) {
            return new RemoveMeasureCommand[size];
        }
    };

    public Measure getMeasure() {
        return measure;
    }

    public void setMeasure(Measure measure) {
        this.measure = measure;
    }

    public Facsimile getFacsimile() {
        return facsimile;
    }

    public void setFacsimile(Facsimile facsimile) {
        this.facsimile = facsimile;
    }

    @Override
    public int execute() {
        if(measure != null) {
            facsimile.removeMeasure(measure);
            facsimile.resort(measure.movement, measure.page);
            facsimile.cleanMovements();
            return facsimile.pages.indexOf(measure.page);
        }
        return -1;
    }

    @Override
    public int unexecute() {
        if (measure != null) {
            facsimile.addMeasure(measure, measure.movement, measure.page);
            facsimile.resort(measure.movement, measure.page);
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
        parcel.writeParcelable(measure, i);
        parcel.writeParcelable(facsimile, i);
    }
}
