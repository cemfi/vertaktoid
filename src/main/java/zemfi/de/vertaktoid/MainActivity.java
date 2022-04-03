package zemfi.de.vertaktoid;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.support.annotation.RequiresApi;
import android.support.v4.provider.DocumentFile;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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

    //autosave
    private final Handler tmpSaveHandler = new Handler();
    private final Runnable tmpSaveRunnable = new Runnable() {

        final long start1 = System.nanoTime();
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
    private final ArrayList < String > imageUrl = new ArrayList();

    /**
     * Creates temporary MEI file.
     * The file name will be set to current datetime plus ".mei" extension.
     */
    protected void saveTemporaryMEI() {

        FacsimileView view = (FacsimileView) findViewById(R.id.facsimile_view);
        if (view.needToSave) {
            Date saveDate = new Date();
            String filename = "" + DateFormat.format("dd-MM-yyyy_kk-mm-ss", saveDate) + ".mei";
            if (view.getFacsimile() != null) {
                DocumentFile systemDir = dir.findFile(Vertaktoid.APP_SUBFOLDER);
                if (systemDir == null)
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

    public void viewProgress(){
        FacsimileView view = (FacsimileView) findViewById(R.id.facsimile_view);



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
        if (systemDir == null || !systemDir.exists()) {
            dir.createDirectory(Vertaktoid.APP_SUBFOLDER);
            systemDir = dir.findFile(Vertaktoid.APP_SUBFOLDER);
        }
        DocumentFile image404 = systemDir.findFile(Vertaktoid.NOT_FOUND_STUBIMG);
        if (image404 == null || !image404.exists()) {
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.facsimile404);
            try {
                image404 = systemDir.createFile("image/png", Vertaktoid.NOT_FOUND_STUBIMG);
                ParcelFileDescriptor pdf = getContentResolver().openFileDescriptor(image404.getUri(), "w");
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
                System.out.println("saved");
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
                    case R.id.action_save:
                        mainMenu.getItem(i).setIcon(R.drawable.save_off);
                }
            }
        }

        FacsimileView view = (FacsimileView) findViewById(R.id.facsimile_view);
        switch (id) {
            case R.id.action_erase:
                item.setIcon(R.drawable.eraser_on);
                mainMenu.getItem(10).setIcon(R.drawable.save_off);
                view.eraseClicked();
                return true;
            case R.id.action_type:
                item.setIcon(R.drawable.textbox_on);
                mainMenu.getItem(10).setIcon(R.drawable.save_off);
                view.typeClicked();
                return true;
            case R.id.action_brush:
                item.setIcon(R.drawable.brush_on);
                mainMenu.getItem(10).setIcon(R.drawable.save_off);
                view.brushClicked();
                return true;
            case R.id.action_open:
                actionOpen(0);
                mainMenu.getItem(10).setIcon(R.drawable.save_off);
                view.resetMenu();
                break;
            case R.id.action_orthogonal_cut:
                item.setIcon(R.drawable.orthogonal_cut_on);
                mainMenu.getItem(10).setIcon(R.drawable.save_off);
                view.OrthogonalCutClicked();
                break;
            case R.id.action_precise_cut:
                item.setIcon(R.drawable.precise_cut_on);
                mainMenu.getItem(10).setIcon(R.drawable.save_off);
                view.PreciseCutClicked();
                break;
            case R.id.action_goto:
                if (view.document != null)
                    view.gotoClicked();
                break;
            case R.id.action_movement:
                item.setIcon(R.drawable.movement_on);
                mainMenu.getItem(10).setIcon(R.drawable.save_off);
                view.movementClicked();
                break;
            case R.id.action_settings:
                if (view.document != null)
                    view.settingsClicked();
                mainMenu.getItem(10).setIcon(R.drawable.save_off);
                break;
            case R.id.action_undo:
                mainMenu.getItem(10).setIcon(R.drawable.save_off);
                view.undoClicked();
                break;
            case R.id.action_redo:
                mainMenu.getItem(10).setIcon(R.drawable.save_off);
                view.redoClicked();
                break;
            case R.id.action_download_IIIF:
                iiifManifestObj.urlInputPopup();
                view.resetMenu();
                 break;
            case R.id.action_save:
                item.setIcon(R.drawable.save_on);
                saveclicked();
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * Shows the system file selection dialog.
     */

    public void actionOpen(int requestCode) {

        Activity activity = (Activity) context;
        Intent intent = new Intent((Intent.ACTION_OPEN_DOCUMENT_TREE));


        try {
            activity.startActivityForResult(intent, requestCode);
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
                    dir2 = DocumentFile.fromTreeUri(this, data.getData());

                    try {
                        iiifManifestObj.downloadImage(dir2);
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }

                }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}