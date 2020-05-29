var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
import { Plugins } from '@capacitor/core';
const { DownloadManagerPlugin } = Plugins;
export class DownloadManager {
    echo(options) {
        return DownloadManagerPlugin.echo(options);
    }
    enqueue(request) {
        return DownloadManagerPlugin.enqueue(request);
    }
    query(id, progress) {
        // return DownloadManagerPlugin.query(ids);
        return new Promise((resolve, reject) => __awaiter(this, void 0, void 0, function* () {
            DownloadManagerPlugin.query(id, (data, error) => {
                if (!error) {
                    if (data['status'] != null) {
                        resolve(data);
                    }
                    else {
                        progress(data);
                    }
                }
                else {
                    reject({
                        status: 'error',
                        message: error
                    });
                }
            });
        }));
    }
    removeDownload(ids) {
        DownloadManagerPlugin.removeDownload(ids);
    }
    addListener(eventName, listenerFunc) {
        let thisRef = this;
        // let connection = window.navigator.connection || window.navigator.mozConnection || window.navigator.webkitConnection;
        // let connectionType = connection ? (connection.type || connection.effectiveType) : 'wifi';
        let downloadBindFunc = listenerFunc.bind(thisRef, { download_id: 'test', status: 'test' });
        // let offlineBindFunc = listenerFunc.bind(thisRef, { connected: false, connectionType: 'none' });
        if (eventName.localeCompare('downloadEvent') === 0) {
            window.addEventListener('download', downloadBindFunc);
            // window.addEventListener('offline', offlineBindFunc);
            return {
                remove: () => {
                    window.removeEventListener('download', downloadBindFunc);
                    // window.removeEventListener('offline', offlineBindFunc);
                }
            };
        }
    }
}
//# sourceMappingURL=plugin.js.map