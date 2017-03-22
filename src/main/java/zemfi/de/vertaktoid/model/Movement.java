package zemfi.de.vertaktoid.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import zemfi.de.vertaktoid.Vertaktoid;

/**
 * Represents the movement in musical notation. Movement can be arranged on the multiple facsimile pages.
 * On the one facsimile page can be multiple movements instead.
 */

public class Movement implements Serializable {
    // The contained measures.
    public ArrayList<Measure> measures;
    // Number of movement on the facsimile.
    public int number;
    // Label for movement.
    public String label = "";
    public String mdivUuid;
    /**
     * The constructor.
     */
    public Movement() {
        mdivUuid = Vertaktoid.MEI_MDIV_ID_PREFIX + UUID.randomUUID().toString();
        measures = new ArrayList<>();
    }

    /**
     * Sorts the measures in the movement by their position on the pages.
     */
    public void sortMeasures() {
        Collections.sort(measures, Measure.MEASURE_POSITION_COMPARATOR);
    }

    /**
     * Calculates the sequence numbers of the measures.
     * The algorithm will try to parse the manualSequenceNumbers to obtain a number.
     */
    public void calculateSequenceNumbers() {
        if(measures.size() == 0) return;
        Measure measure;
        int num = 1;
        for (int i = 0; i < measures.size(); i++) {
            measure = measures.get(i);
            // Calculate the next sequence number by default
            if(i > 0) {
                num = measures.get(i-1).sequenceNumber + (measures.get(i-1).rest > 1 ? measures.get(i-1).rest : 1);
            }
            // Try to parse manual sequence number if not null.
            // If no number can be parsed from manual sequence number,
            // then use the next number calculated by default.
            if(measure.manualSequenceNumber != null) {
                try {
                    String modified = measure.manualSequenceNumber.replaceAll("[\\D]", " ");
                    modified = modified.trim();
                    String mnumStrs[] = modified.split(" ");
                    if(mnumStrs.length == 0) {
                        measure.sequenceNumber = num;
                    }
                    else {
                        String mnumStr = mnumStrs[0];
                        int mnum = Integer.parseInt(mnumStr);
                        if (String.valueOf(mnum).equals(measure.manualSequenceNumber) && mnum == num) {
                            measure.manualSequenceNumber = null;
                        }
                        measure.sequenceNumber = mnum;
                    }
                }
                catch (NumberFormatException e) {
                    measure.sequenceNumber = num;
                }
            }
            else {
                measure.sequenceNumber = num;
            }
        }
    }

    /**
     * Removes a measure from the movement.
     * @param measure The measure.
     */
    public void removeMeasure(Measure measure) {
        measures.remove(measure);
        calculateSequenceNumbers();
    }

    /**
     * Removes measures from the movement.
     * @param measures The measures.
     */
    public void removeMeasures(ArrayList<Measure> measures) {
        int index = measures.size();
        for (Measure measure : measures) {
            measures.remove(measure);

        }
        calculateSequenceNumbers();
    }

    /**
     * Gets a string name for the movement. It gets a label or default string created by its number.
     * @return
     */
    public String getName() {
        if(!label.equals("")) return label;
        else return "Movement " + number;
    }
}
