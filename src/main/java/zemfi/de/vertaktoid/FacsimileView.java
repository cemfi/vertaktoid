package zemfi.de.vertaktoid;

import android.app.Dialog;
import android.content.Context;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ArrayAdapter;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.io.File;
import java.util.ArrayList;

/**
 * Contains the presentation and user interaction functions. Directs the UI layouts.
 * Extends the SubsamplingScaleImageView class.
 */

public class FacsimileView extends SubsamplingScaleImageView {

    CommandManager commandManager;

    /**
     * Constructor
     * @param context android application context
     * @param attr attributes
     */
    public FacsimileView(Context context, AttributeSet attr) {
        super(context, attr);
        init();
        commandManager = new CommandManager();
    }

    /**
     * Constructor
     * @param context android application context
     */
    public FacsimileView(Context context) {
        super(context);
        init();
        commandManager = new CommandManager();
    }

    //drawing path
    private Path drawPath;
    private ArrayList<PointF> pointPath;
    private Paint drawPaint;
    private Paint largeBoldText = new Paint();
    private Paint smallBoldText = new Paint();
    private Paint whiteAlpha = new Paint();
    private Rect pageNameRect = new Rect();
    private Rect measureNameRect = new Rect();
    private Rect movementNameRect = new Rect();
    private ArrayList<HSLColor> movementColors;
    HSLColor fillColor;
    private float s = 100f;
    private float l = 30f;
    private float a = 1f;
    private float currentBrushSize = 5;
    public int horOverlapping = 0;
    Path grayPath;
    Path polygonHoverPath;
    protected Facsimile document;
    public final ObservableInt pageNumber = new ObservableInt(-1);
    int currentMovementNumber = 0;
    public final ObservableField<String> currentPath = new ObservableField<>();
    public final ObservableInt maxPageNumber = new ObservableInt(0);
    public boolean needToSave = false;
    public enum Action {DRAW, ERASE, ADJUST_MEASURE, CUT, ADJUST_MOVEMENT}
    Action nextAction = Action.DRAW;
    float downX = 0.0f;
    float downY = 0.0f;
    float leftMost = -1.0f;
    float topMost = -1.0f;
    float rightMost = -1.0f;
    float bottomMost = -1.0f;
    PointF firstPoint;
    PointF lastPolygonPoint;
    boolean isFirstPoint = true;
    PointF lastPoint = null; // in bitmap coordinates
    float trackLength;

