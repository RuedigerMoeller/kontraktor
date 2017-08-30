import {KClient} from 'kontraktor-client'

export default const global = {
  kclient: new KClient(), // loaded in index.html, no import required
  app: null,
  server: null,
  session: null
};
