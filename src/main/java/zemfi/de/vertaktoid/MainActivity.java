package zemfi.de.vertaktoid;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import android.text.format.DateFormat;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import zemfi.de.vertaktoid.databinding.ActivityMainBinding;
import zemfi.de.vertaktoid.helpers.Status;
import zemfi.de.vertaktoid.helpers.StatusStrings;
import zemfi.de.vertaktoid.model.Facsimile;

import android.databinding.DataBindingUtil;

public class MainActivity extends AppCompatActivity {

    final String TAG = "de.zemfi.vertaktoid";
    final Status status = new Status();
    Menu mainMenu;
    String path = null;
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
                boolean result = view.getFacsimile().saveToDisk(path + "/" + Vertaktoid.APP_SUBFOLDER, filename);
                status.setDate(saveDate);
                status.setAction(StatusStrings.ActionId.TMP_SAVED);
                if (result) status.setStatus(StatusStrings.StatusId.SUCCESS);
                else status.setStatus(StatusStrings.StatusId.FAIL);
            }
            view.needToSave = false;
        }
    }

    /*private String getContentName(ContentResolver resolver, Uri uri){
        Cursor cursor = resolver.query(uri, null, null, null, null);
        cursor.moveToFirst();
        int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
        if (nameIndex >= 0) {
            return cursor.getString(nameIndex);
        } else {
            return null;
        }
    }*/

    /**
     * Android application lifecycle: onCreate event.
     * @param savedInstanceState The instance state bundle.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        viewPager = (CustomViewPager) findViewById(R.id.view_pager);
        viewPager.setOffscreenPageLimit(1);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        facsimileView = (FacsimileView) findViewById(R.id.facsimile_view);

        SharedPreferences prefs = this.getSharedPreferences("zemfi.de.vertaktoid", Context.MODE_PRIVATE);
        path = prefs.getString("zemfi.de.vertaktoid.path", "");
        File dir = new File(path);
        File files[] = dir.listFiles();

        if(path.equals("")) {
            //important on start-up
            //view.setImage(ImageSource.resource(R.drawable.handel));
            Toast.makeText(this, "No folder selected. Please choose a file.", Toast.LENGTH_LONG).show();
        } else if(files==null)
        {
            //important on moved folder
            Toast.makeText(this, "Image file not found. Please choose a file.", Toast.LENGTH_LONG).show();
        } else {
            loadFacsimile(path);
        }

        binding.setFview(facsimileView);
        status.setStatus(StatusStrings.StatusId.SUCCESS);
        status.setAction(StatusStrings.ActionId.STARTED);
        binding.setCstatus(status);

        tmpSaveHandler.postDelayed(tmpSaveRunnable, 300000);
    }

    private void loadFacsimile(String path) {
        Facsimile facsimile = new Facsimile();
        File dir = new File(path);
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

        SharedPreferences prefs = this.getSharedPreferences("zemfi.de.vertaktoid", Context.MODE_PRIVATE);
        SharedPreferences.Editor mEditor = prefs.edit();
        mEditor.putString("zemfi.de.vertaktoid.path", path).apply();

        status.setDate(new Date());
        status.setAction(StatusStrings.ActionId.LOADED);
        status.setStatus(StatusStrings.StatusId.SUCCESS);
    }

    /**
     * Creates subfolder and dummy image file for not founded pages.
     * The temporary MEI files will be stored in created subfolder.
     * @param dir The directory.
     */
    private void prepareApplicationFiles(File dir) {
        File systemDir = new File (dir, Vertaktoid.APP_SUBFOLDER);
        if(!systemDir.exists()) {
            systemDir.mkdir();
        }
        File image404 = new File (systemDir, Vertaktoid.NOT_FOUND_STUBIMG);
        if(!image404.exists()) {
            Bitmap bm = BitmapFactory.decodeResource( getResources(), R.drawable.facsimile404);
            try {
                FileOutputStream outStream = new FileOutputStream(image404);
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
        savedInstanceState.putString("path", path);
    }

    /**
     * Android application lifecycle: onRestoreInstanceState event.
     * @param savedInstanceState The instance state bundle.
     */
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        try {
            super.onRestoreInstanceState(savedInstanceState);
            path = savedInstanceState.getString("path");
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
     * onCreateIptionMenu events routine
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
                view.gotoClicked();
                break;
            case R.id.action_movement:
                item.setIcon(R.drawable.movement_on);
                view.movementClicked();
                break;
            case R.id.action_settings:
                view.settingsClicked();
                break;
            case R.id.action_undo:
                view.undoClicked();
                break;
            case R.id.action_redo:
                view.redoClicked();
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * Shows the system file selection dialog.
     */
    private void actionOpen() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "Choose Directory"), 0);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
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
                    Uri uri = data.getData();
                    path = getPath(getApplicationContext(), uri);
                    File dir = new File(path);
                    if(path != null) {
                        File jpgFile = new File(path);
                        dir = new File(jpgFile.getParent());
                        path = dir.getAbsolutePath();
                    }
                    loadFacsimile(path);
                }
                else {
                    status.setDate(new Date());
                    status.setAction(StatusStrings.ActionId.LOADED);
                    status.setStatus(StatusStrings.StatusId.FAIL);

                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
