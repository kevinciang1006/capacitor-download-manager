import { WebPlugin } from '@capacitor/core';
import { DownloadManagerPlugin, DownloadRequest } from './definitions';

export class DownloadManagerPluginWeb extends WebPlugin implements DownloadManagerPlugin {
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
    return DownloadManagerPlugin.enqueue(request);
  }
}

const DownloadManagerPlugin = new DownloadManagerPluginWeb();

export { DownloadManagerPlugin };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(DownloadManagerPlugin);
