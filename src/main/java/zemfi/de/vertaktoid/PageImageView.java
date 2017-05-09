package zemfi.de.vertaktoid;

import android.app.Dialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import zemfi.de.vertaktoid.helpers.Geometry;
import zemfi.de.vertaktoid.helpers.HSLColor;
import zemfi.de.vertaktoid.helpers.Point2D;
import zemfi.de.vertaktoid.model.Facsimile;
import zemfi.de.vertaktoid.model.Measure;
import zemfi.de.vertaktoid.model.Movement;
import zemfi.de.vertaktoid.model.Page;
import zemfi.de.vertaktoid.model.Zone;

public class PageImageView extends SubsamplingScaleImageView {

    private final FacsimileView facsimileView;
    private Facsimile facsimile;
    private final Page page;
    private Path boundingPath;
    private Path verticesPath;
    private ArrayList<Point2D> pointPath;
    private Paint selectionPaint;
    private Paint cutLinePaint;
    private Paint drawPaint;
    private Paint largeTextPaint = new Paint();
    private Paint smallTextPaint = new Paint();
    private Rect pageNameRect = new Rect();
    private Rect measureNameRect = new Rect();
    private Rect movementNameRect = new Rect();
    private List<Measure> selectedMeasures = new ArrayList<>();
    private int selectionColor = 0xff404040;
    private int cutLineColor = 0xff0f0f0f;
    HSLColor fillColor;
    private float s = 100f;
    private float l = 30f;
    private float a = 1f;
    private float brushSize = 5;
    Path grayPath;
    Path polygonHoverPath;
    Point2D firstDrawPoint;
    Point2D firstGesturePoint;
    Point2D touchBitmapPosition;
    Point2D lastPolygonPoint;
    Point2D firstCutPoint;
    Point2D lastCutPoint;
    Point2D lastPoint = null; // in bitmap coordinates
    float trackLength;

    /**
     * Constructor
     */
    public PageImageView(PageView pageView, Page page, FacsimileView facsimileView, Facsimile facsimile) {
        super(pageView.getContext());
        this.facsimile = facsimile;
        this.facsimileView = facsimileView;
        this.page = page;
        fillColor = new HSLColor();
        init();
    }

