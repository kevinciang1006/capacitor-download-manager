import { Plugins } from '@capacitor/core';
const { DownloadManagerPlugin } = Plugins;
export class DownloadManager2 {
    echo(options) {
        return DownloadManagerPlugin.echo();
    }
    enqueue(request) {
        return DownloadManagerPlugin.enqueue(request);
    }
}
//# sourceMappingURL=plugin.js.map