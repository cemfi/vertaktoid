package zemfi.de.vertaktoid.commands;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import zemfi.de.vertaktoid.Vertaktoid;
import zemfi.de.vertaktoid.helpers.SizedStack;
import zemfi.de.vertaktoid.model.Facsimile;
import zemfi.de.vertaktoid.model.Measure;
import zemfi.de.vertaktoid.model.Movement;
import zemfi.de.vertaktoid.model.Page;


public class CommandManager implements Parcelable {
    private int historyMaxSize = Vertaktoid.DEFAULT_UNDOREDO_STACK_SIZE;
    private SizedStack<ICommand> undoCommands;
    private SizedStack<ICommand> redoCommands;

    public CommandManager() {
        undoCommands = new SizedStack<>(historyMaxSize);
        redoCommands = new SizedStack<>(historyMaxSize);
    }

    protected CommandManager(Parcel in) {
        historyMaxSize = in.readInt();
    }

    public static final Creator<CommandManager> CREATOR = new Creator<CommandManager>() {
        @Override
        public CommandManager createFromParcel(Parcel in) {
            return new CommandManager(in);
        }

        @Override
        public CommandManager[] newArray(int size) {
            return new CommandManager[size];
        }
    };

    public int getUndoStackSize() {
        return undoCommands.size();
    }

    public int getRedoStackSize() {
        return redoCommands.size();
    }

    public int getHistoryMaxSize() {
        return historyMaxSize;
    }

    public void setHistoryMaxSize(int maxSize) {
        if(maxSize >= 0) {
            historyMaxSize = maxSize;
        }
        undoCommands.setMaxSize(historyMaxSize);
        redoCommands.setMaxSize(historyMaxSize);
    }

    public int redo(int levels) {
        int index = -1;
        for(int i = 0; i < levels; levels++) {
            if(redoCommands.size() > 0) {
                ICommand command = redoCommands.pop();
                index = command.execute();
                undoCommands.push(command);
            }
        }
        return index;
    }

    public int redo() {
        int index = -1;
        if(redoCommands.size() > 0) {
            ICommand command = redoCommands.pop();
            index = command.execute();
            undoCommands.push(command);
        }
        return index;
    }

    public int undo(int levels) {
        int index = -1;
        for(int i = 0; i < levels; levels++) {
            if(undoCommands.size() > 0) {
                ICommand command = undoCommands.pop();
                index = command.unexecute();
                redoCommands.push(command);
            }
        }
        return index;
    }

    public int undo() {
        int index = -1;
        if(undoCommands.size() > 0) {
            ICommand command = undoCommands.pop();
            index = command.unexecute();
            redoCommands.push(command);
        }
        return index;
    }


    public void processCreateMeasureCommand(Measure measure, Facsimile facsimile,
                                            Page page, Movement movement) {
        ICommand command = new CreateMeasureCommand(measure, facsimile, page, movement);
        command.execute();
        redoCommands.clear();
    }

    public void processCreateMeasureCommand(Measure measure, Facsimile facsimile,
                                            Page page) {
        ICommand command = new CreateMeasureCommand(measure, facsimile, page);
        command.execute();
        undoCommands.push(command);
        redoCommands.clear();
    }

    public void processRemoveMeasureCommand(Measure measure, Facsimile facsimile) {
        ICommand command = new RemoveMeasureCommand(measure, facsimile);
        command.execute();
        undoCommands.push(command);
        redoCommands.clear();
    }

    public void processRemoveMeasuresCommand(List<Measure> measures, Facsimile facsimile) {
        ICommand command = new RemoveMeasuresCommand(measures, facsimile);
        command.execute();
        undoCommands.push(command);
        redoCommands.clear();
    }

    public void processCutMeasureCommand(Facsimile facsimile, Measure oldMeasure,
                                         Measure leftMeasure, Measure rightMeasure) {
        ICommand command = new CutMeasureCommand(facsimile, oldMeasure, leftMeasure, rightMeasure);
        command.execute();
        undoCommands.push(command);
        redoCommands.clear();
    }

    public void processAdjustMeasureCommand(Facsimile facsimile, Measure measure,
                                            String manualSequenceNumber, String rest) {
        ICommand command = new AdjustMeasureCommand(facsimile, measure, manualSequenceNumber, rest);
        command.execute();
        undoCommands.push(command);
        redoCommands.clear();
    }

    public void processAdjustMovementCommand(Facsimile facsimile, Measure targetMeasure, String userOption,
                                             String newLabel, String optionDef, String optionElse) {
        ICommand command = new AdjustMovementCommand(facsimile, targetMeasure,
                userOption, newLabel, optionDef, optionElse);
        command.execute();
        undoCommands.push(command);
        redoCommands.clear();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(historyMaxSize);
    }
}