    /**
     * Initialization
     */
    public void init() {
        setImage(findImageForPage());
        this.setMinimumDpi(Vertaktoid.MIN_DPI);
        grayPath = new Path();
        boundingPath = new Path();
        verticesPath = new Path();
        pointPath = new ArrayList<>();
        polygonHoverPath = new Path();
        drawPaint = new Paint();
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(brushSize);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
        selectionPaint = new Paint();
        selectionPaint.setAntiAlias(true);
        selectionPaint.setStrokeWidth(brushSize);
        selectionPaint.setStyle(Paint.Style.STROKE);
        selectionPaint.setStrokeJoin(Paint.Join.ROUND);
        selectionPaint.setStrokeCap(Paint.Cap.ROUND);
        selectionPaint.setColor(selectionColor);
        cutLinePaint = new Paint();
        cutLinePaint.setAntiAlias(true);
        cutLinePaint.setStyle(Paint.Style.STROKE);
        cutLinePaint.setPathEffect(new DashPathEffect(new float[]{10f, 5f}, 0));
        cutLinePaint.setStrokeWidth(brushSize);
        cutLinePaint.setColor(cutLineColor);


        setOnHoverListener(new View.OnHoverListener() {
            @Override
            public boolean onHover(View v, MotionEvent event) {
                if (facsimileView.nextAction == FacsimileView.Action.DRAW ||
                        facsimileView.nextAction == FacsimileView.Action.ORTHOGONAL_CUT ||
                        facsimileView.nextAction == FacsimileView.Action.PRECISE_CUT) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_HOVER_ENTER:
                            if (!facsimileView.isFirstPoint) {
                                polygonHoverPath.reset();
                                PointF lastPoint = pointPath.get(pointPath.size() - 1).getPointF();
                                PointF lastPointTouch = sourceToViewCoord(lastPoint);
                                polygonHoverPath.moveTo(lastPointTouch.x, lastPointTouch.y);
                                polygonHoverPath.lineTo(event.getX(), event.getY());
                            }
                            invalidate();
                            break;
                        case MotionEvent.ACTION_HOVER_MOVE:
                            if (!facsimileView.isFirstPoint) {
                                polygonHoverPath.reset();
                                PointF lastPoint = pointPath.get(pointPath.size() - 1).getPointF();
                                PointF lastPointTouch = sourceToViewCoord(lastPoint);
                                polygonHoverPath.moveTo(lastPointTouch.x, lastPointTouch.y);
                                polygonHoverPath.lineTo(event.getX(), event.getY());
                            }
                            invalidate();
                            break;
                        case MotionEvent.ACTION_HOVER_EXIT:
                            polygonHoverPath.reset();
                            break;
                    }
                }
                return false;
            }

        });
    }

    /**
     * Refreshes the current drawing path
     */
    void refresh() {
        pointPath.clear();
    }

    /**
     * Searches the image file of selected page in the opened directory. Shows dummy page if not found.
     * @return image source
     */
    ImageSource findImageForPage() {
        File appSubFolder = new File(facsimile.dir, Vertaktoid.APP_SUBFOLDER);
        File stubImg = new File(appSubFolder, Vertaktoid.NOT_FOUND_STUBIMG);
        if(page == null) {
            return null;
        }
        if(!page.imageFile.exists()) {
            return ImageSource.uri(Uri.fromFile(stubImg));
        }
        else {
            return page.getImage();
        }
    }

    /**
     * Rendering function
     * @param canvas canvas
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (facsimile == null || facsimile.pages.size() == 0) {
            return;
        }

        largeTextPaint.setColor(Color.BLACK);
        largeTextPaint.setTextSize(50);
        largeTextPaint.setFakeBoldText(true);

        smallTextPaint.setColor(Color.BLACK);
        smallTextPaint.setTextSize(36);
        smallTextPaint.setFakeBoldText(true);

        facsimileView.generateColors();

        if (!page.imageFile.exists()) {
            largeTextPaint.getTextBounds(page.imageFile.getName(), 0,
                    page.imageFile.getName().length(), pageNameRect);
            canvas.drawText("" + page.imageFile.getName(),
                    (this.getRight() - this.getLeft()) / 2  - pageNameRect.centerX(), 100, largeTextPaint);
            return;
        }

        for (int i = 0; i < page.measures.size(); i++) {
            boundingPath.reset();
            verticesPath.reset();
            Measure measure = page.measures.get(i);
            int index = facsimile.movements.indexOf(measure.movement);
            if(index < 0) {
                facsimile.movements.add(0, measure.movement);
            }
            largeTextPaint.setColor(HSLColor.toRGB(facsimileView.movementColors.get(
                    facsimile.movements.indexOf(measure.movement))));
            smallTextPaint.setColor(HSLColor.toRGB(facsimileView.movementColors.get(
                    facsimile.movements.indexOf(measure.movement))));

            PointF topLeft = sourceToViewCoord((float)measure.zone.getBoundLeft(), (float) measure.zone.getBoundTop());
            PointF bottomRight = sourceToViewCoord((float) measure.zone.getBoundRight(), (float) measure.zone.getBoundBottom());

            if(topLeft == null) {
                // still loading image
                return;
            }

            if(measure.zone.getAnnotationType() == Facsimile.AnnotationType.ORTHOGONAL_BOX) {
                if (facsimileView.cornerType == FacsimileView.CornerTypes.ROUNDED) {
                    boundingPath.addRoundRect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y, 15f, 15f, Path.Direction.CW);
                } else if (facsimileView.cornerType == FacsimileView.CornerTypes.STRAIGHT) {
                    boundingPath.addRect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y, Path.Direction.CW);
                }
            }
            List<Point2D> vertices = measure.zone.getVertices();
            final PointF fp = sourceToViewCoord(vertices.get(0).getPointF());
            verticesPath.moveTo(fp.x, fp.y);
            for(int j = 1; j < vertices.size(); j++) {
                final PointF cp = sourceToViewCoord(vertices.get(j).getPointF());
                verticesPath.lineTo(cp.x, cp.y);
            }
            verticesPath.close();
            drawPaint.setStyle(Paint.Style.FILL);
            fillColor.a = 0.1f;
            fillColor.h = facsimileView.movementColors.get(
                    facsimile.movements.indexOf(measure.movement)).h;
            fillColor.s = s;
            fillColor.l = l;
            drawPaint.setColor(HSLColor.toARGB(fillColor));
            if(measure.zone.getAnnotationType() == Facsimile.AnnotationType.ORTHOGONAL_BOX) {
                canvas.drawPath(boundingPath, drawPaint);
            } else {
                canvas.drawPath(verticesPath, drawPaint);
            }
            drawPaint.setStyle(Paint.Style.STROKE);
            drawPaint.setColor(HSLColor.toRGB(facsimileView.movementColors.get(
                    facsimile.movements.indexOf(measure.movement))));
            if(selectedMeasures.contains(measure) &&
                    (facsimileView.nextAction == FacsimileView.Action.ERASE ||
                            facsimileView.nextAction == FacsimileView.Action.ORTHOGONAL_CUT ||
                            facsimileView.nextAction == FacsimileView.Action.PRECISE_CUT)){
                canvas.drawPath(verticesPath, selectionPaint);
            } else {
                canvas.drawPath(verticesPath, drawPaint);
            }

            boundingPath.reset();

            String measureLabel = measure.manualSequenceNumber != null ?
                    "" + measure.manualSequenceNumber : "" + measure.sequenceNumber;

            String movementLabel = measure.movement.getName() + " >>";

            largeTextPaint.getTextBounds(measureLabel, 0, measureLabel.length(), measureNameRect);
            smallTextPaint.getTextBounds(movementLabel, 0, movementLabel.length(), movementNameRect);

            Point2D centroid = Geometry.centroid2D(measure.zone.getVertices());
            PointF centroidF = sourceToViewCoord((float) centroid.x(), (float) centroid.y());
            float leftTextBox = centroidF.x - measureNameRect.width() / 2 - 5;
            float topTextBox = centroidF.y - 20 - measureNameRect.height() /2 ;
            float rightTextBox = centroidF.x + measureNameRect.width() / 2 + 5;
            float bottomTextBox = centroidF.y - 15 + measureNameRect.height() / 2;

            if(measure.manualSequenceNumber != null) {
                canvas.drawRect(leftTextBox, topTextBox, rightTextBox, bottomTextBox, drawPaint);
            }

            canvas.drawText(measureLabel, centroidF.x - measureNameRect.centerX(), centroidF.y, largeTextPaint);
            if(measure.movement.measures.indexOf(measure) == 0) {
                canvas.drawText(movementLabel, centroidF.x - movementNameRect.centerX(),centroidF.y + 30, smallTextPaint);

            }
        }

        verticesPath.reset();
        for (int i = 0; i < pointPath.size(); i++) {
            PointF bitmapCoord = pointPath.get(i).getPointF();
            PointF touchCoord = sourceToViewCoord(bitmapCoord);
            if (i == 0) {
                verticesPath.addCircle(touchCoord.x, touchCoord.y, 10, Path.Direction.CW);
                verticesPath.moveTo(touchCoord.x, touchCoord.y);
            } else {
                verticesPath.lineTo(touchCoord.x, touchCoord.y);
            }
        }
        canvas.drawPath(verticesPath, drawPaint);
        canvas.drawPath(polygonHoverPath, drawPaint);
        if((facsimileView.nextAction == FacsimileView.Action.ORTHOGONAL_CUT ||
                facsimileView.nextAction == FacsimileView.Action.PRECISE_CUT) &&
                firstCutPoint != null && lastCutPoint != null) {
            PointF fcut = sourceToViewCoord(firstCutPoint.getPointF());
            PointF lcut = sourceToViewCoord(lastCutPoint.getPointF());
            canvas.drawLine(fcut.x, fcut.y, lcut.x, lcut.y, cutLinePaint);
        }
    }

    private Point2D[] preciseCutPreview(List<Measure> measures, Point2D[] touchSegment) {
        Point2D[] cutPreview = new Point2D[2];
        List<Point2D> collisions = new ArrayList<>();
        for(Measure measure : measures) {
            List<Point2D> vertices = measure.zone.getVertices();
            for(int i = 0; i < vertices.size(); i++) {
                int j = i+1;
                if(j == vertices.size()) {
                    j = 0;
                }
                Point2D collision = Geometry.lineIntersectSegment(
                        new Point2D[]{vertices.get(i), vertices.get(j)}, touchSegment);
                if(collision != null) {
                    collisions.add(collision);
                }
            }
        }
        Collections.sort(collisions, Point2D.X_ORDER);
        if(touchSegment[0].x() < touchSegment[1].x()) {
            cutPreview[0] = collisions.get(0);
            cutPreview[1] = collisions.get(collisions.size()-1);
        } else {
            cutPreview[1] = collisions.get(0);
            cutPreview[0] = collisions.get(collisions.size()-1);
        }
        return cutPreview;
    }

    private Point2D[] orthogonalCutPreview(List<Measure> measures, Point2D[] touchSegment) {
        Geometry.Direction direction = Geometry.Direction.VERTICAL;
        if(Math.abs(touchSegment[1].x() - touchSegment[0].x()) >
                Math.abs(touchSegment[1].y() - touchSegment[0].y()) &&
                touchSegment[0].distanceTo(touchSegment[1]) >= Vertaktoid.MIN_GESTURE_LENGTH) {
            direction = Geometry.Direction.HORIZONTAL;
        }
        Point2D[] cutPreview = new Point2D[2];
        List<Measure> copy = new ArrayList<>(measures);
        Collections.sort(copy, Measure.MEASURE_POSITION_COMPARATOR);

        if(direction == Geometry.Direction.VERTICAL) {
            List<Point2D[]> horSidesUpperBox = Geometry.orientedSegments(copy.get(0).zone.getVertices(),
                    Geometry.Direction.HORIZONTAL);
            Point2D[] upperSide = new Point2D[2];
            double averageY = Double.MAX_VALUE;
            for(Point2D[] side : horSidesUpperBox) {
                if((side[0].y() + side[1].y()) / 2 < averageY) {
                    averageY = (side[0].y() + side[1].y()) / 2;
                    upperSide = side;
                }
            }
            List<Point2D[]> horSidesLowerBox = Geometry.orientedSegments(copy.get(copy.size()-1).zone.getVertices(),
                    Geometry.Direction.HORIZONTAL);
            Point2D[] lowerSide = new Point2D[2];
            averageY = Double.MIN_VALUE;
            for(Point2D[] side : horSidesLowerBox) {
                if((side[0].y() + side[1].y()) / 2 > averageY) {
                    averageY = (side[0].y() + side[1].y()) / 2;
                    lowerSide = side;
                }
            }
            Point2D[] cutLine = Geometry.parallelLine(Geometry.orientedSegments(measures.get(0).zone.getVertices(),
                    Geometry.Direction.VERTICAL).get(0), touchSegment[0]);
            Point2D upperCutPoint = Geometry.lineIntersectSegment(upperSide, cutLine);
            Point2D lowerCutPoint = Geometry.lineIntersectSegment(lowerSide, cutLine);
            cutPreview[0] = upperCutPoint;
            cutPreview[1] = lowerCutPoint;
        } else if (direction == Geometry.Direction.HORIZONTAL) {
            List<Point2D[]> verSidesLeftBox = Geometry.orientedSegments(copy.get(0).zone.getVertices(),
                    Geometry.Direction.VERTICAL);
            Point2D[] leftSide = new Point2D[2];
            double averageX = Double.MAX_VALUE;
            for(Point2D[] side : verSidesLeftBox) {
                if((side[0].x() + side[1].x()) / 2 < averageX) {
                    averageX = (side[0].x() + side[1].x()) / 2;
                    leftSide = side;
                }
            }
            List<Point2D[]> verSidesRightBox = Geometry.orientedSegments(copy.get(copy.size()-1).zone.getVertices(),
                    Geometry.Direction.VERTICAL);
            Point2D[] rightSide = new Point2D[2];
            averageX = Double.MIN_VALUE;
            for(Point2D[] side : verSidesRightBox) {
                if((side[0].x() + side[1].x()) / 2 > averageX) {
                    averageX = (side[0].x() + side[1].x()) / 2;
                    rightSide = side;
                }
            }
            Point2D[] cutLine = Geometry.parallelLine(Geometry.orientedSegments(measures.get(0).zone.getVertices(),
                    Geometry.Direction.HORIZONTAL).get(0), touchSegment[0]);
            Point2D leftCutPoint = Geometry.lineIntersectSegment(leftSide, cutLine);
            Point2D rightCutPoint = Geometry.lineIntersectSegment(rightSide, cutLine);
            cutPreview[0] = leftCutPoint;
            cutPreview[1] = rightCutPoint;
        }
        return cutPreview;
    }

    private void cutMeasures(List<Measure> measures, Point2D[] touchSegment) {
        for(Measure measure : measures) {
            if(!Geometry.lineIntersectsPolygon(measure.zone.getVertices(), touchSegment)) {
                break;
            }

            boolean trigger = false;
            Measure m1 = new Measure();
            Measure m2 = new Measure();
            List<Point2D> vertices1 = new ArrayList<>();
            List<Point2D> vertices2 = new ArrayList<>();

            for(int i = 0; i < measure.zone.getVertices().size() - 1; i++) {
                Point2D[] side = new Point2D[2];
                side[0] = measure.zone.getVertices().get(i);
                side[1] = measure.zone.getVertices().get(i+1);
                Point2D collision = Geometry.lineIntersectSegment(side, touchSegment);
                if(collision == null) {
                    if(!trigger) {
                        vertices1.add(side[0]);
                    } else {
                        vertices2.add(side[0]);
                    }
                } else {
                    if(!trigger) {
                        vertices1.add(side[0]);
                        if(facsimileView.cutOverlapping > 0) {
                            double dx = (side[1].x() - side[0].x()) * facsimileView.cutOverlapping / side[1].distanceTo(side[0]);
                            double dy = (side[1].y() - side[0].y()) * facsimileView.cutOverlapping / side[1].distanceTo(side[0]);
                            vertices1.add(new Point2D(collision.x() + dx, collision.y() + dy));
                            vertices2.add(new Point2D(collision.x() - dx, collision.y() - dy));
                        } else {
                            vertices1.add(collision);
                            vertices2.add(collision);
                        }
                    } else {
                        vertices2.add(side[0]);
                        if(facsimileView.cutOverlapping > 0) {
                            double dx = (side[1].x() - side[0].x()) * facsimileView.cutOverlapping / side[1].distanceTo(side[0]);
                            double dy = (side[1].y() - side[0].y()) * facsimileView.cutOverlapping / side[1].distanceTo(side[0]);
                            vertices2.add(new Point2D(collision.x() + dx, collision.y() + dy));
                            vertices1.add(new Point2D(collision.x() - dx, collision.y() - dy));
                        } else {
                            vertices2.add(collision);
                            vertices1.add(collision);
                        }
                    }
                    trigger = !trigger;
                }
            }

            Point2D[] side = new Point2D[2];
            side[0] = measure.zone.getVertices().get(measure.zone.getVertices().size() - 1);
            side[1] = measure.zone.getVertices().get(0);
            Point2D collision = Geometry.lineIntersectSegment(side, touchSegment);
            if(collision == null) {
                if(!trigger) {
                    vertices1.add(side[0]);
                } else {
                    vertices2.add(side[0]);
                }
            } else {
                if(!trigger) {
                    vertices1.add(side[0]);
                    if(facsimileView.cutOverlapping > 0) {
                        double dx = (side[1].x() - side[0].x()) * facsimileView.cutOverlapping / side[1].distanceTo(side[0]);
                        double dy = (side[1].y() - side[0].y()) * facsimileView.cutOverlapping / side[1].distanceTo(side[0]);
                        vertices1.add(new Point2D(collision.x() + dx, collision.y() + dy));
                        vertices2.add(new Point2D(collision.x() - dx, collision.y() - dy));
                    } else {
                        vertices1.add(collision);
                        vertices2.add(collision);
                    }
                } else {
                    vertices2.add(side[0]);
                    if(facsimileView.cutOverlapping > 0) {
                        double dx = (side[1].x() - side[0].x()) * facsimileView.cutOverlapping / side[1].distanceTo(side[0]);
                        double dy = (side[1].y() - side[0].y()) * facsimileView.cutOverlapping / side[1].distanceTo(side[0]);
                        vertices2.add(new Point2D(collision.x() + dx, collision.y() + dy));
                        vertices1.add(new Point2D(collision.x() - dx, collision.y() - dy));
                    } else {
                        vertices2.add(collision);
                        vertices1.add(collision);
                    }
                }

            }

            m1.zone.setVertices(vertices1);
            m2.zone.setVertices(vertices2);
            m1.zone.setAnnotationType(m1.zone.checkType());
            m2.zone.setAnnotationType(m1.zone.checkType());
            facsimileView.commandManager.processCutMeasureCommand(facsimile, measure, m1, m2);
        }
    }

    /**
     * Processes the touch events.
     * @param event touch event
     * @return true if the touch input was correctly processed
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Window window;
        WindowManager.LayoutParams wlp;
        if(facsimile == null) {

            return false;
        }
        float touchX = event.getX();
        float touchY = event.getY();
        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER &&
                facsimileView.nextAction != FacsimileView.Action.ADJUST_MEASURE &&
                facsimileView.nextAction != FacsimileView.Action.ADJUST_MOVEMENT) {
            return super.onTouchEvent(event);

        }
        touchBitmapPosition = new Point2D(viewToSourceCoord(touchX, touchY));
        final ArrayList<Measure> measures = page.getMeasuresAt(touchBitmapPosition);
        final Measure measure = page.getMeasureAt(touchBitmapPosition);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                firstGesturePoint = touchBitmapPosition;
                selectedMeasures = new ArrayList<>();
                selectedMeasures.addAll(measures);
                switch (facsimileView.nextAction) {
                    case ORTHOGONAL_CUT:
                        if (measure == null) {
                            pointPath = new ArrayList<>();
                            facsimileView.resetState();
                            facsimileView.resetMenu();
                            // continue and handle the ActionId as a click in brush state
                        }
                        break;
                    case PRECISE_CUT:
                        if (measure == null) {
                            pointPath = new ArrayList<>();
                            facsimileView.resetState();
                            facsimileView.resetMenu();
                            // continue and handle the ActionId as a click in brush state
                        }
                        break;
                    case DRAW:
                        if (facsimileView.isFirstPoint) {
                            pointPath = new ArrayList<>();
                            pointPath.add(touchBitmapPosition);
                            firstDrawPoint = touchBitmapPosition;
                            facsimileView.isFirstPoint = false;
                            lastPolygonPoint = touchBitmapPosition;
                            trackLength = 0.0f;
                            invalidate();
                        }
                        break;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                Point2D[] trackSegment = new Point2D[2];
                trackSegment[0] = lastPoint;
                trackSegment[1] = touchBitmapPosition;
                List<Measure> underlyingMeasures = page.getMeasuresAtSegment(trackSegment);
                for (Measure m : underlyingMeasures) {
                    if(!selectedMeasures.contains(m)) {
                        selectedMeasures.add(m);
                    }
                }
                switch (facsimileView.nextAction) {
                    case ORTHOGONAL_CUT:
                        Point2D[] cutPreview = orthogonalCutPreview(selectedMeasures,
                                new Point2D[]{firstGesturePoint, touchBitmapPosition});
                        firstCutPoint = cutPreview[0];
                        lastCutPoint = cutPreview[1];
                        break;
                    case PRECISE_CUT:
                        Point2D[] cut2Preview = preciseCutPreview(selectedMeasures,
                                new Point2D[]{firstGesturePoint, touchBitmapPosition});
                        firstCutPoint = cut2Preview[0];
                        lastCutPoint = cut2Preview[1];
                        break;
                    case DRAW:
                        if (!facsimileView.isFirstPoint) {
                            trackLength += Math.abs(lastPolygonPoint.x() - touchBitmapPosition.x()) + Math.abs(lastPolygonPoint.y() - touchBitmapPosition.y());
                            pointPath.add(touchBitmapPosition);
                            lastPolygonPoint = touchBitmapPosition;
                        }
                        break;
                }
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
                switch (facsimileView.nextAction) {
                    case ORTHOGONAL_CUT:
                        if(firstCutPoint != null && lastCutPoint != null) {
                            if (firstCutPoint.distanceTo(touchBitmapPosition) >= Vertaktoid.MIN_GESTURE_LENGTH) {
                                cutMeasures(selectedMeasures, new Point2D[]{firstCutPoint, lastCutPoint});
                                firstCutPoint = null;
                                lastCutPoint = null;
                            }
                        }

                        break;
                    case PRECISE_CUT:
                        if(firstCutPoint != null && lastCutPoint != null) {
                            if (firstCutPoint.distanceTo(touchBitmapPosition) >= Vertaktoid.MIN_GESTURE_LENGTH) {
                                cutMeasures(selectedMeasures, new Point2D[]{firstGesturePoint, touchBitmapPosition});
                                firstCutPoint = null;
                                lastCutPoint = null;
                            }
                        }
                        break;
                    case ERASE:
                        eraseMeasures(selectedMeasures);
                        if(facsimileView.currentMovementNumber >= facsimile.movements.size()) {
                            facsimileView.currentMovementNumber = facsimile.movements.get(facsimile.movements.size() - 1).number;
                        }
                        break;
                    case ADJUST_MOVEMENT:
                        final Dialog editMODialog = new Dialog(getContext());
                        window = editMODialog.getWindow();
                        wlp = window.getAttributes();
                        wlp.gravity = Gravity.TOP;
                        window.setAttributes(wlp);
                        editMODialog.setContentView(R.layout.dialog_editmo);
                        editMODialog.setTitle(R.string.dialog_editmo_titel);
                        TextView editMOMovementLabel = (TextView) editMODialog.findViewById(R.id.dialog_editmo_movement_label);
                        editMOMovementLabel.setText(R.string.dialog_editmo_movement_label);
                        final Spinner editMOMovementInput = (Spinner) editMODialog.findViewById(R.id.dialog_editmo_movement_input);
                        ArrayList<String> movementOptions = new ArrayList<>();
                        movementOptions.add(getResources().getString(R.string.dialog_editmo_spinner_optdef));
                        for(Movement movement : facsimile.movements) {
                            movementOptions.add(movement.getName());
                        }
                        movementOptions.add(getResources().getString(R.string.dialog_editmo_spinner_optelse));
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_dropdown_item, movementOptions);
                        editMOMovementInput.setAdapter(adapter);
                        editMOMovementInput.setSelection(0);
                        TextView editMOLabelLabel = (TextView) editMODialog.findViewById(R.id.dialog_editmo_label_label);
                        editMOLabelLabel.setText(R.string.dialog_editmo_label_label);
                        final EditText editMOLabelInput = (EditText) editMODialog.findViewById(R.id.dialog_editmo_label_input);
                        editMOLabelInput.setHint(R.string.dialog_editmo_label_input_hint);
                        editMOLabelInput.setInputType(InputType.TYPE_CLASS_TEXT);
                        Button editMOButtonNegative = (Button) editMODialog.findViewById(R.id.dialog_editmo_button_negative);
                        editMOButtonNegative.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                editMODialog.cancel();
                            }
                        });

                        Button editMOButtonPositive = (Button) editMODialog.findViewById(R.id.dialog_editmo_button_positive);
                        editMOButtonPositive.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String option = editMOMovementInput.getSelectedItem().toString();
                                String labelStr = editMOLabelInput.getText().toString();
                                facsimileView.commandManager.processAdjustMovementCommand(facsimile, measure, option, labelStr,
                                        getResources().getString(R.string.dialog_editmo_spinner_optelse),
                                        getResources().getString(R.string.dialog_editmo_spinner_optdef));
                                facsimileView.currentMovementNumber = facsimile.movements.indexOf(measure.movement);
                                editMODialog.dismiss();
                                facsimileView.adjustHistoryNavigation();
                                invalidate();
                            }
                        });
                        editMODialog.show();
                        break;
                    case ADJUST_MEASURE:
                        if (page.getMeasureAt(touchBitmapPosition) != null) {
                            final Dialog editMEDialog = new Dialog(getContext());
                            window = editMEDialog.getWindow();
                            wlp = window.getAttributes();
                            wlp.gravity = Gravity.TOP;
                            window.setAttributes(wlp);
                            editMEDialog.setContentView(R.layout.dialog_editme);
                            editMEDialog.setTitle(R.string.dialog_editme_titel);
                            TextView editMENameLabel = (TextView) editMEDialog.findViewById(R.id.dialog_editme_name_label);
                            editMENameLabel.setText(R.string.dialog_editme_name_label);
                            final EditText editMENameInput = (EditText) editMEDialog.findViewById(R.id.dialog_editme_name_input);
                            editMENameInput.setHint(measure.getName());
                            editMENameInput.setInputType(InputType.TYPE_CLASS_TEXT);
                            TextView editMERestLabel = (TextView) editMEDialog.findViewById(R.id.dialog_editme_rest_label);
                            editMERestLabel.setText(R.string.dialog_editme_rest_label);
                            String repeatTxt = "" + measure.rest;
                            final EditText editMERestInput = (EditText) editMEDialog.findViewById(R.id.dialog_editme_rest_input);
                            editMERestInput.setHint(repeatTxt);
                            editMERestInput.setInputType(InputType.TYPE_CLASS_NUMBER);
                            Button editMEButtonNegative = (Button) editMEDialog.findViewById(R.id.dialog_editme_button_negative);
                            editMEButtonNegative.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    editMEDialog.cancel();
                                }
                            });

                            Button editMEButtonPositive = (Button) editMEDialog.findViewById(R.id.dialog_editme_button_positive);
                            editMEButtonPositive.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    String manualSequenceNumber = editMENameInput.getText().toString();
                                    String rest = editMERestInput.getText().toString();
                                    if(rest.equals("")) {
                                        rest = "" + measure.rest;
                                    }
                                    facsimileView.commandManager.processAdjustMeasureCommand(facsimile, measure, manualSequenceNumber, rest);
                                    editMEDialog.dismiss();
                                    facsimileView.adjustHistoryNavigation();
                                    invalidate();
                                }
                            });

                            editMEDialog.show();
                        }
                        break;
                    case DRAW:
                        if(lastPolygonPoint == null) {
                            break;
                        }
                        trackLength += Math.abs(lastPolygonPoint.x() - touchBitmapPosition.x()) + Math.abs(lastPolygonPoint.y() - touchBitmapPosition.y());
                        pointPath.add(touchBitmapPosition);
                        lastPolygonPoint = new Point2D(touchX, touchY);
                        PointF firstPointInTouch = sourceToViewCoord(firstDrawPoint.getPointF()); // due to scrolling this may be another position than initially stored in firstDrawPoint
                        double distanceToFirstPoint = Math.sqrt((double) (touchX - firstPointInTouch.x) * (touchX - firstPointInTouch.x) + (touchY - firstPointInTouch.y) * (touchY - firstPointInTouch.y));
                        if (distanceToFirstPoint < 20.0f && trackLength > 100.0f) {
                            pointPath.remove(pointPath.size() - 1);
                            Measure newMeasure = new Measure();
                            newMeasure.zone.setVertices(pointPath);
                            switch (facsimile.nextAnnotationsType) {
                                case ORTHOGONAL_BOX:
                                    newMeasure.zone.convertToOrthogonalBox();
                                    newMeasure.zone.setAnnotationType(Facsimile.AnnotationType.ORTHOGONAL_BOX);
                                    break;
                                case ORIENTED_BOX:
                                    newMeasure.zone.convertToOrientedBox();
                                    newMeasure.zone.setAnnotationType(Facsimile.AnnotationType.ORIENTED_BOX);
                                    break;
                                case POLYGON:
                                    newMeasure.zone.convertToPolygon();
                                    newMeasure.zone.setAnnotationType(Facsimile.AnnotationType.POLYGON);
                                    break;
                            }
                            if(newMeasure.zone.getBoundRight() - newMeasure.zone.getBoundLeft() > 50 &&
                                    newMeasure.zone.getBoundBottom() - newMeasure.zone.getBoundTop() > 50) {
                                if (facsimileView.currentMovementNumber > facsimile.movements.size() - 1) {
                                    facsimileView.currentMovementNumber = facsimile.movements.size() - 1;
                                }
                                facsimileView.commandManager.processCreateMeasureCommand(newMeasure, facsimile, page);
                                invalidate();
                            }
                            pointPath = new ArrayList<>();
                            facsimileView.isFirstPoint = true;
                            invalidate();
                        }
                }
                selectedMeasures.clear();
                break;
        } // end switch
        facsimileView.adjustHistoryNavigation();
        lastPoint = new Point2D(viewToSourceCoord(touchX, touchY));
        facsimileView.needToSave = true;
        return true;
    } // end onTouchEvent

    private void eraseMeasures(List<Measure> measures){
        if(measures.size() > 0) {
            facsimileView.commandManager.processRemoveMeasuresCommand(measures, facsimile);
            invalidate();
        }
    }
}
