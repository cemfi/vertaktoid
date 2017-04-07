package zemfi.de.vertaktoid.model;


import android.graphics.PointF;
import com.goebl.simplify.PointExtractor;
import com.goebl.simplify.Simplify;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import zemfi.de.vertaktoid.helpers.RotatingCalipers;

public class Zone implements Comparable<Zone>, Serializable {
    public String zoneUuid = null;
    private float boundLeft = Float.MAX_VALUE;
    private float boundRight = Float.MIN_VALUE;
    private float boundTop = Float.MIN_VALUE;
    private float boundBottom = Float.MAX_VALUE;
    private List<float[]> vertices;

    public Zone() {
        vertices = new ArrayList<>();
    }

    public List<float[]> getVertices() {
        return vertices;
    }

    public void setVertices(List<float[]> vertices) {
        this.vertices = vertices;
        calculateBoundingBox();
    }

    public void convertToCanonical() {
        vertices.clear();
        vertices.add(new float[]{boundLeft, boundTop});
        vertices.add(new float[]{boundLeft, boundBottom});
        vertices.add(new float[]{boundRight, boundBottom});
        vertices.add(new float[]{boundRight, boundTop});
    }

    public void convertToExtended() {
        convertToPolygonal();
        List<PointF> verticesPF = new ArrayList<>();
        for(float[] vertex: vertices) {
            verticesPF.add(new PointF(vertex[0], vertex[1]));
        }
        PointF[] minBoundingBox = RotatingCalipers.getMinimumBoundingRectangle(verticesPF);
        vertices.clear();
        for(PointF point: minBoundingBox) {
            vertices.add(new float[]{point.x, point.y});
        }
    }

    public void convertToPolygonal() {
        PointF[] verticesPF = new PointF[vertices.size()];
        for(int i = 0; i < vertices.size(); i++) {
            verticesPF[i] = new PointF(vertices.get(i)[0], vertices.get(i)[1]);
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

        PointF[] simplifiedVertices = simplify.simplify((PointF[]) verticesPF, 20f, false);
        vertices.clear();
        for(PointF vertex: simplifiedVertices) {
            vertices.add(new float[]{vertex.x, vertex.y});
        }
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
        for(float[] vertex : vertices) {
            if(boundLeft > vertex[0]) {
                boundLeft = vertex[0];
            }
            if(boundRight < vertex[0]) {
                boundRight = vertex[0];
            }
            if(boundTop > vertex[1]) {
                boundTop = vertex[1];
            }
            if(boundBottom < vertex[1]) {
                boundBottom = vertex[1];
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
}
