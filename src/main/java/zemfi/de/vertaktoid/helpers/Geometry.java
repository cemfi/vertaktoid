package zemfi.de.vertaktoid.helpers;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

public class Geometry {
    public enum Orientation {HORIZONTAL, VERTICAL}

    public static List<Point2D[]> orientedSegments(List<Point2D> vertices, Orientation orientation) {
        List<Point2D[]> segments = new ArrayList<>();
        double localDelta = 0.0;
        for(int i = 0; i < vertices.size() - 1; i++) {
            Point2D p1 = vertices.get(i);
            Point2D p2 = vertices.get(i+1);
            localDelta = Math.abs(p2.x() - p1.x()) / Math.abs(p2.y() - p1.y());
            if(localDelta < 1 && orientation == Orientation.VERTICAL ||
                    localDelta >= 1 && orientation == Orientation.HORIZONTAL) {
                Point2D[] segment = new Point2D[2];
                segment[0] = p1;
                segment[1] = p2;
                segments.add(segment);
            }
        }
        Point2D p1 = vertices.get(vertices.size()-1);
        Point2D p2 = vertices.get(0);
        localDelta = Math.abs(p2.x() - p1.x()) / Math.abs(p2.y() - p1.y());
        if(localDelta < 1 && orientation == Orientation.VERTICAL ||
                localDelta >= 1 && orientation == Orientation.HORIZONTAL) {
            Point2D[] segment = new Point2D[2];
            segment[0] = p1;
            segment[1] = p2;
            segments.add(segment);
        }
        return segments;
    }



    public static Point2D projectionPointToSegment(Point2D[] segment, Point2D point) {
        double k = ((segment[1].y()-segment[0].y()) * (point.x()-segment[0].x()) -
                (segment[1].x()-segment[0].x()) * (point.y()-segment[0].y())) /
                (Math.pow(segment[1].y()-segment[0].y(), 2) + Math.pow(segment[1].x()-segment[0].x(), 2));
        return new Point2D(point.x() - k * (segment[1].y() - segment[0].y()),
                point.y() + k * (segment[1].x() - segment[0].x()));
    }

    public static boolean polygonContainsPoint(List<Point2D> vertices, Point2D point) {
        int i;
        int j;
        boolean result = false;
        for (i = 0, j = vertices.size() - 1; i < vertices.size(); j = i++) {
            if ((vertices.get(i).y() > point.y()) != (vertices.get(j).y() > point.y()) &&
                    (point.x() < (vertices.get(j).x() - vertices.get(i).x()) * (point.y() - vertices.get(i).y()) /
                            (vertices.get(j).y()-vertices.get(i).y()) + vertices.get(i).x())) {
                result = !result;
            }
        }
        return result;
    }

    public static boolean polygonContainsSegment(List<Point2D> vertices, Point2D first, Point2D second) {
        int i;
        int j;
        boolean includesFirst = false;
        boolean includesSecond = false;
        for (i = 0, j = vertices.size() - 1; i < vertices.size(); j = i++) {
            if ((vertices.get(i).y() > first.y()) != (vertices.get(j).y() > first.y()) &&
                    (first.x() < (vertices.get(j).x() - vertices.get(i).x()) * (first.y() - vertices.get(i).y()) /
                            (vertices.get(j).y()-vertices.get(i).y()) + vertices.get(i).x())) {
                includesFirst = !includesFirst;
            }
            if ((vertices.get(i).y() > second.y()) != (vertices.get(j).y() > second.y()) &&
                    (second.x() < (vertices.get(j).x() - vertices.get(i).x()) * (second.y() - vertices.get(i).y()) /
                            (vertices.get(j).y()-vertices.get(i).y()) + vertices.get(i).x())) {
                includesSecond = !includesSecond;
            }
        }
        return includesFirst && includesSecond;
    }

    /**
     * Calculate centroid of 2D non crossing polygon, To accommodate that points
     * are correct using Gift wrapping algorithm(Finding Convex Hull)
     *
     * @ref http://en.wikipedia.org/wiki/Centroid#Centroid_of_polygon
     * @param vertices list of vertices
     * @return centroid
     */
    public static Point2D centroid2D(final List<Point2D> vertices)
    {
        if (vertices == null)
            return new Point2D(0f, 0f);

        List<Point2D> hull = null;
        if (vertices.size() < 2)
            hull = new ArrayList<Point2D>(vertices);
        else {
            GrahamScan grahamScan = new GrahamScan(vertices);
            hull = grahamScan.hull();
        }


        // Now we can calculate the centroid of polygon using standard mean
        final int len = hull.size();
        final double xy[] = new double[] { 0, 0 };
        for (int i = 0; i < len; ++i)
        {
            final Point2D p = hull.get(i);
            xy[0] += p.x();
            xy[1] += p.y();
        }

        final double x = (xy[0] / len);
        final double y = (xy[1] / len);

        return new Point2D(x, y);
    }
}
