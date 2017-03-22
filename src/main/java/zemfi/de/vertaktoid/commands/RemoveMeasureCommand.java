package zemfi.de.vertaktoid.commands;


import java.io.Serializable;
import zemfi.de.vertaktoid.model.Facsimile;
import zemfi.de.vertaktoid.model.Measure;


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
}
