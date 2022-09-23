package zemfi.de.vertaktoid;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.provider.DocumentFile;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import zemfi.de.vertaktoid.mei.MEIHelper;

/**
 * Download Images from IIIF Manifest
 */
public class IiifManifest extends Activity {

    public String url = "", ids;
    public String usrname, pss= "";
    private RequestQueue mQueue;
    public static String[] imgs;
    public JSONObject canv, img, res, resource, item2, body, body2;
    public JSONArray canvas, image, item1, item3;
    public ArrayList<String> imageUrl = new ArrayList();
    public int progress = 0;
    public Dialog downloadProgressDialogue = null;
    public ProgressBar text;
    public TextView text2;

    public ArrayList<Long> downloadIds = new ArrayList<>();
    public DownloadManager dm;
    public boolean canceled = false;
    public String usernamePasswordBase64 = "";


    /**
     * Display popup input window to insert url of IIIF manifest
     */


    public void urlInputPopup() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.context);
        builder.setTitle("URL");

        // Set up the input
        final EditText input = new EditText(MainActivity.context);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_TEXT);


        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(DialogInterface dialog, int which) {
                url = input.getText().toString();

                jsonparser(null);

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
    /**
     * Display popup input window to insert username and password
     */


    @RequiresApi(api = Build.VERSION_CODES.O)
    public void authenticationPopup() {
    /*
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.context);
        builder.setTitle("Please insert your username and password");

        // Set up the username
        final EditText username = new EditText(MainActivity.context);
        final EditText password = new EditText(MainActivity.context);

        // Specify the type of username expected; this, for example, sets the username as a password, and will mask the text
        username.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_TEXT);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_TEXT);


        builder.setView(username);
        builder.setView(password);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {


            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
        */
        System.out.println("try it at 142");
        final AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.context);

        LinearLayout lila1= new LinearLayout(MainActivity.context);
        lila1.setOrientation(LinearLayout.VERTICAL);
        final EditText username = new EditText(MainActivity.context);
        final EditText password = new EditText(MainActivity.context);
        username.setHint("username");
        password.setHint("password");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setTransformationMethod(PasswordTransformationMethod.getInstance());
        lila1.addView(username);
        lila1.addView(password);
        alert.setView(lila1);

        alert.setTitle("Login");
        System.out.println("try it at 156");
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String authorization = username.getText().toString()+":"+password.getText().toString();
                jsonparser(authorization);

            }                     });
        alert.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();    }     });
       alert.show();
}

    /**
     * Parse manifest file from the given url
     */

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void jsonparser(String autherization) {
        if (autherization == null){

        }else  {
            System.out.println("188 " + autherization);
            setUsernamePasswordBase64(Base64.getEncoder().encodeToString(autherization.getBytes()));
        }



        mQueue = Volley.newRequestQueue(MainActivity.context);
        imgs = new String[1];
        System.out.println("authentication " + autherization);


        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onResponse(JSONObject response) {

                        System.out.println("check at line 196");
                        System.out.println(response.toString());

                        try {
                            if (response.has("sequences")) {
                                JSONArray jsonArray = response.getJSONArray("sequences");
                                canv = jsonArray.getJSONObject(0);
                                canvas = canv.getJSONArray("canvases");

                                imgs = new String[canvas.length()];

                                for (int i = 0; i < canvas.length(); i++) {
                                    img = canvas.getJSONObject(i);
                                    image = img.getJSONArray("images");
                                    res = image.getJSONObject(0);
                                    resource = res.getJSONObject("resource");
                                    ids = resource.getString("@id");
                                    imgs[i] = ids;
                                    imageUrl.add(imgs[i]);

                                }


                            } else {

                                JSONArray jsonArray = response.getJSONArray("items");

                                imgs = new String[jsonArray.length()];


                                for (int i = 0; i < jsonArray.length(); i++) {

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
                                        imageUrl.add(imgs[i]);
                                    }

                                }

                            }
                            chooseDownloadDirectory(1);

                        } catch (JSONException | IOException e) {
                            e.printStackTrace();

                        }


                    }
                },
                new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {

                if(volleyError.toString().contains("Bad URL")){
                    displayError();
                }
                else if(volleyError.networkResponse.statusCode == 401){
                    authenticationPopup();
                }
            }
        }) {/**
             * Passing some request headers
             */
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                // headers.put("Content-Type", "application/json");
                headers.put("Authorization", "Basic " + getUsernamePasswordBase64());
                return headers;
            }
        };

        if (imgs == null) {
            displayError();
        }

        mQueue.add(request);


    }

    @RequiresApi(api = Build.VERSION_CODES.O)

    /**
     * Choose directory to download
     */
    public void chooseDownloadDirectory(int requestCode) throws IOException {
        MainActivity mainActivity = new MainActivity();
        mainActivity.actionOpen(requestCode);
        FacsimileView view = (FacsimileView) MainActivity.context.findViewById(R.id.facsimile_view);
        view.resetMenu();
    }

    /**
     * Get username and password
     *
     */
    public String getUsernamePasswordBase64(){
        return this.usernamePasswordBase64;
    }
    public void setUsernamePasswordBase64(String up){
        this.usernamePasswordBase64 = up;
    }
    /**
     * Display error message for wrong url
     */

    public void displayError() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.context);
        builder.setTitle("Error");
        // Set up the buttons
        builder.setMessage("Wrong url input");
        builder.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)

    /**
     * Download images into the selected folder
     */

    public void downloadImage(DocumentFile df) throws IOException, InterruptedException {

        String directorypath = df.getUri().toString();
        String decodedurl = URLDecoder.decode(directorypath, "UTF-8");
        String trimedurl = decodedurl.replace(":", "/");
        String parseurl = trimedurl.substring(trimedurl.indexOf("document/primary/"));
        String finalurl = parseurl.replace("document/primary/", "");
        String[] parts = finalurl.split("/");
        String subPath = "";
        System.out.println("Line 342");
        progress = 0;
        downloadIds = new ArrayList<>();
        dm = (DownloadManager) MainActivity.context.getSystemService(Context.DOWNLOAD_SERVICE);
        canceled = false;
        System.out.println("Line 347");


        if (parts.length > 1) {
            for (int i = 1; i < parts.length; i++) {
                subPath = subPath + "/" + parts[i];
            }
        }
        final String subPathFinal = subPath;
        System.out.println("Line 356");

        progressBarContent(imageUrl.size());

        Button cancel = (Button) downloadProgressDialogue.findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {

               canceled = true;
               ArrayList<Long> clone = (ArrayList<Long>) downloadIds.clone();

               try {
                   for(long l : clone) {
                       dm.remove(l);
                   }
               }catch (Exception e) {
                   System.out.println(e);
               }

               downloadProgressDialogue.dismiss();
           }
       });
        displayProgress();

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < imageUrl.size(); i++) {

                    if(canceled) break;

                    try {


                        Uri downloadUri = Uri.parse(imageUrl.get(i));
                        DownloadManager.Request request = new DownloadManager.Request(downloadUri);
                        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                                .setAllowedOverRoaming(false)
                                .setTitle(MEIHelper.date)
                                .setMimeType("image/jpg")
                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                .setDestinationInExternalPublicDir(parts[0], "/" + subPathFinal + "/" + leadingZeros(i) + ".jpg")
                                .addRequestHeader("Authorization", "Basic " + getUsernamePasswordBase64());

                        if(canceled) break;
                        long downloadId = dm.enqueue(request);
                        downloadIds.add(0, downloadId);

                        getDownloadStatus(downloadId, imageUrl.size(), dm);

                    }catch (Exception e){
                        System.out.println("Failed with error " + e);
                    }finally {

                    }
                }
                imageUrl.clear();
            }
        }).start();
    }

    public void progressBarContent(int size){

        downloadProgressDialogue = new Dialog(MainActivity.context);
        downloadProgressDialogue.requestWindowFeature(Window.FEATURE_NO_TITLE);
        downloadProgressDialogue.setCancelable(false);
        downloadProgressDialogue.setContentView(R.layout.download_progress);

        text = (ProgressBar) downloadProgressDialogue.findViewById(R.id.progress_horizontal);
        text.setMax(size);
        text2 = (TextView) downloadProgressDialogue.findViewById(R.id.value123);
        text2.setText("0");

    }
    public void displayProgress(){
        downloadProgressDialogue.show();
        Window window = downloadProgressDialogue.getWindow();
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }
    private String leadingZeros(int i) {
        if (i < 10)
            return "0000" + i;
        if (i < 100)
            return "000" + i;
        if (i < 1000)
            return "00" + i;
        if (i < 10000)
            return "0" + i;

        return "" + i;

    }

    public void updateProgressBar(int totalImage){

                progress++;
                if (progress == totalImage){
                    downloadProgressDialogue.dismiss();
                    return;
                }
                text.setProgress(progress);
                text2.setText(String.valueOf(Math.round(((float) progress/(float)totalImage)*100.00)));
    }


    private void getDownloadStatus(long downloadId, int totalImage, DownloadManager dm) {

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor cursor = ((DownloadManager) MainActivity.context.getSystemService(Context.DOWNLOAD_SERVICE))
                .query(query);

        if (cursor.moveToFirst())
        {
            cursor.close();
            final Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {

                    if(canceled) {
                        timer.cancel();
                        return;
                    }

                    query.setFilterById(downloadId);
                    Cursor cursor = ((DownloadManager)MainActivity.context.getSystemService(Context.DOWNLOAD_SERVICE))
                            .query(query);
                    cursor.moveToFirst();

                    try {
                        int status=cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                        int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
                        cursor.close();

                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            timer.cancel();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateProgressBar(totalImage);
                                }
                            });
                        }else {

                            // System.out.println("error is occured here " +  reason);
                        }
                    }catch(Exception e) {
                        timer.cancel();
                        System.out.println(e);
                    }
                }
            }, 100,1);
        }
    }

}







