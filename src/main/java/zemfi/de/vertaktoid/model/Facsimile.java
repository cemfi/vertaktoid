package zemfi.de.vertaktoid.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import zemfi.de.vertaktoid.mei.MEIHelper;
import zemfi.de.vertaktoid.Vertaktoid;

/**
 * Represents a scanned music source. Consists from pages. Contains movements.
 */

public class Facsimile implements Parcelable {
    public ArrayList<Page> pages;
    public ArrayList<Movement> movements;
    public File dir;
    public enum AnnotationType {ORTHOGONAL_BOX, ORIENTED_BOX, POLYGON}
    public AnnotationType nextAnnotationsType = AnnotationType.ORTHOGONAL_BOX;

    protected Facsimile(Parcel in) {
        pages = in.createTypedArrayList(Page.CREATOR);
        movements = in.createTypedArrayList(Movement.CREATOR);
    }

    public static final Creator<Facsimile> CREATOR = new Creator<Facsimile>() {
        @Override
        public Facsimile createFromParcel(Parcel in) {
            return new Facsimile(in);
        }

        @Override
        public Facsimile[] newArray(int size) {
            return new Facsimile[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedList(pages);
        parcel.writeTypedList(movements);
    }

    /**
     * Standard constructor
     */
    public Facsimile() {
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
    public int measuresCount() {
        int count = 0;
        for(Movement movement : movements) {
            count += movement.measures.size();
        }
        return count;
    }

    /**
     * Adds measure to giving movement and page object.
     * Creates references to the parent movement\page in measure.
     * @param measure new measure
     * @param movement target movement
     * @param page target page
     */
    public void addMeasure(Measure measure, Movement movement, Page page) {
        measure.movement = movement;
        measure.page = page;
        movement.measures.add(measure);
        page.measures.add(measure);
    }

    /**
     * Adds measure to page object and calculated movement.
     * Creates references to the parent movement\page in measure.
     * The movement is calculated based on the position of the measure.
     * @param measure new measure
     * @param page target page
     */
    public void addMeasure(Measure measure, Page page) {
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
    public void resort(Movement movement, Page page) {
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
    public void removeMeasure(Measure measure) {
        measure.movement.removeMeasure(measure);
        measure.page.removeMeasure(measure);
    }

    /**
     * Removes a list of measures from the corresponding movements and pages.
     * @param measures list of measures
     */
    public void removeMeasures(List<Measure> measures) {
        for(Measure measure : measures) {
            measure.movement.removeMeasure(measure);
            measure.page.removeMeasure(measure);
        }
    }

    /**
     * Finds and removes movements without measures.
     */
    public void cleanMovements() {
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
    public void openDirectory(File dir) {
        this.dir = dir;
        File files[] = dir.listFiles();
        ArrayList<File> images = new ArrayList<>();

        // files==null when folder doesn't exist
        for (int i = 0; i < files.length; i++) {
            if (!files[i].getName().startsWith(".")) {
                if (files[i].getName().toLowerCase().endsWith(".jpg") || files[i].getName().toLowerCase().endsWith(".png")) {
                    images.add(files[i]);
                }
            }
        }
        Collections.sort(images, FILE_NAME_COMPARATOR); // make alphabetical order

        File meiFile = new File(dir.getAbsolutePath() + "/" + dir.getName() + Vertaktoid.DEFAULT_MEI_EXTENSION);
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
            pages.clear();
            movements.clear();
            MEIHelper.clearDocument();
            for (int i = 0; i < images.size(); i++) {
                pages.add(new Page(images.get(i), i + 1));
            }
            Movement movement = new Movement();
            movements.add(movement);
        }
    }

    /**
     * Export the MEI to default file. Creates the file if not exists.
     * @return true if the MEI output was properly saved
     */
    public boolean saveToDisk() {
        File meiFile = new File(dir.getAbsolutePath() + "/" + dir.getName() + Vertaktoid.DEFAULT_MEI_EXTENSION);
        return MEIHelper.writeMEI(meiFile, this);

    }

    /**
     * Export the MEI to giving file at giving location. Creates the file if not exists.
     * @param path path to location
     * @param filename name of file
     * @return true if the MEI output was properly saved
     */
    public boolean saveToDisk(String path, String filename) {
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

    /**
     * Calculates the top and bottom positions of whole system.
     * @param measures measures in system
     * @return positions in the form {top, bottom}
     */
    private double[] getSystemPositions(ArrayList<Measure> measures){
        double top = Double.MAX_VALUE;
        double bottom = Double.MIN_VALUE;
        for(Measure measure : measures) {
            if(top > measure.zone.getBoundTop()) {
                top = measure.zone.getBoundTop();
            }
            if(bottom < measure.zone.getBoundBottom()) {
                bottom = measure.zone.getBoundBottom();
            }
        }
        return new double[]{top, bottom};
    }

    /**
     * Finds the system and page breaks. The results will be stored in the corresponding measures
     * via boolean attributes "lastAtSystem" and "lastAtPage".
     */
    public void calculateBreaks() {
        ArrayList<Measure> predecessors = new ArrayList<>();
        for(int i = 0; i < movements.size(); i++)
        {
            Movement curMovement = movements.get(i);
            Movement nextMovement = null;
            if(i < movements.size() - 1) {
                nextMovement = movements.get(i + 1);
            }
            for(int j = 0; j < curMovement.measures.size(); j++) {
                Measure curMeasure = curMovement.measures.get(j);
                predecessors.add(curMeasure);
                Measure nexMeasure = null;
                if(j < curMovement.measures.size() - 1) {
                    nexMeasure = curMovement.measures.get(j + 1);
                } else if(nextMovement != null)
                {
                    if(nextMovement.measures.size() > 0) {
                        nexMeasure = nextMovement.measures.get(0);
                    }
                }

                if(nexMeasure != null) {
                    double[] systemPositions = getSystemPositions(predecessors);
                    double yIsectFactor = (Math.min(systemPositions[1], nexMeasure.zone.getBoundBottom()) - Math.max(systemPositions[0], nexMeasure.zone.getBoundTop())) /
                            Math.min(systemPositions[1] - systemPositions[0], nexMeasure.zone.getBoundBottom()  - nexMeasure.zone.getBoundTop());
                    if (yIsectFactor < 0.5) {
                        curMeasure.lastAtSystem = true;
                        predecessors.clear();
                    } else {
                        curMeasure.lastAtSystem = false;
                    }
                }
                curMeasure.lastAtPage = false;
            }
        }
        for(Page page : pages) {
            if(page.measures.size() > 0) {
                page.measures.get(page.measures.size() - 1).lastAtPage = true;
                page.measures.get(page.measures.size() - 1).lastAtSystem = false;
            }
        }
    }
}