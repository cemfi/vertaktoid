package zemfi.de.vertaktoid;

import android.app.Dialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Parcelable;
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
import java.util.List;

import zemfi.de.vertaktoid.helpers.HSLColor;
import zemfi.de.vertaktoid.helpers.HSLColorsGenerator;
import zemfi.de.vertaktoid.model.Facsimile;
import zemfi.de.vertaktoid.model.Measure;
import zemfi.de.vertaktoid.model.Movement;
import zemfi.de.vertaktoid.model.Page;

public class PageImageView extends SubsamplingScaleImageView {

    private final FacsimileView facsimileView;
    private Facsimile facsimile;
    private final PageView pageView;
    private final Page page;
    private Path boundingPath;
    private Path verticesPath;
    private ArrayList<PointF> pointPath;
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
    PointF firstPoint;
    PointF lastPolygonPoint;
    PointF lastPoint = null; // in bitmap coordinates
    float trackLength;

    /**
     * Constructor
     */
    public PageImageView(PageView pageView, Page page, FacsimileView facsimileView, Facsimile facsimile) {
        super(pageView.getContext());
        this.facsimile = facsimile;
        this.facsimileView = facsimileView;
        this.pageView = pageView;
        this.page = page;
        init();
    }

    /**
     * Initialization
     */
    public void init() {
        setImage(findImageForPage());
        this.setMinimumDpi(80);
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
                                PointF lastPoint = pointPath.get(pointPath.size() - 1);
                                PointF lastPointTouch = transformCoordBitmapToTouch(lastPoint.x, lastPoint.y);
                                polygonHoverPath.moveTo(lastPointTouch.x, lastPointTouch.y);
                                polygonHoverPath.lineTo(event.getX(), event.getY());
                            }
                            invalidate();
                            break;
                        case MotionEvent.ACTION_HOVER_MOVE:
                            if (!facsimileView.isFirstPoint) {
                                polygonHoverPath.reset();
                                PointF lastPoint = pointPath.get(pointPath.size() - 1);
                                PointF lastPointTouch = transformCoordBitmapToTouch(lastPoint.x, lastPoint.y);
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
            return ImageSource.uri(Uri.fromFile(page.imageFile));
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

        int i;

        for (i = 0; i < page.measures.size(); i++) {
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

            PointF topLeft = transformCoordBitmapToTouch(measure.zone.getBoundLeft(), measure.zone.getBoundTop());
            PointF bottomRight = transformCoordBitmapToTouch(measure.zone.getBoundRight(), measure.zone.getBoundBottom());
            if (topLeft == null) {
                // still loading image
                return;
            }

            if(facsimileView.cornerType == FacsimileView.CornerTypes.ROUNDED) {
                boundingPath.addRoundRect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y, 15f, 15f, Path.Direction.CW);
            } else if(facsimileView.cornerType == FacsimileView.CornerTypes.STRAIGHT) {
                boundingPath.addRect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y, Path.Direction.CW);
            }
            List<float[]> vertices = measure.zone.getVertices();
            final PointF fp = transformCoordBitmapToTouch(vertices.get(0)[0], vertices.get(0)[1]);
            verticesPath.moveTo(fp.x, fp.y);
            for(int j = 1; j < vertices.size(); j++) {
                final PointF cp = transformCoordBitmapToTouch(vertices.get(j)[0], vertices.get(j)[1]);
                verticesPath.lineTo(cp.x, cp.y);
            }
            verticesPath.lineTo(fp.x, fp.y);
            drawPaint.setStyle(Paint.Style.FILL);
            fillColor = new HSLColor();
            fillColor.a = 0.1f;
            fillColor.h = facsimileView.movementColors.get(
                    facsimile.movements.indexOf(measure.movement)).h;
            fillColor.s = s;
            fillColor.l = l;
            drawPaint.setColor(HSLColor.toARGB(fillColor));
            canvas.drawPath(boundingPath, drawPaint);
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

            float leftTextBox = (topLeft.x + bottomRight.x) / 2 - measureNameRect.centerX() - 5;
            float topTextBox = topLeft.y + 50 - measureNameRect.height();
            float rightTextBox = (topLeft.x + bottomRight.x) / 2 + measureNameRect.centerX() + 5;
            float bottomTextBox = topLeft.y + 50;

