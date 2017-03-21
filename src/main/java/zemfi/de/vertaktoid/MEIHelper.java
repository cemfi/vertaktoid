package zemfi.de.vertaktoid;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;

import nu.xom.*;

/**
 * MEI input\output routines.
 */

class MEIHelper {

    static Document meiDocument;

    static void clearDocument() {
        meiDocument = new Document(new Element("mei", Vertaktoid.MEI_NS));
    }

    static Element findElementByUiid(Elements elements, String uuid) {
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
    static boolean writeMEI(File meiFile, Facsimile document) {
        boolean returnValue = true;
        document.calculateBreaks();
        if(meiDocument == null) {
            meiDocument = new Document(new Element("mei", Vertaktoid.MEI_NS));
        }
        Element meiElement = meiDocument.getRootElement();

        Element meiHead = meiElement.getFirstChildElement("meiHead", Vertaktoid.MEI_NS);
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
            a = new Attribute("target", page.imageFile.getName());
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
                Element zone = findElementByUiid(zones, measure.zoneUuid);
                existingZones.add(zone);
                if(zone == null) {
                    zone = new Element("zone", Vertaktoid.MEI_NS);
                    surface.appendChild(zone);
                }
                a = new Attribute("id", measure.zoneUuid);
                a.setNamespace("xml", "http://www.w3.org/XML/1998/namespace"); // set its namespace to xml
                zone.addAttribute(a);
                a = new Attribute("type", "measure");
                zone.addAttribute(a);
                a = new Attribute("ulx", "" + normalize(measure.left, page.imageWidth));
                zone.addAttribute(a);
                a = new Attribute("uly", "" + normalize(measure.top, page.imageHeight));
                zone.addAttribute(a);
                a = new Attribute("lrx", "" + normalize(measure.right, page.imageWidth));
                zone.addAttribute(a);
                a = new Attribute("lry", "" + normalize(measure.bottom, page.imageHeight));
                zone.addAttribute(a);

            }

            for(int j = 0; j < zones.size(); j++) {
                if(!existingZones.contains(zones.get(j))) {
                    surface.removeChild(zones.get(j));
                }
            }
        }

        ArrayList<Element> exitstingMdivs = new ArrayList<>();
        for(Movement movement : document.movements) {
            Element mdiv = findElementByUiid(mdivs, movement.mdivUuid);
            exitstingMdivs.add(mdiv);
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
            for(Measure measure : movement.measures) {
                Element measureElem = findElementByUiid(measureElems, measure.measureUuid);
                if(measureElem == null) {
                    measureElem = new Element("measure", Vertaktoid.MEI_NS);
                }
                corrMeasureElems.add(new MeasureElementPair(measure, measureElem));

                a = new Attribute("n", measure.manualSequenceNumber == null ?
                        String.valueOf(measure.sequenceNumber) : measure.manualSequenceNumber);
                measureElem.addAttribute(a);
                a = new Attribute("id", measure.measureUuid);
                a.setNamespace("xml", "http://www.w3.org/XML/1998/namespace");
                measureElem.addAttribute(a);
                a = new Attribute("facs", "#" + measure.zoneUuid);
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
                } else
                if(measure.lastAtSystem) {
                    Element sb = new Element("sb", Vertaktoid.MEI_NS);
                    section.insertChild(sb, section.indexOf(measureElem) + 1);
                }
            }
        }

        for(int i = 0; i < mdivs.size(); i++) {
            if(!exitstingMdivs.contains(mdivs.get(i))) {
                body.removeChild(mdivs.get(i));
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

    /**
     * Reads the data from MEI file.
     * @param meiFile The MEI file.
     * @param document The facsimile.
     * @return true if properly readed.
     */
    static boolean readMEI(File meiFile, Facsimile document) {
        File dir = meiFile.getParentFile();
        Attribute a;

        if(!meiFile.exists()) {
            return false;
        }

        Builder builder = new Builder(false);
        meiDocument = new Document(new Element("mei", Vertaktoid.MEI_NS));
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
                    String name = element.getAttributeValue("n");
                    Measure measure = new Measure();
                    measure.manualSequenceNumber = name;
                    a = element.getAttribute("facs");
                    if(a != null) {
                        measure.zoneUuid = element.getAttributeValue("facs");
                        if (measure.zoneUuid.startsWith("#")) {
                            measure.zoneUuid = measure.zoneUuid.substring(1);
                        }
                        if (measure.zoneUuid.substring(0, 1).matches("\\d")) {
                            measure.zoneUuid = Vertaktoid.MEI_ZONE_ID_PREFIX + measure.zoneUuid;
                            a.setValue("#" + measure.zoneUuid);
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
                float ulx = normalize(Float.parseFloat(zone.getAttributeValue("ulx")), page.imageWidth);
                float uly = normalize(Float.parseFloat(zone.getAttributeValue("uly")), page.imageHeight);
                float lrx = normalize(Float.parseFloat(zone.getAttributeValue("lrx")), page.imageWidth);
                float lry = normalize(Float.parseFloat(zone.getAttributeValue("lry")), page.imageHeight);
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
                    measure.left = ulx;
                    measure.top = uly;
                    measure.right = lrx;
                    measure.bottom = lry;
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

    private static int normalize(float value, int max) {
        if(value <= 0) return 0;
        if(value >= max) return max;
        return Math.round(value);
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
