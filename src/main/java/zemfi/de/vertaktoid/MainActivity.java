package zemfi.de.vertaktoid;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.support.annotation.RequiresApi;
import android.support.v4.provider.DocumentFile;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

import zemfi.de.vertaktoid.databinding.ActivityMainBinding;
import zemfi.de.vertaktoid.helpers.Status;
import zemfi.de.vertaktoid.helpers.StatusStrings;
import zemfi.de.vertaktoid.model.Facsimile;



public class MainActivity extends AppCompatActivity {

    public static Activity context = null;
    public String url = "", ids;
    private RequestQueue mQueue;
    public String images[];
    public static String imgs[];
    public JSONObject canv, img,res,resource, item2, body, body2;
    public JSONArray canvas, image, item1, item3;





    final String TAG = "de.zemfi.vertaktoid";
    // bindable status for bar
    final Status status = new Status();
    Menu mainMenu;

    //autosave
    private Handler tmpSaveHandler = new Handler();
    private Runnable tmpSaveRunnable = new Runnable() {
        @Override
        public void run() {
            saveTemporaryMEI();
            tmpSaveHandler.postDelayed(this, 300000);
        }
    };

    private CustomViewPager viewPager;
    private Toolbar toolbar;
    private FacsimileView facsimileView;
    private DocumentFile dir;

    /**
     * Creates temporary MEI file.
     * The file name will be set to current datetime plus ".mei" extension.
     */
    protected void saveTemporaryMEI() {
        FacsimileView view = (FacsimileView) findViewById(R.id.facsimile_view);
        if(view.needToSave) {
            Date saveDate = new Date();
            String filename = "" + DateFormat.format("dd-MM-yyyy_kk-mm-ss", saveDate) + ".mei";
            if (view.getFacsimile() != null) {
                DocumentFile systemDir = dir.findFile(Vertaktoid.APP_SUBFOLDER);
                if(systemDir == null)
                    systemDir = dir.createDirectory(Vertaktoid.APP_SUBFOLDER);

                boolean result = view.getFacsimile().saveToDisk(systemDir, filename);
                status.setDate(saveDate);
                status.setAction(StatusStrings.ActionId.TMP_SAVED);
                if (result) status.setStatus(StatusStrings.StatusId.SUCCESS);
                else status.setStatus(StatusStrings.StatusId.FAIL);
            }
            view.needToSave = false;
        }
    }

    /**
     * Android application lifecycle: onCreate event.
     * @param savedInstanceState The instance state bundle.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;

        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        viewPager = (CustomViewPager) findViewById(R.id.view_pager);
        viewPager.setOffscreenPageLimit(1);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        facsimileView = (FacsimileView) findViewById(R.id.facsimile_view);

        Toast.makeText(this, "No folder selected. Please choose a file.", Toast.LENGTH_LONG).show();

        // link activity ui to facsimileView
        binding.setFview(facsimileView);
        status.setStatus(StatusStrings.StatusId.SUCCESS);
        status.setAction(StatusStrings.ActionId.STARTED);
        binding.setCstatus(status);

        tmpSaveHandler.postDelayed(tmpSaveRunnable, 300000);
    }

    private void loadFacsimile(DocumentFile dir) {
        // facsimile contains pages, movements, breaks
        Facsimile facsimile = new Facsimile();

        // create subfolder (for MEI) and dummy image file
        prepareApplicationFiles(dir);

        facsimile.openDirectory(dir);

        facsimileView.setFacsimile(facsimile);
        viewPager.setAdapter(new CustomPagerAdapter(facsimileView));
        viewPager.clearOnPageChangeListeners();
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                facsimileView.pageNumber.set(position);
                facsimileView.refresh();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        status.setDate(new Date());
        status.setAction(StatusStrings.ActionId.LOADED);
        status.setStatus(StatusStrings.StatusId.SUCCESS);
    }

    /**
     * Creates subfolder and dummy image file for not founded pages.
     * The temporary MEI files will be stored in created subfolder.
     * @param dir The directory.
     */
    private void prepareApplicationFiles(DocumentFile dir) {

        DocumentFile systemDir = dir.findFile(Vertaktoid.APP_SUBFOLDER);
        if(systemDir == null || !systemDir.exists()) {
            dir.createDirectory(Vertaktoid.APP_SUBFOLDER);
            systemDir = dir.findFile(Vertaktoid.APP_SUBFOLDER);
        }
        DocumentFile image404 = systemDir.findFile(Vertaktoid.NOT_FOUND_STUBIMG);
        if(image404 == null || !image404.exists()) {
            Bitmap bm = BitmapFactory.decodeResource( getResources(), R.drawable.facsimile404);
            try {
                image404 = systemDir.createFile("image/png", Vertaktoid.NOT_FOUND_STUBIMG);
                ParcelFileDescriptor pdf = getContentResolver().openFileDescriptor(image404.getUri(), "w");
                FileOutputStream outStream = new FileOutputStream(pdf.getFileDescriptor());
                bm.compress(Bitmap.CompressFormat.PNG, 100, outStream);
                outStream.flush();
                outStream.close();
            }
            catch (FileNotFoundException ex) {

            }
            catch (IOException ex) {

            }
        }
    }

