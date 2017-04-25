package zemfi.de.vertaktoid.helpers;

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

    public static Point2D lineIntersectSegment(Point2D sp1, Point2D sp2, Point2D lp1, Point2D lp2) {
        Point2D intersection = linesIntersection(sp1, sp2, lp1, lp2);
        if((Math.min(sp1.x(), sp2.x()) <= intersection.x() &&  Math.max(sp1.x(), sp2.x()) >= intersection.x()) &&
                (Math.min(sp1.y(), sp2.y()) <= intersection.y() &&  Math.max(sp1.y(), sp2.y()) >= intersection.y())) {
            return intersection;
        }
        return null;
    }

    public static Point2D segmentsIntersection(Point2D s1p1, Point2D s1p2, Point2D s2p1, Point2D s2p2) {
        Point2D intersection = linesIntersection(s1p1, s1p2, s2p1, s2p2);
        if(intersection == null) {
            return null;
        }
        if((Math.min(s1p1.x(), s1p2.x()) <= intersection.x() &&  Math.max(s1p1.x(), s1p2.x()) >= intersection.x()) &&
                (Math.min(s2p1.x(), s2p2.x()) <= intersection.x() &&  Math.max(s2p1.x(), s2p2.x()) >= intersection.x()) &&
                (Math.min(s1p1.y(), s1p2.y()) <= intersection.y() &&  Math.max(s1p1.y(), s1p2.y()) >= intersection.y()) &&
                (Math.min(s2p1.y(), s2p2.y()) <= intersection.y() &&  Math.max(s2p1.y(), s2p2.y()) >= intersection.y())) {
            return intersection;
        }
        return null;
    }

    public static Point2D linesIntersection(Point2D l1p1, Point2D l1p2, Point2D l2p1, Point2D l2p2) {
        if(l1p1.x() == l1p2.x() || l2p1.x() == l2p2.x()) {
            if(l1p1.x() == l1p2.x() && l2p1.x() == l2p2.x()) {
                if (l1p1.x() != l2p1.x()) {
                    return null;
                } else {
                    if(Math.min(l1p1.y(), l1p2.y()) > Math.max(l2p1.y(), l2p2.y()) ||
                            Math.max(l1p1.y(), l1p2.y()) < Math.min(l2p1.y(), l2p2.y())) {
                        return null;
                    } else {
                        if(Math.min(l2p1.y(), l2p2.y()) <= Math.min(l1p1.y(), l1p2.y())) {
                            return new Point2D(l1p1.x(), Math.min(l1p1.y(), l1p2.y()));
                        } else {
                            return new Point2D(l1p1.x(), Math.min(l2p1.y(), l2p2.y()));
                        }
                    }
                }
            } else {
                if(l1p1.x() == l1p2.x()) {
                    double a2 = (l2p2.y() - l2p1.y()) / (l2p2.x() - l2p1.x());
                    double b2 = l2p1.y() - a2 * l2p1.x();
                    double interX = l1p1.x();
                    double interY = interX * a2 + b2;
                    return new Point2D(interX, interY);

                } else {
                    double a1 = (l1p2.y() - l1p1.y()) / (l1p2.x() - l1p1.x());
                    double b1 = l1p1.y() - a1 * l1p1.x();
                    double interX = l2p1.x();
                    double interY = interX * a1 + b1;
                    return new Point2D(interX, interY);
                }
            }
        }

        double a1 = (l1p2.y() - l1p1.y()) / (l1p2.x() - l1p1.x());
        double b1 = l1p1.y() - a1 * l1p1.x();
        double a2 = (l2p2.y() - l2p1.y()) / (l2p2.x() - l2p1.x());
        double b2 = l2p1.y() - a2 * l2p1.x();

        if(a1 == a2) {
            if(b1 == b2) {
                if(Math.min(l1p1.x(), l1p2.x()) > Math.max(l2p1.x(), l2p2.x()) ||
                        Math.max(l1p1.x(), l1p2.x()) < Math.min(l2p1.x(), l2p2.x())) {
                    return null;
                } else {
                    if(Math.min(l2p1.x(), l2p2.x()) <= Math.min(l1p1.x(), l1p2.x())) {
                        return new Point2D(Math.min(l1p1.x(), l1p2.x()), l1p1.y());
                    } else {
                        return new Point2D(Math.min(l2p1.x(), l2p2.x()), l1p1.y());
                    }
                }
            } else {
                return null;
            }
        }
        double interX = (b2 - b1) / (a1 - a2);
        double interY = interX * a1 + b1;
        return new Point2D(interX, interY);
    }

    public static boolean segmentIntersectsPolygon(List<Point2D> vertices, Point2D first, Point2D second) {
        for(int i = 1; i < vertices.size(); i++) {
            if(segmentsIntersection(vertices.get(i-1), vertices.get(i), first, second) != null) {
                return true;
            }
        }
        if(segmentsIntersection(vertices.get(vertices.size() - 1), vertices.get(0), first, second) != null) {
            return true;
        }
        return false;
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
