import { PluginListenerHandle } from '@capacitor/core';
import { IDownloadManagerPlugin, DownloadRequest } from './definitions';
export declare class DownloadManager implements IDownloadManagerPlugin {
    echo(options: {
        value: string;
    }): Promise<{
        value: string;
    }>;
    enqueue(request: DownloadRequest): Promise<any>;
    query(id: string, progress?: Function): Promise<any>;
    removeDownload(ids: string[]): void;
    addListener(eventName: 'downloadEvent', listenerFunc: (downloadStatus: any) => void): PluginListenerHandle;
}
