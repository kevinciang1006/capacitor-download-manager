declare module "@capacitor/core" {
  interface PluginRegistry {
    DownloadManagerPlugin: IDownloadManagerPlugin;
  }
}

export interface IDownloadManagerPlugin {
  echo(options: { value: string }): Promise<{value: string}>;
  enqueue(request: DownloadRequest): Promise<any>;
  query(ids: string[], progress?: Function): Promise<any>;
  addListener(eventName: 'downloadEvent', listenerFunc: (status: any) => void): any;
}

export enum NotificationVisibility {
  Visible = 0,
  VisibleNotifyCompleted = 1,
  VisibilityHidden = 2,
  VisibleNotifyOnlyCompletion = 3
}

export interface DownloadHttpHeader {
  header: string;
  value: string;
}

export interface DestinationDirectory {
  dirType: string;
  subPath: string;
}

export interface DownloadRequest {
  /**
   * Location of the resource to download
   */
  uri: string;

  /**
   * Set the title of this download, to be displayed in notifications (if enabled).
   * If no title is given, a default one will be assigned based on the download filename, once the download starts.
   */
  title?: string;
  /**
   * Set a description of this download, to be displayed in notifications (if enabled)
   */
  description?: string;
  /**
   * Set the MIME content type of this download. This will override the content type declared in the server's response.
   */
  mimeType?: string;
  /**
   * Set whether this download should be displayed in the system's Downloads UI. True by default.
   */
  visibleInDownloadsUi?: boolean;
  /**
   * Control whether a system notification is posted by the download manager while this download is running or when it is completed.
   */
  notificationVisibility?: NotificationVisibility;
  /**
   * Set the local destination for the downloaded file to a path within the application's external files directory
   */
  destinationInExternalFilesDir?: DestinationDirectory;
  /**
   * Set the local destination for the downloaded file to a path within the public external storage directory
   */
  destinationInExternalPublicDir?: DestinationDirectory;
  /**
   * Set the local destination for the downloaded file.
   * Must be a file URI to a path on external storage, and the calling application must have the WRITE_EXTERNAL_STORAGE permission.
   */
  destinationUri?: string;
  /**
   * Add an HTTP header to be included with the download request. The header will be added to the end of the list.
   */
  headers?: DownloadHttpHeader[];
}