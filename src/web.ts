import { WebPlugin } from '@capacitor/core';
import { IDownloadManagerPlugin, DownloadRequest, Options } from './definitions';

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
  
  async query(options: Options): Promise<any> {
    return DownloadManagerPlugin.query(options);
  }
  async remove(options: Options): Promise<any> {
    return DownloadManagerPlugin.remove(options);
  }

  // addListener(eventName: 'downloadEvent', listenerFunc: (status: any) => void): any { }
}

const DownloadManagerPlugin = new DownloadManagerPluginWeb();

export { DownloadManagerPlugin };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(DownloadManagerPlugin);
