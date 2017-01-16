package zemfi.de.vertaktoid;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ArrayAdapter;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by aristotelis on 23.08.16.
 */

public class FacsimileView extends SubsamplingScaleImageView {

    public FacsimileView(Context context, AttributeSet attr) {
        super(context, attr);
        init();
    }

    public FacsimileView(Context context) {
        super(context);
        init();
    }

    //drawing path
    private Path drawPath;
    private ArrayList<PointF> pointPath;
    private Paint drawPaint;
    private ArrayList<HSLColor> movementColors;
    private float s = 100f;
    private float l = 30f;
    private float a = 1f;
    private float currentBrushSize = 5;
    Path grayPath;
    Path polygonHoverPath;
    protected Facsimile document;
    public final ObservableInt pageNumber = new ObservableInt(-1);
    int currentMovementNumber = 0;
    public final ObservableField<String> currentPath = new ObservableField<>();
    public final ObservableInt maxPageNumber = new ObservableInt(0);
    public boolean needToSave = false;
    public enum Action {DRAW, ERASE, TYPE, CUT, MOVEMENT}
    Action nextAction = Action.DRAW;

    private void init() {
        //HSLColorsGenerator.resetHueToRandom();
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


    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("instanceState", super.onSaveInstanceState());
        bundle.putSerializable("document", document);
        bundle.putInt("pageNumber", pageNumber.get());
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            document = (Facsimile) bundle.getSerializable("document");
            pageNumber.set(bundle.getInt("pageNumber"));
            setPage(pageNumber.get());
            super.onRestoreInstanceState(bundle.getParcelable("instanceState"));
            maxPageNumber.set(document.pages.size());
            currentPath.set(document.dir.getPath());
            return;
        }

