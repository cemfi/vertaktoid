package zemfi.de.vertaktoid.model;


import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;

import com.goebl.simplify.PointExtractor;
import com.goebl.simplify.Simplify;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import zemfi.de.vertaktoid.helpers.RotatingCalipers;

public class Zone implements Comparable<Zone>, Parcelable {
    public String zoneUuid = null;
    private Facsimile.AnnotationType annotationType;
    private float boundLeft = Float.MAX_VALUE;
    private float boundRight = Float.MIN_VALUE;
    private float boundTop = Float.MIN_VALUE;
    private float boundBottom = Float.MAX_VALUE;
    private List<PointF> vertices;

    public Zone() {
        vertices = new ArrayList<>();
        annotationType = Facsimile.AnnotationType.ORTHOGONAL_BOX;
    }

    public Zone(Facsimile.AnnotationType type) {
        vertices = new ArrayList<>();
        annotationType = type;
    }

    protected Zone(Parcel in) {
        zoneUuid = in.readString();
        boundLeft = in.readFloat();
        boundRight = in.readFloat();
        boundTop = in.readFloat();
        boundBottom = in.readFloat();
        vertices = in.createTypedArrayList(PointF.CREATOR);
    }

    public static final Creator<Zone> CREATOR = new Creator<Zone>() {
        @Override
        public Zone createFromParcel(Parcel in) {
            return new Zone(in);
        }

        @Override
        public Zone[] newArray(int size) {
            return new Zone[size];
        }
    };

    public Facsimile.AnnotationType getAnnotationType() {
        return annotationType;
    }

    public void setAnnotationType(Facsimile.AnnotationType annotationType) {
        this.annotationType = annotationType;
    }

    public List<PointF> getVertices() {
        return vertices;
    }

    public void setVertices(List<PointF> vertices) {
        this.vertices = vertices;
        calculateBoundingBox();
    }

    public void convertToOrthogonalBox() {
        vertices.clear();
        vertices.add(new PointF(boundLeft, boundTop));
        vertices.add(new PointF(boundLeft, boundBottom));
        vertices.add(new PointF(boundRight, boundBottom));
        vertices.add(new PointF(boundRight, boundTop));
    }

    public void convertToOrientedBox() {
        convertToPolygon();
        PointF[] minBoundingBox = RotatingCalipers.getMinimumBoundingRectangle(vertices);
        vertices = new ArrayList<PointF>(Arrays.asList(minBoundingBox));
    }

    public void convertToPolygon() {
        PointF[] verticesArray = new PointF[vertices.size()];
        for(int i = 0; i < vertices.size(); i++) {
            verticesArray[i] = vertices.get(i);
        }
        Simplify<PointF> simplify = new Simplify<PointF>(new PointF[0],
                new PointExtractor<PointF>() {
                    @Override
                    public double getX(PointF point) {
                        return point.x;
                    }

                    @Override
                    public double getY(PointF point) {
                        return point.y;
                    }
                });

        PointF[] simplifiedVertices = simplify.simplify((PointF[]) verticesArray, 30f, true);
        vertices = new ArrayList<PointF>(Arrays.asList(simplifiedVertices));
    }

    public float getBoundLeft() {
        return boundLeft;
    }

    public float getBoundRight() {
        return boundRight;
    }

    public float getBoundTop() {
        return boundTop;
    }

    public float getBoundBottom() {
        return boundBottom;
    }

    private void calculateBoundingBox() {
        boundLeft = Float.MAX_VALUE;
        boundRight = Float.MIN_VALUE;
        boundTop = Float.MAX_VALUE;
        boundBottom = Float.MIN_VALUE;
        for(PointF vertex : vertices) {
            if(boundLeft > vertex.x) {
                boundLeft = vertex.x;
            }
            if(boundRight < vertex.x) {
                boundRight = vertex.x;
            }
            if(boundTop > vertex.y) {
                boundTop = vertex.y;
            }
            if(boundBottom < vertex.y) {
                boundBottom = vertex.y;
            }
        }
    }

    /**
     * Checks if the measure contains a point.
     * @param x The x coordinate of the giving point.
     * @param y The y coordinate of the giving point.
     * @return true if the point inside the measure is.
     */
    public boolean containsPoint(float x, float y) {
        return x >= boundLeft && x <= boundRight && y >= boundTop && y <= boundBottom;
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
        boolean insideY = p1y >= boundTop && p2y >= boundTop && p1y <= boundBottom && p2y <= boundBottom;
        if (!insideY) {
            return false;
        }
        float smallerX = Math.min(p1x, p2x);
        float largerX = Math.max(p1x, p2x);
        return largerX > boundLeft && smallerX < boundRight;
    }

    @Override
    public int compareTo(Zone zone) {
        if (this.boundLeft == zone.boundLeft && this.boundTop == zone.boundTop) {
            return 0;
        }
        if (this.boundTop < zone.boundTop && this.boundLeft < zone.boundLeft) {
            return -1;
        }
        if (this.boundTop > zone.boundTop && this.boundLeft > zone.boundLeft) {
            return 1;
        }
        float yIsectFactor = (Math.min(this.boundBottom, zone.boundBottom) - Math.max(this.boundTop, zone.boundTop)) /
                Math.min(this.boundBottom - this.boundTop, zone.boundBottom - zone.boundTop);
        if (yIsectFactor < 0.5) {
            return (int) (zone.boundLeft - this.boundLeft);
        }
        return (int) (this.boundLeft - zone.boundLeft);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(zoneUuid);
        parcel.writeFloat(boundLeft);
        parcel.writeFloat(boundRight);
        parcel.writeFloat(boundTop);
        parcel.writeFloat(boundBottom);
        parcel.writeTypedList(vertices);
    }
}
