package zemfi.de.vertaktoid.commands;


import java.io.Serializable;
import java.util.ArrayList;
import zemfi.de.vertaktoid.model.Facsimile;
import zemfi.de.vertaktoid.model.Measure;
import zemfi.de.vertaktoid.model.Movement;


public class RemoveMeasuresCommand implements ICommand, Serializable {
    private Facsimile facsimile;
    private ArrayList<Measure> measures;

    RemoveMeasuresCommand(ArrayList<Measure> measures, Facsimile facsimile) {
        this.facsimile = facsimile;
        this.measures = measures;
    }

    public RemoveMeasuresCommand() {
    }

    public Facsimile getFacsimile() {
        return facsimile;
    }

    public void setFacsimile(Facsimile facsimile) {
        this.facsimile = facsimile;
    }

    public ArrayList<Measure> getMeasures() {
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
}