        super.onRestoreInstanceState(state);
    }

    void clean() {
        pointPath.clear();
        isFirstPoint = true;
    }

    public void setPage(int page) {
        if(document == null) {
            return;
        }
        if (page >= 0 && page < document.pages.size()) {
            this.pageNumber.set(page);
            setImage(findImageForPage(pageNumber.get()));
        }
    }

    public void plusClicked() {
        int newPageNumber = pageNumber.get() + 1;
        if(newPageNumber < document.pages.size() && newPageNumber >= 0) {
            pageNumber.set(newPageNumber);
            clean();
            setPage(newPageNumber);
        }
    }

    public void minusClicked() {
        int newPageNumber = pageNumber.get() - 1;
        if(newPageNumber >= 0) {
            pageNumber.set(newPageNumber);
            clean();
            setPage(newPageNumber);
        }
    }

    public  void gotoClicked(){
        resetState();
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Go to page");
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("1 - " + (document.pages.size() + 1));
        builder.setView(input);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    int newPageNumber = Integer.parseInt(input.getText().toString()) - 1;
                    if(newPageNumber >= 0 && newPageNumber < document.pages.size()) {
                        pageNumber.set(newPageNumber);
                        clean();
                        setPage(newPageNumber);
                    }
                }
                catch (NumberFormatException e) {

                }
                invalidate();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    PointF transformCoordBitmapToTouch(float x, float y) {
        PointF point = new PointF();
        point.x = x;
        point.y = y;
        return sourceToViewCoord(point);
    }

    PointF transformCoordTouchToBitmap(float x, float y) {
        PointF point = new PointF();
        point.x = x;
        point.y = y;
        return viewToSourceCoord(point);
    }

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

    public void setFacsimile(Facsimile facsimile) {
        this.document = facsimile;
        pageNumber.set(0);
        setImage(findImageForPage(0));
        maxPageNumber.set(document.pages.size());
        currentPath.set(document.dir.getPath());
    }

    public Facsimile getFacsimile() {
        return this.document;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (document == null || document.pages.size() == 0) {
            return;
        }

        int colorsToGenerate = document.movements.size() - movementColors.size();
        movementColors.addAll(HSLColorsGenerator.generateColorSet(colorsToGenerate, s, l, a));

        if (!document.pages.get(pageNumber.get()).imageFile.exists()) {

            Paint paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setTextSize(50);
            paint.setFakeBoldText(true);
            Rect rect = new Rect();

            paint.getTextBounds(document.pages.get(pageNumber.get()).imageFile.getName(), 0,
                    document.pages.get(pageNumber.get()).imageFile.getName().length(), rect);
            canvas.drawText("" + document.pages.get(pageNumber.get()).imageFile.getName(),
                    (this.getRight() - this.getLeft()) / 2  - rect.centerX(), 100,  paint);
            return;
        }

        int i;
        drawPath.reset();
        Page page = document.pages.get(pageNumber.get());
        for (i = 0; i < page.measures.size(); i++) {
            Measure measure = page.measures.get(i);
            drawPaint.setColor(HSLColor.toRGB(movementColors.get(
                    document.movements.indexOf(measure.movement))));
            PointF topLeft = transformCoordBitmapToTouch(measure.left, measure.top);
            PointF bottomRight = transformCoordBitmapToTouch(measure.right, measure.bottom);
            if (topLeft == null) {
                // still loading image
                return;
            }
            drawPath.addRect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y, Path.Direction.CW);
            canvas.drawPath(drawPath, drawPaint);
            drawPath.reset();

            Paint largeBoldText = new Paint();
            largeBoldText.setColor(HSLColor.toRGB(movementColors.get(
                    document.movements.indexOf(measure.movement))));
            largeBoldText.setTextSize(50);
            largeBoldText.setFakeBoldText(true);

            Rect measureNameRect = new Rect();

            Paint whiteAlpha = new Paint();
            whiteAlpha.setColor(0x55ffffff);
            whiteAlpha.setStyle(Paint.Style.FILL);

            String measureLabel = measure.manualSequenceNumber != null ?
                    "" + measure.manualSequenceNumber : "" + measure.sequenceNumber;
            if(measure.movement.measures.indexOf(measure) == 0) {
                measureLabel += ", mdiv " + measure.movement.number;
            }

            largeBoldText.getTextBounds(measureLabel, 0, measureLabel.length(), measureNameRect);

            float leftTextBox = (topLeft.x + bottomRight.x) / 2 - measureNameRect.centerX() - 5;
            float topTextBox = topLeft.y + 50 - measureNameRect.height();
            float rightTextBox = (topLeft.x + bottomRight.x) / 2 + measureNameRect.centerX() + 5;
            float bottomTextBox = topLeft.y + 50;
            //canvas.drawRect(leftTextBox, topTextBox, rightTextBox, bottomTextBox, whiteAlpha);
            if(measure.manualSequenceNumber != null) {
                canvas.drawRect(leftTextBox, topTextBox, rightTextBox, bottomTextBox, drawPaint);
            }

            canvas.drawText(measureLabel, (topLeft.x + bottomRight.x) / 2 - measureNameRect.centerX(), topLeft.y + 50, largeBoldText);

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



    public void brushClicked() {
        resetState();
    }

    public void eraseClicked() {
        resetState();
        nextAction = Action.ERASE;
    }

    public void typeClicked() {
        resetState();
        nextAction = Action.TYPE;
    }


    public void cutClicked() {
        resetState();
        nextAction = Action.CUT;
    }

    public  void movementClicked(){
        resetState();
        nextAction = Action.MOVEMENT;
    }

    void resetState() {
        isFirstPoint = true;
        nextAction = Action.DRAW;
        pointPath = new ArrayList<>();
        invalidate();
    }



    // Remember last point position for dragging
    private PointF last = new PointF();
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        PointF curr = new PointF(event.getX(), event.getY());
        float touchX = event.getX();
        float touchY = event.getY();
        final Page currentPage = document.pages.get(pageNumber.get());
        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER) {
            //if (isFirstPoint) {
                // do not allow navigation while entering the polygon
                return super.onTouchEvent(event);
            //}
            //return true;
        }
        PointF bitmapCoord = transformCoordTouchToBitmap(touchX, touchY);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                switch (nextAction) {
                    case ERASE:
                        ArrayList<Measure> measures = currentPage.getMeasuresAt(bitmapCoord.x, bitmapCoord.y);
                        if (measures.size() == 0) {
                            //resetState();
                            //resetMenu();
                            // continue and handle the ActionId as a click in brush state
                        } else {
                            final PointF p = transformCoordTouchToBitmap(touchX, touchY);
                            document.removeMeasures(measures);
                            ArrayList<Movement> changedMovements = new ArrayList<>();
                            for (Measure measure : measures) {
                                if (!changedMovements.contains(measure.movement)) {
                                    changedMovements.add(measure.movement);
                                }
                            }

                            for (Movement movement : changedMovements) {
                                document.resort(movement, currentPage);
                            }
                            invalidate();
                        }
                        break;
                    case CUT:
                        Measure measure = currentPage.getMeasureAt(bitmapCoord.x, bitmapCoord.y);
                        if (measure == null) {
                            resetState();
                            resetMenu();
                            // continue and handle the ActionId as a click in brush state
                        } else {
                            Measure mleft = new Measure(measure.left, measure.top, bitmapCoord.x, measure.bottom);
                            Measure mright = new Measure(bitmapCoord.x, measure.top, measure.right, measure.bottom);
                            document.removeMeasureAt(bitmapCoord.x, bitmapCoord.y, currentPage);
                            document.addMeasure(mleft, measure.movement, currentPage);
                            document.addMeasure(mright, measure.movement, currentPage);
                            document.resort(measure.movement, currentPage);
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
                        if(document.removeMeasuresAt(bitmapCoord.x, bitmapCoord.y, lastPoint.x, lastPoint.y, currentPage)) {
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
                            ArrayList<Measure> measures = currentPage.getMeasuresAt(bitmapCoord.x, bitmapCoord.y);
                            if (measures.size() > 0) {
                                document.removeMeasures(measures);
                                ArrayList<Movement> changedMovements = new ArrayList<>();
                                for (Measure measure : measures) {
                                    if (!changedMovements.contains(measure.movement)) {
                                        changedMovements.add(measure.movement);
                                    }
                                }

                                for (Movement movement : changedMovements) {
                                    document.resort(movement, currentPage);
                                }
                                invalidate();
                            }
                            break;
                        case MOVEMENT:
                            final Measure measureToMove = currentPage.getMeasureAt(bitmapCoord.x, bitmapCoord.y);
                            final ArrayList<Measure> measuresToMove = new ArrayList<>();
                            if(measureToMove != null) {
                                Movement currentMov = measureToMove.movement;
                                for (int i = currentMov.measures.indexOf(measureToMove); i < currentMov.measures.size(); i++) {
                                    measuresToMove.add(currentMov.measures.get(i));
                                }
                            }
                            final AlertDialog.Builder moBuilder = new AlertDialog.Builder(getContext());
                            moBuilder.setTitle("Set Movement Anchor");
                            TextView moheader1 = new TextView(getContext());
                            moheader1.setText("Select existing movement");
                            final Spinner movSpinner = new Spinner(getContext());
                            ArrayList<String> movementOptions = new ArrayList();
                            movementOptions.add("no action");
                            movementOptions.add("create new");
                            for(Movement movement : document.movements) {
                                movementOptions.add(movement.getName());
                            }
                            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_dropdown_item, movementOptions);
                            movSpinner.setAdapter(adapter);
                            movSpinner.setSelection(0);
                            TextView moheader2 = new TextView(getContext());
                            moheader2.setText("Label for movement");
                            final EditText label = new EditText(getContext());
                            label.setHint("optional");

                            LinearLayout moLayout = new LinearLayout(getContext());
                            moLayout.setOrientation(LinearLayout.VERTICAL);
                            moLayout.addView(moheader1);
                            moLayout.addView(movSpinner);
                            moLayout.addView(moheader2);
                            moLayout.addView(label);
                            moBuilder.setView(moLayout);
                            moBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            String option = movSpinner.getSelectedItem().toString();
                                            String labelStr = label.getText().toString();
                                            if(option.equals("no action")) {
                                                return;
                                            }
                                            if(option.equals("create new")) {
                                                Movement newMovement = new Movement();
                                                newMovement.number = document.movements.get
                                                        (document.movements.size() - 1).number + 1;
                                                newMovement.label = labelStr;
                                                document.movements.add(newMovement);
                                                currentMovementNumber = document.movements.indexOf(newMovement);
                                                if(measureToMove != null) {
                                                    for(Measure measure : measuresToMove) {
                                                        measure.changeMovement(newMovement);
                                                    }
                                                    document.resort(measureToMove.movement, measureToMove.page);
                                                    document.cleanMovements();
                                                }
                                            }
                                            else {
                                                Movement oldMovement = null;
                                                for(Movement movement : document.movements) {
                                                    if(movement.getName().equals(option)) {
                                                        oldMovement = movement;
                                                        break;
                                                    }
                                                }
                                                if(oldMovement != null) {
                                                    oldMovement.label = labelStr;
                                                    currentMovementNumber = document.movements.indexOf(oldMovement);
                                                    if(measureToMove != null) {
                                                        for(Measure measure : measuresToMove) {
                                                            measure.changeMovement(oldMovement);
                                                        }
                                                        document.resort(measureToMove.movement, measureToMove.page);
                                                        document.cleanMovements();
                                                    }
                                                }
                                            }
                                        }

                            });
                            moBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                            moBuilder.show();


                            break;
                        case TYPE:
                            if (currentPage.getMeasureAt(bitmapCoord.x, bitmapCoord.y) != null) {
                                final Measure measureToType = currentPage.getMeasureAt(bitmapCoord.x, bitmapCoord.y);
                                final PointF p = transformCoordTouchToBitmap(touchX, touchY);
                                AlertDialog.Builder meBuilder = new AlertDialog.Builder(getContext());
                                meBuilder.setTitle("Edit Measure");
                                LinearLayout meLayout = new LinearLayout(getContext());
                                meLayout.setOrientation(LinearLayout.VERTICAL);
                                final EditText name = new EditText(getContext());
                                final EditText repeat = new EditText(getContext());
                                TextView meheader1 = new TextView(getContext());
                                meheader1.setText("Measure name:");
                                TextView meheader2 = new TextView(getContext());
                                meheader2.setText("Repeat:");
                                name.setInputType(InputType.TYPE_CLASS_TEXT);
                                name.setHint(measureToType.getName());
                                repeat.setInputType(InputType.TYPE_CLASS_NUMBER);
                                String repeatTxt = "" + measureToType.repeat;
                                repeat.setHint(repeatTxt);
                                meLayout.addView(meheader1);
                                meLayout.addView(name);
                                meLayout.addView(meheader2);
                                meLayout.addView(repeat);
                                meBuilder.setView(meLayout);
                                meBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String text = name.getText().toString();
                                        measureToType.manualSequenceNumber = text.equals("") ? null : text;
                                        try {
                                            measureToType.repeat = Integer.parseInt(repeat.getText().toString());
                                        }
                                        catch (NumberFormatException e) {
                                            measureToType.repeat = 0;
                                        }
                                        measureToType.movement.calculateSequenceNumbers();
                                        measureToType.page.sortMeasures();
                                        invalidate();
                                    }
                                });
                                meBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                });
                                meBuilder.show();
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
                                    Measure measure = new Measure(leftMost, topMost, rightMost, bottomMost);
                                    document.addMeasure(measure, document.movements.get(currentMovementNumber), currentPage);
                                    document.resort(measure.movement, measure.page);
                                    invalidate();
                                }
                                pointPath = new ArrayList<>();
                                isFirstPoint = true;
                                invalidate();
                            }
                    }
                }
        } // end switch

        lastPoint = transformCoordTouchToBitmap(touchX, touchY);
        needToSave = true;
        return true;
    } // end onTouchEvent


    Menu menu;
    public void setMenu(Menu menu) {
        this.menu = menu;
    }

    void resetMenu() {
        for (int i = 0; i < menu.size(); i++) {
            // Set default icons
            if (menu.getItem(i).getItemId() == R.id.action_erase) {
                menu.getItem(i).setIcon(R.drawable.eraseroff);
            } else if (menu.getItem(i).getItemId() == R.id.action_type) {
                menu.getItem(i).setIcon(R.drawable.textboxoff);
            } else if (menu.getItem(i).getItemId() == R.id.action_cut) {
                menu.getItem(i).setIcon(R.drawable.cutoff);
            } else if (menu.getItem(i).getItemId() == R.id.action_brush) {
                menu.getItem(i).setIcon(R.drawable.brushon);
            }
        }
    }

}
