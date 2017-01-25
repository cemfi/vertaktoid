package zemfi.de.vertaktoid;

import android.graphics.PointF;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Facsimile implements Serializable {
    ArrayList<Page> pages;
    ArrayList<Movement> movements;
    File dir;

    Facsimile() {
        pages = new ArrayList<>();
        movements = new ArrayList<>();
        Movement movement = new Movement();
        movement.number = 1;
        movements.add(movement);
    }

    int measuresCount() {
        int count = 0;
        for(Movement movement : movements) {
            count += movement.measures.size();
        }
        return count;
    }

    void addMeasure(Measure measure, Movement movement, Page page) {
        measure.movement = movement;
        measure.page = page;
        movement.measures.add(measure);
        page.measures.add(measure);

    }

    void resort(Movement movement, Page page) {
        movement.sortMeasures();
        movement.calculateSequenceNumbers();
        page.sortMeasures();
    }

    void removeMeasure(Measure measure) {
        measure.movement.removeMeasure(measure);
        measure.page.removeMeasure(measure);
    }

    void removeMeasures(ArrayList<Measure> measures) {
        for(Measure measure : measures) {
            measure.movement.removeMeasure(measure);
            measure.page.removeMeasure(measure);
        }
    }

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
            movement.number = 1;
            movements.add(movement);
        }

        for(int i = 0; i < movements.size(); i++) {
            movements.get(i).number = i + 1;
        }
    }

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
            MEIHelper.readMEI(meiFile, pages, movements);
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

    boolean removeMeasureAt(float x, float y, Page page) {
        Measure toRemove = page.getMeasureAt(x, y);
        if(toRemove != null) {
            removeMeasure(toRemove);
            return true;
        }
        return  false;
    }

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

    boolean removeMeasuresAt(float startx, float starty, float endx, float endy, Page page) {
        ArrayList<Measure> toRemove = new ArrayList<>();
        boolean result = false;
        for(Measure measure : page.measures) {
            if(measure.containsLine(startx, starty, endx, endy)) {
                toRemove.add(measure);
                result = true;
            }
        }
        removeMeasures(toRemove);
        return result;
    }

    boolean saveToDisk() {
        File meiFile = new File(dir.getAbsolutePath() + "/" + Vertaktoid.DEFAULT_MEI_FILENAME);
        return MEIHelper.writeMEI(meiFile, pages, movements);

    }

    boolean saveToDisk(String path, String filename) {
        File meiFile = new File(path + "/" + filename);
        return MEIHelper.writeMEI(meiFile, pages, movements);
    }

    public static final Comparator<File> FILE_NAME_COMPARATOR = new Comparator<File>() {
        @Override
        public int compare(File f1, File f2) {
            return f1.getName().compareTo(f2.getName());
        }
    };
}