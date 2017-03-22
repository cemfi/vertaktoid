package zemfi.de.vertaktoid.commands;

import java.io.Serializable;

import zemfi.de.vertaktoid.commands.ICommand;
import zemfi.de.vertaktoid.model.Facsimile;
import zemfi.de.vertaktoid.model.Measure;

/**
 * Created by eugen on 16.03.17.
 */

public class CutMeasureCommand implements ICommand, Serializable {
    private Facsimile facsimile;
    private Measure oldMeasure;
    private Measure leftMeasure;
    private Measure rightMeasure;

    public CutMeasureCommand(Facsimile facsimile, Measure oldMeasure,
                             Measure leftMeasure, Measure rightMeasure) {
        this.facsimile = facsimile;
        this.oldMeasure = oldMeasure;
        this.leftMeasure = leftMeasure;
        this.rightMeasure = rightMeasure;
    }

    public CutMeasureCommand() {

    }

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

    public Measure getLeftMeasure() {
        return leftMeasure;
    }

    public void setLeftMeasure(Measure leftMeasure) {
        this.leftMeasure = leftMeasure;
    }

    public Measure getRightMeasure() {
        return rightMeasure;
    }

    public void setRightMeasure(Measure rightMeasure) {
        this.rightMeasure = rightMeasure;
    }

    @Override
    public int execute() {
        if(oldMeasure != null) {
            facsimile.removeMeasure(oldMeasure);
            facsimile.addMeasure(leftMeasure, oldMeasure.movement, oldMeasure.page);
            facsimile.addMeasure(rightMeasure, oldMeasure.movement, oldMeasure.page);
            facsimile.resort(oldMeasure.movement, oldMeasure.page);
            return facsimile.pages.indexOf(oldMeasure.page);
        }
        return -1;
    }

    @Override
    public int unexecute() {
        if(oldMeasure != null) {
            facsimile.removeMeasure(leftMeasure);
            facsimile.removeMeasure(rightMeasure);
            facsimile.addMeasure(oldMeasure, oldMeasure.movement, oldMeasure.page);
            facsimile.resort(oldMeasure.movement, oldMeasure.page);
            return facsimile.pages.indexOf(oldMeasure.page);
        }
        return -1;
    }
}
