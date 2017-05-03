package zemfi.de.vertaktoid.commands;


import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import zemfi.de.vertaktoid.model.Facsimile;
import zemfi.de.vertaktoid.model.Measure;
import zemfi.de.vertaktoid.model.Movement;


public class RemoveMeasuresCommand implements ICommand, Parcelable {
    private Facsimile facsimile;
    private List<Measure> measures;

    RemoveMeasuresCommand(List<Measure> measures, Facsimile facsimile) {
        this.facsimile = facsimile;
        this.measures = new ArrayList<>(measures);
    }

    public RemoveMeasuresCommand() {
    }

    protected RemoveMeasuresCommand(Parcel in) {
        facsimile = in.readParcelable(Facsimile.class.getClassLoader());
        measures = in.createTypedArrayList(Measure.CREATOR);
    }

    public static final Creator<RemoveMeasuresCommand> CREATOR = new Creator<RemoveMeasuresCommand>() {
        @Override
        public RemoveMeasuresCommand createFromParcel(Parcel in) {
            return new RemoveMeasuresCommand(in);
        }

        @Override
        public RemoveMeasuresCommand[] newArray(int size) {
            return new RemoveMeasuresCommand[size];
        }
    };

    public Facsimile getFacsimile() {
        return facsimile;
    }

    public void setFacsimile(Facsimile facsimile) {
        this.facsimile = facsimile;
    }

    public List<Measure> getMeasures() {
        return measures;
    }

    public void setMeasures(ArrayList<Measure> measures) {
        this.measures = measures;
    }

    @Override
    public int execute(){
        if(measures.size() > 0) {
            facsimile.removeMeasures(measures);
            ArrayList<Movement> changedMovements = new ArrayList<>();
            for (Measure measure : measures) {
                if (!changedMovements.contains(measure.movement)) {
                    changedMovements.add(measure.movement);
                }
            }

            for (Movement movement : changedMovements) {
                facsimile.resort(movement, measures.get(0).page);
            }
            facsimile.cleanMovements();
            return facsimile.pages.indexOf(measures.get(0).page);
        }
        return -1;
    }

    @Override
    public int unexecute(){
        if(measures.size() > 0) {
            ArrayList<Movement> changedMovements = new ArrayList<>();
            for (Measure measure : measures) {
                if (!changedMovements.contains(measure.movement)) {
                    changedMovements.add(measure.movement);
                }
                facsimile.addMeasure(measure, measure.movement, measure.page);
            }
            for (Movement movement : changedMovements) {
                facsimile.resort(movement, measures.get(0).page);
            }
            return facsimile.pages.indexOf(measures.get(0).page);
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
        parcel.writeTypedList(measures);
    }
}
