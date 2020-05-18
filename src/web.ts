import { WebPlugin } from '@capacitor/core';
import { IDownloadManagerPlugin, DownloadRequest } from './definitions';

export class DownloadManagerPluginWeb extends WebPlugin implements IDownloadManagerPlugin {
  constructor() {
    super({
      name: 'DownloadManagerPlugin',
      platforms: ['web']
    });
  }

  async echo(options: { value: string }): Promise<{value: string}> {
    console.log('ECHO', options);
    return options;
  } 

  async enqueue(request: DownloadRequest): Promise<any> {
    console.log('enqueue plugin: ', JSON.stringify(request));
    return request;
    // return DownloadManagerPlugin.enqueue(request);
    // return new Promise((resolve, reject) => {
    //   DownloadManagerPlugin.enqueue(request).then
    // });
  }
}

const DownloadManagerPlugin = new DownloadManagerPluginWeb();

export { DownloadManagerPlugin };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(DownloadManagerPlugin);
