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

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import org.json.JSONException;

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

    private Map<String, PluginCall> watchingCalls = new HashMap<>();
    private Map<String, DownloadData> downloadsData = new HashMap<>();
//    private DownloadListener` `

    //    long downloadId = -1;
    ArrayList<Long> downloadIds = new ArrayList<Long>();
    boolean downloading = false;

    private String TAG = "Cap";
    private Thread mProgressThread;
    private BroadcastReceiver mBroadCastReceiver;
    private ArrayList<Long> mFinishedFilesFromNotif = new ArrayList<Long>();
    private List<DownloadData> downloadList;
    private boolean isDownloadSuccess;
    class MyThread extends Thread {

        private DownloadData downloadData;

        public MyThread(DownloadData downloadData) {
            this.downloadData = downloadData;
        }

        @Override
        public void run() {
//            System.out.println("hello " + call);
            Log.d(TAG, "mProgressThread");

            // Preparing the query for the download manager ...
            DownloadManager.Query q = new DownloadManager.Query();
            long[] ids = new long[downloadIds.size()];
            final List<Long> idsArrList= new ArrayList<>();
            int i = 0;
            for (Long id: downloadIds) {
                ids[i++] = id;
                idsArrList.add(id);
            }
            q.setFilterById(ids);

            Log.d(TAG, "idsArrList: " + idsArrList.toString());

            // getting the total size of the data ...
            Cursor c;

            while(true) {

                // check if the downloads are already completed ...
                // Here I have created a set of download ids from download manager to keep
                // track of all the files that are dowloaded, which I populate by creating
                if(mFinishedFilesFromNotif.containsAll(idsArrList)) {
                    isDownloadSuccess = true;

                    Log.d(TAG, "isDownloadSuccess: " + idsArrList.toString());
                    // TODO - Take appropriate action. Download is finished successfully
                    return;
                }

                // start iterating and noting progress ..
                c = downloadManager.query(q);
                if(c != null) {
                    int filesDownloaded = 0;
                    float fileFracs = 0f; // this stores the fraction of all the files in
                    // download
                    final int columnTotalSize = c.getColumnIndex
                            (DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                    final int columnStatus = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    final int columnId = c.getColumnIndex(DownloadManager.COLUMN_ID);
                    final int columnDwnldSoFar = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                    final int reason = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON));

                    while (c.moveToNext()) {
                        // checking the progress ..
                        if(c.getInt(columnStatus) == DownloadManager.STATUS_SUCCESSFUL) {
                            filesDownloaded++;
                        }
                        // If the file is partially downloaded, take its fraction ..
                        else if(c.getInt(columnTotalSize) > 0) {
                            fileFracs += ((c.getInt(columnDwnldSoFar) * 1.0f) /
                                    c.getInt(columnTotalSize));
                        } else if(c.getInt(columnStatus) == DownloadManager.STATUS_FAILED) {
                            // TODO - Take appropriate action. Error in downloading one of the
                            // files.

                            Log.d(TAG, "error download: " + columnId);
                            return;
                        }
                    }

                    c.close();

                    // calculate the progress to show ...
                    float progress = (filesDownloaded + fileFracs)/ids.length;
//                        Log.d(TAG, "progress: " + progress);

                    // setting the progress text and bar...
                    final int percentage = Math.round(progress * 100.0f);
                    final String txt = "Loading ... " + percentage + "%";

                    // Show the progress appropriately ...
                    JSObject ret = new JSObject();
                    ret.put("download_id", columnId);
                    ret.put("columnTotalSize", columnTotalSize);
                    ret.put("columnStatus", columnStatus);
                    ret.put("dl_progress", percentage);
                    ret.put("status_message", statusMessage(c));
                    ret.put("status_code", c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS)));
                    ret.put("reason", reason);
                    downloadData.getCallback().success();
                    Log.d(TAG, txt);
                }
            }
        }
    }
    /**
     * Monitor for download status changes and fire our event.
     */
    @SuppressWarnings("MissingPermission")
    public void load() {

//        new MyThread("hello").start();

        downloadManager = (DownloadManager)getContext().getSystemService(Context.DOWNLOAD_SERVICE);

        if (downloadsData == null) {
            downloadsData = new HashMap<>();
        }

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                mFinishedFilesFromNotif
                        .add(intent.getExtras()
                                .getLong(DownloadManager.EXTRA_DOWNLOAD_ID));

                Log.d(TAG, "onReceive FINISH " + mFinishedFilesFromNotif.toString());

                //Fetching the download id received with the broadcast
//                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
//                //Checking if the received broadcast is for our enqueued download by matching download id
//                if (downloadId == id) {
//                    Log.d(TAG, "onReceive: Download Completed = " + id);
//
//                    downloadManager = (DownloadManager)getContext().getSystemService(Context.DOWNLOAD_SERVICE);
//                    DownloadManager.Query query = new DownloadManager.Query();
//                    query.setFilterById(downloadId);
//                    Cursor cursor = downloadManager.query(query);
//                    if (cursor != null && cursor.getCount() > 0) {
//                        cursor.moveToFirst();
//                        int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
//                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
//                            Log.d(TAG, "onReceive: Download sucessful = " + id + " status: " + status);
//                        } else {
//                            Log.d(TAG, "onReceive: Download assumed cancelled 1 = " + id + " status: " + status);
//                        }
//                    } else {
//                        JSObject ret = new JSObject();
//                        ret.put("download_id", id);
//                        ret.put("status", "cancelled");
//                        notifyListeners(DOWNLOAD_EVENT, ret);
//                        Log.d(TAG, "onReceive: Download assumed cancelled 2 = " + id);
//                    }
//
//                }
//                areAllDownloadsComplete(downloadIds);
//                Log.d(TAG, "onReceive: Still downloading? = " + downloading);
            }
        };
