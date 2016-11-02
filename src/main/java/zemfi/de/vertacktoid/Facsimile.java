package zemfi.de.vertacktoid;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by aristotelis on 25.08.16.
 */
public class Facsimile implements Serializable {

    public ArrayList<String> files;
    public ArrayList<Page> pages;


    public Facsimile() {
        pages = new ArrayList<Page>();
        files = new ArrayList<String>();
    }


    public void updateSequenceNumbers() {
        int i;
        int startsWith = 1;
        for (i = 0; i < pages.size(); i++) {
            pages.get(i).startsWith = startsWith;
            pages.get(i).updateSequenceNumbers();
            startsWith = pages.get(i).endsWith + 1;
        }
    }

    protected String path;
    protected void openDirectory(String path) {
        this.path = path;
        File f = new File(path);
        File file[] = f.listFiles();
        files = new ArrayList<String>();

        for (int i=0; i < file.length; i++) {
            if (!file[i].getName().startsWith(".")) {
                if (file[i].getName().toLowerCase().endsWith(".jpg") || file[i].getName().toLowerCase().endsWith(".png")) {
                    //Log.d("Files", "FileName:" + file[i].getName());
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
        for (i = 0; i < pages.size(); i++) {
            pages.get(i).stripManualSequenceNumbers();
        }
        while (pages.size() < files.size()) {
            // add empty pages
            pages.add(new Page());
        }
    }

    public void saveToDisk() {
        MEIInOut meiInOut = new MEIInOut();
        meiInOut.writeMei(path, pages, files);
    }
}
