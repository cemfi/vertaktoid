package zemfi.de.vertaktoid;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.CoordinatorLayout;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.ceylonlabs.imageviewpopup.ImagePopup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.net.ssl.SSLHandshakeException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import zemfi.de.vertaktoid.commands.CommandManager;
import zemfi.de.vertaktoid.helpers.HSLColor;
import zemfi.de.vertaktoid.helpers.HSLColorsGenerator;
import zemfi.de.vertaktoid.mei.MEIHelper;
import zemfi.de.vertaktoid.model.Facsimile;
import zemfi.de.vertaktoid.model.Measure;
import zemfi.de.vertaktoid.model.Page;
import zemfi.de.vertaktoid.model.Zone;

/**
 * Contains the presentation and user interaction functions (click on icon). Directs the UI layouts.
 * Extends the SubsamplingScaleImageView class.
 */

public class FacsimileView extends CoordinatorLayout {

    CommandManager commandManager;
    public String fullResponse;
    public JSONObject currentPageUrlId;


    /**
     * Constructor
     *
     * @param context android application context
     * @param attr attributes
     */
    public FacsimileView(Context context, AttributeSet attr) {
        super(context, attr);
        commandManager = new CommandManager();
        movementColors = new ArrayList<>();
    }



    public void generateColors() {
        int colorsToGenerate = document.movements.size() - movementColors.size();
        movementColors.addAll(HSLColorsGenerator.generateColorSet(colorsToGenerate, s, l, a));
    }

    /**
     * Constructor
     * @param context android application context
     */
    public FacsimileView(Context context) {
        super(context);
        commandManager = new CommandManager();
    }

    private float s = 100f;
    private float l = 30f;
    private float a = 1f;
    public int cutOverlapping = 0;
    public Facsimile document;
    public final ObservableInt pageNumber = new ObservableInt(-1);
    public int currentMovementNumber = 0;
    public final ObservableField<String> currentPath = new ObservableField<>();
    public final ObservableInt maxPageNumber = new ObservableInt(0);
    public boolean needToSave = false;



    public enum Action {DRAW, DRAW2, ERASE, ADJUST_MEASURE, ORTHOGONAL_CUT, PRECISE_CUT, IIIF_ZOOM, ADJUST_MOVEMENT;
    }

