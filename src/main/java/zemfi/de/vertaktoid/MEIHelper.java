package zemfi.de.vertaktoid;

import android.graphics.PointF;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import nu.xom.*;

/**
 * Created by yevgen on 03.01.2017.
 */

public class MEIHelper {

    static boolean writeMEI(File meiFile, ArrayList<Page> pages, ArrayList<Movement> movements) {
        Element mei = new Element("mei", Vertaktoid.MEI_NS);
        Document meiDocument = new Document(mei);
        Element meiHead = new Element("meiHead", Vertaktoid.MEI_NS);
        Element fileDesc = new Element("fileDesc", Vertaktoid.MEI_NS);
        Element titleStmt = new Element("titleStmt", Vertaktoid.MEI_NS);
        Element title = new Element("title", Vertaktoid.MEI_NS);
        Element pubStmt = new Element("pubStmt", Vertaktoid.MEI_NS);
        Element music = new Element("music", Vertaktoid.MEI_NS);
        Element facsimile = new Element("facsimile", Vertaktoid.MEI_NS);
        //Element surface = new Element("surface", Vertaktoid.MEI_NS);
        Element body = new Element("body", Vertaktoid.MEI_NS);
        Attribute a;
        boolean returnValue = true;

        mei.appendChild(meiHead);
        meiHead.appendChild(fileDesc);
        fileDesc.appendChild(titleStmt);
        fileDesc.appendChild(pubStmt);
        mei.appendChild(music);
        music.appendChild(facsimile);
        music.appendChild(body);

        for(int i = 0; i < pages.size(); i++) {
            Page page = pages.get(i);
            Element surface = new Element("surface", Vertaktoid.MEI_NS);
            a = new Attribute("n", "" + (i + 1));
            surface.addAttribute(a);
            a = new Attribute("ulx", "" + 0);
            surface.addAttribute(a);
            a = new Attribute("uly", "" + 0);
            surface.addAttribute(a);
            a = new Attribute("lrx", "" + page.imageWidth);
            surface.addAttribute(a);
            a = new Attribute("lry", "" + page.imageHeight);
            surface.addAttribute(a);
            Element graphic = new Element("graphic", Vertaktoid.MEI_NS);
            a = new Attribute("target", page.imageFile.getName());
            //a.setNamespace("xml", "http://www.w3.org/XML/1998/namespace");
            graphic.addAttribute(a);
            a = new Attribute("type", "facsimile");
            graphic.addAttribute(a);
            a = new Attribute("width", "" + page.imageWidth);
            graphic.addAttribute(a);
            a = new Attribute("height", "" + page.imageHeight);
            graphic.addAttribute(a);
            surface.appendChild(graphic);

            for(Measure measure : page.measures) {
                a = new Attribute("id", measure.zoneUuid);
                a.setNamespace("xml", "http://www.w3.org/XML/1998/namespace"); // set its namespace to xml
                Element zone = new Element("zone", Vertaktoid.MEI_NS);
                zone.addAttribute(a);
                a = new Attribute("type", "measure");
                zone.addAttribute(a);
                a = new Attribute("ulx", "" + measure.left);
                zone.addAttribute(a);
                a = new Attribute("uly", "" + measure.top);
                zone.addAttribute(a);
                a = new Attribute("lrx", "" + measure.right);
                zone.addAttribute(a);
                a = new Attribute("lry", "" + measure.bottom);
                zone.addAttribute(a);
                surface.appendChild(zone);
            }
            facsimile.appendChild(surface);
        }

        for(Movement movement : movements) {
            Element mdiv = new Element("mdiv", Vertaktoid.MEI_NS);
            Element score = new Element("score", Vertaktoid.MEI_NS);
            Element scoreDef = new Element("scoreDef", Vertaktoid.MEI_NS);
            Element section = new Element("section", Vertaktoid.MEI_NS);
            a = new Attribute("n", "" + (movements.indexOf(movement) + 1));
            mdiv.addAttribute(a);
            a = new Attribute("label", movement.label);
            mdiv.addAttribute(a);

            body.appendChild(mdiv);
            mdiv.appendChild(score);
            score.appendChild(scoreDef);
            score.appendChild(section);
            for(Measure measure : movement.measures) {
                Element measureElem = new Element("measure", Vertaktoid.MEI_NS);
                a = new Attribute("n", measure.manualSequenceNumber == null ?
                        String.valueOf(measure.sequenceNumber) : measure.manualSequenceNumber);
                measureElem.addAttribute(a);
                a = new Attribute("id", measure.measureUuid);
                a.setNamespace("xml", "http://www.w3.org/XML/1998/namespace");
                measureElem.addAttribute(a);
                a = new Attribute("facs", "#" + measure.zoneUuid);
                measureElem.addAttribute(a);
                section.appendChild(measureElem);
            }
        }

        File dir = meiFile.getParentFile();
        dir.mkdirs();
        if(meiFile.exists()) {
            meiFile.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(meiFile);
            Serializer serializer;
            try {
                serializer = new Serializer(out, "UTF-8"); // connect serializer with FileOutputStream and specify encoding
                serializer.setIndent(4);                                // specify indents in xml code
                serializer.write(meiDocument);                             // write data from mei to file
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

    static boolean readMEI(File meiFile, ArrayList<Page> pages, ArrayList<Movement> movements) {
        File dir = meiFile.getParentFile();

        if(!meiFile.exists()) {
            return false;
        }

        Builder builder = new Builder(false);
        Document meiDocument = new Document(new Element("mei", Vertaktoid.MEI_NS));
        try {
            meiDocument = builder.build(meiFile);

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
        Element meiElement = meiDocument.getRootElement();
        Elements musics = meiElement.getChildElements("music", Vertaktoid.MEI_NS);
        Element music = musics.get(0);
        Elements facsimiles = music.getChildElements("facsimile", Vertaktoid.MEI_NS);
        Element facsimile = facsimiles.get(0);
        Elements surfaces = facsimile.getChildElements("surface", Vertaktoid.MEI_NS);
        Elements bodies = music.getChildElements("body", Vertaktoid.MEI_NS);
        Element body = bodies.get(0);

        Elements mdivs = body.getChildElements("mdiv", Vertaktoid.MEI_NS);
        for(int j = 0; j < mdivs.size(); j++) {
            Element mdiv = mdivs.get(j);
            Movement movement = new Movement();
            String movementN = mdiv.getAttributeValue("n");
            try {
                movement.number = Integer.getInteger(movementN);
            }
            catch (Exception e) {
                movement.number = j + 1;
            }
            movement.label = mdiv.getAttributeValue("label");

            Elements scores = mdiv.getChildElements("score", Vertaktoid.MEI_NS);
            Element score = scores.get(0);
            Elements sections = score.getChildElements("section", Vertaktoid.MEI_NS);
            Element section = sections.get(0);
            Elements insideSection = section.getChildElements();
            for (int i = 0; i < insideSection.size(); i++) {
                Element element = insideSection.get(i);
                if (element.getLocalName().equals("measure")) {
                    String name = element.getAttributeValue("n");
                    Measure measure = new Measure();
                    measure.manualSequenceNumber = name;
                    measure.zoneUuid = element.getAttributeValue("facs");
                    if(measure.zoneUuid.startsWith("#")) {
                        measure.zoneUuid = measure.zoneUuid.substring(1);
                    }
                    measure.measureUuid = element.getAttributeValue("id", "http://www.w3.org/XML/1998/namespace");
                    measure.movement = movement;
                    movement.measures.add(measure);
                }
            }
            movements.add(movement);
        }

        for(int i = 0; i < surfaces.size(); i++) {
            Element surface = surfaces.get(i);
            Elements graphics = surface.getChildElements("graphic", Vertaktoid.MEI_NS);
            Element graphic = graphics.get(0);
            //final String filename = graphic.getAttributeValue("target", "http://www.w3.org/XML/1998/namespace");
            final File file = new File(graphic.getAttributeValue("target"));
            final String filename = file.getName();
            File[] images = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File file, String s) {
                    return s.equals(filename);
                }
            });
            Page page;
            if(images.length > 0) {
                page = new Page(images[0], i + 1);
            }
            else {
                page = new Page(new File(dir, filename), i + 1);
            }
            page.imageWidth = Integer.parseInt(graphic.getAttributeValue("width"));
            page.imageHeight = Integer.parseInt(graphic.getAttributeValue("height"));

            Elements zones = surface.getChildElements("zone", Vertaktoid.MEI_NS);

            for(int j = 0; j < zones.size(); j++) {
                Element zone = zones.get(j);
                float ulx = Float.parseFloat(zone.getAttributeValue("ulx"));
                float uly = Float.parseFloat(zone.getAttributeValue("uly"));
                float lrx = Float.parseFloat(zone.getAttributeValue("lrx"));
                float lry = Float.parseFloat(zone.getAttributeValue("lry"));
                String uuidZone = zone.getAttributeValue("id", "http://www.w3.org/XML/1998/namespace");
                Measure measure = findMeasureFor(uuidZone, movements);
                if(measure != null) {
                    measure.left = ulx;
                    measure.top = uly;
                    measure.right = lrx;
                    measure.bottom = lry;
                    measure.page = page;
                    page.measures.add(measure);
                }

            }
            pages.add(page);
        }
        for (Movement movement : movements) {
            movement.sortMeasures();
        }
        for (Page page : pages) {
            page.sortMeasures();
        }
        return true;
    }

    private static Measure findMeasureFor(String uuidZone, ArrayList<Movement> movements) {
        for(Movement movement : movements) {
            for (Measure measure : movement.measures) {
                if(measure.zoneUuid.equals(uuidZone)) {
                    return measure;
                }
            }
        }
        return null;
    }
}
