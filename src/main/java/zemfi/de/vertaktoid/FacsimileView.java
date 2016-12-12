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
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.graphics.Point;

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
    private int paintColor = 0xA5AF2B2B;
    private float currentBrushSize;
    Path grayPath;
    Path polygonHoverPath;
    protected Facsimile document;
    public final ObservableInt pageNumber = new ObservableInt(-1);
    public final ObservableField<String> currentPath = new ObservableField<>();
    public final ObservableInt maxPageNumber = new ObservableInt(0);
    public boolean needToSave = false;

    private void init() {
        currentBrushSize = 5; //getResources().getInteger(R.integer.medium_size);
        grayPath = new Path();
        drawPath = new Path();
        pointPath = new ArrayList<>();
        polygonHoverPath = new Path();
        drawPaint = new Paint();
        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(currentBrushSize);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        pageNumber.set(0);

        setOnHoverListener(new View.OnHoverListener() {
            @Override
            public boolean onHover(View v, MotionEvent event) {
                if (!shouldErase && !shouldType) {
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
            currentPath.set(document.path);
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
            setImage(ImageSource.uri(Uri.fromFile(new File(document.pages.get(pageNumber.get()).filePath))));
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

    public void setFacsimile(Facsimile facsimile) {
        this.document = facsimile;
        pageNumber.set(0);
        setImage(ImageSource.uri(Uri.fromFile(new File(document.pages.get(0).filePath))));
        maxPageNumber.set(document.pages.size());
        currentPath.set(document.path);
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


        Paint hoverPaint = new Paint();
        hoverPaint.setColor(0x55555555);
        hoverPaint.setAntiAlias(true);
        hoverPaint.setStrokeWidth(3);
        hoverPaint.setStyle(Paint.Style.FILL);
        hoverPaint.setStrokeJoin(Paint.Join.ROUND);
        hoverPaint.setStrokeCap(Paint.Cap.ROUND);


        int i;
        drawPath.reset();

        for (i = 0; i < document.pages.get(pageNumber.get()).numberOfBoxes(); i++) {
            Box box = document.pages.get(pageNumber.get()).getBox(i);
            PointF topLeft = transformCoordBitmapToTouch(box.left, box.top);
            PointF bottomRight = transformCoordBitmapToTouch(box.right, box.bottom);
            if (topLeft == null) {
                // still loading image
                return;
            }
            drawPath.addRect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y, Path.Direction.CW);
            canvas.drawPath(drawPath, drawPaint);
            drawPath.reset();

            Paint paint = new Paint();
            paint.setColor(0xA5AF2B2B);//Color.BLACK);
            paint.setTextSize(50);
            paint.setFakeBoldText(true);
            Rect rect = new Rect();

            Paint whiteAlpha = new Paint();
            whiteAlpha.setColor(0x55ffffff);
            whiteAlpha.setStyle(Paint.Style.FILL);

            if (box.manualSequenceNumber != null) {
                Paint darkAlpha = new Paint();
                darkAlpha.setColor(0x99555555);
                darkAlpha.setStrokeWidth(10);
                darkAlpha.setStyle(Paint.Style.STROKE);

                paint.getTextBounds(box.manualSequenceNumber, 0, box.manualSequenceNumber.length(), rect);

                float leftTextBox = (topLeft.x + bottomRight.x) / 2 - rect.centerX() - 5;
                float topTextBox = topLeft.y + 50 - rect.height() ;
                float rightTextBox = (topLeft.x + bottomRight.x) / 2 + rect.centerX() + 5;
                float bottomTextBox = topLeft.y + 50;

                canvas.drawRect(leftTextBox, topTextBox, rightTextBox, bottomTextBox, whiteAlpha);
                canvas.drawRect(leftTextBox, topTextBox, rightTextBox, bottomTextBox, drawPaint);

                canvas.drawText(box.manualSequenceNumber, (topLeft.x + bottomRight.x) / 2 - rect.centerX(), topLeft.y + 50,  paint);
                //canvas.drawLine(leftTextBox, bottomTextBox, rightTextBox, bottomTextBox, drawPaint);
            } else {
                String str = "" + box.sequenceNumber;
                paint.getTextBounds(str, 0, str.length(), rect);

                float leftTextBox = (topLeft.x + bottomRight.x) / 2 - rect.centerX() - 5;
                float topTextBox = topLeft.y + 50 - rect.height() ;
                float rightTextBox = (topLeft.x + bottomRight.x) / 2 + rect.centerX() + 5;
                float bottomTextBox = topLeft.y + 50;
                canvas.drawRect(leftTextBox, topTextBox, rightTextBox, bottomTextBox, whiteAlpha);

                canvas.drawText("" + box.sequenceNumber, (topLeft.x + bottomRight.x) / 2 - rect.centerX(), topLeft.y + 50,  paint);
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

        //uncomment the following line to enable the lines drawing for debugging
        //debugLinesDraw(canvas);
    }

    @Deprecated
    private void debugLinesDraw(Canvas canvas) {
        Paint linesPaint = new Paint();
        linesPaint.setColor(Color.BLUE);
        linesPaint.setStrokeWidth(2);
        linesPaint.setStyle(Paint.Style.STROKE);

        Context context = this.getContext();
        WindowManager vm = (WindowManager) context.getSystemService(context.WINDOW_SERVICE);
        Display display = vm.getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);

        for(int j = 0; j < document.pages.get(pageNumber.get()).lines.size(); j ++) {
            Line line = document.pages.get(pageNumber.get()).lines.get(j);
            PointF lineTopLeft = transformCoordBitmapToTouch(0, line.top);
            PointF lineBottomLeft = transformCoordBitmapToTouch(size.x, line.bottom);
            canvas.drawRect(0, lineTopLeft.y, size.x, lineBottomLeft.y, linesPaint);
        }
    }


    public void brushClicked() {
        resetState();
    }

    boolean shouldErase = false;
    public void eraseClicked() {
        resetState();
        shouldErase = true;
    }

    boolean shouldType = false;
    public void typeClicked() {
        resetState();
        shouldType = true;
    }


    boolean shouldCut = false;
    public void cutClicked() {
        resetState();
        shouldCut = true;
    }

    void resetState() {
        isFirstPoint = true;
        shouldErase = false;
        shouldType = false;
        shouldCut = false;
        pointPath = new ArrayList<>();
        invalidate();
    }



    // Remember last point position for dragging
    private PointF last = new PointF();
    float downX = 0.0f;
    float downY = 0.0f;
    float lastX = 0.0f;
    float lastY = 0.0f;

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
        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER) {
            //if (isFirstPoint) {
                // do not allow navigation while entering the polygon
                return super.onTouchEvent(event);
            //}
            //return true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (shouldType || shouldErase) {
                    Page currentPage = document.pages.get(pageNumber.get());
                    PointF bitmapCoord = transformCoordTouchToBitmap(touchX, touchY);
                    Box box = currentPage.getBoxAt(bitmapCoord.x, bitmapCoord.y);
                    if (box == null) {
                        //resetState();
                        //resetMenu();
                        // continue and handle the ActionId as a click in brush state
                    } else {
                        if (shouldErase) {
                            final PointF p = transformCoordTouchToBitmap(touchX, touchY);
                            if (document.pages.get(pageNumber.get()).deleteBox(p.x, p.y)) {
                                invalidate();
                            }
                        }
                        // wait until action_up for typing
                        break;
                    }
                }

                if (shouldCut) {
                    PointF bitmapCoord = transformCoordTouchToBitmap(touchX, touchY);
                    Page currentPage = document.pages.get(pageNumber.get());
                    Box box = currentPage.getBoxAt(bitmapCoord.x, bitmapCoord.y);
                    if (box == null) {
                        resetState();
                        resetMenu();
                        // continue and handle the ActionId as a click in brush state
                    } else {
                        currentPage.deleteBox(bitmapCoord.x, bitmapCoord.y);
                        currentPage.addBox(box.left, bitmapCoord.x, box.top, box.bottom);
                        currentPage.addBox(bitmapCoord.x, box.right, box.top, box.bottom);
                        // do not continue
                        break;
                    }
                }
                if (isFirstPoint) {
                    pointPath = new ArrayList<>();
                    PointF bitmapCoord = transformCoordTouchToBitmap(touchX, touchY);
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

            case MotionEvent.ACTION_MOVE:
                if (shouldErase) {
                    final PointF p = transformCoordTouchToBitmap(touchX, touchY);
                    if (document.pages.get(pageNumber.get()).deleteBox(p.x, p.y, lastPoint.x, lastPoint.y)) {
                        invalidate();
                    }
                }
                if (shouldErase || shouldType || shouldCut) {
                    break;
                }
                if (!isFirstPoint) {
                    PointF bitmapCoord = transformCoordTouchToBitmap(touchX, touchY);
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

            case MotionEvent.ACTION_UP:
                final PointF p = transformCoordTouchToBitmap(touchX, touchY);
                if (event.getToolType(0) != MotionEvent.TOOL_TYPE_FINGER) {
                    if (shouldErase) {
                        if (document.pages.get(pageNumber.get()).deleteBox(p.x, p.y)) {
                            invalidate();
                            break;
                        }
                    } else if (shouldType) {
                        if (document.pages.get(pageNumber.get()).getBoxAt(p.x, p.y) != null) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                            builder.setTitle("Taktnummer");
                            final EditText input = new EditText(getContext());
                            input.setInputType(InputType.TYPE_CLASS_TEXT);
                            builder.setView(input);
                            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String text = input.getText().toString();
                                    Box box = document.pages.get(pageNumber.get()).getBoxAt(p.x, p.y);
                                    box.manualSequenceNumber = text;
                                    document.pages.get(pageNumber.get()).updateSequenceNumbers();
                                    invalidate();
                                }
                            });
                            builder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                            builder.show();
                        }
                    }
                    else if (shouldCut) {
                        // do nothing
                    } else {
                        PointF bitmapCoord = transformCoordTouchToBitmap(touchX, touchY);
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
                                //boxes.add(new RectF(left, top, right, bottom));
                                document.pages.get(pageNumber.get()).addBox(leftMost, rightMost, topMost, bottomMost);
                                updateSequenceNumbers();
                                //pages.get(pageNumber).addBox(leftMost, rightMost, topMost, bottomMost);
                            }
                            Log.v("bla", "complete" + trackLength);
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

    void updateSequenceNumbers() {
        document.updateSequenceNumbers();
        invalidate();
    }

}