    public Action nextAction = Action.DRAW;
    public boolean isFirstPoint = true;
    public ArrayList<HSLColor> movementColors;
    public Menu menu;
    public ProgressDialog progress = new ProgressDialog(MainActivity.context);
    /**
     * Stores the instance state.
     *
     * @return parcelable object
     */
    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("instanceState", super.onSaveInstanceState());
        bundle.putParcelable("document", document);
        bundle.putInt("pageNumber", pageNumber.get());
        bundle.putInt("currentMovementNumber", currentMovementNumber);
        bundle.putInt("cutOverlapping", cutOverlapping);
        bundle.putParcelable("history", commandManager);
        bundle.putParcelableArrayList("colors", movementColors);
        return bundle;
    }

    /**
     * Restores the instance state.
     *
     * @param state parcelable object
     */
    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            document = (Facsimile) bundle.getParcelable("document");
            if (document == null) {
                return;
            }
            pageNumber.set(bundle.getInt("pageNumber"));
            currentMovementNumber = bundle.getInt("currentMovementNumber");
            cutOverlapping = bundle.getInt("cutOverlapping");
            setPage(pageNumber.get());
            commandManager = (CommandManager) bundle.getParcelable("history");
            maxPageNumber.set(document.pages.size());
            currentPath.set(document.dir.getName());
            movementColors = bundle.getParcelableArrayList("colors");
            super.onRestoreInstanceState(bundle.getParcelable("instanceState"));
            return;
        }

        super.onRestoreInstanceState(state);
    }

    public void setPage(int page) {
        if (document == null) {
            return;
        }
        if (page >= 0 && page < document.pages.size()) {
            pageNumber.set(page);
            CustomViewPager viewPager = (CustomViewPager) findViewById(R.id.view_pager);
            viewPager.setCurrentItem(page);
        }
    }

    /**
     * Sets Icons after actions to show possible undo and redo actions.
     */
    public void adjustHistoryNavigation() {
        if (commandManager.getUndoStackSize() > 0) {
            menu.findItem(R.id.action_undo).setIcon(R.drawable.undo_on);
        } else {
            menu.findItem(R.id.action_undo).setIcon(R.drawable.undo_off);
        }
        if (commandManager.getRedoStackSize() > 0) {
            menu.findItem(R.id.action_redo).setIcon(R.drawable.redo_on);
        } else {
            menu.findItem(R.id.action_redo).setIcon(R.drawable.redo_off);
        }
    }

    /**
     * Shows dialog and obtains the user defined next page number. Changes to the page with giving number.
     */
    public void gotoClicked() {
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
                resetMenu();
                resetState();
                invalidate();
                gotoDialog.cancel();
            }
        });

        Button gotoButtonPositive = (Button) gotoDialog.findViewById(R.id.dialog_goto_button_positive);
        gotoButtonPositive.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int newPageNumber = Integer.parseInt(gotoPageInput.getText().toString()) - 1;
                    if (newPageNumber >= 0 && newPageNumber < document.pages.size()) {
                        pageNumber.set(newPageNumber);
                        setPage(newPageNumber);
                        refresh();
                    }
                } catch (NumberFormatException e) {

                }
                resetMenu();
                resetState();
                invalidate();
                gotoDialog.dismiss();
            }
        });

        gotoDialog.show();
    }

    /**
     * Shows the settings dialog and process the user input.
     * Currently obtains the horizontal overlapping parameter.
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
        final EditText settingsHoroverInput = (EditText) settingsDialog.findViewById(R.id.dialog_settings_horover_input);
        settingsHoroverInput.setHint("" + cutOverlapping);
        settingsHoroverInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        final RadioGroup settingsHoroverType = (RadioGroup) settingsDialog.findViewById(R.id.dialog_settings_horover_type);
        settingsHoroverType.check(R.id.regions);
        final EditText settingsUndosizeInput = (EditText) settingsDialog.findViewById(R.id.dialog_settings_undosize_input);
        settingsUndosizeInput.setHint("" + commandManager.getHistoryMaxSize());
        settingsUndosizeInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        final RadioGroup settingsMEIType = (RadioGroup) settingsDialog.findViewById(R.id.dialog_settings_mei_type);
        final RadioButton upbeat = (RadioButton) settingsDialog.findViewById(R.id.upbeat);


        if (document.nextAnnotationsType == Facsimile.AnnotationType.ORTHOGONAL_BOX) {
            settingsMEIType.check(R.id.dialog_settings_annotation_type_orthogonal);
        } else if (document.nextAnnotationsType == Facsimile.AnnotationType.ORIENTED_BOX) {
            settingsMEIType.check(R.id.dialog_settings_annotation_type_oriented);
        } else if (document.nextAnnotationsType == Facsimile.AnnotationType.POLYGON) {
            settingsMEIType.check(R.id.dialog_settings_annotation_type_polygon);
        }
        Button settingsButtonNegative = (Button) settingsDialog.findViewById(R.id.dialog_settings_button_negative);

        settingsButtonNegative.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                settingsDialog.cancel();
                resetState();
                resetMenu();
                invalidate();
            }
        });

        Button settingsButtonPositive = (Button) settingsDialog.findViewById(R.id.dialog_settings_button_positive);
        settingsButtonPositive.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if(upbeat.isChecked()){
                        Vertaktoid.metcon = false;
                    }

                    String historyMaxSize = settingsUndosizeInput.getText().toString();
                    if (!historyMaxSize.equals("")) {
                        commandManager.setHistoryMaxSize(Integer.parseInt(settingsUndosizeInput.getText().toString()));
                    }
                    String horover = settingsHoroverInput.getText().toString();
                    if (!horover.equals("")) {
                        if (settingsHoroverType.getCheckedRadioButtonId() == R.id.regions) {
                            cutOverlapping = Integer.parseInt(settingsHoroverInput.getText().toString());
                        } else if (settingsHoroverType.getCheckedRadioButtonId() == R.id.dialog_settings_horover_type_percents) {
                            float percent = Float.parseFloat(settingsHoroverInput.getText().toString());
                            if (percent > 100) {
                                percent = percent % 100;
                            }
                            cutOverlapping = Math.round(document.pages.get(pageNumber.get()).imageWidth * percent / 100);
                        } else {
                            cutOverlapping = 0;
                        }
                    }
                    if (settingsMEIType.getCheckedRadioButtonId() == R.id.dialog_settings_annotation_type_orthogonal) {
                        document.nextAnnotationsType = Facsimile.AnnotationType.ORTHOGONAL_BOX;
                    } else if (settingsMEIType.getCheckedRadioButtonId() == R.id.dialog_settings_annotation_type_oriented) {
                        document.nextAnnotationsType = Facsimile.AnnotationType.ORIENTED_BOX;
                    } else if (settingsMEIType.getCheckedRadioButtonId() == R.id.dialog_settings_annotation_type_polygon) {
                        document.nextAnnotationsType = Facsimile.AnnotationType.POLYGON;
                    }
                } catch (NumberFormatException e) {
                    // do nothing
                }
                resetState();
                resetMenu();
                invalidate();
                settingsDialog.dismiss();
            }
        });

        settingsDialog.show();
    }

    /**
     * Sets the current facsimile
     *
     * @param facsimile facsimile
     */
    public void setFacsimile(Facsimile facsimile) {
        movementColors = new ArrayList<>();

        this.document = facsimile;
        pageNumber.set(0);
        currentMovementNumber = document.movements.size() - 1;
        maxPageNumber.set(document.pages.size());
        currentPath.set(document.dir.getName());
        HSLColorsGenerator.resetHueToDefault();
        generateColors();
        cutOverlapping = 0;
    }

    /**
     * Gets current facsimile.
     *
     * @return facsimile
     */
    public Facsimile getFacsimile() {
        return this.document;
    }


    /**
     * Menu entry "brush" clicked.
     */
    public void brushClicked() {
        nextAction = Action.DRAW;
        refresh();
    }

    /**
     * Menu entry "erase" clicked.
     */
    public void eraseClicked() {
        nextAction = Action.ERASE;
        refresh();
    }
    /**
     * Menu entry "erase page" clicked.
     */
    public void erasePageClicked() {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.context).create();
        alertDialog.setTitle("Alert");
        alertDialog.setMessage("Are you sure, you want to delete all measures of the current page?");
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "YES",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        if (document.pages.get(pageNumber.get()).measures.size() > 0) {
                            commandManager.processRemoveMeasuresCommand(document.pages.get(pageNumber.get()).measures, document);
                        }
                        refresh();
                        dialog.dismiss();
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "NO",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }
    /**
     * Menu entry "erase all measures" clicked.
     */
    public void eraseAllClicked() {

        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.context).create();
        alertDialog.setTitle("Alert");
        alertDialog.setMessage("Are you sure, you want to delete all measures from all pages?");
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "YES",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        if (document.measuresCount()  > 0) {
                            for (int i = 0; i < document.pages.size(); i++) {
                                commandManager.processRemoveMeasuresCommand(document.pages.get(i).measures, document);
                            }
                        }
                        refresh();
                        dialog.dismiss();
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "NO",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();
                    }
                });
        alertDialog.show();

    }
    /**
     * Menu entry "measureAllClicked" clicked.

    @RequiresApi(api = Build.VERSION_CODES.R)
    public void measureAllClicked() {
        refresh();
        setProgressBar("Vertaktoid is adding measures, please wait");
        progress.show();
        String[] paths = new String[document.pages.size()];
        int[] pageNumbers = new int[document.pages.size()];
        for (int i = 0; i < document.pages.size(); i++) {

            String path = document.dir.getUri().getPath();
            String folder_name2 = path.replace("tree/primary:", "");
            String folder_name = folder_name2.substring(folder_name2.indexOf(":") + 1);
            String path2 = "/storage/emulated/0/" + folder_name + "/" + document.pages.get(i).getImageFileName();

            paths[i] = path2;
            pageNumbers[i] = i;
        }
        getMeasureDetector(paths, pageNumbers);
    }
     */
    public void setProgressBar(String text){
        progress.setTitle("Loading");
        progress.setMessage(text);
        progress.setCancelable(false);
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    public void measurPageClicked() throws InterruptedException {
        refresh();
        setProgressBar("Vertaktoid is adding measures, please wait");

        progress.show();

        String path = document.dir.getUri().getPath();
        String folder_name2 = path.replace("tree/primary:", "");
        String folder_name = folder_name2.substring(folder_name2.indexOf(":") + 1);

        String path2 = "/storage/emulated/0/" + folder_name + "/" + document.pages.get(pageNumber.get()).getImageFileName();
        String[] paths = {path2};
        int[] pageNumbers = {pageNumber.get()};
        getMeasureDetector(paths, pageNumbers);
    }
    public String stringUrlParser(){
        String path = document.dir.getUri().getPath();
        String folder_name2 = path.replace("tree/primary:", "");
        String folder_name = folder_name2.substring(folder_name2.indexOf(":") + 1);
        return folder_name;
    }
    /**
     * Menu entry "type"
     */
    public void typeClicked() {
        refresh();
        nextAction = Action.ADJUST_MEASURE;
    }

    /**
     * Menu entry "vertical cut" clicked.
     */
    public void OrthogonalCutClicked() {
        refresh();
        nextAction = Action.ORTHOGONAL_CUT;
    }

    /**
     * Menu entry "horizontal cut" clicked.
     */
    public void PreciseCutClicked() {
        refresh();
        nextAction = Action.PRECISE_CUT;
    }
    /**
     * Menu entry "movement" clicked.
     */
    public void movementClicked() {
        refresh();
        nextAction = Action.ADJUST_MOVEMENT;
    }


    public void getMeasureDetector(String[] paths, int[] pageNumbers) {
        Runnable runnable = new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.R)
            @Override
            public void run() {
                for(int i = 0; i < paths.length; i++) {

                    float lrx;
                    float ulx;
                    float lry;
                    float uly;

                    try {
                        OkHttpClient client = new OkHttpClient().newBuilder()
                                .build();
                        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                                .addFormDataPart("Content-Type", "image/jpg")
                                .addFormDataPart("filename", "test.jpg")
                                .addFormDataPart("image", "test.jpeg",
                                        RequestBody.create(MediaType.parse("application/octet-stream"),
                                                new File(paths[i])))
                                .build();
                        Request request = new Request.Builder()
                                .url("https://measure-detector.edirom.de/upload")
                                .method("POST", body)
                                .addHeader("method", "post")
                                .addHeader("path", "/upload")
                                .addHeader("Accept", "application/json, text/plain, */*")
                                .addHeader("Content-Disposition", "form-data; name=\"image\"; filename=\"Screenshot_1644164566.jpeg\"")
                                .build();
                        Response response = client.newCall(request).execute();
                        fullResponse = response.body().string();
                        JSONObject obj = new JSONObject(fullResponse);


                        JSONArray arr = obj.getJSONArray("measures"); // notice that `"posts": [...]`
                        for (int j = 0; j < arr.length(); j++) {

                            uly = Float.parseFloat(arr.getJSONObject(j).getString("uly"));
                            lry = Float.parseFloat(arr.getJSONObject(j).getString("lry"));
                            ulx = Float.parseFloat(arr.getJSONObject(j).getString("ulx"));
                            lrx = Float.parseFloat(arr.getJSONObject(j).getString("lrx"));
                            MEIHelper.getMeasureDetector(uly, lry, ulx, lrx, document.movements, document, document.pages.get(pageNumbers[i]), document.pages.get(pageNumbers[i]).measures);

                        }
                        MEIHelper.sortMeasures(document);
                        refresh();
                    } catch (FileNotFoundException e){
                        Thread thread = new Thread(){
                            public void run(){
                                Looper.prepare();//Call looper.prepare()
                                String message = MainActivity.context.getString(R.string.access_denied_error);
                                showPermissionError(message);
                                Looper.loop();
                            }
                        };
                        thread.start();
                    }catch (SSLHandshakeException | UnknownHostException e){
                        Thread thread = new Thread(){
                            public void run(){
                                Looper.prepare();//Call looper.prepare()
                                String message = MainActivity.context.getString(R.string.internet_connection_error);
                                showPermissionError(message);
                                Looper.loop();
                            }
                        };
                        thread.start();
                    }catch (IOException | JSONException e) {
                        e.printStackTrace();
                    } finally {
                    }

                }
                progress.dismiss();
            }

        };

        new Thread(runnable).start();

    }

    private void showPermissionError(String message) {
        Context context = MainActivity.context;
        CharSequence text = message;
        int duration = Toast.LENGTH_LONG;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    public void undoClicked(){
     /** int pageIndex = commandManager.undo();
        if (pageIndex != -1 && pageIndex != pageNumber.get()) {
            pageNumber.set(pageIndex);
            setPage(pageIndex);
        }
        adjustHistoryNavigation();
        refresh(); **/
        nextAction = Action.DRAW2;
        refresh();
    }

    public void redoClicked() {
        int pageIndex = commandManager.redo();
        if (pageIndex != -1 && pageIndex != pageNumber.get()) {
            pageNumber.set(pageIndex);
            setPage(pageIndex);
        }
        adjustHistoryNavigation();
        refresh();
    }


    public void openpdfClicked(File dest, String folderName) throws IOException  {


        PdfRenderer renderer = null;
        try {
            ParcelFileDescriptor parcelFileDescriptor = ParcelFileDescriptor.open(dest, ParcelFileDescriptor.MODE_READ_ONLY);
            renderer = new PdfRenderer(parcelFileDescriptor);
        } catch (Exception e) {
            e.printStackTrace();
        }

        int pageCount = renderer.getPageCount();

        for (int i = 0; i < pageCount; i++) {
            PdfRenderer.Page page = renderer.openPage(i);
            int width = getResources().getDisplayMetrics().densityDpi / 72 * page.getWidth();
            int height = getResources().getDisplayMetrics().densityDpi / 72 * page.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();

            // Save the Bitmap to disk.
            int bwidth = bitmap.getWidth();
            int bheight = bitmap.getHeight();

            Bitmap newBitmap = Bitmap.createBitmap(bwidth, bheight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(newBitmap);
            canvas.drawColor(Color.WHITE);
            canvas.drawBitmap(bitmap, 0, 0, null);
            System.out.println();

            File imageFile = new File((String.valueOf(folderName)), "page" + i + ".png");
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(imageFile);
                newBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            if (imageFile.exists()) {
            } else {
            }
        }
        renderer.close();


    }

    /**
     * Reset current menu state to default "brush" entry
     */
    void resetState() {
        nextAction = Action.DRAW;
        refresh();
    }

    public void refresh() {
        isFirstPoint = true;
        invalidate();
        CustomViewPager viewPager = (CustomViewPager) findViewById(R.id.view_pager);
        CustomPagerAdapter pagerAdapter = (CustomPagerAdapter) viewPager.getAdapter();
        // pageAdapter is null if opened the first time and no image is loaded
        if (pagerAdapter != null)
            pagerAdapter.refresh();
    }


    /**
     * Setter for menu.
     *
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
            } else if (menu.getItem(i).getItemId() == R.id.action_orthogonal_cut) {
                menu.getItem(i).setIcon(R.drawable.orthogonal_cut_off);
            } else if (menu.getItem(i).getItemId() == R.id.action_precise_cut) {
                menu.getItem(i).setIcon(R.drawable.precise_cut_off);
            } else if (menu.getItem(i).getItemId() == R.id.action_brush) {
                menu.getItem(i).setIcon(R.drawable.brush_on);
            } else if (menu.getItem(i).getItemId() == R.id.action_erase_page) {
                menu.getItem(i).setIcon(R.drawable.eraser_off);

            }
        }

    }
}
