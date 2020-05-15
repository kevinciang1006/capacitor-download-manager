import { WebPlugin } from '@capacitor/core';
import { DownloadManagerPlugin, DownloadRequest } from './definitions';
export declare class DownloadManagerPluginWeb extends WebPlugin implements DownloadManagerPlugin {
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
