package zemfi.de.vertaktoid;

import java.io.Serializable;
import java.util.ArrayList;

public class AdjustMovementCommand implements ICommand, Serializable {
    private Measure targetMeasure;
    private ArrayList<Measure> affectedMeasures;
    private Facsimile facsimile;
    private String optionDef;
    private String optionElse;
    private String userOption;
    private String newLabel;
    private String oldLabel;
    private  Movement targetMovement;
    private Movement oldMovement;
    private int oldMovementIndex;

    public AdjustMovementCommand() {
        this.affectedMeasures = new ArrayList<>();
    }

    public AdjustMovementCommand(Facsimile facsimile, Measure targetMeasure, String userOption,
                                 String newLabel, String optionDef, String optionElse) {
        this.affectedMeasures = new ArrayList<>();
        this.facsimile = facsimile;
        this.targetMeasure = targetMeasure;
        this.userOption = userOption;
        this.newLabel = newLabel;
        this.optionDef = optionDef;
        this.optionElse = optionElse;

        if(targetMeasure != null) {
            Movement currentMov = targetMeasure.movement;
            for (int i = currentMov.measures.indexOf(targetMeasure); i < currentMov.measures.size(); i++) {
                affectedMeasures.add(currentMov.measures.get(i));
            }
        }

    }

    public String getNewLabel() {
        return newLabel;
    }

    public void setNewLabel(String newLabel) {
        this.newLabel = newLabel;
    }

    public Facsimile getFacsimile() {
        return facsimile;
    }

    public void setFacsimile(Facsimile facsimile) {
        this.facsimile = facsimile;
    }

    public String getOptionDef() {
        return optionDef;
    }

    public void setOptionDef(String optionDef) {
        this.optionDef = optionDef;
    }

    public String getOptionElse() {
        return optionElse;
    }

    public void setOptionElse(String optionElse) {
        this.optionElse = optionElse;
    }

    public String getUserOption() {
        return userOption;
    }

    public void setUserOption(String userOption) {
        this.userOption = userOption;
    }

    public Measure getTargetMeasure() {
        return targetMeasure;
    }

    public void setTargetMeasure(Measure targetMeasure) {
        this.targetMeasure = targetMeasure;
    }

    @Override
    public void execute() {
        if (userOption.equals(optionDef)) {
            if (targetMeasure != null && !newLabel.equals("")) {
                oldLabel = targetMeasure.movement.label;
                targetMeasure.movement.label = newLabel;
            }
        } else if (userOption.equals(optionElse)) {
            Movement newMovement = new Movement();
            newMovement.number = facsimile.movements.get
                    (facsimile.movements.size() - 1).number + 1;
            newMovement.label = newLabel;
            facsimile.movements.add(newMovement);
            if (targetMeasure != null) {
                oldMovement = targetMeasure.movement;
                oldMovementIndex = facsimile.movements.indexOf(oldMovement);
                for (Measure measure : affectedMeasures) {
                    measure.changeMovement(newMovement);
                }
                facsimile.resort(targetMeasure.movement, targetMeasure.page);
                facsimile.cleanMovements();
            }
        } else {
            for (Movement movement : facsimile.movements) {
                if (movement.getName().equals(userOption)) {
                    targetMovement = movement;
                    break;
                }
            }
            if (!newLabel.equals("")) {
                oldLabel = targetMovement.label;
                targetMovement.label = newLabel;
            }
            if (targetMeasure != null) {
                oldMovement = targetMeasure.movement;
                oldMovementIndex = facsimile.movements.indexOf(oldMovement);
                for (Measure measure : affectedMeasures) {
                    measure.changeMovement(targetMovement);
                }
                facsimile.resort(targetMeasure.movement, targetMeasure.page);
                facsimile.cleanMovements();
            }
        }
    }

    @Override
    public void unexecute() {
        if (userOption.equals(optionDef)) {
            if (targetMeasure != null && !newLabel.equals("")) {
                targetMeasure.movement.label = oldLabel;
            }
        } else if (userOption.equals(optionElse)) {
            if (targetMeasure != null) {
                if(!facsimile.movements.contains(oldMovement)) {
                    facsimile.movements.add(oldMovementIndex, oldMovement);
                }
                for (Measure measure : affectedMeasures) {
                    measure.changeMovement(oldMovement);
                }
                facsimile.resort(targetMeasure.movement, targetMeasure.page);
                facsimile.cleanMovements();
            }

        } else {
            if (!newLabel.equals("")) {
                targetMovement.label = oldLabel;
            }
            if (targetMeasure != null) {
                for (Measure measure : affectedMeasures) {
                    measure.changeMovement(oldMovement);
                }
                facsimile.resort(targetMeasure.movement, targetMeasure.page);
                facsimile.cleanMovements();
            }
        }
    }
}
