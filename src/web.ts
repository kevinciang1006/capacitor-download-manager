import { Plugins } from '@capacitor/core';
import { IDownloadManagerPlugin, DownloadRequest } from './definitions';
const { DownloadManager } = Plugins;

export class DownloadManagerPlugin implements IDownloadManagerPlugin {
  // constructor() {
  //   super({
  //     name: 'DownloadManagerPlugin',
  //     platforms: ['web']
  //   });
  // }

  async echo(options: { value: string }): Promise<{value: string}> {
    console.log('ECHO', options);
    return options;
  } 

  async enqueue(request: DownloadRequest): Promise<any> {
    console.log('enqueue plugin: ', JSON.stringify(request));
    return DownloadManager.enqueue(request);
  }
}

// const DownloadManagerPlugin = new DownloadManagerPluginWeb();

// export { DownloadManagerPlugin };

// import { registerWebPlugin } from '@capacitor/core';
// registerWebPlugin(DownloadManagerPlugin);
