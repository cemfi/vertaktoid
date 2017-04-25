package zemfi.de.vertaktoid;

import android.app.Dialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.shapes.PathShape;
import android.graphics.drawable.shapes.Shape;
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
import java.util.Comparator;
import java.util.List;

import zemfi.de.vertaktoid.helpers.Geometry;
import zemfi.de.vertaktoid.helpers.HSLColor;
import zemfi.de.vertaktoid.helpers.Point2D;
import zemfi.de.vertaktoid.model.Facsimile;
import zemfi.de.vertaktoid.model.Measure;
import zemfi.de.vertaktoid.model.Movement;
import zemfi.de.vertaktoid.model.Page;

public class PageImageView extends SubsamplingScaleImageView {

    private final FacsimileView facsimileView;
    private Facsimile facsimile;
    private final Page page;
    private Path boundingPath;
    private Path verticesPath;
    private ArrayList<Point2D> pointPath;
    private Paint drawPaint;
    private Paint largeBoldText = new Paint();
    private Paint smallBoldText = new Paint();
    private Rect pageNameRect = new Rect();
    private Rect measureNameRect = new Rect();
    private Rect movementNameRect = new Rect();

    HSLColor fillColor;
    private float s = 100f;
    private float l = 30f;
    private float a = 1f;
    private float currentBrushSize = 5;
    Path grayPath;
    Path polygonHoverPath;
    float downX = 0.0f;
    float downY = 0.0f;
    Point2D firstPoint;
    Point2D lastPolygonPoint;
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
        drawPaint.setStrokeWidth(currentBrushSize);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        setOnHoverListener(new View.OnHoverListener() {
            @Override
            public boolean onHover(View v, MotionEvent event) {
                if (facsimileView.nextAction == FacsimileView.Action.DRAW || facsimileView.nextAction == FacsimileView.Action.CUT) {
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

        largeBoldText.setColor(Color.BLACK);
        largeBoldText.setTextSize(50);
        largeBoldText.setFakeBoldText(true);

        smallBoldText.setColor(Color.BLACK);
        smallBoldText.setTextSize(36);
        smallBoldText.setFakeBoldText(true);

        facsimileView.generateColors();

        if (!page.imageFile.exists()) {
            largeBoldText.getTextBounds(page.imageFile.getName(), 0,
                    page.imageFile.getName().length(), pageNameRect);
            canvas.drawText("" + page.imageFile.getName(),
                    (this.getRight() - this.getLeft()) / 2  - pageNameRect.centerX(), 100,  largeBoldText);
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
            largeBoldText.setColor(HSLColor.toRGB(facsimileView.movementColors.get(
                    facsimile.movements.indexOf(measure.movement))));
            smallBoldText.setColor(HSLColor.toRGB(facsimileView.movementColors.get(
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
            canvas.drawPath(verticesPath, drawPaint);

            boundingPath.reset();

            String measureLabel = measure.manualSequenceNumber != null ?
                    "" + measure.manualSequenceNumber : "" + measure.sequenceNumber;

            String movementLabel = measure.movement.getName() + " >>";

            largeBoldText.getTextBounds(measureLabel, 0, measureLabel.length(), measureNameRect);
            smallBoldText.getTextBounds(movementLabel, 0, movementLabel.length(), movementNameRect);

            Point2D centroid = Geometry.centroid2D(measure.zone.getVertices());
            PointF centroidF = sourceToViewCoord((float) centroid.x(), (float) centroid.y());
            float leftTextBox = centroidF.x - measureNameRect.width() - 5;
            float topTextBox = centroidF.y - 20 - measureNameRect.height() /2 ;
            float rightTextBox = centroidF.x + measureNameRect.width() + 5;
            float bottomTextBox = centroidF.y - 15 + measureNameRect.height() / 2;

            if(measure.manualSequenceNumber != null) {
                canvas.drawRect(leftTextBox, topTextBox, rightTextBox, bottomTextBox, drawPaint);
            }

            canvas.drawText(measureLabel, centroidF.x - measureNameRect.centerX(), centroidF.y, largeBoldText);
            if(measure.movement.measures.indexOf(measure) == 0) {
                canvas.drawText(movementLabel, centroidF.x - movementNameRect.centerX(),centroidF.y + 30, smallBoldText);

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
        Point2D bitmapCoord = new Point2D(viewToSourceCoord(touchX, touchY));
        final ArrayList<Measure> measures = page.getMeasuresAt(bitmapCoord);
        final Measure measure = page.getMeasureAt(bitmapCoord);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                switch (facsimileView.nextAction) {
                    case ERASE:
                        eraseMeasures(measures);
                        break;
                    case CUT:
                        if (measure == null) {
                            pointPath = new ArrayList<>();
                            facsimileView.resetState();
                            facsimileView.resetMenu();
                            // continue and handle the ActionId as a click in brush state
                        } else {
                            Measure mleft = new Measure();
                            Measure mright = new Measure();
                            List<Point2D> verticesLeft = new ArrayList<>();
                            List<Point2D> verticesRight = new ArrayList<>();
                            switch (measure.zone.getAnnotationType()) {
                                case ORTHOGONAL_BOX:
                                    verticesLeft.add(new Point2D(measure.zone.getBoundLeft(), measure.zone.getBoundTop()));
                                    verticesLeft.add(new Point2D(measure.zone.getBoundLeft(), measure.zone.getBoundBottom()));
                                    verticesLeft.add(new Point2D(bitmapCoord.x() + facsimileView.horOverlapping, measure.zone.getBoundBottom()));
                                    verticesLeft.add(new Point2D(bitmapCoord.x() + facsimileView.horOverlapping, measure.zone.getBoundTop()));
                                    verticesRight.add(new Point2D(bitmapCoord.x() - facsimileView.horOverlapping, measure.zone.getBoundTop()));
                                    verticesRight.add(new Point2D(bitmapCoord.x() - facsimileView.horOverlapping, measure.zone.getBoundBottom()));
                                    verticesRight.add(new Point2D(measure.zone.getBoundRight(), measure.zone.getBoundBottom()));
                                    verticesRight.add(new Point2D(measure.zone.getBoundRight(), measure.zone.getBoundTop()));
                                    break;
                                case ORIENTED_BOX:

                                    List<Point2D[]> horSegments = Geometry.orientedSegments(measure.zone.getVertices(), Geometry.Orientation.HORIZONTAL);
                                    // for in 45 degrees rotated rectangles
                                    while(horSegments.size() > 2) {
                                        horSegments.remove(horSegments.size()-1);
                                    }
                                    Point2D[] upperSide;
                                    Point2D[] lowerSide;
                                    double s3minY = Math.min(horSegments.get(0)[0].y(), horSegments.get(0)[1].y());
                                    double s4minY = Math.min(horSegments.get(1)[0].y(), horSegments.get(1)[1].y());
                                    if(s3minY <= s4minY) {
                                        upperSide = horSegments.get(0);
                                        lowerSide = horSegments.get(1);
                                    } else {
                                        upperSide = horSegments.get(1);
                                        lowerSide = horSegments.get(0);
                                    }

                                    Point2D upperIntersectPoint = Geometry.projectionPointToSegment(upperSide, bitmapCoord);
                                    Point2D lowerIntersectPoint = Geometry.projectionPointToSegment(lowerSide, bitmapCoord);

                                    verticesLeft.add(upperIntersectPoint);
                                    verticesLeft.add(lowerIntersectPoint);
                                    if(Point2D.X_ORDER.compare(lowerSide[0], lowerSide[1]) <= 0) {
                                        verticesLeft.add(lowerSide[0]);
                                    } else {
                                        verticesLeft.add(lowerSide[1]);
                                    }
                                    if(Point2D.X_ORDER.compare(upperSide[0], upperSide[1]) <= 0) {
                                        verticesLeft.add(upperSide[0]);
                                    } else {
                                        verticesLeft.add(upperSide[1]);
                                    }

                                    verticesRight.add(lowerIntersectPoint);
                                    verticesRight.add(upperIntersectPoint);
                                    if(Point2D.X_ORDER.compare(upperSide[0], upperSide[1]) >= 0) {
                                        verticesRight.add(upperSide[0]);
                                    } else {
                                        verticesRight.add(upperSide[1]);
                                    }
                                    if(Point2D.X_ORDER.compare(lowerSide[0], lowerSide[1]) >= 0) {
                                        verticesRight.add(lowerSide[0]);
                                    } else {
                                        verticesRight.add(lowerSide[1]);
                                    }
                                    mleft.zone.setAnnotationType(Facsimile.AnnotationType.ORIENTED_BOX);
                                    mright.zone.setAnnotationType(Facsimile.AnnotationType.ORIENTED_BOX);
                                    break;
                                case POLYGON:
                                    //TODO Polygon splitting
                                    verticesLeft.add(new Point2D(measure.zone.getBoundLeft(), measure.zone.getBoundTop()));
                                    verticesLeft.add(new Point2D(measure.zone.getBoundLeft(), measure.zone.getBoundBottom()));
                                    verticesLeft.add(new Point2D(bitmapCoord.x() + facsimileView.horOverlapping, measure.zone.getBoundBottom()));
                                    verticesLeft.add(new Point2D(bitmapCoord.x() + facsimileView.horOverlapping, measure.zone.getBoundTop()));
                                    verticesRight.add(new Point2D(bitmapCoord.x() - facsimileView.horOverlapping, measure.zone.getBoundTop()));
                                    verticesRight.add(new Point2D(bitmapCoord.x() - facsimileView.horOverlapping, measure.zone.getBoundBottom()));
                                    verticesRight.add(new Point2D(measure.zone.getBoundRight(), measure.zone.getBoundBottom()));
                                    verticesRight.add(new Point2D(measure.zone.getBoundRight(), measure.zone.getBoundTop()));
                                    break;
                            }
                            mleft.zone.setVertices(verticesLeft);
                            mright.zone.setVertices(verticesRight);
                            facsimileView.commandManager.processCutMeasureCommand(facsimile, measure, mleft, mright);
                        }
                        break;
                    case DRAW:
                        if (facsimileView.isFirstPoint) {
                            pointPath = new ArrayList<>();
                            firstPoint = bitmapCoord;
                            pointPath.add(bitmapCoord);
                            facsimileView.isFirstPoint = false;
                            lastPolygonPoint = bitmapCoord;
                            trackLength = 0.0f;
                            invalidate();
                        }
                        downX = touchX;
                        downY = touchY;
                        break;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                switch (facsimileView.nextAction) {
                    case ERASE:
                        eraseMeasures(measures);
                        break;
                    case DRAW:
                        if (!facsimileView.isFirstPoint) {
                            trackLength += Math.abs(lastPolygonPoint.x() - bitmapCoord.x()) + Math.abs(lastPolygonPoint.y() - bitmapCoord.y());
                            pointPath.add(bitmapCoord);
                            lastPolygonPoint = bitmapCoord;
                            invalidate();
                        }
                        break;
                }
                break;

            case MotionEvent.ACTION_UP:
                switch (facsimileView.nextAction) {
                    case ERASE:
                        eraseMeasures(measures);
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
                        if (page.getMeasureAt(bitmapCoord) != null) {
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
                        trackLength += Math.abs(lastPolygonPoint.x() - bitmapCoord.x()) + Math.abs(lastPolygonPoint.y() - bitmapCoord.y());
                        pointPath.add(bitmapCoord);
                        lastPolygonPoint = new Point2D(touchX, touchY);
                        PointF firstPointInTouch = sourceToViewCoord(firstPoint.getPointF()); // due to scrolling this may be another position than initially stored in firstPoint
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
        } // end switch
        facsimileView.adjustHistoryNavigation();
        lastPoint = new Point2D(viewToSourceCoord(touchX, touchY));
        facsimileView.needToSave = true;
        return true;
    } // end onTouchEvent

    private void eraseMeasures(ArrayList<Measure> measures){
        if(measures.size() > 0) {
            facsimileView.commandManager.processRemoveMeasuresCommand(measures, facsimile);
            invalidate();
        }
    }
}
