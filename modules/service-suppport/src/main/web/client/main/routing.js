class Routing {

  constructor() {
    this.currentRoute = "";
    this.routeListeners = [];
    this.fullRoute = [""];
    window.addEventListener("hashchange", this.processRouteChange.bind(this));
  }

  addRouteListener( fun ) {
    this.routeListeners.push(fun);
  }

  fireRouteChange( event ) {
    for ( var i = 0; i < this.routeListeners.length; i++ ) {
      this.routeListeners[i].apply(null,[event]);
    }
  }

  processRouteChange( ev ) {
    var newURL = decodeURI(ev.newURL);
    var oldURL = decodeURI(ev.oldURL);

    var newRoute = "home";
    var idx = newURL.indexOf("#");
    if ( idx > 0 ) {
      newRoute = newURL.substring(idx+1);
    } else {
      window.location.hash = "#home";
      return;
    }

    var fullRoute = [""];
    var split = newRoute.split("/");
    if ( split && split.length > 1 ) {
      newRoute = split[0];
      fullRoute = split;
    }

    this.fireRouteChange( { type: "pre", oldRoute: this.currentRoute, newRoute: newRoute, fullRoute: fullRoute } );

    var oldRoute = this.currentRoute;
    var oldFullRoute = this.fullRoute;
    this.currentRoute = newRoute;

    this.fireRouteChange({ type: "post", oldRoute: oldRoute, newRoute: this.currentRoute, fullRoute: fullRoute, oldFullRoute: oldFullRoute } );
  }

  page(string) {
    if ( string.indexOf("#") < 0 ) {
      string = '#' + string;
    }
    window.location.hash = string;
  }

}