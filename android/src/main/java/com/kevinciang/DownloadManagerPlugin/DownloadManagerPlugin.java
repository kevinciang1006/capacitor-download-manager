package com.kevinciang.DownloadManagerPlugin;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.net.Uri;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
//import com.getcapacitor.PluginResult;
//
//import org.json.JSONObject;
//import org.json.JSONArray;
import org.json.JSONException;

@NativePlugin()
public class DownloadManagerPlugin extends Plugin {

    private static final String LOG_TAG = "Downloader";

    public static final String WRITE_EXTERNAL_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    public static final int DOWNLOAD_ACTION_PERMISSION_REQ_CODE = 1;

    public static final String PERMISSION_DENIED_ERROR = "PERMISSION DENIED";

    DownloadManager downloadManager;
    BroadcastReceiver receiver;

    long downloadId = 0;

    @PluginMethod()
    public void echo(PluginCall call) {
        String value = call.getString("value");

        JSObject ret = new JSObject();
        ret.put("value", value);
        call.success(ret);
    }

    @PluginMethod()
    public void enqueue(PluginCall call) {

        DownloadManager.Request req = new DownloadManager.Request((Uri.parse(call.getString("uri"))));

        req.setTitle(call.getString("title"));
        req.setDescription(call.getString("description"));
        req.setMimeType(call.getString("mimeType"));
        req.setVisibleInDownloadsUi(call.getBoolean("visibleInDownloadsUi", true));
        req.setNotificationVisibility(call.getInt("notificationVisibility"));

        if (call.getObject("destinationInExternalFilesDir", null) != null) {
            Context context = bridge.getActivity()
                    .getApplication()
                    .getApplicationContext();

            JSObject params = call.getObject("destinationInExternalFilesDir");
            req.setDestinationInExternalFilesDir(context, params.getString("dirType"), params.getString("subPath"));
        }
        else if (call.getObject("destinationInExternalPublicDir", null) != null) {
            JSObject params = call.getObject("destinationInExternalPublicDir");
            req.setDestinationInExternalPublicDir(params.getString("dirType"), params.getString("subPath"));
        }
        else if (call.getString("destinationUri", null) != null) {
            req.setDestinationUri(Uri.parse(call.getString("destinationUri")));
        }

        if (call.getArray("headers", null) != null) {
            JSArray arrHeaders = call.getArray("headers", new JSArray());
            for (int i = 0; i < arrHeaders.length(); i++) {
                JSObject headerObj = null;
                try {
                    headerObj = (JSObject) arrHeaders.getJSONObject(i);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                req.addRequestHeader(headerObj.getString("header"), headerObj.getString("value"));
            }
        }

        long downloadId = downloadManager.enqueue(req);

        JSObject ret = new JSObject();
        ret.put("id", Long.toString(downloadId));

        call.resolve(ret);
    }

//    protected DownloadManager.Request deserialiseRequest(JSONObject obj) throws JSONException {
//        DownloadManager.Request req = new DownloadManager.Request(Uri.parse(obj.getString("uri")));
//
//        req.setTitle(obj.optString("title"));
//        req.setDescription(obj.optString("description"));
//        req.setMimeType(obj.optString("mimeType", null));
//
//        if (obj.has("destinationInExternalFilesDir")) {
//            Context context = bridge.getActivity()
//                    .getApplication()
//                    .getApplicationContext();
//
//            JSONObject params = obj.getJSONObject("destinationInExternalFilesDir");
//            req.setDestinationInExternalFilesDir(context, params.optString("dirType"), params.optString("subPath"));
//        }
//        else if (obj.has("destinationInExternalPublicDir")) {
//            JSONObject params = obj.getJSONObject("destinationInExternalPublicDir");
//            req.setDestinationInExternalPublicDir(params.optString("dirType"), params.optString("subPath"));
//        }
//        else if (obj.has("destinationUri")) {
//            req.setDestinationUri(Uri.parse(obj.getString("destinationUri")));
//        }
//
//        req.setVisibleInDownloadsUi(obj.optBoolean("visibleInDownloadsUi", true));
//        req.setNotificationVisibility(obj.optInt("notificationVisibility"));
//
//        if (obj.has("headers")) {
//            JSONArray arrHeaders = obj.optJSONArray("headers");
//            for (int i = 0; i < arrHeaders.length(); i++) {
//                JSONObject headerObj = arrHeaders.getJSONObject(i);
//                req.addRequestHeader(headerObj.optString("header"), headerObj.optString("value"));
//            }
//        }
//
//        return req;
//    }
}
