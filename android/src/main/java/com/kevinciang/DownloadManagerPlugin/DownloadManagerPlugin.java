package com.kevinciang.DownloadManagerPlugin;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.webkit.DownloadListener;
import android.widget.Toast;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Logger;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
//import com.getcapacitor.PluginResult;
//
//import org.json.JSONObject;
//import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;

@NativePlugin(
    permissions={
        Manifest.permission.INTERNET
    }
)
public class DownloadManagerPlugin extends Plugin {

    private static final String LOG_TAG = "Downloader";

    public static final String WRITE_EXTERNAL_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    public static final int DOWNLOAD_ACTION_PERMISSION_REQ_CODE = 1;

    public static final String PERMISSION_DENIED_ERROR = "PERMISSION DENIED";

    private DownloadManager downloadManager;
    private BroadcastReceiver receiver;

    private Map<String, PluginCall> watchingCalls = new HashMap<>();
//    private DownloadListener` `

    long downloadId = -1;

    /**
     * Monitor for download status changes and fire our event.
     */
    @SuppressWarnings("MissingPermission")
    public void load() {

        downloadManager = (DownloadManager) bridge.getActivity()
                .getApplication()
                .getApplicationContext()
                .getSystemService(Context.DOWNLOAD_SERVICE);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Fetching the download id received with the broadcast
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                //Checking if the received broadcast is for our enqueued download by matching download id
                if (downloadId == id) {
                    Logger.debug("Download Completed");
//                    notifyListeners(NETWRO);
//                    Toast.makeText(getActivity(), "Download Completed", Toast.LENGTH_SHORT).show();
                }
            }
        };

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        getContext().registerReceiver(receiver, filter);
    }


    /**
     * Register the IntentReceiver on resume
     */
    @Override
    protected void handleOnResume() {
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        getContext().registerReceiver(receiver, filter);
    }

    /**
     * Unregister the IntentReceiver on pause to avoid leaking it
     */
    @Override
    protected void handleOnPause() {
        getContext().unregisterReceiver(receiver);
    }

    @PluginMethod()
    public void echo(PluginCall call) {
        String value = call.getString("value");

        JSObject ret = new JSObject();
        ret.put("value", value);
        call.success(ret);
    }

    @PluginMethod()
    public void enqueue(PluginCall call) {

        downloadManager = (DownloadManager) bridge.getActivity()
                .getApplication()
                .getApplicationContext()
                .getSystemService(Context.DOWNLOAD_SERVICE);

        DownloadManager.Request req = new DownloadManager.Request((Uri.parse(call.getString("uri"))));

        req.setTitle(call.getString("title"));
        req.setDescription(call.getString("description"));
        req.setMimeType(call.getString("mimeType"));
        req.setVisibleInDownloadsUi(call.getBoolean("visibleInDownloadsUi", true));
        req.setNotificationVisibility(call.getInt("notificationVisibility"));

        if (call.getData().has("destinationInExternalFilesDir")) {
            Context context = bridge.getActivity()
                    .getApplication()
                    .getApplicationContext();

            JSObject params = call.getObject("destinationInExternalFilesDir");
            req.setDestinationInExternalFilesDir(context, params.getString("dirType"), params.getString("subPath"));
        }
        // cannot use in android 10 above?
//        else if (call.getData().has("destinationInExternalPublicDir")) {
//            JSObject params = call.getObject("destinationInExternalPublicDir");
//            req.setDestinationInExternalPublicDir(params.getString("dirType"), params.getString("subPath"));
//        }
        else if (call.getData().has("destinationUri")) {
            req.setDestinationUri(Uri.parse(call.getString("destinationUri")));
        }

        if (call.getData().has("headers")) {
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

        downloadId = downloadManager.enqueue(req);

        JSObject ret = new JSObject();
        ret.put("id", Long.toString(downloadId));

        call.success(ret);
    }

    @PluginMethod(returnType=PluginMethod.RETURN_CALLBACK)
    public void query(PluginCall call) {

        call.save();

        downloadManager = (DownloadManager)getContext().getSystemService(Context.DOWNLOAD_SERVICE);

        DownloadManager.Query query = new DownloadManager.Query();

        long[] ids = new long[0];
        try {
            ids = longsFromJSON(call.getArray("ids"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        query.setFilterById(ids);

        if (call.getData().has("status")) {
            query.setFilterByStatus(call.getInt("status"));
        }

        boolean downloading = true;

        Logger.debug("trying to progress");
        while (downloading) {
            Cursor cursor = downloadManager.query(query);
            cursor.moveToFirst();
            int bytes_downloaded = cursor.getInt(cursor
                    .getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

            if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                downloading = false;
            }

            final int dl_progress = (int) ((bytes_downloaded * 100l) / bytes_total);

            Logger.debug(statusMessage(cursor));

            JSObject ret = new JSObject();
            ret.put("bytes_downloaded", bytes_downloaded);
            ret.put("bytes_total", bytes_total);
            ret.put("dl_progress", dl_progress);
            call.success(ret);

            cursor.close();
        }

        JSObject ret = new JSObject();
        ret.put("download", "finished");
        call.resolve(ret);


    }

    private String statusMessage(Cursor c) {
        String msg = "???";

        switch (c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
            case DownloadManager.STATUS_FAILED:
                msg = "Download failed!";
                break;

            case DownloadManager.STATUS_PAUSED:
                msg = "Download paused!";
                break;

            case DownloadManager.STATUS_PENDING:
                msg = "Download pending!";
                break;

            case DownloadManager.STATUS_RUNNING:
                msg = "Download in progress!";
                break;

            case DownloadManager.STATUS_SUCCESSFUL:
                msg = "Download complete!";
                break;

            default:
                msg = "Download is nowhere in sight";
                break;
        }

        return (msg);
    }

//    private BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            //Fetching the download id received with the broadcast
//            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
//            //Checking if the received broadcast is for our enqueued download by matching download id
//            if (downloadId == id) {
////                Toast.makeText(MainActivity.this, "Download Completed", Toast.LENGTH_SHORT).show();
//            }
//        }
//    };

    private void watchProgress(PluginCall call) {
        watchingCalls.put(call.getCallbackId(), call);
    }

    private void getProgress(final PluginCall call) {

    }

    private static long[] longsFromJSON(JSArray arr) throws JSONException {
        if (arr == null) return null;

        long[] longs = new long[arr.length()];

        for (int i = 0; i < arr.length(); i++) {
            String str = arr.getString(i);
            longs[i] = Long.valueOf(str);
        }

        return longs;
    }

    private static JSArray JSONFromCursor(Cursor cursor) {
        JSArray result = new JSArray();
        cursor.moveToFirst();
        do {
            JSObject rowObject = new JSObject();
            rowObject.put("id", cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_ID)));
            rowObject.put("title", cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)));
            rowObject.put("description", cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION)));
            rowObject.put("mediaType", cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE)));
//            rowObject.put("localFilename", cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME)));
//            rowObject.put("localUri", cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)));
//            rowObject.put("mediaproviderUri", cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIAPROVIDER_URI)));
//            rowObject.put("uri", cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI)));
//            rowObject.put("lastModifiedTimestamp", cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP)));
            rowObject.put("status", cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)));
            rowObject.put("reason", cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON)));
            rowObject.put("bytesDownloadedSoFar", cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)));
            rowObject.put("totalSizeBytes", cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)));
            result.put(rowObject);
        } while (cursor.moveToNext());

        return result;
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
