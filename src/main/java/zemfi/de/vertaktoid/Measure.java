package zemfi.de.vertaktoid;

import java.io.Serializable;
import java.util.Comparator;
import java.util.UUID;

/**
 * Represents the measure in music notation.
 * Contains the coordinates for graphical presentation and semantic properties.
 */

class Measure implements Comparable<Measure>, Serializable {

    // Automatically calculated sequence number of measure.
    int sequenceNumber = -1;
    // Manually created name of measure. Is a string.
    // If the string contains number, the humber will be used as sequence number.
    String manualSequenceNumber = null;
    // Rest value (musical pause).
    int rest = 0;
    // Id of referenced zone element in MEI.
    String zoneUuid = null;
    // Id of referenced measure element in MEI.
    String measureUuid = null;
    // Reference to the parent movement.
    Movement movement;
    // Reference to the parent page.
    Page page;
    // Coordinates for representing rectangle
    float left = 0.0f;
    float right = 0.0f;
    float top = 0.0f;
    float bottom = 0.0f;

    /**
     * The constructor.
     * @param left The start x coordinate.
     * @param top The start y coordinate.
     * @param right The end x coordinate.
     * @param bottom The end y coordinate.
     */
    Measure(float left, float top, float right, float bottom) {
        zoneUuid = Vertaktoid.MEI_ZONE_ID_PREFIX + UUID.randomUUID().toString();
        measureUuid = Vertaktoid.MEI_MEASURE_ID_PREFIX + UUID.randomUUID().toString();
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
    }

    /**
     * The constructor.
     */
    Measure() {
        zoneUuid = Vertaktoid.MEI_ZONE_ID_PREFIX +  UUID.randomUUID().toString();
        measureUuid = Vertaktoid.MEI_MEASURE_ID_PREFIX + UUID.randomUUID().toString();
    }

    /**
     * Checks if the measure contains a point.
     * @param x The x coordinate of the giving point.
     * @param y The y coordinate of the giving point.
     * @return true if the point inside the measure is.
     */
    boolean containsPoint(float x, float y) {
        return x >= left && x <= right && y >= top && y <= bottom;
    }

    /**
     * Checks if the measure contains a segment defined with two points.
     * @param p1x The x coordinate of point 1.
     * @param p1y The y coordinate of point 1.
     * @param p2x the x coordinate of point 2.
     * @param p2y The y coordinate of point 2.
     * @return true if the segment inside the measure is.
     */
    public boolean containsSegment(float p1x, float p1y, float p2x, float p2y) {
        boolean insideY = p1y >= top && p2y >= top && p1y <= bottom && p2y <= bottom;
        if (!insideY) {
            return false;
        }
        float smallerX = Math.min(p1x, p2x);
        float largerX = Math.max(p1x, p2x);
        return largerX > left && smallerX < right;
    }

    /**
     * Change the parent movement to another. Removes the measure from old movement and adds it to new.
     * The references will be adjusted.
     * @param newMovement new movement.
     */
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

    /**
     * Compare the measures by their sequence number.
     */
    static final Comparator<Measure> MEASURE_NUMBER_COMPARATOR = new Comparator<Measure>() {
        @Override
        public int compare(Measure m1, Measure m2) {
            return m1.compareTo(m2);
        }
    };

    /**
     * Compare the measures by their position at the facsimile.
     */
    static final Comparator<Measure> MEASURE_POSITION_COMPARATOR = new Comparator<Measure>() {
        @Override
        public int compare(Measure m1, Measure m2) {
            if(m1.page.number == m2.page.number) {
                if (m1.left == m2.left && m1.top == m2.top) {
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

    /**
     * Gets the name of measure. It means the manualSequenceNumber if exists and the sequenceNumber else.
     * @return
     */
    public String getName() {
        if(manualSequenceNumber != null)
            return manualSequenceNumber;
        else return "" + sequenceNumber;
    }

    /**
     * Compare the measures by their sequence number by default.
     * @param measure
     * @return
     */
    @Override
    public int compareTo(Measure measure) {
        return this.sequenceNumber - measure.sequenceNumber;

    }
}
