package zemfi.de.vertaktoid;

import java.io.Serializable;
import java.util.ArrayList;
import java.io.File;
import java.util.Collections;
import java.util.Comparator;

import android.graphics.BitmapFactory;
import android.graphics.PointF;

/**
 * Created by yevgen on 16.12.2016.
 */

public class Page implements Serializable {
    ArrayList<Measure> measures;
    File imageFile;
    int number;

    int imageWidth;
    int imageHeight;

    public Page(File imageFile, int number) {
        this.imageFile = imageFile;
        measures = new ArrayList<>();
        this.number = number;
    }

    void sortMeasures() {
        Collections.sort(measures, Measure.MEASURE_NUMBER_COMPARATOR);
    }

    Measure getMeasureAt(float x, float y) {
        for (Measure measure : measures) {
            if (measure.containsPoint(x, y)) {
                return measure;
            }

        }
        return null;
    }

    ArrayList<Measure> getMeasuresAt(float x, float y) {
        ArrayList<Measure> result = new ArrayList<>();
        for (Measure measure : measures) {
            if (measure.containsPoint(x, y)) {
                result.add(measure);
            }

        }
        return result;
    }

    void removeMeasure(Measure measure) {
        measures.remove(measure);
        measure.page = null;
    }

    void removeMeasures(ArrayList<Measure> measures) {
        for(Measure measure : measures) {
            removeMeasure(measure);
        }
    }

    void calculateDimensions() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        //The option inJustDecodeBounds is very important here.
        //Without this option the Bitmap will be read and stored in the memory, what cause the memory leaks.
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
        imageHeight = options.outHeight;
        imageWidth = options.outWidth;
    }
}


/*
package zemfi.de.vertaktoid;

import android.graphics.BitmapFactory;

import java.io.Serializable;
import java.util.ArrayList;

*/
/**
 * Created by aristotelis on 01.08.16.
 *//*

public class Page implements Serializable {
    ArrayList<Line> lines = new ArrayList<>();
    String fileName;
    String filePath;

    int imageWidth;
    int imageHeight;

    int startsWith = 1;
    int endsWith = 0;

    Page(String filename) {
        this.fileName = filename;
        filePath = "";
    }

    void calculateDimensions() {
        if(filePath.equals("")) { return;}
        BitmapFactory.Options options = new BitmapFactory.Options();
        //The option inJustDecodeBounds is very important here.
        //Without this option the Bitmap will be read and stored in the memory, what cause the memory leaks.
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        imageHeight = options.outHeight;
        imageWidth = options.outWidth;
    }

    void addBox(Box box) {
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
                    System.out.println("add box in existing line");
                    lines.get(i).addBox(box);
                    //TODO what will be happen, if the box can be contained in more that one line?
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

    void addBox(float left, float right, float top, float bottom) {
        Box box = new Box(left, right, top, bottom);
        addBox(box);
    }

    void updateSequenceNumbers() {
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

    private void cleanLines() {
        for(int i = 0; i < lines.size(); i++) {
            cleanLines(i);
        }
    }

    private   void cleanLines(int index) {
        if(lines.get(index).boxes.size() == 0) {
            lines.remove(index);
        }
        else {
            lines.get(index).recalcSize();
        }
    }

    int numberOfBoxes() {
        int count = 0;
        int i;
        for (i = 0; i < lines.size(); i++) {
            count += lines.get(i).boxes.size();
        }
        return count;
    }

    ArrayList<Box> getBoxes() {
        ArrayList<Box> boxes = new ArrayList<>();
        int i;
        for(i = 0 ; i < lines.size(); i++) {
            boxes.addAll(lines.get(i).boxes);
        }
        return boxes;
    }

    Box getBox(int i) {
        int currentLine = 0;
        int firstBoxInLine = 0;
        while (i >= firstBoxInLine + lines.get(currentLine).boxes.size()) {
            firstBoxInLine += lines.get(currentLine).boxes.size();
            currentLine++;
        }
        return lines.get(currentLine).boxes.get(i - firstBoxInLine);
    }

    boolean deleteBox(float x, float y) {
        int i;
        for (i = 0; i < lines.size(); i++) {
            int j;
            for (j = 0; j < lines.get(i).boxes.size(); j++) {
                Box box = lines.get(i).boxes.get(j);
                if(box.containsPoint(x, y)) {
                    lines.get(i).boxes.remove(j);
                    updateSequenceNumbers();
                    cleanLines(i);
                    return true;
                }
            }
        }
        return false;
    }

    boolean deleteBox(float startX, float startY, float endX, float endY) {
        //Log.v("bla", "delete from (" + startX + ", " + startY + ") to (" + endX + ", " + endY + ")");
        boolean hasDeleted = false;
        int i;
        for (i = 0; i < lines.size(); i++) {
            int j;
            for (j = 0; j < lines.get(i).boxes.size(); j++) {
                Box box = lines.get(i).boxes.get(j);
                if(box.containsLine(startX, startY, endX, endY)) {
                    lines.get(i).boxes.remove(j);
                    //TODO check that line[i] is not empty. If empty - remove line.
                    hasDeleted = true;
                }
            }
        }
        if (hasDeleted) {
            updateSequenceNumbers();
            cleanLines();
        }
        return hasDeleted;
    }

    Box getBoxAt(float x, float y) {
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

    void stripManualSequenceNumbers() {
        updateSequenceNumbers();
        int i;
        for (i = 0; i < lines.size(); i++) {
            lines.get(i).stripManualSequenceNumbers();
        }
    }
}

*/
