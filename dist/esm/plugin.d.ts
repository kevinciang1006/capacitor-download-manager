import { IDownloadManagerPlugin, DownloadRequest, Options } from './definitions';
export declare class DownloadManager implements IDownloadManagerPlugin {
    echo(options: {
        value: string;
    }): Promise<{
        value: string;
    }>;
    enqueue(request: DownloadRequest): Promise<any>;
    query(options: Options, progress?: Function): Promise<any>;
    remove(options: Options): Promise<any>;
}
