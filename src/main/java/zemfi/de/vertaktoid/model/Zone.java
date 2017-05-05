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
import zemfi.de.vertaktoid.helpers.Point2D;

public class Zone implements Comparable<Zone>, Parcelable {
    public String zoneUuid = null;
    private Facsimile.AnnotationType annotationType;
    private double boundLeft = Double.MAX_VALUE;
    private double boundRight = Double.MIN_VALUE;
    private double boundTop = Double.MIN_VALUE;
    private double boundBottom = Double.MAX_VALUE;
    private List<Point2D> vertices;

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
        boundLeft = in.readDouble();
        boundRight = in.readDouble();
        boundTop = in.readDouble();
        boundBottom = in.readDouble();
        vertices = in.createTypedArrayList(Point2D.CREATOR);
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

    public List<Point2D> getVertices() {
        return vertices;
    }

    public void setVertices(List<Point2D> vertices) {
        this.vertices = vertices;
        calculateBoundingBox();
    }

    public void convertToOrthogonalBox() {
        vertices.clear();
        vertices.add(new Point2D(boundLeft, boundTop));
        vertices.add(new Point2D(boundLeft, boundBottom));
        vertices.add(new Point2D(boundRight, boundBottom));
        vertices.add(new Point2D(boundRight, boundTop));
    }

    public void convertToOrientedBox() {
        convertToPolygon();
        Point2D[] minBoundingBox = RotatingCalipers.getMinimumBoundingRectangle(vertices);
        vertices = new ArrayList<Point2D>(Arrays.asList(minBoundingBox));
    }

    public void convertToPolygon() {
        Point2D[] verticesArray = new Point2D[vertices.size()];
        for(int i = 0; i < vertices.size(); i++) {
            verticesArray[i] = vertices.get(i);
        }
        Simplify<Point2D> simplify = new Simplify<Point2D>(new Point2D[0],
                new PointExtractor<Point2D>() {
                    @Override
                    public double getX(Point2D point) {
                        return point.x();
                    }

                    @Override
                    public double getY(Point2D point) {
                        return point.y();
                    }
                });

        Point2D[] simplifiedVertices = simplify.simplify(verticesArray, 10f, true);
        vertices = new ArrayList<Point2D>(Arrays.asList(simplifiedVertices));
    }

    public double getBoundLeft() {
        return boundLeft;
    }

    public double getBoundRight() {
        return boundRight;
    }

    public double getBoundTop() {
        return boundTop;
    }

    public double getBoundBottom() {
        return boundBottom;
    }

    private void calculateBoundingBox() {
        boundLeft = Float.MAX_VALUE;
        boundRight = Float.MIN_VALUE;
        boundTop = Float.MAX_VALUE;
        boundBottom = Float.MIN_VALUE;
        for(Point2D vertex : vertices) {
            if(boundLeft > vertex.x()) {
                boundLeft = vertex.x();
            }
            if(boundRight < vertex.x()) {
                boundRight = vertex.x();
            }
            if(boundTop > vertex.y()) {
                boundTop = vertex.y();
            }
            if(boundBottom < vertex.y()) {
                boundBottom = vertex.y();
            }
        }
    }

    @Override
    public int compareTo(Zone zone) {
        int result = 0;
        if (this.boundLeft == zone.boundLeft && this.boundTop == zone.boundTop) {
            return 0;
        }
        if (this.boundTop < zone.boundTop && this.boundLeft < zone.boundLeft) {
            return -1;
        }
        if (this.boundTop > zone.boundTop && this.boundLeft > zone.boundLeft) {
            return 1;
        }
        if(Math.min(this.boundBottom - this.boundTop, zone.boundBottom - zone.boundTop) == 0) {
            result = (int) (zone.boundLeft - this.boundLeft);
            if(result == 0) {
                result = (int) (zone.boundTop - this.boundTop);
            }
            return result;
        }
        double yIsectFactor = (Math.min(this.boundBottom, zone.boundBottom) - Math.max(this.boundTop, zone.boundTop)) /
                Math.min(this.boundBottom - this.boundTop, zone.boundBottom - zone.boundTop);
        if (yIsectFactor < 0.5) {
            result = (int) (zone.boundLeft - this.boundLeft);
            if(result == 0) {
                result = (int) (this.boundTop - zone.boundTop);
            }
            return result;
        }
        result = (int) (this.boundLeft - zone.boundLeft);
        if(result == 0) {
            result = (int) (this.boundTop - zone.boundTop);
        }
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(zoneUuid);
        parcel.writeDouble(boundLeft);
        parcel.writeDouble(boundRight);
        parcel.writeDouble(boundTop);
        parcel.writeDouble(boundBottom);
        parcel.writeTypedList(vertices);
    }
}
