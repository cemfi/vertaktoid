package zemfi.de.vertaktoid;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by yevgen on 16.12.2016.
 */

public class Movement implements Serializable {
    ArrayList<Measure> measures;
    int number;
    String label = "";

    Movement() {
        measures = new ArrayList<>();
    }

    void sortMeasures() {
        Collections.sort(measures, Measure.MEASURE_POSITION_COMPARATOR);
    }

    void calculateSequenceNumbers() {
        if(measures.size() == 0) return;
        Measure measure;
        int num = 1;
        for (int i = 0; i < measures.size(); i++) {
            measure = measures.get(i);
            if(i > 0) {
                num = measures.get(i-1).sequenceNumber + measures.get(i-1).repeat + 1;
            }
            if(measure.manualSequenceNumber != null) {
                try {
                    String modified = measure.manualSequenceNumber.replaceAll("[\\D]", "");
                    modified.trim();
                    String mnumStr = modified.split(" ")[0];
                    int mnum = Integer.parseInt(mnumStr);
                    if (String.valueOf(mnum).equals(measure.manualSequenceNumber) && mnum == num) {
                        measure.manualSequenceNumber = null;
                    }
                    measure.sequenceNumber = mnum;
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

    void removeMeasure(Measure measure) {
        measures.remove(measure);
        calculateSequenceNumbers();
    }

    void removeMeasures(ArrayList<Measure> measures) {
        int index = measures.size();
        for (Measure measure : measures) {
            measures.remove(measure);

        }
        calculateSequenceNumbers();
    }

    String getName() {
        if(!label.equals("")) return label;
        else return "Movement " + number;
    }
}
