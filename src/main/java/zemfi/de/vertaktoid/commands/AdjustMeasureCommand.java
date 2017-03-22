package zemfi.de.vertaktoid.commands;

import java.io.Serializable;
import zemfi.de.vertaktoid.model.Facsimile;
import zemfi.de.vertaktoid.model.Measure;

/**
 * Created by eugen on 16.03.17.
 */

public class AdjustMeasureCommand implements ICommand, Serializable {

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
}
