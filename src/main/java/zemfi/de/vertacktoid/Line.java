package zemfi.de.vertacktoid;

import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by aristotelis on 01.08.16.
 */
public class Line implements Serializable {
    public float top = Float.POSITIVE_INFINITY;
    public float bottom = Float.NEGATIVE_INFINITY;
    public ArrayList<Box> boxes = new ArrayList<Box>();

    public void addBox(Box box) {
        if (box.top < this.top) {
            this.top = box.top;
        }
        if (box.bottom > this.bottom) {
            this.bottom = box.bottom;
        }

        if (boxes.size() == 0) {
            // create first entry
            boxes.add(box);
        }
        else {
            // one or more boxes already exist
            int j;
            int index = -1;
            for (j = 0; j < boxes.size(); j++) {
                if (box.left < boxes.get(j).left) {
                    index = j;
                    break;
                }
                }
                if (index == -1) {
                    index = boxes.size();
                }
                boxes.add(index, box);
        }
    }

    public boolean isContained(Box box) {
        // returns true if the smaller element (box or line) is contained at least 50% in the larger element

        float smallerTop;
        float smallerBottom;
        float largerTop;
        float largerBottom;

        if (box.bottom - box.top < bottom - top) {
            // box is smaller than line
            smallerTop = box.top;
            smallerBottom = box.bottom;
            largerTop = top;
            largerBottom = bottom;
        }
        else {
            smallerTop = top;
            smallerBottom = bottom;
            largerTop = box.top;
            largerBottom = box.bottom;
        }

        float startIntersect = smallerTop > largerTop ? smallerTop : largerTop;
        float stopIntersect = smallerBottom < largerBottom ? smallerBottom : largerBottom;
        float areaIntersect = stopIntersect - startIntersect;
        areaIntersect /= smallerBottom - smallerTop;
        return areaIntersect > 0.5f;
    }

    public void stripManualSequenceNumbers() {
        int i;
        for (i = 0; i < boxes.size(); i++) {
            try {
                Box box = boxes.get(i);
                int x = Integer.parseInt(box.manualSequenceNumber);
                if (x == box.sequenceNumber) {
                    box.manualSequenceNumber = null;
                }
            }
            catch (Exception e) {
                // int parsing failed => do not remove manual sequence number
            }
        }
    }
}