    /**
     * Initialization
     */
    private void init() {
        movementColors = new ArrayList<>();
        grayPath = new Path();
        drawPath = new Path();
        pointPath = new ArrayList<>();
        polygonHoverPath = new Path();
        drawPaint = new Paint();
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(currentBrushSize);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
        pageNumber.set(0);

        setOnHoverListener(new View.OnHoverListener() {
            @Override
            public boolean onHover(View v, MotionEvent event) {
                if (nextAction == Action.DRAW || nextAction == Action.CUT) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_HOVER_ENTER:
                            if (!isFirstPoint) {
                                polygonHoverPath.reset();
                                PointF lastPoint = pointPath.get(pointPath.size() - 1);
                                PointF lastPointTouch = transformCoordBitmapToTouch(lastPoint.x, lastPoint.y);
                                polygonHoverPath.moveTo(lastPointTouch.x, lastPointTouch.y);
                                polygonHoverPath.lineTo(event.getX(), event.getY());
                            }
                            invalidate();
                            break;
                        case MotionEvent.ACTION_HOVER_MOVE:
                            if (!isFirstPoint) {
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
     * Stores the instance state.
     * @return parcelable object
     */
    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("instanceState", super.onSaveInstanceState());
        bundle.putSerializable("document", document);
        bundle.putInt("pageNumber", pageNumber.get());
        bundle.putInt("currentMovementNumber", currentMovementNumber);
        bundle.putInt("horOverlapping", horOverlapping);
        bundle.putSerializable("history", commandManager);
        return bundle;
    }

    /**
     * Restores the instance state.
     * @param state parcelable object
     */
    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            document = (Facsimile) bundle.getSerializable("document");
            if(document == null) {
                return;
            }
            pageNumber.set(bundle.getInt("pageNumber"));
            currentMovementNumber = bundle.getInt("currentMovementNumber");
            horOverlapping = bundle.getInt("horOverlapping");
            setPage(pageNumber.get());
            commandManager = (CommandManager) bundle.getSerializable("history");
            maxPageNumber.set(document.pages.size());
            currentPath.set(document.dir.getPath());
            HSLColorsGenerator.resetHueToDefault();
            super.onRestoreInstanceState(bundle.getParcelable("instanceState"));
            return;
        }

        super.onRestoreInstanceState(state);
    }

    /**
     * Cleans the current drawing path
     */
    void clean() {
        pointPath.clear();
        isFirstPoint = true;
    }

    /**
     * Sets the current page to giving number
     * @param page page number
     */
    public void setPage(int page) {
        if(document == null) {
            return;
        }
        if (page >= 0 && page < document.pages.size()) {
            pageNumber.set(page);
            setImage(findImageForPage(pageNumber.get()));
        }
    }

    public void adjustPageNavigation() {
        if(document == null) {
            return;
        }
        if(pageNumber.get() < document.pages.size() - 1) {
            menu.findItem(R.id.action_plus).setIcon(R.drawable.arrowright_on);
        } else {
            menu.findItem(R.id.action_plus).setIcon(R.drawable.arrowright_off);
        }
        if(pageNumber.get() > 0) {
            menu.findItem(R.id.action_minus).setIcon(R.drawable.arrowleft_on);
        } else {
            menu.findItem(R.id.action_minus).setIcon(R.drawable.arrowleft_off);
        }
    }

    public void adjustHistoryNavigation() {
        if(commandManager.getUndoStackSize() > 0) {
            menu.findItem(R.id.action_undo).setIcon(R.drawable.undo_on);
        } else {
            menu.findItem(R.id.action_undo).setIcon(R.drawable.undo_off);
        }
        if(commandManager.getRedoStackSize() > 0) {
            menu.findItem(R.id.action_redo).setIcon(R.drawable.redo_on);
        } else {
            menu.findItem(R.id.action_redo).setIcon(R.drawable.redo_off);
        }
    }

    /**
     * Changes to the next page.
     */
    public void nextPageClicked() {
        int newPageNumber = pageNumber.get() + 1;
        if(newPageNumber < document.pages.size() && newPageNumber >= 0) {
            pageNumber.set(newPageNumber);
            clean();
            setPage(newPageNumber);
            adjustPageNavigation();
        }
    }

    /**
     * Changes to the previous page
     */
    public void prevPageClicked() {
        int newPageNumber = pageNumber.get() - 1;
        if(newPageNumber >= 0) {
            pageNumber.set(newPageNumber);
            clean();
            setPage(newPageNumber);
            adjustPageNavigation();
        }
    }

    /**
     * Shows dialog and obtains the user defined next page number. Changes to the page with giving number.
     */
    public  void gotoClicked(){
        resetState();
        final Dialog gotoDialog = new Dialog(getContext());
        Window window = gotoDialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.TOP;
        window.setAttributes(wlp);
        gotoDialog.setContentView(R.layout.dialog_goto);
        gotoDialog.setTitle(R.string.dialog_goto_titel);
        TextView gotoPageLabel = (TextView) gotoDialog.findViewById(R.id.dialog_goto_page_label);
        gotoPageLabel.setText(R.string.dialog_goto_page_label);
        final EditText gotoPageInput = (EditText) gotoDialog.findViewById(R.id.dialog_goto_page_input);
        gotoPageInput.setHint("1 - " + (document.pages.size()));
        gotoPageInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        Button gotoButtonNegative = (Button) gotoDialog.findViewById(R.id.dialog_goto_button_negative);
        gotoButtonNegative.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoDialog.cancel();
            }
        });

