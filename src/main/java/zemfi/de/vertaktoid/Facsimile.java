package zemfi.de.vertaktoid;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Represents a scanned music source. Consists from pages. Contains movements.
 */

public class Facsimile implements Serializable {
    ArrayList<Page> pages;
    ArrayList<Movement> movements;
    File dir;

    /**
     * Standard constructor
     */
    Facsimile() {
        pages = new ArrayList<>();
        movements = new ArrayList<>();
        Movement movement = new Movement();
        movement.number = 1;
        movements.add(movement);
    }

    /**
     * Calculates the whole number of measures in movements.
     * @return number of measures.
     */
    int measuresCount() {
        int count = 0;
        for(Movement movement : movements) {
            count += movement.measures.size();
        }
        return count;
    }

    /**
     * Adds measure to giving movement and page objects.
     * Creates references to the parent movement\page in measure.
     * @param measure new measure
     * @param movement target movement
     * @param page target page
     */
    void addMeasure(Measure measure, Movement movement, Page page) {
        measure.movement = movement;
        measure.page = page;
        movement.measures.add(measure);
        page.measures.add(measure);
    }

    void addMeasure(Measure measure, Page page) {
        measure.page = page;
        page.measures.add(measure);
        for(int i = movements.size() - 1; i >= 0; i--) {
            if(movements.get(i).measures.size() > 0) {
                if (Measure.MEASURE_POSITION_COMPARATOR.compare(movements.get(i).measures.get(0), measure) < 0) {
                    measure.movement = movements.get(i);
                    movements.get(i).measures.add(measure);
                    return;
                }
            }
        }
        measure.movement = movements.get(0);
        movements.get(0).measures.add(measure);
    }

    /**
     * Runs the sort function for giving movement and page.
     * Recalculates the measure numbers in movement.
     * @param movement target movement
     * @param page target page
     */
    void resort(Movement movement, Page page) {
        if(movement == null) {
            return;
        }
        movement.sortMeasures();
        movement.calculateSequenceNumbers();
        page.sortMeasures();
    }

    /**
     * Removes the measure from corresponding movement and page.
     * @param measure measure
     */
    void removeMeasure(Measure measure) {
        measure.movement.removeMeasure(measure);
        measure.page.removeMeasure(measure);
    }

    /**
     * Removes a list of measures from the corresponding movements and pages.
     * @param measures list of measures
     */
    void removeMeasures(ArrayList<Measure> measures) {
        for(Measure measure : measures) {
            measure.movement.removeMeasure(measure);
            measure.page.removeMeasure(measure);
        }
    }

    /**
     * Finds and removes movements without measures.
     */
    void cleanMovements() {
        ArrayList<Movement> toRemove = new ArrayList<>();
        for (Movement movement : movements) {
            if(movement.measures.size() == 0) {
                toRemove.add(movement);
            }
        }
        movements.removeAll(toRemove);

        if(movements.size() == 0) {
            Movement movement = new Movement();
            movements.add(movement);
        }

        for(int i = 0; i < movements.size(); i++) {
            movements.get(i).number = i + 1;
        }
    }

    /**
     * Loads scanned music source and reads the MEI file if exists.
     * @param dir reference to directory
     */
    void openDirectory(File dir) {
        this.dir = dir;
        File files[] = dir.listFiles();
        ArrayList<File> images = new ArrayList<>();

        for (int i = 0; i < files.length; i++) {
            if (!files[i].getName().startsWith(".")) {
                if (files[i].getName().toLowerCase().endsWith(".jpg") || files[i].getName().toLowerCase().endsWith(".png")) {
                    images.add(files[i]);
                }
            }
        }
        Collections.sort(images, FILE_NAME_COMPARATOR); // make alphabetical order

        File meiFile = new File(dir.getAbsolutePath() + "/" + Vertaktoid.DEFAULT_MEI_FILENAME);
        if(meiFile.exists()) {
            pages.clear();
            movements.clear();
            MEIHelper.readMEI(meiFile, this);
            for (Movement movement : movements) {
                movement.calculateSequenceNumbers();
            }

            for (int i = pages.size(); i < images.size(); i++) {
                pages.add(new Page(images.get(i), i + 1));
            }
        }
        else {
            for (int i = 0; i < images.size(); i++) {
                pages.add(new Page(images.get(i), i + 1));
            }
        }
    }

    /**
     * Removes the first found measure that intersect the giving point coordinates at the target page.
     * @param x x coordinate
     * @param y y coordinate
     * @param page target page
     * @return true if some measure was removed
     */
    boolean removeMeasureAt(float x, float y, Page page) {
        Measure toRemove = page.getMeasureAt(x, y);
        if(toRemove != null) {
            removeMeasure(toRemove);
            return true;
        }
        return  false;
    }

    /**
     * Removes all found measures that intersect the giving point coordinates at the target page.
     * @param x x coordinate
     * @param y y coordinate
     * @param page target page
     * @return true if some measure was removed
     */
    boolean removeMeasuresAt(float x, float y, Page page) {
        ArrayList<Measure> toRemove = page.getMeasuresAt(x, y);
        if (toRemove.size() > 0) {
            for (Measure measure : toRemove) {
                removeMeasure(measure);
            }
            return true;
        }
        return false;
    }

    /**
     * Removes all found measures that intersect the line from start point to end point at the target page.
     * @param startx x coordinate for start point
     * @param starty y coordinate for start point
     * @param endx x coordinate for end point
     * @param endy y coordinate for end point
     * @param page target page
     * @return true if some measure was removed
     */
    boolean removeMeasuresAt(float startx, float starty, float endx, float endy, Page page) {
        ArrayList<Measure> toRemove = new ArrayList<>();
        boolean result = false;
        for(Measure measure : page.measures) {
            if(measure.containsSegment(startx, starty, endx, endy)) {
                toRemove.add(measure);
                result = true;
            }
        }
        removeMeasures(toRemove);
        return result;
    }

    /**
     * Export the MEI to default file. Creates the file if not exists.
     * @return true if the MEI output was properly saved
     */
    boolean saveToDisk() {
        File meiFile = new File(dir.getAbsolutePath() + "/" + Vertaktoid.DEFAULT_MEI_FILENAME);
        return MEIHelper.writeMEI(meiFile, this);

    }

    /**
     * Export the MEI to giving file at giving location. Creates the file if not exists.
     * @param path path to location
     * @param filename name of file
     * @return true if the MEI output was properly saved
     */
    boolean saveToDisk(String path, String filename) {
        File meiFile = new File(path + "/" + filename);
        return MEIHelper.writeMEI(meiFile, this);
    }

    /**
     * Compares the files alphabetically by their names.
     */
    public static final Comparator<File> FILE_NAME_COMPARATOR = new Comparator<File>() {
        @Override
        public int compare(File f1, File f2) {
            return f1.getName().compareTo(f2.getName());
        }
    };
}