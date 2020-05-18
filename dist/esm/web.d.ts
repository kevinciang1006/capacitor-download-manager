import { WebPlugin } from '@capacitor/core';
import { IDownloadManagerPlugin, DownloadRequest } from './definitions';
export declare class DownloadManagerPluginWeb extends WebPlugin implements IDownloadManagerPlugin {
    constructor();
    echo(options: {
        value: string;
    }): Promise<{
        value: string;
    }>;
    enqueue(request: DownloadRequest): Promise<any>;
}
declare const DownloadManagerPlugin: DownloadManagerPluginWeb;
export { DownloadManagerPlugin };
