package zemfi.de.vertaktoid;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by aristotelis on 25.08.16.
 */
public class Facsimile implements Serializable {

    ArrayList<Page> pages;


    Facsimile() {
        pages = new ArrayList<>();
    }


    void updateSequenceNumbers() {
        int i;
        int startsWith = 1;
        for (i = 0; i < pages.size(); i++) {
            pages.get(i).startsWith = startsWith;
            pages.get(i).updateSequenceNumbers();
            startsWith = pages.get(i).endsWith + 1;
        }
    }

    String path;
    void openDirectory(String path) {
        this.path = path;
        File f = new File(path);
        File file[] = f.listFiles();
        ArrayList<String> files = new ArrayList<>();

        for (int i=0; i < file.length; i++) {
            if (!file[i].getName().startsWith(".")) {
                if (file[i].getName().toLowerCase().endsWith(".jpg") || file[i].getName().toLowerCase().endsWith(".png")) {
                    files.add(file[i].getAbsolutePath());
                }
            }
        }
        Collections.sort(files); // make alphabetical order

        MEIInOut meiInOut = new MEIInOut();
        meiInOut.readMeiFile(path + "/mei.mei");
        meiInOut.parseXml();
        pages = meiInOut.getPages();
        updateSequenceNumbers();
        int i;

        for(i = 0; i < files.size(); i++) {
            if(i < pages.size()) {
                pages.get(i).stripManualSequenceNumbers();
            }
            else {
                pages.add(new Page(files.get(i).substring(files.get(i).lastIndexOf("/") + 1)));
            }
            pages.get(i).filePath = files.get(i);
            pages.get(i).calculateDimensions();
        }
    }

    boolean saveToDisk() {
        MEIInOut meiInOut = new MEIInOut();
        return meiInOut.writeMei(path, pages);
    }

    boolean saveToDisk(String path, String filename) {
        MEIInOut meiInOut = new MEIInOut();
        return meiInOut.writeMei(path, filename, pages);
    }
}
