package zemfi.de.vertaktoid.helpers;

import java.util.ArrayList;
import java.util.List;

public class Geometry {
    public static enum Direction {HORIZONTAL, VERTICAL}

    public static List<Point2D[]> orientedSegments(List<Point2D> vertices, Direction direction) {
        List<Point2D[]> segments = new ArrayList<>();
        double localDelta = 0.0;
        for(int i = 0; i < vertices.size() - 1; i++) {
            Point2D p1 = vertices.get(i);
            Point2D p2 = vertices.get(i+1);
            localDelta = Math.abs(p2.x() - p1.x()) / Math.abs(p2.y() - p1.y());
            if(localDelta < 1 && direction == Direction.VERTICAL ||
                    localDelta >= 1 && direction == Direction.HORIZONTAL) {
                Point2D[] segment = new Point2D[2];
                segment[0] = p1;
                segment[1] = p2;
                segments.add(segment);
            }
        }
        Point2D p1 = vertices.get(vertices.size()-1);
        Point2D p2 = vertices.get(0);
        localDelta = Math.abs(p2.x() - p1.x()) / Math.abs(p2.y() - p1.y());
        if(localDelta < 1 && direction == Direction.VERTICAL ||
                localDelta >= 1 && direction == Direction.HORIZONTAL) {
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

    public static Point2D[] parallelLine(Point2D[] segment, Point2D point) {
        Point2D[] result = new Point2D[2];
        result[0] = point;
        double a = (segment[1].y() - segment[0].y()) / (segment[1].x() - segment[0].x());
        double b1 = segment[1].y() - a * segment[0].x();
        double b2 = point.y() - point.x() * a;
        double x2 = point.x() + 100;
        result[1] = new Point2D(x2, a * x2 + b2);
        return result;
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

    public static Point2D lineIntersectSegment(Point2D[] segment, Point2D[] line) {
        Point2D intersection = linesIntersection(segment, line);
        if((Math.min(segment[0].x(), segment[1].x()) <= intersection.x() &&  Math.max(segment[0].x(), segment[1].x()) >= intersection.x()) &&
                (Math.min(segment[0].y(), segment[1].y()) <= intersection.y() &&  Math.max(segment[0].y(), segment[1].y()) >= intersection.y())) {
            return intersection;
        }
        return null;
    }

    public static Point2D segmentsIntersection(Point2D[] segment1, Point2D[] segment2) {
        Point2D intersection = linesIntersection(segment1, segment2);
        if(intersection == null) {
            return null;
        }
        if((Math.min(segment1[0].x(), segment1[1].x()) <= intersection.x() &&  Math.max(segment1[0].x(), segment1[1].x()) >= intersection.x()) &&
                (Math.min(segment2[0].x(), segment2[1].x()) <= intersection.x() &&  Math.max(segment2[0].x(), segment2[1].x()) >= intersection.x()) &&
                (Math.min(segment1[0].y(), segment1[1].y()) <= intersection.y() &&  Math.max(segment1[0].y(), segment1[1].y()) >= intersection.y()) &&
                (Math.min(segment2[0].y(), segment2[1].y()) <= intersection.y() &&  Math.max(segment2[0].y(), segment2[1].y()) >= intersection.y())) {
            return intersection;
        }
        return null;
    }

    public static Point2D linesIntersection(Point2D[] line1, Point2D[] line2) {
        if(line1[0].x() == line1[1].x() || line2[0].x() == line2[1].x()) {
            if(line1[0].x() == line1[1].x() && line2[0].x() == line2[1].x()) {
                if (line1[0].x() != line2[0].x()) {
                    return null;
                } else {
                    if(Math.min(line1[0].y(), line1[1].y()) > Math.max(line2[0].y(), line2[1].y()) ||
                            Math.max(line1[0].y(), line1[1].y()) < Math.min(line2[0].y(), line2[1].y())) {
                        return null;
                    } else {
                        if(Math.min(line2[0].y(), line2[1].y()) <= Math.min(line1[0].y(), line1[1].y())) {
                            return new Point2D(line1[0].x(), Math.min(line1[0].y(), line1[1].y()));
                        } else {
                            return new Point2D(line1[0].x(), Math.min(line2[0].y(), line2[1].y()));
                        }
                    }
                }
            } else {
                if(line1[0].x() == line1[1].x()) {
                    double a2 = (line2[1].y() - line2[0].y()) / (line2[1].x() - line2[0].x());
                    double b2 = line2[0].y() - a2 * line2[0].x();
                    double interX = line1[0].x();
                    double interY = interX * a2 + b2;
                    return new Point2D(interX, interY);

                } else {
                    double a1 = (line1[1].y() - line1[0].y()) / (line1[1].x() - line1[0].x());
                    double b1 = line1[0].y() - a1 * line1[0].x();
                    double interX = line2[0].x();
                    double interY = interX * a1 + b1;
                    return new Point2D(interX, interY);
                }
            }
        }

        double a1 = (line1[1].y() - line1[0].y()) / (line1[1].x() - line1[0].x());
        double b1 = line1[0].y() - a1 * line1[0].x();
        double a2 = (line2[1].y() - line2[0].y()) / (line2[1].x() - line2[0].x());
        double b2 = line2[0].y() - a2 * line2[0].x();

        if(a1 == a2) {
            if(b1 == b2) {
                if(Math.min(line1[0].x(), line1[1].x()) > Math.max(line2[0].x(), line2[1].x()) ||
                        Math.max(line1[0].x(), line1[1].x()) < Math.min(line2[0].x(), line2[1].x())) {
                    return null;
                } else {
                    if(Math.min(line2[0].x(), line2[1].x()) <= Math.min(line1[0].x(), line1[1].x())) {
                        return new Point2D(Math.min(line1[0].x(), line1[1].x()), line1[0].y());
                    } else {
                        return new Point2D(Math.min(line2[0].x(), line2[1].x()), line1[0].y());
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

    public static boolean segmentIntersectsPolygon(List<Point2D> vertices, Point2D[] segment) {
        Point2D[] side;
        for(int i = 1; i < vertices.size(); i++) {
            side = new Point2D[]{vertices.get(i-1), vertices.get(i)};
            if(segmentsIntersection(side, segment) != null) {
                return true;
            }
        }
        side = new Point2D[]{vertices.get(vertices.size() - 1), vertices.get(0)};
        if(segmentsIntersection(side, segment) != null) {
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
