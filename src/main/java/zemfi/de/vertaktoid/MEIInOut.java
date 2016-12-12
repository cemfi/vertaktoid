package zemfi.de.vertaktoid;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Collection;

import nu.xom.*;

/**
 * Created by aristotelis on 08.08.16.
 */
class MEIInOut {

    private nu.xom.Document meiDocument;


    private Element surface;
    private Element section;

    private void makEmptyMei() {
        Element mei = new Element("mei");
        meiDocument = new nu.xom.Document(mei);
        Element meiHead = new Element("meiHead");
        Element fileDesc = new Element("fileDesc");
        Element titleStmt = new Element("titleStmt");
        Element title = new Element("title");
        Element pubStmt = new Element("pubStmt");
        Element music = new Element("music");
        Element facsimile = new Element("facsimile");
        surface = new Element("surface");
        Element body = new Element("body");
        Element mdiv = new Element("mdiv");
        Element score = new Element("score");
        Element scoreDef = new Element("scoreDef");
        section = new Element("section");

        mei.appendChild(meiHead);
        meiHead.appendChild(fileDesc);
        fileDesc.appendChild(titleStmt);
        fileDesc.appendChild(pubStmt);
        mei.appendChild(music);
        music.appendChild(facsimile);
        facsimile.appendChild(surface);
        music.appendChild(body);
        body.appendChild(mdiv);
        mdiv.appendChild(score);
        score.appendChild(scoreDef);
        score.appendChild(section);
    }


    private Element lastMeasure = null;
    private String lastMeasureName = null;
    private String lastUuidZone = null;
    private  void addPage(Page page) {

        Element graphic = new Element("graphic");
        Attribute a = new Attribute("target", page.fileName);
        a.setNamespace("xml", "http://www.w3.org/XML/1998/namespace");
        graphic.addAttribute(a);
        a = new Attribute("type", "facsimile");
        graphic.addAttribute(a);
        a = new Attribute("width", "" + page.imageWidth);
        graphic.addAttribute(a);
        a = new Attribute("height", "" + page.imageHeight);
        graphic.addAttribute(a);

        Collection<Box> boxes = page.getBoxes();

        for (Box box: boxes) {
            String uuidZone = UUID.randomUUID().toString();
            a = new Attribute("id", uuidZone);
            a.setNamespace("xml", "http://www.w3.org/XML/1998/namespace"); // set its namespace to xml
            Element zone = new Element("zone");
            zone.addAttribute(a);
            a = new Attribute("type", "measure");
            zone.addAttribute(a);
            a = new Attribute("ulx", "" + box.left);
            zone.addAttribute(a);
            a = new Attribute("uly", "" + box.top);
            zone.addAttribute(a);
            a = new Attribute("lrx", "" + box.right);
            zone.addAttribute(a);
            a = new Attribute("lry", "" + box.bottom);
            zone.addAttribute(a);
            graphic.appendChild(zone);

            String newMeasureName;
            if (box.manualSequenceNumber != null) {
                newMeasureName = box.manualSequenceNumber;
            } else {
                newMeasureName = "" + box.sequenceNumber;
            }
            if (lastMeasureName == null || !newMeasureName.equals(lastMeasureName)) {
                // new zone and new measure
                Element measure = new Element("measure");
                a = new Attribute("n", newMeasureName);

                measure.addAttribute(a);
                String uuidMeasure = UUID.randomUUID().toString();
                a = new Attribute("id", uuidMeasure);
                a.setNamespace("xml", "http://www.w3.org/XML/1998/namespace");
                measure.addAttribute(a);
                a = new Attribute("facs", uuidZone);
                measure.addAttribute(a);
                section.appendChild(measure);
                lastMeasure = measure;
                lastMeasureName = newMeasureName;
                lastUuidZone = uuidZone;
            } else {
                // new zone for old measure
                lastUuidZone = lastUuidZone + " " + uuidZone;
                a = new Attribute("facs", lastUuidZone);
                lastMeasure.addAttribute(a);
            }
        }

        surface.appendChild(graphic);
    }


    boolean writeMei(String path, ArrayList<Page> pages) {
        return writeMei(path, "mei.mei", pages);
    }