//
//        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
//        getContext().registerReceiver(receiver, filter);
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

//        DownloadManager downloadManager = (DownloadManager)getContext().getSystemService(Context.DOWNLOAD_SERVICE);
//
//        DownloadData data = new DownloadData();
//
//        DownloadManager.Request req = new DownloadManager.Request((Uri.parse(call.getString("uri"))));
//
//        req.setTitle(call.getString("title"));
//        req.setDescription(call.getString("description"));
//        req.setMimeType(call.getString("mimeType"));
//        req.setVisibleInDownloadsUi(call.getBoolean("visibleInDownloadsUi", true));
//        req.setNotificationVisibility(call.getInt("notificationVisibility"));
//
//        if (call.getData().has("destinationInExternalFilesDir")) {
//            Context context = bridge.getActivity()
//                    .getApplication()
//                    .getApplicationContext();
//
//            JSObject params = call.getObject("destinationInExternalFilesDir");
//            req.setDestinationInExternalFilesDir(context, params.getString("dirType"), params.getString("subPath"));
//        }
//        // cannot use in android 10 above?
////        else if (call.getData().has("destinationInExternalPublicDir")) {
////            JSObject params = call.getObject("destinationInExternalPublicDir");
////            req.setDestinationInExternalPublicDir(params.getString("dirType"), params.getString("subPath"));
////        }
//        else if (call.getData().has("destinationUri")) {
//            req.setDestinationUri(Uri.parse(call.getString("destinationUri")));
//        }
//
//        if (call.getData().has("headers")) {
//            JSArray arrHeaders = call.getArray("headers", new JSArray());
//            for (int i = 0; i < arrHeaders.length(); i++) {
//                JSObject headerObj = null;
//                try {
//                    headerObj = (JSObject) arrHeaders.getJSONObject(i);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//                req.addRequestHeader(headerObj.getString("header"), headerObj.getString("value"));
//            }
//        }
//
//        this.downloadId = downloadManager.enqueue(req);
//        this.downloadIds.add(this.downloadId);
//
//        this.downloading = true;
//
//        JSObject ret = new JSObject();
//        ret.put("id", Long.toString(this.downloadId));
//
//        call.success(ret);
    }

    @PluginMethod(returnType=PluginMethod.RETURN_CALLBACK)
    public void query(PluginCall call) {

        String downloadDataId = call.getString("id");
        Log.d(TAG, "id: " + downloadDataId);
//        try {
//            downloadDataId = call.getArray("ids").get(0).toString();
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        DownloadData data = new DownloadData();
        DownloadData data = downloadsData.get(downloadDataId);
        call.save();
        data.setCallback(call);

        // starting the thread to track the progress of the download ..
        new MyThread(data).start();
//        mProgressThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//
//            }
//        });
//
//        mProgressThread.start();

        Log.d(TAG, "thread has started for: " + downloadDataId);

//        return;
//        call.reject("test");
//
//        call.save();
//
//        downloadManager = (DownloadManager)getContext().getSystemService(Context.DOWNLOAD_SERVICE);
//
//        DownloadManager.Query query = new DownloadManager.Query();
//
//        long[] ids = new long[0];
//        try {
//            ids = longsFromJSON(call.getArray("ids"));
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        query.setFilterById(ids);
//
//        if (call.getData().has("status")) {
//            query.setFilterByStatus(call.getInt("status"));
//        }
//
//        Log.d(TAG, "Trying to progress");
//        while (this.downloading) {
//            Cursor cursor = downloadManager.query(query);
//            if (cursor != null && cursor.getCount() > 0) {
//
//                cursor.moveToFirst();
//                int bytes_downloaded = cursor.getInt(cursor
//                        .getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
//                int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
//
//                final int dl_progress = (int) ((bytes_downloaded * 100l) / bytes_total);
//
//                String downloadIdString = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_ID));
//                int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
//
//                Log.d(TAG, statusMessage(cursor));
//
//                JSObject ret = new JSObject();
//                ret.put("download_id", downloadIdString);
//                ret.put("bytes_downloaded", bytes_downloaded);
//                ret.put("bytes_total", bytes_total);
//                ret.put("dl_progress", dl_progress);
//                ret.put("status_message", statusMessage(cursor));
//                ret.put("status_code", cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)));
//                ret.put("reason", reason);
//                call.success(ret);
//
//                cursor.close();
//
//            } else {
//                call.reject("Download failed");
//            }
//        }
//
//        JSObject ret = new JSObject();
//        ret.put("status", "download manager finished");
//        call.resolve(ret);

    }

    @PluginMethod()
    public void removeDownload(PluginCall call) {

        Log.d(TAG, "remove in");

        downloadManager = (DownloadManager)getContext().getSystemService(Context.DOWNLOAD_SERVICE);

//        DownloadManager.Query query = new DownloadManager.Query();

        long[] ids = new long[0];
        try {
            ids = longsFromJSON(call.getArray("ids"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
//        query.setFilterById(ids);

        Log.d(TAG, call.getArray("ids").toString());

        int removed = downloadManager.remove(ids);

        JSObject ret = new JSObject();
        ret.put("removed_id", removed);
        call.resolve(ret);

//        c.close();

//        if (ids.length > 0) {
//
//            Cursor c = downloadManager.query(query);
//            while(c.moveToNext()) {
//                int removed = downloadManager.remove(ids);
//
//                JSObject ret = new JSObject();
//                ret.put("removed_id", removed);
//                call.resolve(ret);
//
//                c.close();
//            }
//        }
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

    private void startDownloadThread(final List<DownloadData> list, PluginCall call) {

        Log.d(TAG, "startDownloadThread");

        // Initializing the broadcast receiver ...
        mBroadCastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mFinishedFilesFromNotif
                        .add(intent.getExtras()
                                .getLong(DownloadManager.EXTRA_DOWNLOAD_ID));

                Log.d(TAG, "onReceive FINISH " + mFinishedFilesFromNotif.toString());
            }
        };

        IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        getContext().registerReceiver(mBroadCastReceiver, intentFilter);

        // initializing the download manager instance ....
        downloadManager = (DownloadManager)getContext().getSystemService(Context.DOWNLOAD_SERVICE);

        // adding files to the download manager list ...
//        for(DownloadData f: list) {
        downloadIds.add(addFileForDownloadInBkg(getContext(), call));
//        }

        // starting the thread to track the progress of the download ..
        mProgressThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "mProgressThread");

                // Preparing the query for the download manager ...
                DownloadManager.Query q = new DownloadManager.Query();
                long[] ids = new long[downloadIds.size()];
                final List<Long> idsArrList= new ArrayList<>();
                int i = 0;
                for (Long id: downloadIds) {
                    ids[i++] = id;
                    idsArrList.add(id);
                }
                q.setFilterById(ids);

                Log.d(TAG, "idsArrList: " + idsArrList.toString());

                // getting the total size of the data ...
                Cursor c;

                while(true) {

                    Log.d(TAG, "while loop running: ");

                    // check if the downloads are already completed ...
                    // Here I have created a set of download ids from download manager to keep
                    // track of all the files that are dowloaded, which I populate by creating
                    //
                    if(mFinishedFilesFromNotif.containsAll(idsArrList)) {
                        isDownloadSuccess = true;

                        Log.d(TAG, "isDownloadSuccess: " + idsArrList.toString());
                        // TODO - Take appropriate action. Download is finished successfully
                        return;
                    }

                    // start iterating and noting progress ..
                    c = downloadManager.query(q);
                    if(c != null) {
                        int filesDownloaded = 0;
                        float fileFracs = 0f; // this stores the fraction of all the files in
                        // download
                        final int columnTotalSize = c.getColumnIndex
                                (DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                        final int columnStatus = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        final int columnId = c.getColumnIndex(DownloadManager.COLUMN_ID);
                        final int columnDwnldSoFar =
                                c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);

                        while (c.moveToNext()) {
                            // checking the progress ..
                            if(c.getInt(columnStatus) == DownloadManager.STATUS_SUCCESSFUL) {
                                filesDownloaded++;
                            }
                            // If the file is partially downloaded, take its fraction ..
                            else if(c.getInt(columnTotalSize) > 0) {
                                fileFracs += ((c.getInt(columnDwnldSoFar) * 1.0f) /
                                        c.getInt(columnTotalSize));
                            } else if(c.getInt(columnStatus) == DownloadManager.STATUS_FAILED) {
                                // TODO - Take appropriate action. Error in downloading one of the
                                // files.
                                return;
                            }
                        }

                        c.close();

                        // calculate the progress to show ...
                        float progress = (filesDownloaded + fileFracs)/ids.length;
//                        Log.d(TAG, "progress: " + progress);

                        // setting the progress text and bar...
                        final int percentage = Math.round(progress * 100.0f);
                        final String txt = "Loading ... " + percentage + "%";

                        // Show the progress appropriately ...

                        Log.d(TAG, txt);
                    }
                }
            }
        });

        mProgressThread.start();

        Log.d(TAG, "thread has started");
    }

    public static long addFileForDownloadInBkg(Context context, PluginCall call) {
//        Uri uri = Uri.parse(url);

        DownloadManager.Request req = new DownloadManager.Request((Uri.parse(call.getString("uri"))));
//        req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
//        req.setDestinationUri(Uri.fromFile(new File(savePath)));

        req.setTitle(call.getString("title"));
        req.setDescription(call.getString("description"));
        req.setMimeType(call.getString("mimeType"));
        req.setVisibleInDownloadsUi(call.getBoolean("visibleInDownloadsUi", true));
        req.setNotificationVisibility(call.getInt("notificationVisibility"));

        if (call.getData().has("destinationInExternalFilesDir")) {
//            Context context = bridge.getActivity()
//                    .getApplication()
//                    .getApplicationContext();

            JSObject params = call.getObject("destinationInExternalFilesDir");
            req.setDestinationInExternalFilesDir(context, params.getString("dirType"), params.getString("subPath"));
        }

        final DownloadManager m = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        return m.enqueue(req);
    }
}
