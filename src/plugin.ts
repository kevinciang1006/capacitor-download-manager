import { Plugins, PluginListenerHandle } from '@capacitor/core';
import { IDownloadManagerPlugin, DownloadRequest } from './definitions';
const { DownloadManagerPlugin } = Plugins;

// export interface PluginListenerHandle {
//   remove: () => void;
// }

declare var window: any;
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
  addListener(eventName: 'downloadEvent', listenerFunc: (downloadStatus: any) => void): PluginListenerHandle {
    let thisRef = this;
    // let connection = window.navigator.connection || window.navigator.mozConnection || window.navigator.webkitConnection;
    // let connectionType = connection ? (connection.type || connection.effectiveType) : 'wifi';

    let downloadBindFunc = listenerFunc.bind(thisRef, { download_id: 'test', status: 'test' });
    // let offlineBindFunc = listenerFunc.bind(thisRef, { connected: false, connectionType: 'none' });

    if (eventName.localeCompare('downloadEvent') === 0) {
      window.addEventListener('download', downloadBindFunc);
      // window.addEventListener('offline', offlineBindFunc);
      return {
        remove: () => {
          window.removeEventListener('download', downloadBindFunc);
          // window.removeEventListener('offline', offlineBindFunc);
        }
      };
    }
  }
}