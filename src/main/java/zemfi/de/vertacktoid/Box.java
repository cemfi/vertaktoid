package zemfi.de.vertacktoid;

import android.graphics.PointF;

import java.io.Serializable;

/**
 * Created by aristotelis on 01.08.16.
 */
public class Box implements Serializable {
    public float left   = 0.0f;
    public float right  = 0.0f;
    public float top    = 0.0f;
    public float bottom = 0.0f;

    public Box(float left, float right, float top, float bottom) {
        assert left <= right;
        assert top <= bottom;
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
    }

    public boolean containsPoint(float x, float y) {
        return x >= left && x <= right && y >= top && y <= bottom;
    }

    public boolean containsLine(float startX, float startY, float endX, float endY) {
        boolean insideY = startY >= top && endY >= top && startY <= bottom && endY <= bottom;
        if (!insideY) {
            return false;
        }
        float smallerX = startX < endX ? startX : endX;
        float largerX = startX > endX ? startX : endX;
        if (largerX < left) {
            return false;
        }
        if (smallerX > right) {
            return false;
        }
        return true;
    }




    public int sequenceNumber = -1;
    public String manualSequenceNumber = null;

    public String toString() {
        return "left: " + left + ", right: " + right + ", top:" + top + ", bottom: " + bottom;
    }

    public String zoneUuid = null;
    public String measureUuid = null;
}

