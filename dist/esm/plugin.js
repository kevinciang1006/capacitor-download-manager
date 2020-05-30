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
    query(options, progress) {
        // return DownloadManagerPlugin.query(ids);
        return new Promise((resolve, reject) => __awaiter(this, void 0, void 0, function* () {
            DownloadManagerPlugin.query(options, (data, error) => {
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
    remove(options) {
        return DownloadManagerPlugin.remove(options);
    }
}
//# sourceMappingURL=plugin.js.map