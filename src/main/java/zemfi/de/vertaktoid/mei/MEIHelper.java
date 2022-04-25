package zemfi.de.vertaktoid.mei;

import static zemfi.de.vertaktoid.Vertaktoid.VERTACTOID_VERSION;

import android.app.Dialog;
import android.os.ParcelFileDescriptor;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import nu.xom.ValidityException;
import zemfi.de.vertaktoid.MainActivity;
import zemfi.de.vertaktoid.Vertaktoid;
import zemfi.de.vertaktoid.helpers.Point2D;
import zemfi.de.vertaktoid.model.Facsimile;
import zemfi.de.vertaktoid.model.Measure;
import zemfi.de.vertaktoid.model.Movement;
import zemfi.de.vertaktoid.model.Page;

/**
 * MEI input\output routines.
 */

public class MEIHelper {

    public static Document meiDocument;
    public static String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());




    public static void clearDocument() {
        meiDocument = new Document(new Element("mei", Vertaktoid.MEI_NS));
    }

    public static Element findElementByUiid(Elements elements, String uuid) {
        for(int i = 0; i < elements.size(); i++) {
            Element element = elements.get(i);
            if(element.getAttribute("id", "http://www.w3.org/XML/1998/namespace") != null) {
                Attribute attr = element.getAttribute("id", "http://www.w3.org/XML/1998/namespace");
                if(attr != null) {
                    String elemUuid = element.getAttributeValue("id", "http://www.w3.org/XML/1998/namespace");
                    if (elemUuid.equals(uuid)) {
                        return element;
                    }
                }
            }

        }
        return null;
    }

    /**
     * Exports data in MEI file. The initial structure of MEI file will be kept so far as possible.
     * The comments will be lost.
     * @param meiFile The path to file.
     * @param document The data to be saved.
     * @return true if no exceptions.
     */
    public static boolean writeMEI(DocumentFile dir, DocumentFile meiFile, Facsimile document) {

        boolean returnValue = true;
        Dialog downloadProgressDialogue = null;
        ProgressBar text;
        TextView text2;
        final Boolean[] canceled = new Boolean[1];

        //save in measures objects if last at system or page
        document.calculateBreaks();

        if(meiDocument == null) {
            meiDocument = new Document(new Element("mei", Vertaktoid.MEI_NS));
        }
        Element meiElement = meiDocument.getRootElement();

        Element meiHead = meiElement.getFirstChildElement("meiHead", Vertaktoid.MEI_NS);
        Attribute a1;

        if(meiHead == null) {
            meiHead = new Element("meiHead", Vertaktoid.MEI_NS);
            meiElement.appendChild(meiHead);
            Element fileDesc = new Element("fileDesc", Vertaktoid.MEI_NS);
            meiHead.appendChild(fileDesc);
            Element titleStmt = new Element("titleStmt", Vertaktoid.MEI_NS);
            fileDesc.appendChild(titleStmt);
            Element title = new Element("title", Vertaktoid.MEI_NS);
            Element pubStmt = new Element("pubStmt", Vertaktoid.MEI_NS);
            fileDesc.appendChild(pubStmt);
            Element encodingDesc = new Element("encodingDesc", Vertaktoid.MEI_NS);
            meiHead.appendChild(encodingDesc);
            Element appInfo = new Element("appInfo", Vertaktoid.MEI_NS);
            encodingDesc.appendChild(appInfo);
            Element application = new Element("application", Vertaktoid.MEI_NS);
            appInfo.appendChild(application);
            a1 = new Attribute("id",Vertaktoid.MEI_APPLICATION_ID_PREFIX + UUID.randomUUID().toString());
            a1.setNamespace("xml", "http://www.w3.org/XML/1998/namespace"); // set its namespace to xml
            application.addAttribute(a1);

            a1 = new Attribute("isodate",date.toString());
            application.addAttribute(a1);

            Element name = new Element("name", Vertaktoid.MEI_NS);
            Element ptr = new Element("ptr", Vertaktoid.MEI_NS);
            name.insertChild(VERTACTOID_VERSION,0);
            application.appendChild(name);
            application.appendChild(ptr);
            a1 = new Attribute("target","https://github.com/cemfi/vertaktoid/releases/tag/v2.0.2");
            ptr.addAttribute(a1);
        }
        Elements musics = meiElement.getChildElements("music", Vertaktoid.MEI_NS);
        Element music;
        if(musics.size() == 0) {
            music = new Element("music", Vertaktoid.MEI_NS);
            meiElement.appendChild(music);

        } else {
            music = musics.get(0);
        }
        Elements facsimiles = music.getChildElements("facsimile", Vertaktoid.MEI_NS);
        Element facsimile;
        if(facsimiles.size() == 0) {
            facsimile = new Element("facsimile", Vertaktoid.MEI_NS);
            music.appendChild(facsimile);
        } else {
            facsimile = facsimiles.get(0);
        }

        Elements bodies = music.getChildElements("body", Vertaktoid.MEI_NS);
        Element body;
        if(bodies.size() == 0) {
            body = new Element("body", Vertaktoid.MEI_NS);
            music.appendChild(body);

        } else {
            body = bodies.get(0);
        }

        Elements surfaces = facsimile.getChildElements("surface", Vertaktoid.MEI_NS);
        Elements mdivs = body.getChildElements("mdiv", Vertaktoid.MEI_NS);

        Attribute a;
        for(int i = 0; i < document.pages.size(); i++) {

            Page page = document.pages.get(i);

            Element surface = findElementByUiid(surfaces, page.surfaceUuid);
            if(surface == null) {
                surface = new Element("surface", Vertaktoid.MEI_NS);
                facsimile.appendChild(surface);
            }
            a = new Attribute("id", page.surfaceUuid);
            a.setNamespace("xml", "http://www.w3.org/XML/1998/namespace"); // set its namespace to xml
            surface.addAttribute(a);
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

            Elements graphics = surface.getChildElements("graphic", Vertaktoid.MEI_NS);
            Element graphic = findElementByUiid(graphics, page.graphicUuid);
            if(graphic == null) {
                graphic = new Element("graphic", Vertaktoid.MEI_NS);
                surface.appendChild(graphic);
            }
            a = new Attribute("id", page.graphicUuid);
            a.setNamespace("xml", "http://www.w3.org/XML/1998/namespace"); // set its namespace to xml
            graphic.addAttribute(a);
            a = new Attribute("target", page.getImageFileName());
            //a.setNamespace("xml", "http://www.w3.org/XML/1998/namespace");
            graphic.addAttribute(a);
            a = new Attribute("type", "facsimile");
            graphic.addAttribute(a);
            a = new Attribute("width", "" + page.imageWidth);
            graphic.addAttribute(a);
            a = new Attribute("height", "" + page.imageHeight);
            graphic.addAttribute(a);

            ArrayList<Element> existingZones = new ArrayList<>();
            Elements zones = surface.getChildElements("zone", Vertaktoid.MEI_NS);
            for(Measure measure : page.measures) {
                Element zone = findElementByUiid(zones, measure.zone.zoneUuid);
                existingZones.add(zone);
                if(zone == null) {
                    zone = new Element("zone", Vertaktoid.MEI_NS);
                    surface.appendChild(zone);
                }
                zone.removeChildren();
                a = new Attribute("id", measure.zone.zoneUuid);
                a.setNamespace("xml", "http://www.w3.org/XML/1998/namespace"); // set its namespace to xml
                zone.addAttribute(a);
                a = new Attribute("type", "measure");
                zone.addAttribute(a);
                a = new Attribute("ulx", "" + toSourceCoords(measure.zone.getBoundLeft(), page.getInSampleSize(),
                        page.imageWidth * page.getInSampleSize()));
                zone.addAttribute(a);
                a = new Attribute("uly", "" + toSourceCoords(measure.zone.getBoundTop(), page.getInSampleSize(),
                        page.imageHeight * page.getInSampleSize()));
                zone.addAttribute(a);
                a = new Attribute("lrx", "" + toSourceCoords(measure.zone.getBoundRight(), page.getInSampleSize(),
                        page.imageWidth * page.getInSampleSize()));
                zone.addAttribute(a);
                a = new Attribute("lry", "" + toSourceCoords(measure.zone.getBoundBottom(), page.getInSampleSize(),
                        page.imageHeight * page.getInSampleSize()));
                zone.addAttribute(a);
                if(measure.zone.getAnnotationType() != Facsimile.AnnotationType.ORTHOGONAL_BOX) {
                    for(Point2D vertex : measure.zone.getVertices()) {
                        Element point = new Element("point", Vertaktoid.MEI_NS);
                        a = new Attribute("x","" + toSourceCoords(vertex.x(), page.getInSampleSize(),
                                page.imageWidth * page.getInSampleSize()));
                        point.addAttribute(a);
                        a = new Attribute("y","" + toSourceCoords(vertex.y(), page.getInSampleSize(),
                                page.imageHeight * page.getInSampleSize()));
                        point.addAttribute(a);
                        zone.appendChild(point);
                    }
                }

            }

            for(int j = 0; j < zones.size(); j++) {
                if(!existingZones.contains(zones.get(j))) {
                    surface.removeChild(zones.get(j));
                }
            }
        }

        ArrayList<Element> existingMdivs = new ArrayList<>();
        for(Movement movement : document.movements) {
            Element mdiv = findElementByUiid(mdivs, movement.mdivUuid);
            existingMdivs.add(mdiv);
            if(mdiv == null) {
                mdiv = new Element("mdiv", Vertaktoid.MEI_NS);
                body.appendChild(mdiv);
            }
            a = new Attribute("id", movement.mdivUuid);
            a.setNamespace("xml", "http://www.w3.org/XML/1998/namespace"); // set its namespace to xml
            mdiv.addAttribute(a);
            a = new Attribute("n", "" + (document.movements.indexOf(movement) + 1));
            mdiv.addAttribute(a);
            a = new Attribute("label", movement.label);
            mdiv.addAttribute(a);

            Element score = mdiv.getFirstChildElement("score", Vertaktoid.MEI_NS);
            if(score == null) {
                score = new Element("score", Vertaktoid.MEI_NS);
                mdiv.appendChild(score);
            }

            Element scoreDef = score.getFirstChildElement("scoreDef", Vertaktoid.MEI_NS);
            if(scoreDef == null) {
                scoreDef = new Element("scoreDef", Vertaktoid.MEI_NS);
                score.appendChild(scoreDef);
            }

            Element section = score.getFirstChildElement("section", Vertaktoid.MEI_NS);
            if(section == null) {
                section = new Element("section", Vertaktoid.MEI_NS);
                score.appendChild(section);
            }

            Elements sbs = section.getChildElements("sb", Vertaktoid.MEI_NS);
            Elements pbs = section.getChildElements("pb", Vertaktoid.MEI_NS);
            for(int i = 0; i < sbs.size(); i++) {
                section.removeChild(sbs.get(i));
            }
            for (int i = 0; i < pbs.size(); i++) {
                section.removeChild(pbs.get(i));
            }


            ArrayList<MeasureElementPair> corrMeasureElems = new ArrayList<>();
            Elements measureElems = section.getChildElements("measure", Vertaktoid.MEI_NS);
            for(int i = 0; i < movement.measures.size(); i++) {
                Measure measure = movement.measures.get(i);
                Element measureElem = findElementByUiid(measureElems, measure.measureUuid);
                if(measureElem == null) {
                    measureElem = new Element("measure", Vertaktoid.MEI_NS);
                }
                corrMeasureElems.add(new MeasureElementPair(measure, measureElem));

                //old
                // a = new Attribute("n", measure.manualSequenceNumber == null ?
                //        String.valueOf(measure.sequenceNumber) : measure.manualSequenceNumber);

                // calculation of a unique and sequent number n
                a = new Attribute("n", "" + (i + 1));
                measureElem.addAttribute(a);
                // label will later be read and used to calculate the sequenceNumber
                a = new Attribute("label", measure.manualSequenceNumber == null ?
                        String.valueOf(measure.sequenceNumber) : measure.manualSequenceNumber);
                measureElem.addAttribute(a);
                a = new Attribute("id", measure.measureUuid);
                a.setNamespace("xml", "http://www.w3.org/XML/1998/namespace");
                measureElem.addAttribute(a);
                a = new Attribute("facs", "#" + measure.zone.zoneUuid);
                measureElem.addAttribute(a);
            }
            section.removeChildren();
            Collections.sort(corrMeasureElems, MeasureElementPair.MEASURE_ELEMENT_PAIR_COMPARATOR);

            for(int i = 0; i < corrMeasureElems.size(); i++) {
                Element measureElem = corrMeasureElems.get(i).getElement();
                Measure measure = corrMeasureElems.get(i).getMeasure();
                section.appendChild(measureElem);
                if(measure.lastAtPage) {
                    Element pb = new Element("pb", Vertaktoid.MEI_NS);
                    section.insertChild(pb, section.indexOf(measureElem) + 1);
                    Element sb = new Element("sb", Vertaktoid.MEI_NS);
                    section.insertChild(sb, section.indexOf(measureElem) + 1);
                } else
                if(measure.lastAtSystem) {
                    Element sb = new Element("sb", Vertaktoid.MEI_NS);
                    section.insertChild(sb, section.indexOf(measureElem) + 1);
                }
            }
            // add pb at the beginning
            Element pb = new Element("pb", Vertaktoid.MEI_NS);
            section.insertChild(pb, 0);
        }

        for(int i = 0; i < mdivs.size(); i++) {
            if(!existingMdivs.contains(mdivs.get(i))) {
                body.removeChild(mdivs.get(i));
            }
        }


        if(meiFile == null) {
            meiFile = dir.createFile("application/xml", dir.getName() + Vertaktoid.DEFAULT_MEI_EXTENSION);
        }

        try {
            ParcelFileDescriptor pfd = MainActivity.context.getContentResolver().
                    openFileDescriptor(meiFile.getUri(), "w");
            FileOutputStream out =
                    new FileOutputStream(pfd.getFileDescriptor());

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
            pfd.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnValue;
    }


    /**
     * Reads the data from MEI file.
     * @param meiFile The MEI file.
     * @param document The facsimile.
     * @return true if properly readed.
     */
    public static boolean readMEI(DocumentFile dir, DocumentFile meiFile, Facsimile document) {
        Attribute a;
        if(!meiFile.exists()) {
            return false;
        }

        Builder builder = new Builder(false);
        meiDocument = new Document(new Element("mei", Vertaktoid.MEI_NS));
        try {

            InputStream inputStream = MainActivity.context.getContentResolver().openInputStream(meiFile.getUri());
            meiDocument = builder.build(inputStream);

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
            a = mdiv.getAttribute("id", "http://www.w3.org/XML/1998/namespace");
            if(a != null) {
                if(a.getValue().substring(0, 1).matches("\\d")) {
                    a.setValue(Vertaktoid.MEI_MDIV_ID_PREFIX + a.getValue());
                }
            } else {
                a = new Attribute("id", Vertaktoid.MEI_MDIV_ID_PREFIX + UUID.randomUUID().toString());
                a.setNamespace("xml", "http://www.w3.org/XML/1998/namespace");
                mdiv.addAttribute(a);
            }
            movement.mdivUuid = mdiv.getAttributeValue("id", "http://www.w3.org/XML/1998/namespace");
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
                if(element.getLocalName().equals("sb")) {
                    section.removeChild(element);
                }
                if(element.getLocalName().equals("pb")) {
                    section.removeChild(element);
                }
                if (element.getLocalName().equals("measure")) {
                    String name = element.getAttributeValue("label");
                    Measure measure = new Measure();
                    measure.manualSequenceNumber = name;
                    a = element.getAttribute("facs");
                    if(a != null) {
                        measure.zone.zoneUuid = element.getAttributeValue("facs");
                        if (measure.zone.zoneUuid.startsWith("#")) {
                            measure.zone.zoneUuid = measure.zone.zoneUuid.substring(1);
                        }
                        if (measure.zone.zoneUuid.substring(0, 1).matches("\\d")) {
                            measure.zone.zoneUuid = Vertaktoid.MEI_ZONE_ID_PREFIX + measure.zone.zoneUuid;
                            a.setValue("#" + measure.zone.zoneUuid);
                        }
                    }
                    a = element.getAttribute("id", "http://www.w3.org/XML/1998/namespace");
                    if(a != null) {
                        if(a.getValue().substring(0, 1).matches("\\d")) {
                            a.setValue(Vertaktoid.MEI_MEASURE_ID_PREFIX + a.getValue());
                        }
                    } else {
                        a = new Attribute("id", Vertaktoid.MEI_MEASURE_ID_PREFIX + UUID.randomUUID().toString());
                        a.setNamespace("xml", "http://www.w3.org/XML/1998/namespace");
                        element.addAttribute(a);
                    }
                    measure.measureUuid = element.getAttributeValue("id", "http://www.w3.org/XML/1998/namespace");
                    measure.movement = movement;
                    movement.measures.add(measure);
                }
            }
            document.movements.add(movement);
        }

        for(int i = 0; i < surfaces.size(); i++) {


            Element surface = surfaces.get(i);
            Elements graphics = surface.getChildElements("graphic", Vertaktoid.MEI_NS);
            Element graphic = graphics.get(0);
            final String filename = graphic.getAttributeValue("target");
            Page page= new Page(dir,filename, i+1);
            page.imageWidth = Integer.parseInt(graphic.getAttributeValue("width"));
            page.imageHeight = Integer.parseInt(graphic.getAttributeValue("height"));
            page.setInSampleSize(page.calculateInSampleSize(page.imageWidth, page.imageHeight));
            a = surface.getAttribute("id", "http://www.w3.org/XML/1998/namespace");
            if(a != null) {
                if(a.getValue().substring(0, 1).matches("\\d")) {
                    a.setValue(Vertaktoid.MEI_SURFACE_ID_PREFIX + a.getValue());
                }
            } else {
                a = new Attribute("id", Vertaktoid.MEI_SURFACE_ID_PREFIX + UUID.randomUUID().toString());
                a.setNamespace("xml", "http://www.w3.org/XML/1998/namespace");
                surface.addAttribute(a);
            }
            page.surfaceUuid = surface.getAttributeValue("id", "http://www.w3.org/XML/1998/namespace");

            a = graphic.getAttribute("id", "http://www.w3.org/XML/1998/namespace");
            if(a != null) {
                if(a.getValue().substring(0, 1).matches("\\d")) {
                    a.setValue(Vertaktoid.MEI_GRAPHIC_ID_PREFIX + a.getValue());
                }
            } else {
                a = new Attribute("id", Vertaktoid.MEI_GRAPHIC_ID_PREFIX + UUID.randomUUID().toString());
                a.setNamespace("xml", "http://www.w3.org/XML/1998/namespace");
                graphic.addAttribute(a);
            }
            page.graphicUuid = graphic.getAttributeValue("id", "http://www.w3.org/XML/1998/namespace");

            Elements zones = surface.getChildElements("zone", Vertaktoid.MEI_NS);

            for(int j = 0; j < zones.size(); j++) {
                Element zone = zones.get(j);
                float ulx = fromSourceCoords(Float.parseFloat(zone.getAttributeValue("ulx")), page.getInSampleSize(),
                        page.imageWidth / page.getInSampleSize());
                float uly = fromSourceCoords(Float.parseFloat(zone.getAttributeValue("uly")), page.getInSampleSize(),
                        page.imageHeight / page.getInSampleSize() );
                float lrx = fromSourceCoords(Float.parseFloat(zone.getAttributeValue("lrx")), page.getInSampleSize(),
                        page.imageWidth / page.getInSampleSize());
                float lry = fromSourceCoords(Float.parseFloat(zone.getAttributeValue("lry")), page.getInSampleSize(),
                        page.imageHeight / page.getInSampleSize());
                List<Point2D> vertices = new ArrayList<>();
                Elements points = zone.getChildElements("point", Vertaktoid.MEI_NS);
                for(int l = 0; l < points.size(); l++) {
                    Element point = points.get(l);
                    float x = fromSourceCoords(Float.parseFloat(point.getAttributeValue("x")), page.getInSampleSize(),
                            page.imageWidth / page.getInSampleSize());
                    float y = fromSourceCoords(Float.parseFloat(point.getAttributeValue("y")), page.getInSampleSize(),
                            page.imageHeight / page.getInSampleSize());
                    vertices.add(new Point2D(x, y));
                }
                a = zone.getAttribute("id", "http://www.w3.org/XML/1998/namespace");
                if(a != null) {
                    if(a.getValue().substring(0, 1).matches("\\d")) {
                        a.setValue(Vertaktoid.MEI_ZONE_ID_PREFIX + a.getValue());
                    }
                } else {
                    a = new Attribute("id", Vertaktoid.MEI_ZONE_ID_PREFIX + UUID.randomUUID().toString());
                    a.setNamespace("xml", "http://www.w3.org/XML/1998/namespace");
                    zone.addAttribute(a);
                }
                String uuidZone = zone.getAttributeValue("id", "http://www.w3.org/XML/1998/namespace");

                Measure measure = findMeasureFor(uuidZone, document.movements);
                if(measure != null) {
                    if(vertices.size() == 4) {
                        measure.zone.setVertices(vertices);
                        measure.zone.setAnnotationType(Facsimile.AnnotationType.ORIENTED_BOX);
                    } else if(vertices.size() != 4 && vertices.size() > 0)  {
                        measure.zone.setVertices(vertices);
                        measure.zone.setAnnotationType(Facsimile.AnnotationType.POLYGON);
                    }
                    else {
                        vertices.clear();
                        vertices.add(new Point2D(ulx, uly));
                        vertices.add(new Point2D(ulx, lry));
                        vertices.add(new Point2D(lrx, lry));
                        vertices.add(new Point2D(lrx, uly));
                        measure.zone.setVertices(vertices);
                        measure.zone.setAnnotationType(Facsimile.AnnotationType.ORTHOGONAL_BOX);
                    }
                    measure.page = page;
                    page.measures.add(measure);
                }

            }
            document.pages.add(page);
        }
        for (Movement movement : document.movements) {
            movement.sortMeasures();
        }
        for (Page page : document.pages) {
            page.sortMeasures();
        }
        return true;
    }

    private static long toSourceCoords(double value, int inSampleSize, long max) {
        long result = Math.round(value * inSampleSize);
        if(result <= 0) return 0;
        if(result >= max) return max;
        return result;
    }

    private static long fromSourceCoords(double value, int inSampleSize, long max) {
        long result = Math.round(value / inSampleSize);
        if(result <= 0) return 0;
        if(result >= max) return max;
        return result;
    }

    private static Measure findMeasureFor(String uuidZone, ArrayList<Movement> movements) {
        for(Movement movement : movements) {
            for (Measure measure : movement.measures) {
                if(measure.zone.zoneUuid.equals(uuidZone)) {
                    return measure;
                }
            }
        }
        return null;
    }
}
