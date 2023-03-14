package zemfi.de.vertaktoid;

import android.app.Dialog;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.support.v4.provider.DocumentFile;
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


import com.ceylonlabs.imageviewpopup.ImagePopup;
import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.goebl.simplify.PointExtractor;
import com.goebl.simplify.Simplify;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


import kotlin.collections.IntIterator;
import zemfi.de.vertaktoid.helpers.Geometry;
import zemfi.de.vertaktoid.helpers.HSLColor;
import zemfi.de.vertaktoid.helpers.Point2D;
import zemfi.de.vertaktoid.helpers.RotatingCalipers;
import zemfi.de.vertaktoid.mei.MEIHelper;
import zemfi.de.vertaktoid.model.Facsimile;
import zemfi.de.vertaktoid.model.Measure;
import zemfi.de.vertaktoid.model.Movement;
import zemfi.de.vertaktoid.model.Page;
import zemfi.de.vertaktoid.model.Zone;

/**
 * Includes rendering functions, dialogs, touch functions.
 * Uses rotating calipers algorithm.
 */

public class PageImageView extends SubsamplingScaleImageView {
    private final FacsimileView facsimileView;
    private final Facsimile facsimile;
    private final Page page;
    private Path boundingPath;
    private Path verticesPath;
    private ArrayList<Point2D> pointPath;   //currentPath
    private Paint selectionPaint;
    private Paint cutLinePaint;
    private Paint drawPaint;
    private final Paint largeTextPaint = new Paint();
    private final Paint smallTextPaint = new Paint();
    private final Rect pageNameRect = new Rect();
    private final Rect measureNameRect = new Rect();
    private final Rect movementNameRect = new Rect();
    private List<Measure> selectedMeasures = new ArrayList<>();
    private final int selectionColor = 0xff404040;
    private final int cutLineColor = 0xff0f0f0f;
    HSLColor fillColor;
    private final float s = 100f;
    private final float l = 30f;
    private final float a = 1f;
    private final float brushSize = 5;
    private List<Point2D> vertices;
    Path grayPath;
    Path polygonHoverPath;
    Point2D firstDrawPoint;
    Point2D firstGesturePoint;
    Point2D touchBitmapPosition;
    Point2D lastPolygonPoint;
    Point2D firstCutPoint;
    Point2D lastCutPoint;
    Point2D lastPoint = null; // in bitmap coordinates
    public String currentPageUrlId;
    public int status;


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

