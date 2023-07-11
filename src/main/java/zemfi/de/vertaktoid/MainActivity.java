package zemfi.de.vertaktoid;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.provider.DocumentFile;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import zemfi.de.vertaktoid.databinding.ActivityMainBinding;
import zemfi.de.vertaktoid.helpers.Status;
import zemfi.de.vertaktoid.helpers.StatusStrings;
import zemfi.de.vertaktoid.model.Facsimile;


public class MainActivity extends AppCompatActivity {

    public static Activity context = null;
    final Status status = new Status();
    public boolean pause = false;

    IiifManifest iiifManifestObj = new IiifManifest() ;
    Menu mainMenu;
    DocumentFile dirf;

    //autosave
    private final Handler tmpSaveHandler = new Handler();
    private final Runnable tmpSaveRunnable = new Runnable() {

        @Override
        public void run() {
            saveTemporaryMEI();
            tmpSaveHandler.postDelayed(this, 300000);
        }
    };
    long end = System.nanoTime();
    private CustomViewPager viewPager;
    private Toolbar toolbar;
    private FacsimileView facsimileView;
    private DocumentFile dir;
    private DocumentFile dir2;

    /**
     * Creates temporary MEI file.
     * The file name will be set to current datetime plus ".mei" extension.
     */
    protected void saveTemporaryMEI() {

        FacsimileView view = (FacsimileView) findViewById(R.id.facsimile_view);
        if (view.needToSave) {
            Date saveDate = new Date();
            if (view.getFacsimile() != null) {
                DocumentFile systemDir = dir.findFile(Vertaktoid.APP_SUBFOLDER);
                if (systemDir == null)
                    systemDir = dir.createDirectory(Vertaktoid.APP_SUBFOLDER);

                boolean result = view.getFacsimile().saveToDisk(systemDir, Vertaktoid.APP_SUBFOLDER+Vertaktoid.DEFAULT_MEI_EXTENSION);
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




    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void loadFacsimile(DocumentFile dir) {
        facsimileView = (FacsimileView) MainActivity.context.findViewById(R.id.facsimile_view);

        // facsimile contains pages, movements, breaks
        Facsimile facsimile = new Facsimile();

        // create subfolder (for MEI) and dummy image file
        prepareApplicationFiles(dir);

        facsimile.openDirectory(dir);
        System.out.println("name of the folder " + facsimile.dir.getName());

        facsimileView.setFacsimile(facsimile);
        viewPager = (CustomViewPager) MainActivity.context.findViewById(R.id.view_pager);

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
        if (systemDir == null || !systemDir.exists()) {
            dir.createDirectory(Vertaktoid.APP_SUBFOLDER);
            systemDir = dir.findFile(Vertaktoid.APP_SUBFOLDER);
        }
        DocumentFile image404 = systemDir.findFile(Vertaktoid.NOT_FOUND_STUBIMG);
        if (image404 == null || !image404.exists()) {
            if(this.context == null){
                this.context = MainActivity.context;
            }
            Bitmap bm = BitmapFactory.decodeResource(MainActivity.context.getResources(), R.drawable.facsimile404);

            try {
                image404 = systemDir.createFile("image/png", Vertaktoid.NOT_FOUND_STUBIMG);
                ParcelFileDescriptor pdf = MainActivity.context.getContentResolver().openFileDescriptor(image404.getUri(), "w");
                FileOutputStream outStream = new FileOutputStream(pdf.getFileDescriptor());
                bm.compress(Bitmap.CompressFormat.PNG, 100, outStream);
                outStream.flush();
                outStream.close();
            } catch (FileNotFoundException ex) {

            } catch (IOException ex) {

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
        } catch (Exception e) {

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
    protected void saveclicked() {
        FacsimileView view = (FacsimileView) findViewById(R.id.facsimile_view);
        if (view.getFacsimile() != null) {
            boolean result = view.getFacsimile().saveToDisk();

            status.setDate(new Date());
            status.setAction(StatusStrings.ActionId.SAVED);
            if (result) status.setStatus(StatusStrings.StatusId.SUCCESS);
            else status.setStatus(StatusStrings.StatusId.FAIL);
        }else{

        }
        pause = true;
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
            if (result) status.setStatus(StatusStrings.StatusId.SUCCESS);
            else status.setStatus(StatusStrings.StatusId.FAIL);
            viewPager.recycle();
        }
        pause = true;
        super.onPause();
    }
    @Override
    protected void onDestroy() {

            FacsimileView view = (FacsimileView) findViewById(R.id.facsimile_view);
            if (view.getFacsimile() != null) {
                boolean result = view.getFacsimile().saveToDisk();
                status.setDate(new Date());
                status.setAction(StatusStrings.ActionId.SAVED);
                if (result) status.setStatus(StatusStrings.StatusId.SUCCESS);
                else status.setStatus(StatusStrings.StatusId.FAIL);
                viewPager.recycle();
            }
            super.onDestroy();


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
    @RequiresApi(api = Build.VERSION_CODES.R)
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
                    case R.id.action_measure_detector:
                        mainMenu.getItem(i).setIcon(R.drawable.ruler);
                        break;
                    case R.id.action_movement:
                        mainMenu.getItem(i).setIcon(R.drawable.movement_off);
                        break;
                    case R.id.action_erase_page:
                        mainMenu.getItem(i).setIcon(R.drawable.eraser_off);
                        break;
                    case R.id.action_save:
                        mainMenu.getItem(i).setIcon(R.drawable.save_off);
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
                actionOpen(0, this.dir);
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
                if (view.document != null)
                    view.gotoClicked();
                break;
            case R.id.action_movement:
                item.setIcon(R.drawable.movement_on);
                view.movementClicked();
                break;
            case R.id.action_settings:
                if (view.document != null)
                    view.settingsClicked();
                break;
            case R.id.action_undo:
                view.undoClicked();
                break;
            case R.id.action_pdf:
                actionOpen(4, this.dir);
                break;
            case R.id.action_redo:
                view.redoClicked();
                break;
            case R.id.action_download_IIIF:
                iiifManifestObj.urlInputPopup();
                view.resetMenu();
                 break;
            case R.id.action_measure_detector:
                item.setIcon(R.drawable.ruler);
                try {
                    view.measurPageClicked();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                view.resetMenu();
                break;
            case R.id.action_erase_page:
                item.setIcon(R.drawable.eraser_on);
                view.erasePageClicked();
                break;
            case R.id.action_erase_all:
                item.setIcon(R.drawable.eraser_on);
                view.eraseAllClicked();
                break;
            case R.id.action_save:
                item.setIcon(R.drawable.save_off);
                saveclicked();

        }
        return super.onOptionsItemSelected(item);
    }
/**
    public void iiif_view() {
        ImagePopup imagePopup = new ImagePopup(this);


        imagePopup.setWindowHeight(800); // Optional
        imagePopup.setWindowWidth(800); // Optional
        imagePopup.setBackgroundColor(Color.WHITE);  // Optional
        imagePopup.setFullScreen(true); // Optional
        imagePopup.setHideCloseIcon(true);  // Optional
        imagePopup.setImageOnClickClose(true);  // Optional

        ImageView imageView = (ImageView) findViewById(R.id.iiifimage);

        imagePopup.initiatePopupWithPicasso("https://ids.lib.harvard.edu/ids/iiif/437958013/125,15,200,200/!800,800/0/default.jpg"); // Load Image from Drawable
        imagePopup.viewPopup();


    }
*/

    /**
     * Shows the system file selection dialog.
     */

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void actionOpen(int requestCode, DocumentFile dirf) {
        Activity activity = (Activity) context;
        Intent intent = new Intent((Intent.ACTION_OPEN_DOCUMENT_TREE));
        this.dirf = dirf;
        if(requestCode == 0){
            try {
                activity.startActivityForResult(Intent.createChooser(intent,"choose pdf file"), requestCode);
            } catch (android.content.ActivityNotFoundException ex) {
                // Potentially direct the user to the Market with a Dialog
                Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
            }
        }
        if(requestCode == 1) {
            try {
                activity.startActivityForResult(Intent.createChooser(intent,"choose pdf file"), requestCode);
            } catch (android.content.ActivityNotFoundException ex) {
                // Potentially direct the user to the Market with a Dialog
                Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
            }

        }
        if(requestCode == 3){
            loadFacsimile(dirf);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            try {
                activity.startActivityForResult(Intent.createChooser(intent,"choose pdf file"), requestCode);
            } catch (android.content.ActivityNotFoundException ex) {
                // Potentially direct the user to the Market with a Dialog
                Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if(requestCode == 4){
            Intent intent2 = new Intent(Intent.ACTION_GET_CONTENT);
            intent2.setType("application/pdf");
            try {
                activity.startActivityForResult(Intent.createChooser(intent2,"choose pdf file"), requestCode);

            } catch (android.content.ActivityNotFoundException ex) {
                // Potentially direct the user to the Market with a Dialog
                Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
            }
        }



    }

    /**
     * Processes the result of system file selection dialog.
     * @param requestCode request code
     * @param resultCode result code
     * @param data intent
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
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
            case 1:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        dir2 = DocumentFile.fromTreeUri(this, data.getData());
                        try {
                            iiifManifestObj.downloadImage(dir2);
                        } catch (IOException | InterruptedException | JSONException e) {
                            e.printStackTrace();
                        }                    }

                }
                break;
            case 4:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    DocumentFile documentFile = DocumentFile.fromSingleUri(context, uri);
                    String path = "/storage/emulated/0/" + documentFile.getUri().getPath().replace("/document/primary:", "");
                    File file = new File(path);
                    String filePathWithoutFileName = file.getParent();

                    try {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setMessage("Are you sure you want to select this file?")
                                .setCancelable(false)
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // User confirmed selection, do something with the selected file
                                        try {
                                            facsimileView.openpdfClicked(file, filePathWithoutFileName);
                                            loadFacsimile(DocumentFile.fromFile(new File(file.getParent())));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }

                                    }
                                })
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // User cancelled selection, remove the alert and go back to file selection
                                        dialog.dismiss();
                                    }
                                });
                        AlertDialog alert = builder.create();
                        alert.show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }


        }
        super.onActivityResult(requestCode, resultCode, data);

    }

    public static String getPath(Context context, Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return getPathFromUriAboveKitkat(context, uri);
        } else {
            return getPathFromUriBelowKitkat(context, uri);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static String getPathFromUriAboveKitkat(Context context, Uri uri) {
        String filePath = "";
        String wholeID = DocumentsContract.getDocumentId(Uri.parse("content://com.android.externalstorage.documents/tree/primary/DCIM"));

        String id = wholeID.split(":")[1];
        String[] column = {MediaStore.Images.Media.DATA};
        String sel = MediaStore.Images.Media._ID + "=?";

        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                column, sel, new String[]{id}, null);

        int columnIndex = cursor.getColumnIndex(column[0]);

        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }
        cursor.close();
        return filePath;
    }

    private static String getPathFromUriBelowKitkat(Context context, Uri uri) {
        String filePath = "";
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);

        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            filePath = cursor.getString(column_index);
        }
        cursor.close();
        return filePath;
    }
}