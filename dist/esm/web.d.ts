import { IDownloadManagerPlugin, DownloadRequest } from './definitions';
export declare class DownloadManagerPlugin implements IDownloadManagerPlugin {
    echo(options: {
        value: string;
    }): Promise<{
        value: string;
    }>;
    enqueue(request: DownloadRequest): Promise<any>;
}
