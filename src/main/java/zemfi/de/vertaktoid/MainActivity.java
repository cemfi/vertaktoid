package zemfi.de.vertaktoid;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.io.File;
import android.text.format.DateFormat;

import java.util.Date;

import zemfi.de.vertaktoid.databinding.ActivityMainBinding;
import android.databinding.DataBindingUtil;

public class MainActivity extends AppCompatActivity {

    final String TAG = "de.zemfi.vertaktoid";
    private Handler tmpSaveHandler = new Handler();
    private Runnable tmpSaveRunnable = new Runnable() {
        @Override
        public void run() {
            saveTemporaryMEI();
            tmpSaveHandler.postDelayed(this, 300000);
        }
    };

    protected void saveTemporaryMEI() {
        FacsimileView view = (FacsimileView) findViewById(R.id.custom_view);
        if(view.needToSave) {
            Date saveDate = new Date();
            String filename = "" + DateFormat.format("dd-MM-yyyy_kk-mm-ss", saveDate) + ".mei";
            if (view.getFacsimile() != null) {
                boolean result = view.getFacsimile().saveToDisk(path, filename);
                status.setDate(saveDate);
                status.setAction(StatusStrings.ActionId.TMP_SAVED);
                if (result) status.setStatus(StatusStrings.StatusId.SUCCESS);
                else status.setStatus(StatusStrings.StatusId.FAIL);
            }
            view.needToSave = false;
        }
    }

    final Status status = new Status();

    private String getContentName(ContentResolver resolver, Uri uri){
        Cursor cursor = resolver.query(uri, null, null, null, null);
        cursor.moveToFirst();
        int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
        if (nameIndex >= 0) {
            return cursor.getString(nameIndex);
        } else {
            return null;
        }
    }


    protected MEIInOut meiInOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SubsamplingScaleImageView view = (SubsamplingScaleImageView) findViewById(R.id.custom_view);
        view.setMinimumDpi(40);
        FacsimileView facsimileView = (FacsimileView) findViewById(R.id.custom_view);

        SharedPreferences prefs = this.getSharedPreferences("zemfi.de.vertaktoid", Context.MODE_PRIVATE);
        path = prefs.getString("zemfi.de.vertaktoid.path", "");
        if(!path.equals("")) {
            Facsimile facsimile = new Facsimile();
            facsimile.openDirectory(path);

            facsimileView.setFacsimile(facsimile);
            status.setDate(new Date());
            status.setAction(StatusStrings.ActionId.LOADED);
            status.setStatus(StatusStrings.StatusId.SUCCESS);
        } else {
            view.setImage(ImageSource.resource(R.drawable.handel));
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    actionOpen();
                }
            });
        }

        binding.setFview(facsimileView);
        status.setStatus(StatusStrings.StatusId.SUCCESS);
        status.setAction(StatusStrings.ActionId.STARTED);
        binding.setCstatus(status);

        Intent intent = getIntent();
        String action = intent.getAction();

        if (action != null) {
            if (action.compareTo(Intent.ACTION_VIEW) == 0) {
                String scheme = intent.getScheme();
                ContentResolver resolver = getContentResolver();
            }
        }

        meiInOut = new MEIInOut();
        tmpSaveHandler.postDelayed(tmpSaveRunnable, 300000);

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString("path", path);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        path = savedInstanceState.getString("path");
    }

    @Override
    protected void onResume() {
        FacsimileView view = (FacsimileView) findViewById(R.id.custom_view);
        view.setImage(ImageSource.uri(Uri.fromFile(new File(view.document.pages.get(view.pageNumber.get()).filePath))));
        super.onResume();
    }

    @Override
    protected void onPause() {
        FacsimileView view = (FacsimileView) findViewById(R.id.custom_view);
        if (view.getFacsimile() != null) {
            boolean result = view.getFacsimile().saveToDisk();
            status.setDate(new Date());
            status.setAction(StatusStrings.ActionId.SAVED);
            if(result) status.setStatus(StatusStrings.StatusId.SUCCESS);
            else status.setStatus(StatusStrings.StatusId.FAIL);
            view.recycle();
        }
        super.onPause();
    }

    Menu mainMenu;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the ActionId bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mainMenu = menu;

        FacsimileView view = (FacsimileView) findViewById(R.id.custom_view);
        view.setMenu(menu);

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle ActionId bar item clicks here. The ActionId bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        int id = item.getItemId();
        if (id != R.id.action_plus && id != R.id.action_minus) {
            for (int i = 0; i < mainMenu.size(); i++) {
                // Set default icons
                if (mainMenu.getItem(i).getItemId() == R.id.action_erase) {
                    mainMenu.getItem(i).setIcon(R.drawable.eraseroff);
                } else if (mainMenu.getItem(i).getItemId() == R.id.action_type) {
                    mainMenu.getItem(i).setIcon(R.drawable.textboxoff);
                } else if (mainMenu.getItem(i).getItemId() == R.id.action_brush) {
                    mainMenu.getItem(i).setIcon(R.drawable.brushoff);
                } else if (mainMenu.getItem(i).getItemId() == R.id.action_cut) {
                    mainMenu.getItem(i).setIcon(R.drawable.cutoff);
                }
            }
        }

        FacsimileView view = (FacsimileView) findViewById(R.id.custom_view);
        if (id == R.id.action_erase) {
            item.setIcon(R.drawable.eraseron);
            view.eraseClicked();
            return true;
        }
        else if (id == R.id.action_type) {
            item.setIcon(R.drawable.textboxon);
            view.typeClicked();
            return true;
        }
        else if (id == R.id.action_brush) {
            item.setIcon(R.drawable.brushon);
            view.brushClicked();
            return true;
        }
        else if (id == R.id.action_open) {

            actionOpen();
            view.resetMenu();

        }
        else if (id == R.id.action_plus) {
            view.plusClicked();
        }
        else if (id == R.id.action_minus) {
            view.minusClicked();
        }
        else if (id == R.id.action_cut) {
            item.setIcon(R.drawable.cuton);
            view.cutClicked();
        }


        return super.onOptionsItemSelected(item);
    }


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


    String path = null;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 0:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    path = getPath(getApplicationContext(), uri);
                    if(path != null) {
                        File jpgFile = new File(path);
                        File f = new File(jpgFile.getParent());
                        path = f.getAbsolutePath();
                    }
                    //Log.v("path: ", path);

                    Facsimile facsimile = new Facsimile();
                    facsimile.openDirectory(path);

                    FacsimileView view = (FacsimileView) findViewById(R.id.custom_view);
                    view.setFacsimile(facsimile);
                    status.setDate(new Date());
                    status.setAction(StatusStrings.ActionId.LOADED);
                    status.setStatus(StatusStrings.StatusId.SUCCESS);
                    view.setOnClickListener(null);

                    SharedPreferences prefs = this.getSharedPreferences("zemfi.de.vertaktoid", Context.MODE_PRIVATE);
                    SharedPreferences.Editor mEditor = prefs.edit();
                    mEditor.putString("zemfi.de.vertaktoid.path", path).apply();
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