            if(measure.manualSequenceNumber != null) {
                canvas.drawRect(leftTextBox, topTextBox, rightTextBox, bottomTextBox, drawPaint);
            }

            canvas.drawText(measureLabel, (topLeft.x + bottomRight.x) / 2 - measureNameRect.centerX(), topLeft.y + 50, largeBoldText);
            if(measure.movement.measures.indexOf(measure) == 0) {
                canvas.drawText(movementLabel, (topLeft.x + bottomRight.x) / 2 - movementNameRect.centerX(), topLeft.y + 100, smallBoldText);

            }
        }

        verticesPath.reset();
        for (i = 0; i < pointPath.size(); i++) {
            PointF bitmapCoord = pointPath.get(i);
            PointF touchCoord = transformCoordBitmapToTouch(bitmapCoord.x, bitmapCoord.y);
            if (i == 0) {
                verticesPath.addCircle(touchCoord.x, touchCoord.y, 10, Path.Direction.CW);
                verticesPath.moveTo(touchCoord.x, touchCoord.y);
            } else {
                verticesPath.lineTo(touchCoord.x, touchCoord.y);
            }
        }
        canvas.drawPath(verticesPath, drawPaint);
        //canvas.drawPath(polygonHoverPath, drawPaint);
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
        PointF bitmapCoord = transformCoordTouchToBitmap(touchX, touchY);
        final ArrayList<Measure> measures = page.getMeasuresAt(bitmapCoord.x, bitmapCoord.y);
        final Measure measure = page.getMeasureAt(bitmapCoord.x, bitmapCoord.y);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                switch (facsimileView.nextAction) {
                    case ERASE:
                        if(measures.size() > 0) {
                            facsimileView.commandManager.processRemoveMeasuresCommand(measures, facsimile);
                            invalidate();
                        }
                        break;
                    case CUT:
                        if (measure == null) {
                            pointPath = new ArrayList<>();
                            facsimileView.resetState();
                            facsimileView.resetMenu();
                            // continue and handle the ActionId as a click in brush state
                        } else {
                            if(facsimile.meiType == Facsimile.MEIType.CANONICAL) {
                                Measure mleft = new Measure();
                                Measure mright = new Measure();
                                List<float[]> verticesLeft = new ArrayList<>();
                                verticesLeft.add(new float[]{measure.zone.getBoundLeft(), measure.zone.getBoundTop()});
                                verticesLeft.add(new float[]{measure.zone.getBoundLeft(), measure.zone.getBoundBottom()});
                                verticesLeft.add(new float[]{bitmapCoord.x + facsimileView.horOverlapping, measure.zone.getBoundBottom()});
                                verticesLeft.add(new float[]{bitmapCoord.x + facsimileView.horOverlapping, measure.zone.getBoundTop()});
                                List<float[]> verticesRight = new ArrayList<>();
                                verticesRight.add(new float[]{bitmapCoord.x - facsimileView.horOverlapping, measure.zone.getBoundTop()});
                                verticesRight.add(new float[]{bitmapCoord.x - facsimileView.horOverlapping, measure.zone.getBoundBottom()});
                                verticesRight.add(new float[]{measure.zone.getBoundRight(), measure.zone.getBoundBottom()});
                                verticesRight.add(new float[]{measure.zone.getBoundRight(), measure.zone.getBoundTop()});
                                mleft.zone.setVertices(verticesLeft);
                                mright.zone.setVertices(verticesRight);
                                facsimileView.commandManager.processCutMeasureCommand(facsimile, measure, mleft, mright);
                            }
                        }
                        break;
                    case DRAW:
                        if (facsimileView.isFirstPoint) {
                            pointPath = new ArrayList<>();
                            firstPoint = bitmapCoord;
                            pointPath.add(bitmapCoord);
                            //leftMost = bitmapCoord.x;
                            //rightMost = bitmapCoord.x;
                            //topMost = bitmapCoord.y;
                            //bottomMost = bitmapCoord.y;
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
                        ArrayList<Measure> toRemove = page.getMeasuresAtSegment
                                (bitmapCoord.x, bitmapCoord.y, lastPoint.x, lastPoint.y);
                        if(toRemove.size() > 0) {
                            facsimileView.commandManager.processRemoveMeasuresCommand(toRemove, facsimile);
                            invalidate();
                        }
                        break;
                    case DRAW:
                        if (!facsimileView.isFirstPoint) {
                            trackLength += Math.abs(lastPolygonPoint.x - bitmapCoord.x) + Math.abs(lastPolygonPoint.y - bitmapCoord.y);
                            //leftMost = bitmapCoord.x < leftMost ? bitmapCoord.x : leftMost;
                            //rightMost = bitmapCoord.x > rightMost ? bitmapCoord.x : rightMost;
                            //topMost = bitmapCoord.y < topMost ? bitmapCoord.y : topMost;
                            //bottomMost = bitmapCoord.y > bottomMost ? bitmapCoord.y : bottomMost;
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
                        if (measures.size() > 0) {
                            facsimileView.commandManager.processRemoveMeasuresCommand(measures, facsimile);
                            invalidate();
                        }
                        if(facsimileView.currentMovementNumber >= facsimile.movements.size()) {
                            facsimileView.currentMovementNumber = facsimile.movements.get(facsimile.movements.size() - 1).number;
                        }
                        break;
                    case ADJUST_MOVEMENT:
                        final ArrayList<Measure> measuresToMove = new ArrayList<>();
                        if(measure != null) {
                            Movement currentMov = measure.movement;
                            for (int i = currentMov.measures.indexOf(measure); i < currentMov.measures.size(); i++) {
                                measuresToMove.add(currentMov.measures.get(i));
                            }
                        }

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
                        if (page.getMeasureAt(bitmapCoord.x, bitmapCoord.y) != null) {
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
                        trackLength += Math.abs(lastPolygonPoint.x - bitmapCoord.x) + Math.abs(lastPolygonPoint.y - bitmapCoord.y);
                        pointPath.add(bitmapCoord);
                        lastPolygonPoint = new PointF(touchX, touchY);
                        PointF firstPointInTouch = transformCoordBitmapToTouch(firstPoint.x, firstPoint.y); // due to scrolling this may be another position than initially stored in firstPoint
                        double distanceToFirstPoint = Math.sqrt((double) (touchX - firstPointInTouch.x) * (touchX - firstPointInTouch.x) + (touchY - firstPointInTouch.y) * (touchY - firstPointInTouch.y));
                        if (distanceToFirstPoint < 20.0f && trackLength > 100.0f) {
                            Measure newMeasure = new Measure();
                            List<float[]> vertices = new ArrayList<>();
                            for(PointF vertex: pointPath) {
                                vertices.add(new float[]{vertex.x, vertex.y});
                            }
                            newMeasure.zone.setVertices(vertices);
                            switch (facsimile.meiType) {
                                case CANONICAL:
                                    newMeasure.zone.convertToCanonical();
                                    break;
                                case EXTENDED:
                                    newMeasure.zone.convertToExtended();
                                    break;
                                case POLYGONAL:
                                    newMeasure.zone.convertToPolygonal();
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
        lastPoint = transformCoordTouchToBitmap(touchX, touchY);
        facsimileView.needToSave = true;
        return true;
    } // end onTouchEvent

    /**
     * Transforms the coordinates from bitmap coordinate system to the touch coordinate system.
     * @param x x coordinate
     * @param y y coordinate
     * @return point in touch coordinate system
     */
    PointF transformCoordBitmapToTouch(float x, float y) {
        PointF point = new PointF();
        point.x = x;
        point.y = y;
        return sourceToViewCoord(point);
    }

    /**
     * Transforms the coordinates from touch coordinate system to the bitmap coordinate system.
     * @param x x coordinate
     * @param y y coordinate
     * @return point in bitmap coordinate system
     */
    PointF transformCoordTouchToBitmap(float x, float y) {
        PointF point = new PointF();
        point.x = x;
        point.y = y;
        return viewToSourceCoord(point);
    }
}
