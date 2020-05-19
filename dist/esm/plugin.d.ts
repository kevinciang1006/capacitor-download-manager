import { IDownloadManagerPlugin, DownloadRequest } from './definitions';
export declare class DownloadManager implements IDownloadManagerPlugin {
    echo(options: {
        value: string;
    }): Promise<{
        value: string;
    }>;
    enqueue(request: DownloadRequest): Promise<any>;
    query(ids: string[]): Promise<any>;
}