    /**
     * Android application lifecycle: onSaveInstanceState event.
     * @param savedInstanceState The instance state bundle.
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Android application lifecycle: onRestoreInstanceState event.
     * @param savedInstanceState The instance state bundle.
     */
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        try {
            super.onRestoreInstanceState(savedInstanceState);
        }
        catch (Exception e) {

        }
    }

    /**
     * Android application lifecycle: onResume event.
     */
    @Override
    protected void onResume() {
        FacsimileView view = (FacsimileView) findViewById(R.id.facsimile_view);
        viewPager.restore();
        /*if(view.document != null) {
            view.setImage(view.findImageForPage(view.pageNumber.get()));
        }*/
        super.onResume();
    }

    /**
     * Android application lifecycle: onPause event.
     */
    @Override
    protected void onPause() {
        FacsimileView view = (FacsimileView) findViewById(R.id.facsimile_view);
        if (view.getFacsimile() != null) {
            boolean result = view.getFacsimile().saveToDisk();
            status.setDate(new Date());
            status.setAction(StatusStrings.ActionId.SAVED);
            if(result) status.setStatus(StatusStrings.StatusId.SUCCESS);
            else status.setStatus(StatusStrings.StatusId.FAIL);
            viewPager.recycle();
        }
        super.onPause();
    }

    /**
     * onCreateOptionsMenu events routine
     * @param menu The menu.
     * @return true value.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the ActionId bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mainMenu = menu;
        facsimileView.setMenu(menu);
        return true;
    }

    /**
     * Processes selection in menu. Calls the corresponding methods in FacsimileView.
     * @param item The selected menu item.
     * @return The boolean value defined in parent.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle ActionId bar item clicks here. The ActionId bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        int id = item.getItemId();
        if (id != R.id.action_goto && id != R.id.action_undo && id != R.id.action_redo) {
            for (int i = 0; i < mainMenu.size(); i++) {
                // Set default icons
                switch (mainMenu.getItem(i).getItemId()) {
                    case R.id.action_erase:
                        mainMenu.getItem(i).setIcon(R.drawable.eraser_off);
                        break;
                    case R.id.action_type:
                        mainMenu.getItem(i).setIcon(R.drawable.textbox_off);
                        break;
                    case R.id.action_brush:
                        mainMenu.getItem(i).setIcon(R.drawable.brush_off);
                        break;
                    case R.id.action_orthogonal_cut:
                        mainMenu.getItem(i).setIcon(R.drawable.orthogonal_cut_off);
                        break;
                    case R.id.action_precise_cut:
                        mainMenu.getItem(i).setIcon(R.drawable.precise_cut_off);
                        break;
                    case R.id.action_download_IIIF:
                        mainMenu.getItem(i).setIcon(android.R.drawable.stat_sys_download);
                        break;
                    case R.id.action_movement:
                        mainMenu.getItem(i).setIcon(R.drawable.movement_off);
                        break;
                }
            }
        }

        FacsimileView view = (FacsimileView) findViewById(R.id.facsimile_view);
        switch (id) {
            case R.id.action_erase:
                item.setIcon(R.drawable.eraser_on);
                view.eraseClicked();
                return true;
            case R.id.action_type:
                item.setIcon(R.drawable.textbox_on);
                view.typeClicked();
                return true;
            case R.id.action_brush:
                item.setIcon(R.drawable.brush_on);
                view.brushClicked();
                return true;
            case R.id.action_open:
                actionOpen();
                view.resetMenu();
                break;
            case R.id.action_orthogonal_cut:
                item.setIcon(R.drawable.orthogonal_cut_on);
                view.OrthogonalCutClicked();
                break;
            case R.id.action_precise_cut:
                item.setIcon(R.drawable.precise_cut_on);
                view.PreciseCutClicked();
                break;
            case R.id.action_goto:
                if(view.document != null)
                    view.gotoClicked();
                break;
            case R.id.action_movement:
                item.setIcon(R.drawable.movement_on);
                view.movementClicked();
                break;
            case R.id.action_settings:
                if(view.document != null)
                    view.settingsClicked();
                break;
            case R.id.action_undo:
                view.undoClicked();
                break;
            case R.id.action_redo:
                view.redoClicked();
                break;
            case R.id.action_download_IIIF:
                actionDownload();
                view.resetMenu();
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * Shows the system file selection dialog.
     */
    private void actionOpen() {
        Intent intent = new Intent((Intent.ACTION_OPEN_DOCUMENT_TREE));

        try {
            startActivityForResult(intent, 0);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }


    // Popup to download images from IIIF manifest file
    private void actionDownload(){


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("URL");


        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_TEXT);


        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                url = input.getText().toString();

                download_image();

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

    // Download images from IIIF manifest

    public void download_image(){


        mQueue = Volley.newRequestQueue(this);
        imgs = new String[1];



            // url = "https://iiif.harvardartmuseums.org/manifests/object/299843";



        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                    new Response.Listener<JSONObject>() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void onResponse(JSONObject response) {


                            try {


                                if (response.has("sequences")) {


                                    JSONArray jsonArray = response.getJSONArray("sequences");




                                    canv = jsonArray.getJSONObject(0);
                                    canvas = canv.getJSONArray("canvases");

                                    imgs = new String[canvas.length()];

                                    for(int i=0; i< canvas.length(); i++){
                                        img = canvas.getJSONObject(i);
                                        image = img.getJSONArray("images");
                                        res = image.getJSONObject(0);
                                        resource = res.getJSONObject("resource");
                                        ids = resource.getString("@id");
                                        imgs[i] = ids;
                                        downloadImageNew("test",imgs[i]);

                                    }


                                }else{

                                    JSONArray jsonArray = response.getJSONArray("items");

                                    imgs = new String[jsonArray.length()];


                                    for (int i=0; i<jsonArray.length(); i++){

                                        canv = jsonArray.getJSONObject(i);

                                        image = canv.getJSONArray("items");
                                        res = image.getJSONObject(0);
                                        item1 = res.getJSONArray("items");
                                        item2 = item1.getJSONObject(0);
                                        item3 = item2.getJSONArray("items");
                                        body = item3.getJSONObject(0);
                                        body2 = body.getJSONObject("body");
                                        ids = body2.getString("id");

                                        imgs[i] = ids;
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            downloadImageNew("test",imgs[i]);
                                        }


                                    }

                                    //canvas = c.getJSONArray("body");


                                }


                            } catch (JSONException | IOException e) {
                                e.printStackTrace();



                            }


                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                    display_popup();

                    error.printStackTrace();
                }

            });




            if(imgs == null){
                display_popup();
            }

        mQueue.add(request);


        }

       @RequiresApi(api = Build.VERSION_CODES.O)
       private void downloadImageNew(String filename, String downloadUrlOfImage) throws IOException {


           try{


               DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
               Uri downloadUri = Uri.parse(downloadUrlOfImage);
               DownloadManager.Request request = new DownloadManager.Request(downloadUri);
               request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                       .setAllowedOverRoaming(false)
                       .setTitle(filename)
                       .setMimeType("image/jpeg")
                       .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                       .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS , File.separator + filename + ".jpg");
               dm.enqueue(request);
               //Toast.makeText(this, "Image download started.", Toast.LENGTH_SHORT).show();
           }catch (Exception e){
               System.out.println(e);
           }
       }


    // Wrong url error message display

    public void display_popup(){


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error");

        // Set up the buttons
        builder.setMessage("Wrong url input");

        builder.show();
    }


    /**
     * Processes the result of system file selection dialog.
     * @param requestCode request code
     * @param resultCode result code
     * @param data intent
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 0:
                if (resultCode == RESULT_OK) {

                    if (data != null) {
                        dir = DocumentFile.fromTreeUri(this, data.getData());

                        loadFacsimile(dir);
                    }

                } else {
                    status.setDate(new Date());
                    status.setAction(StatusStrings.ActionId.LOADED);
                    status.setStatus(StatusStrings.StatusId.FAIL);

                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
