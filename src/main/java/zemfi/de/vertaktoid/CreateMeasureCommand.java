package zemfi.de.vertaktoid;

import java.io.Serializable;

public class CreateMeasureCommand implements ICommand, Serializable {
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
}
