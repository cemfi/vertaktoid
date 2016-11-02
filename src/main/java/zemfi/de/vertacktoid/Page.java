package zemfi.de.vertacktoid;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by aristotelis on 01.08.16.
 */
public class Page implements Serializable {
    ArrayList<Line> lines = new ArrayList<Line>();


    public Page() {
        
    }
    public String filename;


    public int startsWith = 1;
    public int endsWith = 0;

    public void addBox(Box box) {
        if (lines.size() == 0) {
            // create new first line
            Line aLine = new Line();
            aLine.addBox(box);
            lines.add(aLine);
        }
        else {
            // one or more lines already exist
            int i;
            for (i = 0; i < lines.size(); i++) {
                if(lines.get(i).isContained(box)) {
                    lines.get(i).addBox(box);
                    break;
                }
            }

            if (i == lines.size()) {
                // box was not contained in any line
                int j;
                int index = -1;
                for (j = 0; j < lines.size(); j++) {
                    if (box.top < lines.get(j).top) {
                        index = j;
                        break;
                    }
                }
                if (index == -1) {
                    index = lines.size();
                }
                Line aLine = new Line();
                aLine.addBox(box);
                lines.add(index, aLine);
            }
        }

        updateSequenceNumbers();
    }

    public void addBox(float left, float right, float top, float bottom) {
        Box box = new Box(left, right, top, bottom);
        addBox(box);
    }

    public void updateSequenceNumbers() {
        int sequenceNumber = startsWith;

        int i;
        for (i = 0; i < lines.size(); i++) {
            int j;
            for (j = 0; j < lines.get(i).boxes.size(); j++) {
                Box box = lines.get(i).boxes.get(j);
                if (box.manualSequenceNumber == null) {
                    box.sequenceNumber = sequenceNumber;
                    sequenceNumber++;
                }
                else {
                    try {
                        String modified = box.manualSequenceNumber.replaceAll("[\\D]", "");
                        modified.trim();
                        String array[] = modified.split(" ");
                        modified = array[0];
                        box.sequenceNumber = Integer.parseInt(modified);
                        sequenceNumber = box.sequenceNumber + 1;
                    }
                    catch (Exception e) {
                        box.sequenceNumber = sequenceNumber;
                        sequenceNumber++;
                    }
                }
            }
        }
        endsWith = sequenceNumber - 1;
    }

    public int numberOfBoxes() {
        int count = 0;
        int i;
        for (i = 0; i < lines.size(); i++) {
            count += lines.get(i).boxes.size();
        }
        return count;
    }

    public Box getBox(int i) {
        int currentLine = 0;
        int firstBoxInLine = 0;
        while (i >= firstBoxInLine + lines.get(currentLine).boxes.size()) {
            firstBoxInLine += lines.get(currentLine).boxes.size();
            currentLine++;
        }
        return lines.get(currentLine).boxes.get(i - firstBoxInLine);
    }

    public boolean deleteBox(float x, float y) {
        int i;
        for (i = 0; i < lines.size(); i++) {
            int j;
            for (j = 0; j < lines.get(i).boxes.size(); j++) {
                Box box = lines.get(i).boxes.get(j);
                if(box.containsPoint(x, y)) {
                    lines.get(i).boxes.remove(j);
                    updateSequenceNumbers();
                    return true;
                }
            }
        }
        return false;
    }

    public boolean deleteBox(float startX, float startY, float endX, float endY) {
        //Log.v("bla", "delete from (" + startX + ", " + startY + ") to (" + endX + ", " + endY + ")");
        boolean hasDeleted = false;
        int i;
        for (i = 0; i < lines.size(); i++) {
            int j;
            for (j = 0; j < lines.get(i).boxes.size(); j++) {
                Box box = lines.get(i).boxes.get(j);
                if(box.containsLine(startX, startY, endX, endY)) {
                    lines.get(i).boxes.remove(j);
                    hasDeleted = true;
                }
            }
        }
        if (hasDeleted) {
            updateSequenceNumbers();
        }
        return hasDeleted;
    }

    public Box getBoxAt(float x, float y) {
        int i;
        for (i = 0; i < lines.size(); i++) {
            int j;
            for (j = 0; j < lines.get(i).boxes.size(); j++) {
                Box box = lines.get(i).boxes.get(j);
                if(box.containsPoint(x, y)) {
                    return lines.get(i).boxes.get(j);
                }
            }
        }
        return null;
    }

    public void stripManualSequenceNumbers() {
        updateSequenceNumbers();
        int i;
        for (i = 0; i < lines.size(); i++) {
            lines.get(i).stripManualSequenceNumbers();
        }
    }
}

