import { WebPlugin } from '@capacitor/core';
import { IDownloadManagerPlugin, DownloadRequest } from './definitions';

// declare var DM: Download

export class DownloadManagerPluginWeb extends WebPlugin implements IDownloadManagerPlugin {
  constructor() {
    super({
      name: 'DownloadManagerPlugin',
      platforms: ['web']
    });
  }

  async echo(options: { value: string }): Promise<{value: string}> {
    console.log('ECHO', JSON.stringify(options));
    return options;
  } 

  async enqueue(request: DownloadRequest): Promise<any> {
    console.log('enqueue plugin: ', JSON.stringify(request));
    return DownloadManagerPlugin.enqueue(request);
  }
  
  async query(ids: string[]): Promise<any> {
    return DownloadManagerPlugin.query(ids);
  }
}

const DownloadManagerPlugin = new DownloadManagerPluginWeb();

export { DownloadManagerPlugin };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(DownloadManagerPlugin);
