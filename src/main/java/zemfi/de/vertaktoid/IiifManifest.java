package zemfi.de.vertaktoid;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.provider.DocumentFile;
import android.text.InputType;
import android.widget.EditText;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;

import zemfi.de.vertaktoid.mei.MEIHelper;





/**
 * Download Images from IIIF Manifest
 */
public class IiifManifest {

    public String url = "", ids;
    private RequestQueue mQueue;
    public static String imgs[];
    public JSONObject canv, img, res, resource, item2, body, body2;
    public JSONArray canvas, image, item1, item3;
    public ArrayList<String> imageUrl = new ArrayList();

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
            @Override
            public void onClick(DialogInterface dialog, int which) {
                url = input.getText().toString();

                jsonparser();

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
     * Parse manifest file from the given url
     */

    public void jsonparser() {


        mQueue = Volley.newRequestQueue(MainActivity.context);
        imgs = new String[1];


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
                }, error -> {

            displayError();

            error.printStackTrace();
        });


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
     * Display error message for wrong url
     */

    public void displayError(){
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

    public void downloadImage(DocumentFile df) throws IOException {

        String directorypath = df.getUri().toString();
        String decodedurl = URLDecoder.decode(directorypath, "UTF-8");
        String trimedurl = decodedurl.replace(":","/");
        String parseurl = trimedurl.substring(trimedurl.indexOf("document/primary/"));
        String finalurl = parseurl.replace("document/primary/", "");


        for(int i=0; i< imageUrl.size(); i++){

            try{
                DownloadManager dm = (DownloadManager) MainActivity.context.getSystemService(Context.DOWNLOAD_SERVICE);
                Uri downloadUri = Uri.parse(imageUrl.get(i));
                DownloadManager.Request request = new DownloadManager.Request(downloadUri);
                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                        .setAllowedOverRoaming(false)
                        .setTitle(MEIHelper.date)
                        .setMimeType("image/jpeg")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(finalurl, File.separator + MEIHelper.date + ".jpg");
                dm.enqueue(request);
            }catch (Exception e){
                System.out.println(e);
            }

        }
        imageUrl.clear();
        chooseDownloadDirectory(0);
    }
}