    boolean writeMei(String path, String filename, ArrayList<Page> pages) {

        makEmptyMei();

        int i;
        for (i = 0; i < pages.size(); i++) {
            addPage(pages.get(i));
        }

        if (path == null) {
            return false;
        }
        // create the file in the file system
        boolean returnValue = true;

        File myDir = new File(path);
        myDir.mkdirs();
        File file = new File (myDir, filename);
        Log.d("zemfi", "path : " + file.getAbsolutePath());
        if (file.exists ()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            Serializer serializer;
            try {
                serializer = new Serializer(out, "UTF-8"); // connect serializer with FileOutputStream and specify encoding
                serializer.setIndent(4);                                // specify indents in xml code
                serializer.write(this.meiDocument);                             // write data from mei to file
            } catch (IOException e) {
                Log.v("Vertaktoid", "IOException");
                e.printStackTrace();
                returnValue = false;
            } catch (NullPointerException e) {
                e.printStackTrace();
                returnValue = false;
            }
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnValue;
    }


    private File file;
    boolean readMeiFile(String path) {

        this.file = new File(path);

        if (!file.exists()) {
            makEmptyMei();
            return false;
        }

        // read file into the mei instance of Document
        Builder builder = new Builder(false);
        try {
            this.meiDocument = builder.build(file);

        }
        catch (ValidityException e) {
            e.printStackTrace();
            for (int i=0; i < e.getErrorCount(); i++) {
                System.out.println(e.getValidityError(i));
            }
        } catch (ParsingException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private ArrayList<Page> pages;
    ArrayList<Page> getPages() {
        return pages;
    }


    void parseXml() {
        Element meiElement = meiDocument.getRootElement();
        ArrayList<Page> result = new ArrayList<>();

        Elements musics = meiElement.getChildElements("music");
        Element music = musics.get(0);
        Elements facsimiles = music.getChildElements("facsimile");
        Element facsimile = facsimiles.get(0);
        Elements surfaces = facsimile.getChildElements("surface");
        Element surface = surfaces.get(0);
        Elements graphics = surface.getChildElements("graphic");

        int i;
        for(i = 0; i < graphics.size(); i++) {
            Element graphic = graphics.get(i);
            String filename = graphic.getAttributeValue("target", "http://www.w3.org/XML/1998/namespace");
            Page currentPage = new Page(filename);

            Elements zones = graphic.getChildElements("zone");

            int j;
            for(j = 0; j < zones.size(); j++) {
                Element zone = zones.get(j);

                float ulx = Float.parseFloat(zone.getAttributeValue("ulx"));
                float uly = Float.parseFloat(zone.getAttributeValue("uly"));
                float lrx = Float.parseFloat(zone.getAttributeValue("lrx"));
                float lry = Float.parseFloat(zone.getAttributeValue("lry"));
                String uuidZone = zone.getAttributeValue("id", "http://www.w3.org/XML/1998/namespace");
                Element measure = findMeasureFor(uuidZone);
                String uuidMeasure = measure.getAttributeValue("id", "http://www.w3.org/XML/1998/namespace");
                String name = measure.getAttributeValue("n");

                Box box = new Box(ulx, lrx, uly, lry);
                box.manualSequenceNumber = name;
                box.zoneUuid = uuidZone;
                box.measureUuid = uuidMeasure;
                currentPage.addBox(box);
            }

            result.add(currentPage);
        }
        this.pages = result;
    }


    private Element findMeasureFor(String uuidZone) {
        Element meiElement = meiDocument.getRootElement();
        Elements musics = meiElement.getChildElements("music");
        Element music = musics.get(0);
        Elements bodies = music.getChildElements("body");
        Element body = bodies.get(0);
        Elements mdivs = body.getChildElements("mdiv");
        Element mdiv = mdivs.get(0);
        Elements scores = mdiv.getChildElements("score");
        Element score = scores.get(0);
        Elements sections = score.getChildElements("section");
        Element section = sections.get(0);
        Elements insideSection = section.getChildElements();
        int i;
        for (i = 0; i < insideSection.size(); i++) {
            Element element = insideSection.get(i);
            if (element.getLocalName().equals("measure")) {
                if (element.getAttributeValue("facs").equals(uuidZone)) {
                    return element;
                }
            }
        }
        return null;
    }


}