        // clicks for finger touches (movement and measure labels)
        // in the touchEvent finger touches are entirely ignored so that zoom actions don't cause other actions
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                final Measure measure = page.getMeasureAt(touchBitmapPosition);
                    switch (facsimileView.nextAction) {
                        case ADJUST_MOVEMENT:
                            if (measure != null) {
                                buildMODialog(measure);
                            }
                            break;

                        case ADJUST_MEASURE:
                            if (measure != null) {
                                buildMEDialog(measure);
                            }
                            break;

                }
            }
        });

        // show preview
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
     * dialog to rename a measure
     * called from touchListener (pen) and clickListener (fingers)
     * @param measure measure to be renamed
     */
    private void buildMEDialog(final Measure measure)
    {
        Window window;
        WindowManager.LayoutParams wlp;
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
                    if (rest.equals("")) {
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
    }

    /**
     * dialog to change the movement
     * called from touchListener (pen) and clickListener (fingers)
     * @param measure measure to be the start of the movement
     */
    private void buildMODialog(final Measure measure)
    {
        Window window;
        WindowManager.LayoutParams wlp;
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
        for (Movement movement : facsimile.movements) {
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
        DocumentFile appSubFolder = facsimile.dir.findFile(Vertaktoid.APP_SUBFOLDER);
        DocumentFile stubImg = appSubFolder.findFile(Vertaktoid.NOT_FOUND_STUBIMG);
        if(page == null) {
            return null;
        }

        if(!page.imageExists()) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = false;
            ImageSource bitmap = null;

            try {
                ParcelFileDescriptor parcelFileDescriptor =
                        MainActivity.context.getContentResolver().openFileDescriptor(stubImg.getUri(), "r");
                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                bitmap = ImageSource.bitmap(BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options));
                parcelFileDescriptor.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return bitmap;
        }
        else {
            return page.getImage();
        }
    }

    /**
     * Rendering function
     * boundingPath is an oriented orthogonal box covering all points
     * verticesPath is the Path connecting the corner marks
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

        // if image is missing draw Name
        if (!page.imageExists()) {
            largeTextPaint.getTextBounds(page.getImageFileName(), 0,
                    page.getImageFileName().length(), pageNameRect);
            canvas.drawText("" + page.getImageFileName(),
                    (this.getRight() - this.getLeft()) / 2  - pageNameRect.centerX(), 100, largeTextPaint);
            return;
        }

        for (int i = 0; i < page.measures.size(); i++) {
            // foreach measure
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

            // calculated in zone by rotating calipers
            if(measure.zone.getAnnotationType() == Facsimile.AnnotationType.ORTHOGONAL_BOX) {
                boundingPath.addRect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y, Path.Direction.CW);

            }

            // adds vertices (corner marks) of each measure (its zone) to verticesPath
            // vertices were set in onTouchEvent (Action Up, draw mode)
            List<Point2D> vertices = measure.zone.getVertices();
            final PointF fp = sourceToViewCoord(vertices.get(0).getPointF());
            verticesPath.moveTo(fp.x, fp.y);
            System.out.println("verices path " + verticesPath);
            for(int j = 1; j < vertices.size(); j++) {
                final PointF cp = sourceToViewCoord(vertices.get(j).getPointF());
                System.out.println("this are the points " + cp);
                verticesPath.lineTo(cp.x, cp.y);
            }
            verticesPath.close();

            // draw border of each measure
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

            // draw sequence number
            String measureLabel = measure.manualSequenceNumber != null ?
                    "" + measure.manualSequenceNumber : "" + measure.sequenceNumber;
            String movementLabel =  measure.movement.getName() +  " >>";
            String metconLabel = "";
            if(measure.metcon == false){
                metconLabel =  "upbeat";

            }

            largeTextPaint.getTextBounds(measureLabel, 0, measureLabel.length(), measureNameRect);
            smallTextPaint.getTextBounds(movementLabel, 0, movementLabel.length(), movementNameRect);
            Point2D centroid = Geometry.centroid2D(measure.zone.getVertices());
            System.out.println("this is " +  centroid);
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
            if(measure.metcon == false){
                canvas.drawText("upbeat", centroidF.x - movementNameRect.centerX(),centroidF.y + 30, smallTextPaint);

            }
        } // end for (foreach measure)

        // build current Path
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

        // execute current path drawing
        canvas.drawPath(verticesPath, drawPaint);
        canvas.drawPath(polygonHoverPath, drawPaint);
        if((facsimileView.nextAction == FacsimileView.Action.ORTHOGONAL_CUT ||
                facsimileView.nextAction == FacsimileView.Action.PRECISE_CUT) &&
                firstCutPoint != null && lastCutPoint != null) {
            PointF fcut = sourceToViewCoord(firstCutPoint.getPointF());
            PointF lcut = sourceToViewCoord(lastCutPoint.getPointF());
            canvas.drawLine(fcut.x, fcut.y, lcut.x, lcut.y, cutLinePaint);
        }
    } // end onDraw

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
    //@Override
    public boolean onTouchEvent(MotionEvent event) {
        if(facsimile == null) {

            return false;
        }

        // position, also used in "onClick" for Measures and Movements
        float touchX = event.getX();
        float touchY = event.getY();
        touchBitmapPosition = new Point2D(viewToSourceCoord(touchX, touchY));
        /** if (event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER)
        {
            return super.onTouchEvent(event);
        } **/

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
                    case DRAW2:
                        if (facsimileView.isFirstPoint) {
                            // add to current Path
                            pointPath = new ArrayList<>();
                            pointPath.add(touchBitmapPosition);
                            firstDrawPoint = touchBitmapPosition;
                            facsimileView.isFirstPoint = false;
                            lastPolygonPoint = touchBitmapPosition;
                            invalidate();
                        }
                        break;
                    case DRAW:
                        if (facsimileView.isFirstPoint) {
                            // add to current Path
                            pointPath = new ArrayList<>();
                            pointPath.add(touchBitmapPosition);
                            firstDrawPoint = touchBitmapPosition;
                            facsimileView.isFirstPoint = false;
                            lastPolygonPoint = touchBitmapPosition;
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
                    if (!selectedMeasures.contains(m)) {
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
                    case IIIF_ZOOM:
                        if (!facsimileView.isFirstPoint) {
                            pointPath.add(touchBitmapPosition);
                            lastPolygonPoint = touchBitmapPosition;
                        }
                    case DRAW2:
                        if (!facsimileView.isFirstPoint) {
                            pointPath.add(touchBitmapPosition);
                            lastPolygonPoint = touchBitmapPosition;
                        }
                        break;
                    case DRAW:
                        if (!facsimileView.isFirstPoint) {
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
                        if (firstCutPoint != null && lastCutPoint != null) {
                            if (firstCutPoint.distanceTo(touchBitmapPosition) >= Vertaktoid.MIN_GESTURE_LENGTH) {
                                cutMeasures(selectedMeasures, new Point2D[]{firstCutPoint, lastCutPoint});
                                firstCutPoint = null;
                                lastCutPoint = null;
                            }
                        }

                        break;
                    case PRECISE_CUT:
                        if (firstCutPoint != null && lastCutPoint != null) {
                            if (firstCutPoint.distanceTo(touchBitmapPosition) >= Vertaktoid.MIN_GESTURE_LENGTH) {
                                cutMeasures(selectedMeasures, new Point2D[]{firstGesturePoint, touchBitmapPosition});
                                firstCutPoint = null;
                                lastCutPoint = null;
                            }
                        }
                        break;
                    case ERASE:
                        eraseMeasures(selectedMeasures);
                        if (facsimileView.currentMovementNumber >= facsimile.movements.size()) {
                            facsimileView.currentMovementNumber = facsimile.movements.get(facsimile.movements.size() - 1).number;
                        }
                        invalidate();
                        break;

                    case ADJUST_MOVEMENT:
                        if (measure != null) {
                            buildMODialog(measure);
                        }
                        break;

                    case ADJUST_MEASURE:
                        if (measure != null) {
                            buildMEDialog(measure);
                        }
                        break;
                    case IIIF_ZOOM:
                        if (lastPolygonPoint == null) {
                            break;
                        }
                        pointPath.add(touchBitmapPosition);
                        lastPolygonPoint = new Point2D(touchX, touchY);
                        PointF firstPointInTouch2 = sourceToViewCoord(firstDrawPoint.getPointF()); // due to scrolling this may be another position than initially stored in firstDrawPoint
                        double trackLength2 = 0;
                        for (int i = 1; i < pointPath.size(); i++) {
                            trackLength2 += pointPath.get(i - 1).distanceTo(pointPath.get(i));
                        }
                        trackLength2 += pointPath.get(pointPath.size() - 1).distanceTo(pointPath.get(0));
                        double distanceToFirstPoint2 = Math.sqrt((double) (touchX - firstPointInTouch2.x) * (touchX - firstPointInTouch2.x) + (touchY - firstPointInTouch2.y) * (touchY - firstPointInTouch2.y));
                        if (distanceToFirstPoint2 < 20.0f && trackLength2 > 20f) {
                            pointPath.remove(pointPath.size() - 1);
                            Measure newMeasure = new Measure();
                            newMeasure.zone.setVertices(pointPath); // sets Vertices to currentPath
                            switch (facsimile.nextAnnotationsType) {
                                case ORTHOGONAL_BOX:
                                    newMeasure.zone.convertToOrthogonalBox();
                                    newMeasure.zone.setAnnotationType(Facsimile.AnnotationType.ORTHOGONAL_BOX);
                                    break;
                                case ORIENTED_BOX:
                                    // uses rotating calipers
                                    newMeasure.zone.convertToOrientedBox();
                                    newMeasure.zone.setAnnotationType(Facsimile.AnnotationType.ORIENTED_BOX);
                                    break;
                                case POLYGON:
                                    newMeasure.zone.convertToPolygon();
                                    newMeasure.zone.setAnnotationType(Facsimile.AnnotationType.POLYGON);
                                    break;
                            }
                            System.out.println("LIST OF IMAGE " + MEIHelper.iiifimg);
                            PointF topLeft = sourceToViewCoord((float) newMeasure.zone.getBoundLeft(), (float) newMeasure.zone.getBoundTop());
                            PointF bottomRight = sourceToViewCoord((float) newMeasure.zone.getBoundRight(), (float) newMeasure.zone.getBoundBottom());
                            System.out.println("left " + topLeft.x);
                            System.out.println("right " + bottomRight.x);
                            System.out.println("top " + topLeft.y);
                            System.out.println("bottom " + bottomRight.y);
                            int pageNumber = page.number;
                            int index = pageNumber - 1;
                            System.out.println("index is " + index);
                            System.out.println("CURRENT URL  " + MEIHelper.iiifimg.get(index));
                            System.out.println("CURRENT PAGE  " + Integer.toString(page.number));
                            currentPageUrlId = MEIHelper.iiifimg.get(index);
                            String[] originalurl = new String[currentPageUrlId.split("/").length];
                            double left = newMeasure.zone.getBoundLeft();
                            double right = newMeasure.zone.getBoundRight();
                            double top = newMeasure.zone.getBoundTop();
                            double bottom = newMeasure.zone.getBoundBottom();

                            double width = Math.abs(left - right);
                            double height = Math.abs(bottom - top);
                            System.out.println(" image height " + page.imageHeight + " image width" + page.imageWidth + " buttom " + bottom + " top" + top + " left " + left + " right " + right);
                            newMeasure.zone.getBoundTop();
                            String region = left + "," + top + "," + width + "," + height;
                            originalurl = currentPageUrlId.toString().split("/");
                            originalurl[originalurl.length - 4] = region;
                            String stringurl = "";
                            for (String string : originalurl) {
                                stringurl = stringurl + "/" + string;
                            }
                            String newurl;
                            newurl = stringurl.substring(1);
                            Thread gfgThread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        try {
                                            HttpURLConnection conn = (HttpURLConnection) (new URL(newurl))
                                                    .openConnection();
                                            conn.setUseCaches(false);
                                            conn.connect();
                                            status = conn.getResponseCode();
                                            System.out.println("stats");
                                            if (status == 404) {
                                                Handler uiHandler = new Handler(Looper.getMainLooper());
                                                uiHandler.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        IiifManifest iiifmanifest = new IiifManifest();
                                                        iiifmanifest.displayError("Unsupported file");
                                                    }
                                                });

                                            } else {
                                                System.out.println(newurl);
                                                displaImage(newurl);
                                            }
                                            conn.disconnect();

                                        } catch (Exception e) {
                                            System.out.println("doHttpGetRequest" + e.toString());
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });

                            gfgThread.start();


                            pointPath = new ArrayList<>();
                            facsimileView.isFirstPoint = true;
                            invalidate();

                        }
                        break;
                    case DRAW2:
                        if (lastPolygonPoint == null) {
                            break;
                        }
                        pointPath.add(touchBitmapPosition);
                        lastPolygonPoint = new Point2D(touchX, touchY);
                        PointF firstPointInTouch3 = sourceToViewCoord(firstDrawPoint.getPointF()); // due to scrolling this may be another position than initially stored in firstDrawPoint
                        double trackLength3 = 0;
                        for (int i = 1; i < pointPath.size(); i++) {
                            trackLength3 += pointPath.get(i - 1).distanceTo(pointPath.get(i));
                        }
                        trackLength3 += pointPath.get(pointPath.size() - 1).distanceTo(pointPath.get(0));
                        double distanceToFirstPoint3 = Math.sqrt((double) (touchX - firstPointInTouch3.x) * (touchX - firstPointInTouch3.x) + (touchY - firstPointInTouch3.y) * (touchY - firstPointInTouch3.y));
                        if (distanceToFirstPoint3 < 20.0f && trackLength3 > 20f) {
                            pointPath.remove(pointPath.size() - 1);
                            pointPath = new ArrayList<>();
                            facsimileView.isFirstPoint = true;
                            invalidate();

                        }

                        invalidate();
                        break;
                    case DRAW:
                        if(lastPolygonPoint == null) {
                            break;
                        }

                        pointPath.add(touchBitmapPosition);
                            lastPolygonPoint = new Point2D(touchX, touchY);
                            PointF firstPointInTouch = sourceToViewCoord(firstDrawPoint.getPointF()); // due to scrolling this may be another position than initially stored in firstDrawPoint
                            double trackLength = 0;
                            for(int i = 1; i < pointPath.size(); i++) {
                                trackLength += pointPath.get(i-1).distanceTo(pointPath.get(i));
                            }
                        trackLength += pointPath.get(pointPath.size()-1).distanceTo(pointPath.get(0));
                        double distanceToFirstPoint = Math.sqrt((double) (touchX - firstPointInTouch.x) * (touchX - firstPointInTouch.x) + (touchY - firstPointInTouch.y) * (touchY - firstPointInTouch.y));
                        if (distanceToFirstPoint < 20.0f && trackLength > 20f) {
                            pointPath.remove(pointPath.size() - 1);
                            Measure newMeasure;
                            if(Vertaktoid.metcon == false){
                                 newMeasure = new Measure(Vertaktoid.metcon);

                            }else{
                                newMeasure = new Measure();
                            }
                            Vertaktoid.metcon = true;
                            newMeasure.zone.setVertices(pointPath); // sets Vertices to currentPath
                            newMeasure.zone.setVertices(pointPath); // sets Vertices to currentPath
                            switch (facsimile.nextAnnotationsType) {
                                case ORTHOGONAL_BOX:
                                    newMeasure.zone.convertToOrthogonalBox();
                                    newMeasure.zone.setAnnotationType(Facsimile.AnnotationType.ORTHOGONAL_BOX);
                                    break;
                                case ORIENTED_BOX:
                                    // uses rotating calipers
                                    newMeasure.zone.convertToOrientedBox();
                                    newMeasure.zone.setAnnotationType(Facsimile.AnnotationType.ORIENTED_BOX);
                                    break;
                                case POLYGON:
                                    newMeasure.zone.convertToPolygon();
                                    newMeasure.zone.setAnnotationType(Facsimile.AnnotationType.POLYGON);
                                    break;
                            }
                            if(newMeasure.zone.getBoundRight() - newMeasure.zone.getBoundLeft() > 5 &&
                                    newMeasure.zone.getBoundBottom() - newMeasure.zone.getBoundTop() > 5) {
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

    private void displaImage(String newurl) {
        Handler uiHandler = new Handler(Looper.getMainLooper());
        uiHandler.post(new Runnable(){
            @Override
            public void run() {
                ImagePopup imagePopup = new ImagePopup(MainActivity.context);
                imagePopup.setWindowHeight(800); // Optional
                imagePopup.setWindowWidth(800); // Optional
                imagePopup.setFullScreen(true); // Optional
                imagePopup.setHideCloseIcon(true);  // Optional
                imagePopup.setImageOnClickClose(true);  // Optional

                imagePopup.initiatePopupWithPicasso(newurl);
                imagePopup.viewPopup();
            }
        });

    }


    private void eraseMeasures(List<Measure> measures){
        if(measures.size() > 0) {
            facsimileView.commandManager.processRemoveMeasuresCommand(measures, facsimile);
            invalidate();
        }
    }
    public String stringUrlParser(){
        String path = facsimile.dir.getUri().getPath();
        String folder_name2 = path.replace("tree/primary:", "");
        String folder_name = folder_name2.substring(folder_name2.indexOf(":") + 1);
        return folder_name;
    }

}
