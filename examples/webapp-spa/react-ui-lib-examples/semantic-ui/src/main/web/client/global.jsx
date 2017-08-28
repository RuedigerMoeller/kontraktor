import {KClient} from 'kontraktor-client'

const global = {
  kclient: new KClient(), // loaded in index.html, no import required
  app: null,
  server: null,
  session: null
};

export default global;