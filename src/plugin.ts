import { Plugins } from '@capacitor/core';
import { IDownloadManagerPlugin, DownloadRequest } from './definitions';
const { DownloadManagerPlugin } = Plugins;
export class DownloadManager implements IDownloadManagerPlugin {
  echo(options: { value: string }): Promise<{value: string}> {
    return DownloadManagerPlugin.echo();
  }
  enqueue(request: DownloadRequest): Promise<any> {
    return DownloadManagerPlugin.enqueue(request);
  }
}