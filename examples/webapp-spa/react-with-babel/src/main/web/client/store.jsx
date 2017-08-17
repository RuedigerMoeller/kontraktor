import { EventEmitter } from 'events';

export class AppStore extends EventEmitter {

  constructor() {
    super();
    this.client = new KClient();
    this.reset();
    const self = this;
    this.client.listener = new class extends KClientListener {
      onInvalidResponse(resp) {
        // assume session timeout
        self.reset();
        self.emit("login");
      }
    };
  }

  reset() {
    this.loggedIn = false;
    this.userData = {};
    this.server = null;
    this.session = null;
    this.client.reset();
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
    return this.client.connect("http://localhost:8080/ep").then( (r,e) => {
      if ( r ) {
        this.server = r;
        this.emit("connected");
      }
      return [r,e];
    });
  }

  getTokenLink() {
    return this.session.createTokenLink();
  }

  queryUsers( cb ) {
    this.session.$queryUsers( cb );
  }

  register(user,password,text) {
    const res = new KPromise();
    this.getServer().then( (serv,err) => {
      if ( serv ) {
        serv.register(user,password,text).then( (r,e) => {
          res.complete(r,e);
          if (!e)
            this.emit("register");
        });
      } else {
        res.complete(null,"connection failure");
        this.emit("connection_failure");
      }
    });
    return res;
  }

  login(user,password) {
    const res = new KPromise();
    if ( ! this.isLoggedIn() ) {
      this.getServer().then( (r,e) => {
        if ( r ) {
          r.login( user, password, null).then( (arr,err) => {
            console.log("login ",arr);
            if ( arr && arr[0] ) {
              this.session = arr[0];
              this.userData = arr[1];
              this.loggedIn = true;
              this.emit("login");
              res.complete(true,null);
            } else {
              res.complete(null,err);
            }
          });
        }
      });
    } else {
      res.complete(null,'already logged in');
      this.emit("login");
    }
    return res;
  }

  addChangeListener(eventName, callback) {
    this.on(eventName, callback);
  }

  deleteUser(key) {
    this.getSession().$delUser(key); // void
  }
}

export const Store = new AppStore();

export const AppActions = new Proxy( {}, {
   get: function(target, property, receiver) {
      return function () {
        if ("target" === property)
          return target;
        const res = Store[property].apply(Store,arguments);
        return res;
      }
    }
  }
);
