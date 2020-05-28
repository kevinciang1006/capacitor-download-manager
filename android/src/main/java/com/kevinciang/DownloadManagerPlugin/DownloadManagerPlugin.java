package com.kevinciang.DownloadManagerPlugin;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.webkit.DownloadListener;
import android.widget.Toast;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.PluginResult;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    public static final String DOWNLOAD_EVENT = "downloadEvent";

    private DownloadManager downloadManager;
    private BroadcastReceiver receiver;

    private Map<String, PluginCall> watchingCalls = new HashMap<>();
//    private DownloadListener` `

    long downloadId = -1;
    ArrayList<Long> downloadIds = new ArrayList<Long>();
    boolean downloading = false;

    private String TAG = "Cap";

    /**
     * Monitor for download status changes and fire our event.
     */
    @SuppressWarnings("MissingPermission")
    public void load() {

        downloadManager = (DownloadManager)getContext().getSystemService(Context.DOWNLOAD_SERVICE);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Fetching the download id received with the broadcast
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                //Checking if the received broadcast is for our enqueued download by matching download id
                if (downloadId == id) {
                    Log.d(TAG, "onReceive: Download Completed = " + id);

                    downloadManager = (DownloadManager)getContext().getSystemService(Context.DOWNLOAD_SERVICE);
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(downloadId);
                    Cursor cursor = downloadManager.query(query);
                    if (cursor != null && cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            Log.d(TAG, "onReceive: Download sucessful = " + id + " status: " + status);
                        } else {
                            Log.d(TAG, "onReceive: Download assumed cancelled 1 = " + id + " status: " + status);
                        }
                    } else {
                        JSObject ret = new JSObject();
                        ret.put("download_id", id);
                        ret.put("status", "cancelled");
                        notifyListeners(DOWNLOAD_EVENT, ret);
                        Log.d(TAG, "onReceive: Download assumed cancelled 2 = " + id);
                    }

                }
                areAllDownloadsComplete(downloadIds);
                Log.d(TAG, "onReceive: Still downloading? = " + downloading);
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

        DownloadManager downloadManager = (DownloadManager)getContext().getSystemService(Context.DOWNLOAD_SERVICE);

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

        this.downloadId = downloadManager.enqueue(req);
        this.downloadIds.add(this.downloadId);

        this.downloading = true;

        JSObject ret = new JSObject();
        ret.put("id", Long.toString(this.downloadId));

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

        Log.d(TAG, "Trying to progress");
        while (this.downloading) {
            Cursor cursor = downloadManager.query(query);
            if (cursor != null && cursor.getCount() > 0) {

                cursor.moveToFirst();
                int bytes_downloaded = cursor.getInt(cursor
                        .getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                final int dl_progress = (int) ((bytes_downloaded * 100l) / bytes_total);

                String downloadIdString = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_ID));
                int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));

                Log.d(TAG, statusMessage(cursor));

                JSObject ret = new JSObject();
                ret.put("download_id", downloadIdString);
                ret.put("bytes_downloaded", bytes_downloaded);
                ret.put("bytes_total", bytes_total);
                ret.put("dl_progress", dl_progress);
                ret.put("status_message", statusMessage(cursor));
                ret.put("status_code", cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)));
                ret.put("reason", reason);
                call.success(ret);

                cursor.close();

            } else {
                call.reject("Download failed");
            }
        }

        JSObject ret = new JSObject();
        ret.put("status", "download manager finished");
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

    public void areAllDownloadsComplete(ArrayList<Long> dids) {

        downloadManager = (DownloadManager)getContext().getSystemService(Context.DOWNLOAD_SERVICE);

        DownloadManager.Query query = new DownloadManager.Query();

        Log.d(TAG, "downloadIds: " + dids.toString());
        for(int i = 0; i < dids.size(); i++) {

            query.setFilterById(dids.get(i));

            Cursor cursor = downloadManager.query(query);

            if(cursor.moveToNext()) {

                if(cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) != DownloadManager.STATUS_SUCCESSFUL) {

                    this.downloading = true;
                    return;
                }
            }
        }

        this.downloading = false;
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
            rowObject.put("status", cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)));
            rowObject.put("reason", cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON)));
            rowObject.put("bytesDownloadedSoFar", cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)));
            rowObject.put("totalSizeBytes", cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)));
            result.put(rowObject);
        } while (cursor.moveToNext());

        return result;
    }
}
