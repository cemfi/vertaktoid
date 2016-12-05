package zemfi.de.vertaktoid;

import java.io.Serializable;
import java.util.ArrayList;

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

    /*public boolean isContained(Box box) {
        if(box.top > bottom || box.bottom < top) return false;
        if(box.top < top && box.bottom > bottom) return true;
        float boxHeight = box.bottom - box.top;
        float intersectionHeight = bottom - box.top;
        System.out.println(intersectionHeight / boxHeight > 0.5f);
        return intersectionHeight / boxHeight > 0.5f;
    }*/

    public void recalcSize(){
        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for(int i = 0; i < boxes.size(); i++) {
            if(minY > boxes.get(i).top) {
                minY = boxes.get(i).top;
            }
            if(maxY < boxes.get(i).bottom) {
                maxY = boxes.get(i).bottom;
            }
        }

        top = minY;
        bottom = maxY;
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