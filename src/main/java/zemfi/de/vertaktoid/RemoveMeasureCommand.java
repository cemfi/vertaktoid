package zemfi.de.vertaktoid;


import java.io.Serializable;

public class RemoveMeasureCommand implements ICommand, Serializable {
    private Measure measure;
    private Facsimile facsimile;

    public RemoveMeasureCommand(Measure measure, Facsimile facsimile) {
        this.measure = measure;
        this.facsimile = facsimile;
    }

    public RemoveMeasureCommand() {

    }

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
    public void execute() {
        if(measure != null) {
            facsimile.removeMeasure(measure);
            facsimile.resort(measure.movement, measure.page);
            facsimile.cleanMovements();
        }
    }

    @Override
    public void unexecute() {
        if (measure != null) {
            facsimile.addMeasure(measure, measure.movement, measure.page);
            facsimile.resort(measure.movement, measure.page);
        }
    }
}
