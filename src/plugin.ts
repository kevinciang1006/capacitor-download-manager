import { Plugins } from '@capacitor/core';
import { IDownloadManagerPlugin, DownloadRequest } from './definitions';
const { DownloadManagerPlugin } = Plugins;
export class DownloadManager implements IDownloadManagerPlugin {
  echo(options: { value: string }): Promise<{ value: string }> {
    return DownloadManagerPlugin.echo(options);
  }
  enqueue(request: DownloadRequest): Promise<any> {
    return DownloadManagerPlugin.enqueue(request);
  }
  query(ids: string[], progress?: Function): Promise<any> {
    // return DownloadManagerPlugin.query(ids);
    return new Promise(async (resolve, reject) => {
      DownloadManagerPlugin.query(ids, (data: any, error: string) => {
        if (!error) {
          if (data['status'] != null) {
            resolve(data);
          } else {
            progress(data);
          }
        } else {
          reject({
            status: 'error',
            message: error
          });
        }
      });
    });
  }
  addListener(eventName: 'downloadEvent', listenerFunc: (status: any) => void): any { }
}