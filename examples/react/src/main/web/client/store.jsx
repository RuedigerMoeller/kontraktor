import AppDispatcher from './dispatcher.jsx';
import { EventEmitter } from 'events';
import AppActions from './actions.jsx';

class AppStore extends EventEmitter {

  constructor() {
    super();
    this.loggedIn = false;
    this.userData = {};
    this.dispatchToken = AppDispatcher.register(this.dispatcherCallback.bind(this));
    this.client = new KClient();
    this.server = null;
    this.session = null;
  }

  getServer() {
    return this.server;
  }

  getSession() {
    return this.session;
  }

  isLoggedIn() {
    return this.loggedIn;
  }

  getUserData() {
    return this.userData;
  }

  getServer() {
    if ( this.server ) {
      return new KPromise(this.server);
    }
    return this.client.connect("http://localhost:8080/ep");
  }

  register(user,password) {
    const res = new KPromise();
    this.getServer().then( (serv,err) => {
      if ( serv ) {
        serv.$register(user,password).then( (r,e) => {
          res.complete(r,e);
          if (!e)
            this.emit("STORE_REGISTERED_USER");
        });
      } else {
        res.complete(null,"connection failure");
        this.emit("STORE_CONNECTION_FAILURE");
      }
    });
    return res;
  }

  login(user,password) {
    if ( ! this.isLoggedIn() ) {
      this.getServer().then( (r,e) => {
        if ( r ) {
          this.server = r;
          AppActions.connected();
          r.$login( user, password, null).then( (arr,err) => {
            console.log("login ",arr);
            if ( arr && arr[0] ) {
              this.session = arr[0];
              this.userData = arr[1];
              this.emit("STORE_LOGIN_CHANGED");
            }
          });
        }
      });
    }
  }

  addChangeListener(eventName, callback) {
    this.on(eventName, callback);
  }

  dispatcherCallback(action) {
    switch (action.actionType) {
      case 'TRY_LOGIN':
        this.login(action.value.user,action.value.password);
        break;
      case 'TRY_REGISTER':
        this.register(action.value.user,action.value.password);
        break;
      case 'LOGIN_CHANGED':
        // this.emit(action.value);
        break;
    }
    this.emit('STORE_' + action.actionType);
    return true;
  }
}

export default new AppStore();
