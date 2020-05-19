import { Plugins } from '@capacitor/core';
const { DownloadManagerPlugin } = Plugins;
export class DownloadManager {
    echo(options) {
        return DownloadManagerPlugin.echo(options);
    }
    enqueue(request) {
        return DownloadManagerPlugin.enqueue(request);
    }
    query(ids) {
        return DownloadManagerPlugin.query(ids);
    }
}
//# sourceMappingURL=plugin.js.map