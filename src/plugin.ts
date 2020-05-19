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
  query(ids: string[]): Promise<any> {
    return DownloadManagerPlugin.query(ids);
  }
}