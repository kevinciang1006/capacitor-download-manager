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

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class DownloadData {
    private String status = null;
    private String path = null;
    private PluginCall call = null;

    String getStatus() {
        return status;
    }

    void setStatus(String status) {
        this.status = status;
    }

    PluginCall getCallback() {
        return call;
    }

    void setCallback(PluginCall call) {
        this.call = call;
    }

    String getPath() {
        return path;
    }

    void setPath(String path) {
        this.path = path;
    }
}

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

    private Map<String, DownloadData> downloadsData = new HashMap<>();
    private Map<String, MyThread> threads = new HashMap<>();

    ArrayList<Long> downloadIds = new ArrayList<Long>();
    boolean downloading = false;

    private String TAG = "Cap";
    private Thread mProgressThread;
    private BroadcastReceiver mBroadCastReceiver;
    private ArrayList<Long> mFinishedFilesFromNotif = new ArrayList<Long>();
    private List<DownloadData> downloadList;
    private boolean isDownloadSuccess;
    class MyThread extends Thread {

        private String downloadId;
        private DownloadData downloadData;

        public MyThread(String downloadId, DownloadData downloadData) {
            this.downloadId = downloadId;
            this.downloadData = downloadData;
        }

        @Override
        public void run() {
            Log.d(TAG, "mProgressThread");

            // Preparing the query for the download manager ...
            DownloadManager.Query q = new DownloadManager.Query();

            final List<Long> idsArrList= new ArrayList<>();
            for (Long id: downloadIds) {
                idsArrList.add(id);
            }
            q.setFilterById(Long.parseLong(downloadId));

            Log.d(TAG, "idsArrList: " + idsArrList.toString());

            Cursor c;

            while(!Thread.currentThread().isInterrupted()) {

                // start iterating and noting progress ..
                c = downloadManager.query(q);
                if(c != null && c.getCount() > 0) {
                    c.moveToFirst();

                    final int columnStatus = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int bytes_downloaded = c.getInt(c
                            .getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    int bytes_total = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    final int reason = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON));
                    final int dl_progress = (int) ((bytes_downloaded * 100l) / bytes_total);

                    // Show the progress appropriately ...
                    JSObject ret = new JSObject();
                    ret.put("download_id", downloadId);
                    ret.put("bytes_downloaded", bytes_downloaded);
                    ret.put("bytes_total", bytes_total);
                    ret.put("dl_progress", dl_progress);
                    ret.put("status_message", statusMessage(c));
                    ret.put("status_code", c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS)));
                    ret.put("reason", reason);
                    downloadData.getCallback().success(ret);

                    if (c.getInt(columnStatus) == DownloadManager.STATUS_SUCCESSFUL) {
                        Log.d(TAG, "STATUS_SUCCESSFUL: " + downloadId);
                        ret = new JSObject();
                        ret.put("download_id", downloadId);
                        ret.put("status", "completed");
                        downloadData.getCallback().resolve(ret);
                        c.close();
                        Thread.currentThread().interrupt();
                    }

                    c.close();
                } else {
                    JSObject ret = new JSObject();
                    ret.put("download_id", downloadId);
                    ret.put("status", "cancelled");
                    downloadData.getCallback().reject("download cancelled");
                    Log.d(TAG, "onReceive: Download assumed cancelled 2 = " + downloadId);
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    /**
     * Monitor for download status changes and fire our event.
     */
    @SuppressWarnings("MissingPermission")
    public void load() {

        downloadManager = (DownloadManager)getContext().getSystemService(Context.DOWNLOAD_SERVICE);

        if (downloadsData == null) {
            downloadsData = new HashMap<>();
        }

        if (threads == null) {
            threads = new HashMap<>();
        }

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                mFinishedFilesFromNotif
                        .add(intent.getExtras()
                                .getLong(DownloadManager.EXTRA_DOWNLOAD_ID));

                Log.d(TAG, "onReceive FINISH " + mFinishedFilesFromNotif.toString());
            }
        };
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

        // initializing the download manager instance ....
        downloadManager = (DownloadManager)getContext().getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request req = new DownloadManager.Request((Uri.parse(call.getString("uri"))));
        DownloadData data = new DownloadData();

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

        long downloadId = downloadManager.enqueue(req);
        // adding download id to list ...
        downloadIds.add(downloadId);
        Log.d(TAG, "enqueue: " + downloadIds.toString());

        JSObject ret = new JSObject();
        ret.put("id", Long.toString(downloadId));
        downloadsData.put(Long.toString(downloadId), data);
        call.resolve(ret);
    }

    @PluginMethod(returnType=PluginMethod.RETURN_CALLBACK)
    public void query(PluginCall call) {

        String downloadDataId = call.getString("id");
        Log.d(TAG, "id: " + downloadDataId);

        DownloadData data = downloadsData.get(downloadDataId);

        if (data != null) {
            call.save();
            data.setCallback(call);

            // starting the thread to track the progress of the download ..
            MyThread mt = new MyThread(downloadDataId, data);
            threads.put(downloadDataId, mt);
            mt.start();

            Log.d(TAG, "thread has started for: " + downloadDataId);
        } else {

            // re query download manager when app loads
            DownloadData d = new DownloadData();
            downloadsData.put(downloadDataId, d);

            call.save();
            d.setCallback(call);

            // starting the thread to track the progress of the download ..
            MyThread mt = new MyThread(downloadDataId, d);
            threads.put(downloadDataId, mt);
            mt.start();
        }
    }

    @PluginMethod()
    public void remove(PluginCall call) {

        Log.d(TAG, "remove in");

        downloadManager = (DownloadManager)getContext().getSystemService(Context.DOWNLOAD_SERVICE);

        String id = call.getString("id");
        MyThread mt = threads.get(id);
        mt.interrupt();
        if (mt.currentThread().isInterrupted()) {
            // cleanup and stop execution
            // for example a break in a loop
            Log.d(TAG, "mt interupted");
        }
        int removed = downloadManager.remove(Long.parseLong(id));

        JSObject ret = new JSObject();
        ret.put("removed_id", removed);
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

}
