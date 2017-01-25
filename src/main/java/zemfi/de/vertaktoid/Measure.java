package zemfi.de.vertaktoid;

import android.graphics.PointF;

import java.io.Serializable;
import java.util.Comparator;
import java.util.UUID;

/**
 * Created by yevgen on 16.12.2016.
 */

class Measure implements Comparable<Measure>, Serializable {

    int sequenceNumber = -1;
    String manualSequenceNumber = null;
    int rest = 0;
    String zoneUuid = null;
    String measureUuid = null;
    Movement movement;
    Page page;
    float left = 0.0f;
    float right = 0.0f;
    float top = 0.0f;
    float bottom = 0.0f;

    Measure(float left, float top, float right, float bottom) {
        zoneUuid = UUID.randomUUID().toString();
        measureUuid = UUID.randomUUID().toString();
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
    }

    Measure() {
        zoneUuid = UUID.randomUUID().toString();
        measureUuid = UUID.randomUUID().toString();
    }

    boolean containsPoint(float x, float y) {
        return x >= left && x <= right && y >= top && y <= bottom;
    }

    public boolean containsLine(float p1x, float p1y, float p2x, float p2y) {
        boolean insideY = p1y >= top && p2y >= top && p1y <= bottom && p2y <= bottom;
        if (!insideY) {
            return false;
        }
        float smallerX = Math.min(p1x, p2x);
        float largerX = Math.max(p1x, p2x);
        return largerX > left && smallerX < right;
    }

    public void changeMovement(Movement newMovement) {
        if (newMovement == null) {
            return;
        }
        if(this.movement != null) {
            this.movement.removeMeasure(this);
        }
        if(!newMovement.measures.contains(this)) {
            newMovement.measures.add(this);
        }
        this.movement = newMovement;
    }

    static final Comparator<Measure> MEASURE_NUMBER_COMPARATOR = new Comparator<Measure>() {
        @Override
        public int compare(Measure m1, Measure m2) {
            return m1.compareTo(m2);
        }
    };

    static final Comparator<Measure> MEASURE_POSITION_COMPARATOR = new Comparator<Measure>() {
        @Override
        public int compare(Measure m1, Measure m2) {
            if(m1.page.number == m2.page.number) {
                if (m1.left == m2.left && m1.right == m2.right &&
                        m1.top == m2.top && m1.bottom == m2.bottom) {
                    return 0;
                }
                if (m1.top < m2.top && m1.left < m2.left) {
                    return -1;
                }
                if (m1.top > m2.top && m1.left > m2.left) {
                    return 1;
                }
                float yIsectFactor = (Math.min(m1.bottom, m2.bottom) - Math.max(m1.top, m2.top)) /
                        Math.min(m1.bottom - m1.top, m2.bottom - m2.top);
                if (yIsectFactor < 0.5) {
                    return (int) (m2.left - m1.left);
                }
                return (int) (m1.left - m2.left);
            }
            else return m1.page.number - m2.page.number;
        }
    };

    public String getName() {
        if(manualSequenceNumber != null)
            return manualSequenceNumber;
        else return "" + sequenceNumber;
    }

    @Override
    public int compareTo(Measure measure) {
        return this.sequenceNumber - measure.sequenceNumber;

    }
}
