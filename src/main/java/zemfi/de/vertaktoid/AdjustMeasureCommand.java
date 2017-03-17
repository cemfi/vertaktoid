package zemfi.de.vertaktoid;

import java.io.Serializable;

/**
 * Created by eugen on 16.03.17.
 */

public class AdjustMeasureCommand implements ICommand, Serializable {

    private Measure measure;
    private String manualSequenceNumber;
    private String rest;
    private String oldManualSequenceNumber;
    private int oldRest;

    public AdjustMeasureCommand(Measure measure, String manualSequenceNumber, String rest) {
        this.measure = measure;
        this.manualSequenceNumber = manualSequenceNumber;
        this.rest = rest;
    }

    public AdjustMeasureCommand() {

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
    public void execute() {
        oldRest = measure.rest;
        oldManualSequenceNumber = measure.manualSequenceNumber;
        measure.manualSequenceNumber = manualSequenceNumber.equals("") ? null : manualSequenceNumber;
        try {

            measure.rest = Integer.parseInt(rest);
        }
        catch (NumberFormatException e) {
            measure.rest = 0;
        }
        measure.movement.calculateSequenceNumbers();
        measure.page.sortMeasures();
    }

    @Override
    public void unexecute() {
        measure.manualSequenceNumber = oldManualSequenceNumber;
        measure.rest = oldRest;
        measure.movement.calculateSequenceNumbers();
        measure.page.sortMeasures();
    }
}