        Button gotoButtonPositive = (Button) gotoDialog.findViewById(R.id.dialog_goto_button_positive);
        gotoButtonPositive.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int newPageNumber = Integer.parseInt(gotoPageInput.getText().toString()) - 1;
                    if(newPageNumber >= 0 && newPageNumber < document.pages.size()) {
                        pageNumber.set(newPageNumber);
                        clean();
                        setPage(newPageNumber);
                    }
                }
                catch (NumberFormatException e) {

                }
                adjustPageNavigation();
                invalidate();
                gotoDialog.dismiss();
            }
        });

        gotoDialog.show();
    }

    /*
    Shows the settings dialog and process the user input.
    Currently obtains the horizontal overlapping parameter.
     */
    public void settingsClicked() {
        resetState();
        final Dialog settingsDialog = new Dialog(getContext());
        Window window = settingsDialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.TOP;
        window.setAttributes(wlp);
        settingsDialog.setContentView(R.layout.dialog_settings);
        settingsDialog.setTitle(R.string.dialog_settings_titel);
        TextView settingsHoroverLabel = (TextView) settingsDialog.findViewById(R.id.dialog_settings_horover_label);
        settingsHoroverLabel.setText(R.string.dialog_settings_horover_label);
        final EditText settingsHoroverInput = (EditText) settingsDialog.findViewById(R.id.dialog_settings_horover_input);
        settingsHoroverInput.setHint("" + horOverlapping);
        settingsHoroverInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        final RadioGroup settingsHoroverType = (RadioGroup) settingsDialog.findViewById(R.id.dialog_settings_horover_type);
        settingsHoroverType.check(R.id.dialog_settings_horover_type_points);
        Button gotoButtonNegative = (Button) settingsDialog.findViewById(R.id.dialog_settings_button_negative);
        gotoButtonNegative.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                settingsDialog.cancel();
            }
        });
        TextView settingsUndosizeLabel = (TextView) settingsDialog.findViewById(R.id.dialog_settings_undosize_label);
        settingsUndosizeLabel.setText(R.string.dialog_settings_undosize_label);
        final EditText settingsUndosizeInput = (EditText) settingsDialog.findViewById(R.id.dialog_settings_undosize_input);
        settingsUndosizeInput.setHint("" + commandManager.getHistoryMaxSize());
        settingsUndosizeInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        Button gotoButtonPositive = (Button) settingsDialog.findViewById(R.id.dialog_settings_button_positive);
        gotoButtonPositive.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String historyMaxSize = settingsUndosizeInput.getText().toString();
                    if(!historyMaxSize.equals("")) {
                        commandManager.setHistoryMaxSize(Integer.parseInt(settingsUndosizeInput.getText().toString()));
                    }
                    String horover = settingsHoroverInput.getText().toString();
                    if(!horover.equals("")) {
                        if (settingsHoroverType.getCheckedRadioButtonId() == R.id.dialog_settings_horover_type_points) {
                            horOverlapping = Integer.parseInt(settingsHoroverInput.getText().toString());
                        } else if (settingsHoroverType.getCheckedRadioButtonId() == R.id.dialog_settings_horover_type_percents) {
                            float percent = Float.parseFloat(settingsHoroverInput.getText().toString());
                            if (percent > 100) {
                                percent = percent % 100;
                            }
                            horOverlapping = Math.round(document.pages.get(pageNumber.get()).imageWidth * percent / 100);
                        } else {
                            horOverlapping = 0;
                        }
                    }
                }
                catch (NumberFormatException e) {
                }
                invalidate();
                settingsDialog.dismiss();
            }
        });

        settingsDialog.show();
    }

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

    /**
     * Searches the image file of selected page in the opened directory. Shows dummy page if not found.
     * @param pagenum page number
     * @return image source
     */
    ImageSource findImageForPage(int pagenum) {
        File appSubFolder = new File(document.dir, Vertaktoid.APP_SUBFOLDER);
        File stubImg = new File(appSubFolder, Vertaktoid.NOT_FOUND_STUBIMG);
        if(document.pages.size() < pagenum || pagenum < 0) {
            return null;
        }
        if(!document.pages.get(pagenum).imageFile.exists()) {
            return ImageSource.uri(Uri.fromFile(stubImg));
        }
        else {
            return ImageSource.uri(Uri.fromFile(document.pages.get(pagenum).imageFile));
        }
    }


    /**
     * Sets the current facsimile
     * @param facsimile facsimile
     */
    public void setFacsimile(Facsimile facsimile) {
        this.document = facsimile;
        pageNumber.set(0);
        currentMovementNumber = document.movements.size() - 1;
        setImage(findImageForPage(0));
        maxPageNumber.set(document.pages.size());
        currentPath.set(document.dir.getPath());
        horOverlapping = 0;
    }

    /**
     * Gets current facsimile.
     * @return facsimile
     */
    public Facsimile getFacsimile() {
        return this.document;
    }


    /**
     * Rendering function
     * @param canvas canvas
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (document == null || document.pages.size() == 0) {
            return;
        }


        largeBoldText.setColor(Color.BLACK);
        largeBoldText.setTextSize(50);
        largeBoldText.setFakeBoldText(true);

        smallBoldText.setColor(Color.BLACK);
        smallBoldText.setTextSize(36);
        smallBoldText.setFakeBoldText(true);

        int colorsToGenerate = document.movements.size() - movementColors.size();
        movementColors.addAll(HSLColorsGenerator.generateColorSet(colorsToGenerate, s, l, a));

        if (!document.pages.get(pageNumber.get()).imageFile.exists()) {

            largeBoldText.getTextBounds(document.pages.get(pageNumber.get()).imageFile.getName(), 0,
                    document.pages.get(pageNumber.get()).imageFile.getName().length(), pageNameRect);
            canvas.drawText("" + document.pages.get(pageNumber.get()).imageFile.getName(),
                    (this.getRight() - this.getLeft()) / 2  - pageNameRect.centerX(), 100,  largeBoldText);
            return;
        }

        int i;
        drawPath.reset();
        Page page = document.pages.get(pageNumber.get());
        for (i = 0; i < page.measures.size(); i++) {
            Measure measure = page.measures.get(i);
            int index = document.movements.indexOf(measure.movement);
            if(index < 0) {
                document.movements.add(0, measure.movement);
            }
            largeBoldText.setColor(HSLColor.toRGB(movementColors.get(
                    document.movements.indexOf(measure.movement))));
            smallBoldText.setColor(HSLColor.toRGB(movementColors.get(
                    document.movements.indexOf(measure.movement))));

            PointF topLeft = transformCoordBitmapToTouch(measure.left, measure.top);
            PointF bottomRight = transformCoordBitmapToTouch(measure.right, measure.bottom);
            if (topLeft == null) {
                // still loading image
                return;
            }
            drawPath.addRect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y, Path.Direction.CW);

            drawPaint.setStyle(Paint.Style.FILL);
            fillColor = new HSLColor();
            fillColor.a = 0.1f;
            fillColor.h = movementColors.get(
                    document.movements.indexOf(measure.movement)).h;
            fillColor.s = s;
            fillColor.l = l;
            drawPaint.setColor(HSLColor.toARGB(fillColor));
            canvas.drawPath(drawPath, drawPaint);

            drawPaint.setStyle(Paint.Style.STROKE);
            drawPaint.setColor(HSLColor.toRGB(movementColors.get(
                    document.movements.indexOf(measure.movement))));
            canvas.drawPath(drawPath, drawPaint);

            drawPath.reset();

            whiteAlpha.setColor(0x55ffffff);
            whiteAlpha.setStyle(Paint.Style.FILL);

            String measureLabel = measure.manualSequenceNumber != null ?
                    "" + measure.manualSequenceNumber : "" + measure.sequenceNumber;

            String movementLabel = measure.movement.getName() + " >>";

            largeBoldText.getTextBounds(measureLabel, 0, measureLabel.length(), measureNameRect);
            smallBoldText.getTextBounds(movementLabel, 0, movementLabel.length(), movementNameRect);

            float leftTextBox = (topLeft.x + bottomRight.x) / 2 - measureNameRect.centerX() - 5;
            float topTextBox = topLeft.y + 50 - measureNameRect.height();
            float rightTextBox = (topLeft.x + bottomRight.x) / 2 + measureNameRect.centerX() + 5;
            float bottomTextBox = topLeft.y + 50;

            //canvas.drawRect(leftTextBox, topTextBox, rightTextBox, bottomTextBox, whiteAlpha);
            if(measure.manualSequenceNumber != null) {
                canvas.drawRect(leftTextBox, topTextBox, rightTextBox, bottomTextBox, drawPaint);
            }

            canvas.drawText(measureLabel, (topLeft.x + bottomRight.x) / 2 - measureNameRect.centerX(), topLeft.y + 50, largeBoldText);
            if(measure.movement.measures.indexOf(measure) == 0) {
                canvas.drawText(movementLabel, (topLeft.x + bottomRight.x) / 2 - movementNameRect.centerX(), topLeft.y + 100, smallBoldText);

            }
        }

        drawPath.reset();
        for (i = 0; i < pointPath.size(); i++) {
            PointF bitmapCoord = pointPath.get(i);
            PointF touchCoord = transformCoordBitmapToTouch(bitmapCoord.x, bitmapCoord.y);
            if (i == 0) {
                drawPath.addCircle(touchCoord.x, touchCoord.y, 10, Path.Direction.CW);
                drawPath.moveTo(touchCoord.x, touchCoord.y);
            } else {
                drawPath.lineTo(touchCoord.x, touchCoord.y);
            }
        }
        canvas.drawPath(drawPath, drawPaint);
        canvas.drawPath(polygonHoverPath, drawPaint);
    }

    /**
     * Menu entry "brush" clicked.
     */
    public void brushClicked() {
        resetState();
    }

    /**
     * Menu entry "erase" clicked.
     */
    public void eraseClicked() {
        resetState();
        nextAction = Action.ERASE;
    }

    /**
     * Menu entry "type"
     */
    public void typeClicked() {
        resetState();
        nextAction = Action.ADJUST_MEASURE;
    }

    /**
     * Menu entry "cut" clicked.
     */
    public void cutClicked() {
        resetState();
        nextAction = Action.CUT;
    }

    /**
     * Menu entry "movement" clicked.
     */
    public void movementClicked(){
        resetState();
        nextAction = Action.ADJUST_MOVEMENT;
    }

    public void undoClicked() {
        int pageIndex = commandManager.undo();
        if(pageIndex != -1 && pageIndex != pageNumber.get()) {
            pageNumber.set(pageIndex);
            clean();
            setPage(pageIndex);
        }
        adjustHistoryNavigation();
        invalidate();
    }

    public void redoClicked() {
        int pageIndex = commandManager.redo();
        if(pageIndex != -1 && pageIndex != pageNumber.get()) {
            pageNumber.set(pageIndex);
            clean();
            setPage(pageIndex);
        }
        adjustHistoryNavigation();
        invalidate();
    }

    /**
     * Reset current menu state to default "brush" entry
     */
    void resetState() {
        isFirstPoint = true;
        nextAction = Action.DRAW;
        pointPath = new ArrayList<>();
        invalidate();
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
        if(document == null) {

            return false;
        }
        float touchX = event.getX();
        float touchY = event.getY();
        final Page currentPage = document.pages.get(pageNumber.get());
        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER) {
            return super.onTouchEvent(event);

        }
        PointF bitmapCoord = transformCoordTouchToBitmap(touchX, touchY);
        final ArrayList<Measure> measures = currentPage.getMeasuresAt(bitmapCoord.x, bitmapCoord.y);
        final Measure measure = currentPage.getMeasureAt(bitmapCoord.x, bitmapCoord.y);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                switch (nextAction) {
                    case ERASE:
                        if(measures.size() > 0) {
                            commandManager.processRemoveMeasuresCommand(measures, document);
                            invalidate();
                        }
                        break;
                    case CUT:
                        if (measure == null) {
                            resetState();
                            resetMenu();
                            // continue and handle the ActionId as a click in brush state
                        } else {
                            Measure mleft = new Measure(measure.left, measure.top,
                                    bitmapCoord.x + horOverlapping, measure.bottom);
                            Measure mright = new Measure(bitmapCoord.x - horOverlapping, measure.top,
                                    measure.right, measure.bottom);
                            commandManager.processCutMeasureCommand(document, measure, mleft, mright);
                            // do not continue
                        }
                        break;
                    case DRAW:
                        if (isFirstPoint) {
                            pointPath = new ArrayList<>();
                            firstPoint = bitmapCoord;
                            pointPath.add(bitmapCoord);
                            leftMost = bitmapCoord.x;
                            rightMost = bitmapCoord.x;
                            topMost = bitmapCoord.y;
                            bottomMost = bitmapCoord.y;
                            isFirstPoint = false;
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
                switch (nextAction) {
                    case ERASE:
                        ArrayList<Measure> toRemove = currentPage.getMeasuresAtSegment
                                (bitmapCoord.x, bitmapCoord.y, lastPoint.x, lastPoint.y);
                        if(toRemove.size() > 0) {
                            commandManager.processRemoveMeasuresCommand(toRemove, document);
                            invalidate();
                        }
                        break;
                    case DRAW:
                        if (!isFirstPoint) {
                            trackLength += Math.abs(lastPolygonPoint.x - bitmapCoord.x) + Math.abs(lastPolygonPoint.y - bitmapCoord.y);
                            leftMost = bitmapCoord.x < leftMost ? bitmapCoord.x : leftMost;
                            rightMost = bitmapCoord.x > rightMost ? bitmapCoord.x : rightMost;
                            topMost = bitmapCoord.y < topMost ? bitmapCoord.y : topMost;
                            bottomMost = bitmapCoord.y > bottomMost ? bitmapCoord.y : bottomMost;
                            pointPath.add(bitmapCoord);
                            lastPolygonPoint = bitmapCoord;
                            invalidate();
                        }
                        break;
                }
                break;

            case MotionEvent.ACTION_UP:
                if (event.getToolType(0) != MotionEvent.TOOL_TYPE_FINGER) {
                    switch (nextAction) {
                        case ERASE:
                            if (measures.size() > 0) {
                                commandManager.processRemoveMeasuresCommand(measures, document);
                                invalidate();
                            }
                            if(currentMovementNumber >= document.movements.size()) {
                                currentMovementNumber = document.movements.get(document.movements.size() - 1).number;
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
                            for(Movement movement : document.movements) {
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
                                    commandManager.processAdjustMovementCommand(document, measure, option, labelStr,
                                            getResources().getString(R.string.dialog_editmo_spinner_optelse),
                                            getResources().getString(R.string.dialog_editmo_spinner_optdef));
                                    currentMovementNumber = document.movements.indexOf(measure.movement);
                                    editMODialog.dismiss();
                                    adjustHistoryNavigation();
                                    invalidate();
                                }
                            });
                            editMODialog.show();
                            break;
                        case ADJUST_MEASURE:
                            if (currentPage.getMeasureAt(bitmapCoord.x, bitmapCoord.y) != null) {
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
                                        commandManager.processAdjustMeasureCommand(document, measure, manualSequenceNumber, rest);
                                        editMEDialog.dismiss();
                                        adjustHistoryNavigation();
                                        invalidate();
                                    }
                                });

                                editMEDialog.show();
                            }
                            break;
                        case DRAW:
                            trackLength += Math.abs(lastPolygonPoint.x - bitmapCoord.x) + Math.abs(lastPolygonPoint.y - bitmapCoord.y);
                            leftMost = bitmapCoord.x < leftMost ? bitmapCoord.x : leftMost;
                            rightMost = bitmapCoord.x > rightMost ? bitmapCoord.x : rightMost;
                            topMost = bitmapCoord.y < topMost ? bitmapCoord.y : topMost;
                            bottomMost = bitmapCoord.y > bottomMost ? bitmapCoord.y : bottomMost;
                            pointPath.add(bitmapCoord);
                            lastPolygonPoint = new PointF(touchX, touchY);
                            PointF firstPointInTouch = transformCoordBitmapToTouch(firstPoint.x, firstPoint.y); // due to scrolling this may be another position than initially stored in firstPoint
                            double distance = Math.sqrt((double) (touchX - firstPointInTouch.x) * (touchX - firstPointInTouch.x) + (touchY - firstPointInTouch.y) * (touchY - firstPointInTouch.y));
                            if (distance < 20.0f && trackLength > 100.0f) {
                                if ((rightMost - leftMost > 50) && (bottomMost - topMost > 50)) {
                                    Measure newMeasure = new Measure(leftMost, topMost, rightMost, bottomMost);
                                    if(currentMovementNumber > document.movements.size() - 1) {
                                        currentMovementNumber = document.movements.size() - 1;
                                    }
                                    commandManager.processCreateMeasureCommand(newMeasure, document, currentPage);
                                    invalidate();
                                }
                                pointPath = new ArrayList<>();
                                isFirstPoint = true;
                                invalidate();
                            }
                    }
                }
        } // end switch
        adjustHistoryNavigation();
        lastPoint = transformCoordTouchToBitmap(touchX, touchY);
        needToSave = true;
        return true;
    } // end onTouchEvent


    Menu menu;

    /**
     * Setter for menu.
     * @param menu menu
     */
    public void setMenu(Menu menu) {
        this.menu = menu;
    }

    /**
     * Reset menu icons.
     */
    void resetMenu() {
        for (int i = 0; i < menu.size(); i++) {
            // Set default icons
            if (menu.getItem(i).getItemId() == R.id.action_erase) {
                menu.getItem(i).setIcon(R.drawable.eraser_off);
            } else if (menu.getItem(i).getItemId() == R.id.action_type) {
                menu.getItem(i).setIcon(R.drawable.textbox_off);
            } else if (menu.getItem(i).getItemId() == R.id.action_cut) {
                menu.getItem(i).setIcon(R.drawable.cut_off);
            } else if (menu.getItem(i).getItemId() == R.id.action_brush) {
                menu.getItem(i).setIcon(R.drawable.brush_on);
            }
        }
    }

}
