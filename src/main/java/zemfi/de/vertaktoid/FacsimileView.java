package zemfi.de.vertaktoid;

import android.app.AlertDialog;
import android.app.Dialog;
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
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
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
    public int horOverlapping = 0;
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
        bundle.putInt("currentMovementNumber", currentMovementNumber);
        bundle.putInt("horOverlapping", horOverlapping);
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            document = (Facsimile) bundle.getSerializable("document");
            pageNumber.set(bundle.getInt("pageNumber"));
            currentMovementNumber = bundle.getInt("currentMovementNumber");
            horOverlapping = bundle.getInt("horOverlapping");
            setPage(pageNumber.get());
            super.onRestoreInstanceState(bundle.getParcelable("instanceState"));
            maxPageNumber.set(document.pages.size());
            currentPath.set(document.dir.getPath());
            HSLColorsGenerator.resetHueToDefault();
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
        final Dialog gotoDialog = new Dialog(getContext());
        gotoDialog.setContentView(R.layout.dialog_goto);
        gotoDialog.setTitle("Go To Page");
        TextView gotoPageLabel = (TextView) gotoDialog.findViewById(R.id.dialog_goto_page_label);
        gotoPageLabel.setText("Page number:");
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
                invalidate();
                gotoDialog.dismiss();
            }
        });

        gotoDialog.show();
        /*AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Go to page");
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("1 - " + (document.pages.size()));
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
        builder.show();*/
    }

    public void settingsClicked() {
        resetState();
        final Dialog settingsDialog = new Dialog(getContext());
        settingsDialog.setContentView(R.layout.dialog_settings);
        settingsDialog.setTitle("Settings");
        TextView settingsLabel = (TextView) settingsDialog.findViewById(R.id.dialog_settings_horover_label);
        settingsLabel.setText("Horizontal overlapping:");
        final EditText settingsHoroverInput = (EditText) settingsDialog.findViewById(R.id.dialog_settings_horover_input);
        settingsHoroverInput.setHint("" + horOverlapping);
        settingsHoroverInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        Button gotoButtonNegative = (Button) settingsDialog.findViewById(R.id.dialog_settings_button_negative);
        gotoButtonNegative.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                settingsDialog.cancel();
            }
        });

        Button gotoButtonPositive = (Button) settingsDialog.findViewById(R.id.dialog_settings_button_positive);
        gotoButtonPositive.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    horOverlapping = Integer.parseInt(settingsHoroverInput.getText().toString());
                }
                catch (NumberFormatException e) {
                }
                invalidate();
                settingsDialog.dismiss();
            }
        });

        settingsDialog.show();

        /*resetState();
        AlertDialog.Builder meBuilder = new AlertDialog.Builder(getContext());
        meBuilder.setTitle("Settings");
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        final EditText horOverInput = new EditText(getContext());
        TextView horOverLabel = new TextView(getContext());
        horOverLabel.setText("Horizontal Overlapping:");
        horOverInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        horOverInput.setHint("" + horOverlapping);
        layout.addView(horOverLabel);
        layout.addView(horOverInput);
        meBuilder.setView(layout);
        meBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    horOverlapping = Integer.parseInt(horOverInput.getText().toString());
                }
                catch (NumberFormatException e) {
                }
                invalidate();
            }
        });
        meBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        meBuilder.show();*/
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
        currentMovementNumber = document.movements.size() - 1;
        setImage(findImageForPage(0));
        maxPageNumber.set(document.pages.size());
        currentPath.set(document.dir.getPath());
        horOverlapping = Math.round(document.pages.get(0).imageWidth * 0.01f);
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

        Paint largeBoldText = new Paint();
        largeBoldText.setColor(Color.BLACK);
        largeBoldText.setTextSize(50);
        largeBoldText.setFakeBoldText(true);

        Paint smallBoldText = new Paint();
        smallBoldText.setColor(Color.BLACK);
        smallBoldText.setTextSize(36);
        smallBoldText.setFakeBoldText(true);

        int colorsToGenerate = document.movements.size() - movementColors.size();
        movementColors.addAll(HSLColorsGenerator.generateColorSet(colorsToGenerate, s, l, a));

        if (!document.pages.get(pageNumber.get()).imageFile.exists()) {

            Rect rect = new Rect();

            largeBoldText.getTextBounds(document.pages.get(pageNumber.get()).imageFile.getName(), 0,
                    document.pages.get(pageNumber.get()).imageFile.getName().length(), rect);
            canvas.drawText("" + document.pages.get(pageNumber.get()).imageFile.getName(),
                    (this.getRight() - this.getLeft()) / 2  - rect.centerX(), 100,  largeBoldText);
            return;
        }

        int i;
        drawPath.reset();
        Page page = document.pages.get(pageNumber.get());
        for (i = 0; i < page.measures.size(); i++) {
            Measure measure = page.measures.get(i);
            largeBoldText.setColor(HSLColor.toRGB(movementColors.get(
                    document.movements.indexOf(measure.movement))));
            smallBoldText.setColor(HSLColor.toRGB(movementColors.get(
                    document.movements.indexOf(measure.movement))));
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

            Rect measureNameRect = new Rect();
            Rect movementNameRect = new Rect();

            Paint whiteAlpha = new Paint();
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
        final ArrayList<Measure> measures = currentPage.getMeasuresAt(bitmapCoord.x, bitmapCoord.y);
        final Measure measure = currentPage.getMeasureAt(bitmapCoord.x, bitmapCoord.y);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                switch (nextAction) {
                    case ERASE:
                        if (measures.size() == 0) {
                            //resetState();
                            //resetMenu();
                            // continue and handle the ActionId as a click in brush state
                        } else {
                            document.removeMeasures(measures);
                            ArrayList<Movement> changedMovements = new ArrayList<>();
                            for (Measure me : measures) {
                                if (!changedMovements.contains(me.movement)) {
                                    changedMovements.add(me.movement);
                                }
                            }

                            for (Movement movement : changedMovements) {
                                document.resort(movement, currentPage);
                            }
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
                            if (measures.size() > 0) {
                                document.removeMeasures(measures);
                                ArrayList<Movement> changedMovements = new ArrayList<>();
                                for (Measure me : measures) {
                                    if (!changedMovements.contains(me.movement)) {
                                        changedMovements.add(me.movement);
                                    }
                                }

                                for (Movement movement : changedMovements) {
                                    document.resort(movement, currentPage);
                                }
                                invalidate();
                            }
                            document.cleanMovements();
                            if(currentMovementNumber >= document.movements.size()) {
                                currentMovementNumber = document.movements.get(document.movements.size() - 1).number;
                            }
                            break;
                        case MOVEMENT:
                            final ArrayList<Measure> measuresToMove = new ArrayList<>();
                            if(measure != null) {
                                Movement currentMov = measure.movement;
                                for (int i = currentMov.measures.indexOf(measure); i < currentMov.measures.size(); i++) {
                                    measuresToMove.add(currentMov.measures.get(i));
                                }
                            }

                            final Dialog editMODialog = new Dialog(getContext());
                            editMODialog.setContentView(R.layout.dialog_editmo);
                            editMODialog.setTitle("Set Movement Anchor");
                            TextView editMOMovementLabel = (TextView) editMODialog.findViewById(R.id.dialog_editmo_movement_label);
                            editMOMovementLabel.setText("Select existing movement or create new one:");
                            final Spinner editMOMovementInput = (Spinner) editMODialog.findViewById(R.id.dialog_editmo_movement_input);
                            ArrayList<String> movementOptions = new ArrayList();
                            movementOptions.add("create new");
                            for(Movement movement : document.movements) {
                                movementOptions.add(movement.getName());
                            }
                            movementOptions.add("change label only");
                            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_dropdown_item, movementOptions);
                            editMOMovementInput.setAdapter(adapter);
                            editMOMovementInput.setSelection(0);
                            TextView editMOLabelLabel = (TextView) editMODialog.findViewById(R.id.dialog_editmo_label_label);
                            editMOLabelLabel.setText("Label for movement");
                            final EditText editMOLabelInput = (EditText) editMODialog.findViewById(R.id.dialog_editmo_label_input);
                            editMOLabelInput.setHint("optional");
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
                                    if(option.equals("add label only")) {
                                        if(measure != null && !labelStr.equals("")) {
                                            measure.movement.label = labelStr;
                                        }
                                        return;
                                    }
                                    if(option.equals("create new")) {
                                        Movement newMovement = new Movement();
                                        newMovement.number = document.movements.get
                                                (document.movements.size() - 1).number + 1;
                                        newMovement.label = labelStr;
                                        document.movements.add(newMovement);
                                        currentMovementNumber = document.movements.indexOf(newMovement);
                                        if(measure != null) {
                                            for(Measure measure : measuresToMove) {
                                                measure.changeMovement(newMovement);
                                            }
                                            document.resort(measure.movement, measure.page);
                                            document.cleanMovements();
                                            if(currentMovementNumber >= document.movements.size()) {
                                                currentMovementNumber = document.movements.get(document.movements.size() - 1).number;
                                            }
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
                                            if(measure != null) {
                                                for(Measure measure : measuresToMove) {
                                                    measure.changeMovement(oldMovement);
                                                }
                                                document.resort(measure.movement, measure.page);
                                                document.cleanMovements();
                                                if(currentMovementNumber >= document.movements.size()) {
                                                    currentMovementNumber = document.movements.get(document.movements.size() - 1).number;
                                                }
                                            }
                                        }
                                    }
                                    editMODialog.dismiss();
                                }
                            });

                            editMODialog.show();
                            /*final AlertDialog.Builder moBuilder = new AlertDialog.Builder(getContext());
                            moBuilder.setTitle("Set Movement Anchor");
                            TextView moheader1 = new TextView(getContext());
                            moheader1.setText("Select existing movement or create new one");
                            final Spinner movSpinner = new Spinner(getContext());
                            ArrayList<String> movementOptions = new ArrayList();
                            movementOptions.add("create new");
                            for(Movement movement : document.movements) {
                                movementOptions.add(movement.getName());
                            }
                            movementOptions.add("change label only");
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
                                            if(option.equals("add label only")) {
                                                if(measure != null && !labelStr.equals("")) {
                                                    measure.movement.label = labelStr;
                                                }
                                                return;
                                            }
                                            if(option.equals("create new")) {
                                                Movement newMovement = new Movement();
                                                newMovement.number = document.movements.get
                                                        (document.movements.size() - 1).number + 1;
                                                newMovement.label = labelStr;
                                                document.movements.add(newMovement);
                                                currentMovementNumber = document.movements.indexOf(newMovement);
                                                if(measure != null) {
                                                    for(Measure measure : measuresToMove) {
                                                        measure.changeMovement(newMovement);
                                                    }
                                                    document.resort(measure.movement, measure.page);
                                                    document.cleanMovements();
                                                    if(currentMovementNumber >= document.movements.size()) {
                                                        currentMovementNumber = document.movements.get(document.movements.size() - 1).number;
                                                    }
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
                                                    if(measure != null) {
                                                        for(Measure measure : measuresToMove) {
                                                            measure.changeMovement(oldMovement);
                                                        }
                                                        document.resort(measure.movement, measure.page);
                                                        document.cleanMovements();
                                                        if(currentMovementNumber >= document.movements.size()) {
                                                            currentMovementNumber = document.movements.get(document.movements.size() - 1).number;
                                                        }
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
                            moBuilder.show();*/


                            break;
                        case TYPE:
                            if (currentPage.getMeasureAt(bitmapCoord.x, bitmapCoord.y) != null) {
                                final Dialog editMEDialog = new Dialog(getContext());
                                editMEDialog.setContentView(R.layout.dialog_editme);
                                editMEDialog.setTitle("Edit Measure");
                                TextView editMENameLabel = (TextView) editMEDialog.findViewById(R.id.dialog_editme_name_label);
                                editMENameLabel.setText("Measure name:");
                                final EditText editMENameInput = (EditText) editMEDialog.findViewById(R.id.dialog_editme_name_input);
                                editMENameInput.setHint(measure.getName());
                                editMENameInput.setInputType(InputType.TYPE_CLASS_TEXT);
                                TextView editMERestLabel = (TextView) editMEDialog.findViewById(R.id.dialog_editme_rest_label);
                                editMERestLabel.setText("Rest bars:");
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
                                        String text = editMENameInput.getText().toString();
                                        measure.manualSequenceNumber = text.equals("") ? null : text;
                                        try {
                                            measure.rest = Integer.parseInt(editMERestInput.getText().toString());
                                        }
                                        catch (NumberFormatException e) {
                                            measure.rest = 0;
                                        }
                                        measure.movement.calculateSequenceNumbers();
                                        measure.page.sortMeasures();
                                        editMEDialog.dismiss();
                                        invalidate();
                                    }
                                });

                                editMEDialog.show();
                                /*AlertDialog.Builder meBuilder = new AlertDialog.Builder(getContext());
              p                  meBuilder.setTitle("Edit Measure");
                                LinearLayout meLayout = new LinearLayout(getContext());
                                meLayout.setOrientation(LinearLayout.VERTICAL);
                                final EditText name = new EditText(getContext());
                                final EditText rest = new EditText(getContext());
                                TextView meheader1 = new TextView(getContext());
                                meheader1.setText("Measure name:");
                                TextView meheader2 = new TextView(getContext());
                                meheader2.setText("Rest bars:");
                                name.setInputType(InputType.TYPE_CLASS_TEXT);
                                name.setHint(measure.getName());
                                rest.setInputType(InputType.TYPE_CLASS_NUMBER);
                                String repeatTxt = "" + measure.rest;
                                rest.setHint(repeatTxt);
                                meLayout.addView(meheader1);
                                meLayout.addView(name);
                                meLayout.addView(meheader2);
                                meLayout.addView(rest);
                                meBuilder.setView(meLayout);
                                meBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String text = name.getText().toString();
                                        measure.manualSequenceNumber = text.equals("") ? null : text;
                                        try {
                                            measure.rest = Integer.parseInt(rest.getText().toString());
                                        }
                                        catch (NumberFormatException e) {
                                            measure.rest = 0;
                                        }
                                        measure.movement.calculateSequenceNumbers();
                                        measure.page.sortMeasures();
                                        invalidate();
                                    }
                                });
                                meBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                });
                                meBuilder.show();*/
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
                                    document.addMeasure(newMeasure, document.movements.get(currentMovementNumber), currentPage);
                                    document.resort(newMeasure.movement, newMeasure.page);
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
