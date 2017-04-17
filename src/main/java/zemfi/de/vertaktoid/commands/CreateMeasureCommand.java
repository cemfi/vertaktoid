package zemfi.de.vertaktoid.commands;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

import zemfi.de.vertaktoid.model.Facsimile;
import zemfi.de.vertaktoid.model.Measure;
import zemfi.de.vertaktoid.model.Movement;
import zemfi.de.vertaktoid.model.Page;


public class CreateMeasureCommand implements ICommand, Parcelable {
    private Measure measure;
    private Facsimile facsimile;
    private Page page;
    private Movement movement;

    public CreateMeasureCommand(Measure measure, Facsimile facsimile, Page page, Movement movement) {
        this.measure = measure;
        this.facsimile = facsimile;
        this.page = page;
        this.movement = movement;
    }

    public CreateMeasureCommand(Measure measure, Facsimile facsimile, Page page) {
        this.measure = measure;
        this.facsimile = facsimile;
        this.page = page;
        this.movement = null;
    }

    public CreateMeasureCommand() {

    }

    protected CreateMeasureCommand(Parcel in) {
        measure = in.readParcelable(Measure.class.getClassLoader());
        facsimile = in.readParcelable(Facsimile.class.getClassLoader());
        page = in.readParcelable(Page.class.getClassLoader());
        movement = in.readParcelable(Movement.class.getClassLoader());
    }

    public static final Creator<CreateMeasureCommand> CREATOR = new Creator<CreateMeasureCommand>() {
        @Override
        public CreateMeasureCommand createFromParcel(Parcel in) {
            return new CreateMeasureCommand(in);
        }

        @Override
        public CreateMeasureCommand[] newArray(int size) {
            return new CreateMeasureCommand[size];
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

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public Movement getMovement() {
        return movement;
    }

    public void setMovement(Movement movement) {
        this.movement = movement;
    }

    @Override
    public int execute() {
        if (movement != null) {
            facsimile.addMeasure(measure, movement, page);
            facsimile.resort(movement, page);
        }
        else {
            facsimile.addMeasure(measure, page);
            movement = measure.movement;
            facsimile.resort(movement, page);
        }
        return facsimile.pages.indexOf(measure.page);
    }
    @Override
    public int unexecute(){
        facsimile.removeMeasure(measure);
        facsimile.resort(movement, page);
        facsimile.cleanMovements();
        return facsimile.pages.indexOf(measure.page);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(measure, i);
        parcel.writeParcelable(facsimile, i);
        parcel.writeParcelable(page, i);
        parcel.writeParcelable(movement, i);
    }
}
