// MICRO =>
(function () {
  function resolve() {
    document.body.removeAttribute("unresolved");
  }

  if (window.WebComponents) {
    addEventListener("WebComponentsReady", resolve);
  } else {
    resolve();
  }
})();

Polymer = {
  Settings: function () {
    var user = window.Polymer || {};
    location.search.slice(1).split("&").forEach(function (o) {
      o = o.split("=");
      o[0] && (user[o[0]] = o[1] || true);
    });
    var wantShadow = user.dom === "shadow";
    var hasShadow = Boolean(Element.prototype.createShadowRoot);
    var nativeShadow = hasShadow && !window.ShadowDOMPolyfill;
    var useShadow = wantShadow && hasShadow;
    var hasNativeImports = Boolean("import" in document.createElement("link"));
    var useNativeImports = hasNativeImports;
    var useNativeCustomElements = !window.CustomElements || window.CustomElements.useNative;
    return {
      wantShadow: wantShadow,
      hasShadow: hasShadow,
      nativeShadow: nativeShadow,
      useShadow: useShadow,
      useNativeShadow: useShadow && nativeShadow,
      useNativeImports: useNativeImports,
      useNativeCustomElements: useNativeCustomElements
    };
  }()
};

(function () {
  var userPolymer = window.Polymer;
  window.Polymer = function (prototype) {
    var ctor = desugar(prototype);
    prototype = ctor.prototype;
    var options = {
      prototype: prototype
    };
    if (prototype.extends) {
      options.extends = prototype.extends;
    }
    Polymer.telemetry._registrate(prototype);
    document.registerElement(prototype.is, options);
    return ctor;
  };
  var desugar = function (prototype) {
    prototype = Polymer.Base.chainObject(prototype, Polymer.Base);
    prototype.registerCallback();
    return prototype.constructor;
  };
  window.Polymer = Polymer;
  if (userPolymer) {
    for (var i in userPolymer) {
      Polymer[i] = userPolymer[i];
    }
  }
  Polymer.Class = desugar;
})();

Polymer.telemetry = {
  registrations: [],
  _regLog: function (prototype) {
    console.log("[" + prototype.is + "]: registered");
  },
  _registrate: function (prototype) {
    this.registrations.push(prototype);
    Polymer.log && this._regLog(prototype);
  },
  dumpRegistrations: function () {
    this.registrations.forEach(this._regLog);
  }
};

Object.defineProperty(window, "currentImport", {
  enumerable: true,
  configurable: true,
  get: function () {
    return (document._currentScript || document.currentScript).ownerDocument;
  }
});

Polymer.Base = {
  _addFeature: function (feature) {
    this.extend(this, feature);
  },
  registerCallback: function () {
    this._registerFeatures();
    this._doBehavior("registered");
  },
  createdCallback: function () {
    Polymer.telemetry.instanceCount++;
    this.root = this;
    this._doBehavior("created");
    this._initFeatures();
  },
  attachedCallback: function () {
    this.isAttached = true;
    this._doBehavior("attached");
  },
  detachedCallback: function () {
    this.isAttached = false;
    this._doBehavior("detached");
  },
  attributeChangedCallback: function (name) {
    this._setAttributeToProperty(this, name);
    this._doBehavior("attributeChanged", arguments);
  },
  extend: function (prototype, api) {
    if (prototype && api) {
      Object.getOwnPropertyNames(api).forEach(function (n) {
        this.copyOwnProperty(n, api, prototype);
      }, this);
    }
    return prototype || api;
  },
  copyOwnProperty: function (name, source, target) {
    var pd = Object.getOwnPropertyDescriptor(source, name);
    if (pd) {
      Object.defineProperty(target, name, pd);
    }
  },
  _log: console.log.apply.bind(console.log, console),
  _warn: console.warn.apply.bind(console.warn, console),
  _error: console.error.apply.bind(console.error, console),
  _logf: function () {
    return this._logPrefix.concat([this.is]).concat(Array.prototype.slice.call(arguments, 0));
  }
};

Polymer.Base._logPrefix = function () {
  var color = window.chrome || /firefox/i.test(navigator.userAgent);
  return color ? ["%c[%s::%s]:", "font-weight: bold; background-color:#EEEE00;"] : ["[%s::%s]:"];
}();

Polymer.Base.chainObject = function (object, inherited) {
  if (object && inherited && object !== inherited) {
    if (!Object.__proto__) {
      object = Polymer.Base.extend(Object.create(inherited), object);
    }
    object.__proto__ = inherited;
  }
  return object;
};

Polymer.Base = Polymer.Base.chainObject(Polymer.Base, HTMLElement.prototype);

Polymer.telemetry.instanceCount = 0;

(function () {
  var modules = {};
  var DomModule = function () {
    return document.createElement("dom-module");
  };
  DomModule.prototype = Object.create(HTMLElement.prototype);
  DomModule.prototype.constructor = DomModule;
  DomModule.prototype.createdCallback = function () {
    var id = this.id || this.getAttribute("name") || this.getAttribute("is");
    if (id) {
      this.id = id;
      modules[id] = this;
    }
  };
  DomModule.prototype.import = function (id, slctr) {
    var m = modules[id];
    if (!m) {
      forceDocumentUpgrade();
      m = modules[id];
    }
    if (m && slctr) {
      m = m.querySelector(slctr);
    }
    return m;
  };
  var cePolyfill = window.CustomElements && !CustomElements.useNative;
  if (cePolyfill) {
    var ready = CustomElements.ready;
    CustomElements.ready = true;
  }
  document.registerElement("dom-module", DomModule);
  if (cePolyfill) {
    CustomElements.ready = ready;
  }
  function forceDocumentUpgrade() {
    if (cePolyfill) {
      var script = document._currentScript || document.currentScript;
      if (script) {
        CustomElements.upgradeAll(script.ownerDocument);
      }
    }
  }
})();

Polymer.Base._addFeature({
  _prepIs: function () {
    if (!this.is) {
      var module = (document._currentScript || document.currentScript).parentNode;
      if (module.localName === "dom-module") {
        var id = module.id || module.getAttribute("name") || module.getAttribute("is");
        this.is = id;
      }
    }
  }
});

Polymer.Base._addFeature({
  behaviors: [],
  _prepBehaviors: function () {
    if (this.behaviors.length) {
      this.behaviors = this._flattenBehaviorsList(this.behaviors);
    }
    this._prepAllBehaviors(this.behaviors);
  },
  _flattenBehaviorsList: function (behaviors) {
    var flat = [];
    behaviors.forEach(function (b) {
      if (b instanceof Array) {
        flat = flat.concat(this._flattenBehaviorsList(b));
      } else if (b) {
        flat.push(b);
      } else {
        this._warn(this._logf("_flattenBehaviorsList", "behavior is null, check for missing or 404 import"));
      }
    }, this);
    return flat;
  },
  _prepAllBehaviors: function (behaviors) {
    for (var i = behaviors.length - 1; i >= 0; i--) {
      this._mixinBehavior(behaviors[i]);
    }
    for (var i = 0, l = behaviors.length; i < l; i++) {
      this._prepBehavior(behaviors[i]);
    }
    this._prepBehavior(this);
  },
  _mixinBehavior: function (b) {
    Object.getOwnPropertyNames(b).forEach(function (n) {
      switch (n) {
        case "hostAttributes":
        case "registered":
        case "properties":
        case "observers":
        case "listeners":
        case "created":
        case "attached":
        case "detached":
        case "attributeChanged":
        case "configure":
        case "ready":
          break;

        default:
          if (!this.hasOwnProperty(n)) {
            this.copyOwnProperty(n, b, this);
          }
          break;
      }
    }, this);
  },
  _doBehavior: function (name, args) {
    this.behaviors.forEach(function (b) {
      this._invokeBehavior(b, name, args);
    }, this);
    this._invokeBehavior(this, name, args);
  },
  _invokeBehavior: function (b, name, args) {
    var fn = b[name];
    if (fn) {
      fn.apply(this, args || Polymer.nar);
    }
  },
  _marshalBehaviors: function () {
    this.behaviors.forEach(function (b) {
      this._marshalBehavior(b);
    }, this);
    this._marshalBehavior(this);
  }
});

Polymer.Base._addFeature({
  _prepExtends: function () {
    if (this.extends) {
      this.__proto__ = this._getExtendedPrototype(this.extends);
    }
  },
  _getExtendedPrototype: function (tag) {
    return this._getExtendedNativePrototype(tag);
  },
  _nativePrototypes: {},
  _getExtendedNativePrototype: function (tag) {
    var p = this._nativePrototypes[tag];
    if (!p) {
      var np = this.getNativePrototype(tag);
      p = this.extend(Object.create(np), Polymer.Base);
      this._nativePrototypes[tag] = p;
    }
    return p;
  },
  getNativePrototype: function (tag) {
    return Object.getPrototypeOf(document.createElement(tag));
  }
});

Polymer.Base._addFeature({
  _prepConstructor: function () {
    this._factoryArgs = this.extends ? [this.extends, this.is] : [this.is];
    var ctor = function () {
      return this._factory(arguments);
    };
    if (this.hasOwnProperty("extends")) {
      ctor.extends = this.extends;
    }
    Object.defineProperty(this, "constructor", {
      value: ctor,
      writable: true,
      configurable: true
    });
    ctor.prototype = this;
  },
  _factory: function (args) {
    var elt = document.createElement.apply(document, this._factoryArgs);
    if (this.factoryImpl) {
      this.factoryImpl.apply(elt, args);
    }
    return elt;
  }
});

Polymer.nob = Object.create(null);

Polymer.Base._addFeature({
  properties: {},
  getPropertyInfo: function (property) {
    var info = this._getPropertyInfo(property, this.properties);
    if (!info) {
      this.behaviors.some(function (b) {
        return info = this._getPropertyInfo(property, b.properties);
      }, this);
    }
    return info || Polymer.nob;
  },
  _getPropertyInfo: function (property, properties) {
    var p = properties && properties[property];
    if (typeof p === "function") {
      p = properties[property] = {
        type: p
      };
    }
    if (p) {
      p.defined = true;
    }
    return p;
  }
});

Polymer.CaseMap = {
  _caseMap: {},
  dashToCamelCase: function (dash) {
    var mapped = Polymer.CaseMap._caseMap[dash];
    if (mapped) {
      return mapped;
    }
    if (dash.indexOf("-") < 0) {
      return Polymer.CaseMap._caseMap[dash] = dash;
    }
    return Polymer.CaseMap._caseMap[dash] = dash.replace(/-([a-z])/g, function (m) {
      return m[1].toUpperCase();
    });
  },
  camelToDashCase: function (camel) {
    var mapped = Polymer.CaseMap._caseMap[camel];
    if (mapped) {
      return mapped;
    }
    return Polymer.CaseMap._caseMap[camel] = camel.replace(/([a-z][A-Z])/g, function (g) {
      return g[0] + "-" + g[1].toLowerCase();
    });
  }
};

Polymer.Base._addFeature({
  _prepAttributes: function () {
    this._aggregatedAttributes = {};
  },
  _addHostAttributes: function (attributes) {
    if (attributes) {
      this.mixin(this._aggregatedAttributes, attributes);
    }
  },
  _marshalHostAttributes: function () {
    this._applyAttributes(this, this._aggregatedAttributes);
  },
  _applyAttributes: function (node, attr$) {
    for (var n in attr$) {
      if (!this.hasAttribute(n) && n !== "class") {
        this.serializeValueToAttribute(attr$[n], n, this);
      }
    }
  },
  _marshalAttributes: function () {
    this._takeAttributesToModel(this);
  },
  _takeAttributesToModel: function (model) {
    for (var i = 0, l = this.attributes.length; i < l; i++) {
      this._setAttributeToProperty(model, this.attributes[i].name);
    }
  },
  _setAttributeToProperty: function (model, attrName) {
    if (!this._serializing) {
      var propName = Polymer.CaseMap.dashToCamelCase(attrName);
      var info = this.getPropertyInfo(propName);
      if (info.defined || this._propertyEffects && this._propertyEffects[propName]) {
        var val = this.getAttribute(attrName);
        model[propName] = this.deserialize(val, info.type);
      }
    }
  },
  _serializing: false,
  reflectPropertyToAttribute: function (name) {
    this._serializing = true;
    this.serializeValueToAttribute(this[name], Polymer.CaseMap.camelToDashCase(name));
    this._serializing = false;
  },
  serializeValueToAttribute: function (value, attribute, node) {
    var str = this.serialize(value);
    (node || this)[str === undefined ? "removeAttribute" : "setAttribute"](attribute, str);
  },
  deserialize: function (value, type) {
    switch (type) {
      case Number:
        value = Number(value);
        break;

      case Boolean:
        value = value !== null;
        break;

      case Object:
        try {
          value = JSON.parse(value);
        } catch (x) {
        }
        break;

      case Array:
        try {
          value = JSON.parse(value);
        } catch (x) {
          value = null;
          console.warn("Polymer::Attributes: couldn`t decode Array as JSON");
        }
        break;

      case Date:
        value = new Date(value);
        break;

      case String:
      default:
        break;
    }
    return value;
  },
  serialize: function (value) {
    switch (typeof value) {
      case "boolean":
        return value ? "" : undefined;

      case "object":
        if (value instanceof Date) {
          return value;
        } else if (value) {
          try {
            return JSON.stringify(value);
          } catch (x) {
            return "";
          }
        }

      default:
        return value != null ? value : undefined;
    }
  }
});

Polymer.Base._addFeature({
  _setupDebouncers: function () {
    this._debouncers = {};
  },
  debounce: function (jobName, callback, wait) {
    this._debouncers[jobName] = Polymer.Debounce.call(this, this._debouncers[jobName], callback, wait);
  },
  isDebouncerActive: function (jobName) {
    var debouncer = this._debouncers[jobName];
    return debouncer && debouncer.finish;
  },
  flushDebouncer: function (jobName) {
    var debouncer = this._debouncers[jobName];
    if (debouncer) {
      debouncer.complete();
    }
  },
  cancelDebouncer: function (jobName) {
    var debouncer = this._debouncers[jobName];
    if (debouncer) {
      debouncer.stop();
    }
  }
});

Polymer.version = "1.0.0";

Polymer.Base._addFeature({
  _registerFeatures: function () {
    this._prepIs();
    this._prepAttributes();
    this._prepBehaviors();
    this._prepExtends();
    this._prepConstructor();
  },
  _prepBehavior: function (b) {
    this._addHostAttributes(b.hostAttributes);
  },
  _marshalBehavior: function (b) {
  },
  _initFeatures: function () {
    this._marshalHostAttributes();
    this._setupDebouncers();
    this._marshalBehaviors();
  }
});

Polymer.Base._addFeature({
  _prepTemplate: function () {
    this._template = this._template || Polymer.DomModule.import(this.is, "template");
    if (!this._template) {
      var script = document._currentScript || document.currentScript;
      var prev = script && script.previousElementSibling;
      if (prev && prev.localName === "template") {
        this._template = prev;
      }
    }
    if (this._template && this._template.hasAttribute("is")) {
      this._warn(this._logf("_prepTemplate", "top-level Polymer template " + "must not be a type-extension, found", this._template, "Move inside simple <template>."));
    }
  },
  _stampTemplate: function () {
    if (this._template) {
      this.root = this.instanceTemplate(this._template);
    }
  },
  instanceTemplate: function (template) {
    var dom = document.importNode(template._content || template.content, true);
    return dom;
  }
});

(function () {
  var baseAttachedCallback = Polymer.Base.attachedCallback;
  Polymer.Base._addFeature({
    _hostStack: [],
    ready: function () {
    },
    _pushHost: function (host) {
      this.dataHost = host = host || Polymer.Base._hostStack[Polymer.Base._hostStack.length - 1];
      if (host && host._clients) {
        host._clients.push(this);
      }
      this._beginHost();
    },
    _beginHost: function () {
      Polymer.Base._hostStack.push(this);
      if (!this._clients) {
        this._clients = [];
      }
    },
    _popHost: function () {
      Polymer.Base._hostStack.pop();
    },
    _tryReady: function () {
      if (this._canReady()) {
        this._ready();
      }
    },
    _canReady: function () {
      return !this.dataHost || this.dataHost._clientsReadied;
    },
    _ready: function () {
      this._beforeClientsReady();
      this._setupRoot();
      this._readyClients();
      this._afterClientsReady();
      this._readySelf();
    },
    _readyClients: function () {
      this._beginDistribute();
      var c$ = this._clients;
      for (var i = 0, l = c$.length, c; i < l && (c = c$[i]); i++) {
        c._ready();
      }
      this._finishDistribute();
      this._clientsReadied = true;
      this._clients = null;
    },
    _readySelf: function () {
      this._doBehavior("ready");
      this._readied = true;
      if (this._attachedPending) {
        this._attachedPending = false;
        this.attachedCallback();
      }
    },
    _beforeClientsReady: function () {
    },
    _afterClientsReady: function () {
    },
    _beforeAttached: function () {
    },
    attachedCallback: function () {
      if (this._readied) {
        this._beforeAttached();
        baseAttachedCallback.call(this);
      } else {
        this._attachedPending = true;
      }
    }
  });
})();

Polymer.ArraySplice = function () {
  function newSplice(index, removed, addedCount) {
    return {
      index: index,
      removed: removed,
      addedCount: addedCount
    };
  }

  var EDIT_LEAVE = 0;
  var EDIT_UPDATE = 1;
  var EDIT_ADD = 2;
  var EDIT_DELETE = 3;

  function ArraySplice() {
  }

  ArraySplice.prototype = {
    calcEditDistances: function (current, currentStart, currentEnd, old, oldStart, oldEnd) {
      var rowCount = oldEnd - oldStart + 1;
      var columnCount = currentEnd - currentStart + 1;
      var distances = new Array(rowCount);
      for (var i = 0; i < rowCount; i++) {
        distances[i] = new Array(columnCount);
        distances[i][0] = i;
      }
      for (var j = 0; j < columnCount; j++) distances[0][j] = j;
      for (var i = 1; i < rowCount; i++) {
        for (var j = 1; j < columnCount; j++) {
          if (this.equals(current[currentStart + j - 1], old[oldStart + i - 1])) distances[i][j] = distances[i - 1][j - 1]; else {
            var north = distances[i - 1][j] + 1;
            var west = distances[i][j - 1] + 1;
            distances[i][j] = north < west ? north : west;
          }
        }
      }
      return distances;
    },
    spliceOperationsFromEditDistances: function (distances) {
      var i = distances.length - 1;
      var j = distances[0].length - 1;
      var current = distances[i][j];
      var edits = [];
      while (i > 0 || j > 0) {
        if (i == 0) {
          edits.push(EDIT_ADD);
          j--;
          continue;
        }
        if (j == 0) {
          edits.push(EDIT_DELETE);
          i--;
          continue;
        }
        var northWest = distances[i - 1][j - 1];
        var west = distances[i - 1][j];
        var north = distances[i][j - 1];
        var min;
        if (west < north) min = west < northWest ? west : northWest; else min = north < northWest ? north : northWest;
        if (min == northWest) {
          if (northWest == current) {
            edits.push(EDIT_LEAVE);
          } else {
            edits.push(EDIT_UPDATE);
            current = northWest;
          }
          i--;
          j--;
        } else if (min == west) {
          edits.push(EDIT_DELETE);
          i--;
          current = west;
        } else {
          edits.push(EDIT_ADD);
          j--;
          current = north;
        }
      }
      edits.reverse();
      return edits;
    },
    calcSplices: function (current, currentStart, currentEnd, old, oldStart, oldEnd) {
      var prefixCount = 0;
      var suffixCount = 0;
      var minLength = Math.min(currentEnd - currentStart, oldEnd - oldStart);
      if (currentStart == 0 && oldStart == 0) prefixCount = this.sharedPrefix(current, old, minLength);
      if (currentEnd == current.length && oldEnd == old.length) suffixCount = this.sharedSuffix(current, old, minLength - prefixCount);
      currentStart += prefixCount;
      oldStart += prefixCount;
      currentEnd -= suffixCount;
      oldEnd -= suffixCount;
      if (currentEnd - currentStart == 0 && oldEnd - oldStart == 0) return [];
      if (currentStart == currentEnd) {
        var splice = newSplice(currentStart, [], 0);
        while (oldStart < oldEnd) splice.removed.push(old[oldStart++]);
        return [splice];
      } else if (oldStart == oldEnd) return [newSplice(currentStart, [], currentEnd - currentStart)];
      var ops = this.spliceOperationsFromEditDistances(this.calcEditDistances(current, currentStart, currentEnd, old, oldStart, oldEnd));
      var splice = undefined;
      var splices = [];
      var index = currentStart;
      var oldIndex = oldStart;
      for (var i = 0; i < ops.length; i++) {
        switch (ops[i]) {
          case EDIT_LEAVE:
            if (splice) {
              splices.push(splice);
              splice = undefined;
            }
            index++;
            oldIndex++;
            break;

          case EDIT_UPDATE:
            if (!splice) splice = newSplice(index, [], 0);
            splice.addedCount++;
            index++;
            splice.removed.push(old[oldIndex]);
            oldIndex++;
            break;

          case EDIT_ADD:
            if (!splice) splice = newSplice(index, [], 0);
            splice.addedCount++;
            index++;
            break;

          case EDIT_DELETE:
            if (!splice) splice = newSplice(index, [], 0);
            splice.removed.push(old[oldIndex]);
            oldIndex++;
            break;
        }
      }
      if (splice) {
        splices.push(splice);
      }
      return splices;
    },
    sharedPrefix: function (current, old, searchLength) {
      for (var i = 0; i < searchLength; i++) if (!this.equals(current[i], old[i])) return i;
      return searchLength;
    },
    sharedSuffix: function (current, old, searchLength) {
      var index1 = current.length;
      var index2 = old.length;
      var count = 0;
      while (count < searchLength && this.equals(current[--index1], old[--index2])) count++;
      return count;
    },
    calculateSplices: function (current, previous) {
      return this.calcSplices(current, 0, current.length, previous, 0, previous.length);
    },
    equals: function (currentValue, previousValue) {
      return currentValue === previousValue;
    }
  };
  return new ArraySplice();
}();

Polymer.EventApi = function () {
  var Settings = Polymer.Settings;
  var EventApi = function (event) {
    this.event = event;
  };
  if (Settings.useShadow) {
    EventApi.prototype = {
      get rootTarget() {
        return this.event.path[0];
      },
      get localTarget() {
        return this.event.target;
      },
      get path() {
        return this.event.path;
      }
    };
  } else {
    EventApi.prototype = {
      get rootTarget() {
        return this.event.target;
      },
      get localTarget() {
        var current = this.event.currentTarget;
        var currentRoot = current && Polymer.dom(current).getOwnerRoot();
        var p$ = this.path;
        for (var i = 0; i < p$.length; i++) {
          if (Polymer.dom(p$[i]).getOwnerRoot() === currentRoot) {
            return p$[i];
          }
        }
      },
      get path() {
        if (!this.event._path) {
          var path = [];
          var o = this.rootTarget;
          while (o) {
            path.push(o);
            o = Polymer.dom(o).parentNode || o.host;
          }
          path.push(window);
          this.event._path = path;
        }
        return this.event._path;
      }
    };
  }
  var factory = function (event) {
    if (!event.__eventApi) {
      event.__eventApi = new EventApi(event);
    }
    return event.__eventApi;
  };
  return {
    factory: factory
  };
}();

Polymer.domInnerHTML = function () {
  var escapeAttrRegExp = /[&\u00A0"]/g;
  var escapeDataRegExp = /[&\u00A0<>]/g;

  function escapeReplace(c) {
    switch (c) {
      case "&":
        return "&amp;";

      case "<":
        return "&lt;";

      case ">":
        return "&gt;";

      case '"':
        return "&quot;";

      case " ":
        return "&nbsp;";
    }
  }

  function escapeAttr(s) {
    return s.replace(escapeAttrRegExp, escapeReplace);
  }

  function escapeData(s) {
    return s.replace(escapeDataRegExp, escapeReplace);
  }

  function makeSet(arr) {
    var set = {};
    for (var i = 0; i < arr.length; i++) {
      set[arr[i]] = true;
    }
    return set;
  }

  var voidElements = makeSet(["area", "base", "br", "col", "command", "embed", "hr", "img", "input", "keygen", "link", "meta", "param", "source", "track", "wbr"]);
  var plaintextParents = makeSet(["style", "script", "xmp", "iframe", "noembed", "noframes", "plaintext", "noscript"]);

  function getOuterHTML(node, parentNode, composed) {
    switch (node.nodeType) {
      case Node.ELEMENT_NODE:
        var tagName = node.localName;
        var s = "<" + tagName;
        var attrs = node.attributes;
        for (var i = 0, attr; attr = attrs[i]; i++) {
          s += " " + attr.name + '="' + escapeAttr(attr.value) + '"';
        }
        s += ">";
        if (voidElements[tagName]) {
          return s;
        }
        return s + getInnerHTML(node, composed) + "</" + tagName + ">";

      case Node.TEXT_NODE:
        var data = node.data;
        if (parentNode && plaintextParents[parentNode.localName]) {
          return data;
        }
        return escapeData(data);

      case Node.COMMENT_NODE:
        return "<!--" + node.data + "-->";

      default:
        console.error(node);
        throw new Error("not implemented");
    }
  }

  function getInnerHTML(node, composed) {
    if (node instanceof HTMLTemplateElement) node = node.content;
    var s = "";
    var c$ = Polymer.dom(node).childNodes;
    c$ = composed ? node._composedChildren : c$;
    for (var i = 0, l = c$.length, child; i < l && (child = c$[i]); i++) {
      s += getOuterHTML(child, node, composed);
    }
    return s;
  }

  return {
    getInnerHTML: getInnerHTML
  };
}();

Polymer.DomApi = function () {
  "use strict";
  var Settings = Polymer.Settings;
  var getInnerHTML = Polymer.domInnerHTML.getInnerHTML;
  var nativeInsertBefore = Element.prototype.insertBefore;
  var nativeRemoveChild = Element.prototype.removeChild;
  var nativeAppendChild = Element.prototype.appendChild;
  var dirtyRoots = [];
  var DomApi = function (node) {
    this.node = node;
    if (this.patch) {
      this.patch();
    }
  };
  DomApi.prototype = {
    flush: function () {
      for (var i = 0, host; i < dirtyRoots.length; i++) {
        host = dirtyRoots[i];
        host.flushDebouncer("_distribute");
      }
      dirtyRoots = [];
    },
    _lazyDistribute: function (host) {
      if (host.shadyRoot && host.shadyRoot._distributionClean) {
        host.shadyRoot._distributionClean = false;
        host.debounce("_distribute", host._distributeContent);
        dirtyRoots.push(host);
      }
    },
    appendChild: function (node) {
      var distributed;
      this._removeNodeFromHost(node);
      if (this._nodeIsInLogicalTree(this.node)) {
        var host = this._hostForNode(this.node);
        this._addLogicalInfo(node, this.node, host && host.shadyRoot);
        this._addNodeToHost(node);
        if (host) {
          distributed = this._maybeDistribute(node, this.node, host);
        }
      }
      if (!distributed && !this._tryRemoveUndistributedNode(node)) {
        var container = this.node._isShadyRoot ? this.node.host : this.node;
        nativeAppendChild.call(container, node);
        addToComposedParent(container, node);
      }
      return node;
    },
    insertBefore: function (node, ref_node) {
      if (!ref_node) {
        return this.appendChild(node);
      }
      var distributed;
      this._removeNodeFromHost(node);
      if (this._nodeIsInLogicalTree(this.node)) {
        saveLightChildrenIfNeeded(this.node);
        var children = this.childNodes;
        var index = children.indexOf(ref_node);
        if (index < 0) {
          throw Error("The ref_node to be inserted before is not a child " + "of this node");
        }
        var host = this._hostForNode(this.node);
        this._addLogicalInfo(node, this.node, host && host.shadyRoot, index);
        this._addNodeToHost(node);
        if (host) {
          distributed = this._maybeDistribute(node, this.node, host);
        }
      }
      if (!distributed && !this._tryRemoveUndistributedNode(node)) {
        ref_node = ref_node.localName === CONTENT ? this._firstComposedNode(ref_node) : ref_node;
        var container = this.node._isShadyRoot ? this.node.host : this.node;
        nativeInsertBefore.call(container, node, ref_node);
        addToComposedParent(container, node, ref_node);
      }
      return node;
    },
    removeChild: function (node) {
      if (factory(node).parentNode !== this.node) {
        console.warn("The node to be removed is not a child of this node", node);
      }
      var distributed;
      if (this._nodeIsInLogicalTree(this.node)) {
        var host = this._hostForNode(this.node);
        distributed = this._maybeDistribute(node, this.node, host);
        this._removeNodeFromHost(node);
      }
      if (!distributed) {
        var container = this.node._isShadyRoot ? this.node.host : this.node;
        if (container === node.parentNode) {
          nativeRemoveChild.call(container, node);
          removeFromComposedParent(container, node);
        }
      }
      return node;
    },
    replaceChild: function (node, ref_node) {
      this.insertBefore(node, ref_node);
      this.removeChild(ref_node);
      return node;
    },
    getOwnerRoot: function () {
      return this._ownerShadyRootForNode(this.node);
    },
    _ownerShadyRootForNode: function (node) {
      if (!node) {
        return;
      }
      if (node._ownerShadyRoot === undefined) {
        var root;
        if (node._isShadyRoot) {
          root = node;
        } else {
          var parent = Polymer.dom(node).parentNode;
          if (parent) {
            root = parent._isShadyRoot ? parent : this._ownerShadyRootForNode(parent);
          } else {
            root = null;
          }
        }
        node._ownerShadyRoot = root;
      }
      return node._ownerShadyRoot;
    },
    _maybeDistribute: function (node, parent, host) {
      var nodeNeedsDistribute = this._nodeNeedsDistribution(node);
      var distribute = this._parentNeedsDistribution(parent) || nodeNeedsDistribute;
      if (nodeNeedsDistribute) {
        this._updateInsertionPoints(host);
      }
      if (distribute) {
        this._lazyDistribute(host);
      }
      return distribute;
    },
    _tryRemoveUndistributedNode: function (node) {
      if (this.node.shadyRoot) {
        if (node.parentNode) {
          nativeRemoveChild.call(node.parentNode, node);
        }
        return true;
      }
    },
    _updateInsertionPoints: function (host) {
      host.shadyRoot._insertionPoints = factory(host.shadyRoot).querySelectorAll(CONTENT);
    },
    _nodeIsInLogicalTree: function (node) {
      return Boolean(node._lightParent || node._isShadyRoot || this._ownerShadyRootForNode(node) || node.shadyRoot);
    },
    _hostForNode: function (node) {
      var root = node.shadyRoot || (node._isShadyRoot ? node : this._ownerShadyRootForNode(node));
      return root && root.host;
    },
    _parentNeedsDistribution: function (parent) {
      return parent && parent.shadyRoot && hasInsertionPoint(parent.shadyRoot);
    },
    _nodeNeedsDistribution: function (node) {
      return node.localName === CONTENT || node.nodeType === Node.DOCUMENT_FRAGMENT_NODE && node.querySelector(CONTENT);
    },
    _removeNodeFromHost: function (node) {
      if (node._lightParent) {
        var root = this._ownerShadyRootForNode(node);
        if (root) {
          root.host._elementRemove(node);
        }
        this._removeLogicalInfo(node, node._lightParent);
      }
      this._removeOwnerShadyRoot(node);
    },
    _addNodeToHost: function (node) {
      var checkNode = node.nodeType === Node.DOCUMENT_FRAGMENT_NODE ? node.firstChild : node;
      var root = this._ownerShadyRootForNode(checkNode);
      if (root) {
        root.host._elementAdd(node);
      }
    },
    _addLogicalInfo: function (node, container, root, index) {
      saveLightChildrenIfNeeded(container);
      var children = factory(container).childNodes;
      index = index === undefined ? children.length : index;
      if (node.nodeType === Node.DOCUMENT_FRAGMENT_NODE) {
        var c$ = Array.prototype.slice.call(node.childNodes);
        for (var i = 0, n; i < c$.length && (n = c$[i]); i++) {
          children.splice(index++, 0, n);
          n._lightParent = container;
        }
      } else {
        children.splice(index, 0, node);
        node._lightParent = container;
      }
    },
    _removeLogicalInfo: function (node, container) {
      var children = factory(container).childNodes;
      var index = children.indexOf(node);
      if (index < 0 || container !== node._lightParent) {
        throw Error("The node to be removed is not a child of this node");
      }
      children.splice(index, 1);
      node._lightParent = null;
    },
    _removeOwnerShadyRoot: function (node) {
      var hasCachedRoot = factory(node).getOwnerRoot() !== undefined;
      if (hasCachedRoot) {
        var c$ = factory(node).childNodes;
        for (var i = 0, l = c$.length, n; i < l && (n = c$[i]); i++) {
          this._removeOwnerShadyRoot(n);
        }
      }
      node._ownerShadyRoot = undefined;
    },
    _firstComposedNode: function (content) {
      var n$ = factory(content).getDistributedNodes();
      for (var i = 0, l = n$.length, n, p$; i < l && (n = n$[i]); i++) {
        p$ = factory(n).getDestinationInsertionPoints();
        if (p$[p$.length - 1] === content) {
          return n;
        }
      }
    },
    querySelector: function (selector) {
      return this.querySelectorAll(selector)[0];
    },
    querySelectorAll: function (selector) {
      return this._query(function (n) {
        return matchesSelector.call(n, selector);
      }, this.node);
    },
    _query: function (matcher, node) {
      node = node || this.node;
      var list = [];
      this._queryElements(factory(node).childNodes, matcher, list);
      return list;
    },
    _queryElements: function (elements, matcher, list) {
      for (var i = 0, l = elements.length, c; i < l && (c = elements[i]); i++) {
        if (c.nodeType === Node.ELEMENT_NODE) {
          this._queryElement(c, matcher, list);
        }
      }
    },
    _queryElement: function (node, matcher, list) {
      if (matcher(node)) {
        list.push(node);
      }
      this._queryElements(factory(node).childNodes, matcher, list);
    },
    getDestinationInsertionPoints: function () {
      return this.node._destinationInsertionPoints || [];
    },
    getDistributedNodes: function () {
      return this.node._distributedNodes || [];
    },
    queryDistributedElements: function (selector) {
      var c$ = this.childNodes;
      var list = [];
      this._distributedFilter(selector, c$, list);
      for (var i = 0, l = c$.length, c; i < l && (c = c$[i]); i++) {
        if (c.localName === CONTENT) {
          this._distributedFilter(selector, factory(c).getDistributedNodes(), list);
        }
      }
      return list;
    },
    _distributedFilter: function (selector, list, results) {
      results = results || [];
      for (var i = 0, l = list.length, d; i < l && (d = list[i]); i++) {
        if (d.nodeType === Node.ELEMENT_NODE && d.localName !== CONTENT && matchesSelector.call(d, selector)) {
          results.push(d);
        }
      }
      return results;
    },
    _clear: function () {
      while (this.childNodes.length) {
        this.removeChild(this.childNodes[0]);
      }
    },
    setAttribute: function (name, value) {
      this.node.setAttribute(name, value);
      this._distributeParent();
    },
    removeAttribute: function (name) {
      this.node.removeAttribute(name);
      this._distributeParent();
    },
    _distributeParent: function () {
      if (this._parentNeedsDistribution(this.parentNode)) {
        this._lazyDistribute(this.parentNode);
      }
    }
  };
  Object.defineProperty(DomApi.prototype, "classList", {
    get: function () {
      if (!this._classList) {
        this._classList = new DomApi.ClassList(this);
      }
      return this._classList;
    },
    configurable: true
  });
  DomApi.ClassList = function (host) {
    this.domApi = host;
    this.node = host.node;
  };
  DomApi.ClassList.prototype = {
    add: function () {
      this.node.classList.add.apply(this.node.classList, arguments);
      this.domApi._distributeParent();
    },
    remove: function () {
      this.node.classList.remove.apply(this.node.classList, arguments);
      this.domApi._distributeParent();
    },
    toggle: function () {
      this.node.classList.toggle.apply(this.node.classList, arguments);
      this.domApi._distributeParent();
    }
  };
  if (!Settings.useShadow) {
    Object.defineProperties(DomApi.prototype, {
      childNodes: {
        get: function () {
          var c$ = getLightChildren(this.node);
          return Array.isArray(c$) ? c$ : Array.prototype.slice.call(c$);
        },
        configurable: true
      },
      children: {
        get: function () {
          return Array.prototype.filter.call(this.childNodes, function (n) {
            return n.nodeType === Node.ELEMENT_NODE;
          });
        },
        configurable: true
      },
      parentNode: {
        get: function () {
          return this.node._lightParent || (this.node.__patched ? this.node._composedParent : this.node.parentNode);
        },
        configurable: true
      },
      firstChild: {
        get: function () {
          return this.childNodes[0];
        },
        configurable: true
      },
      lastChild: {
        get: function () {
          var c$ = this.childNodes;
          return c$[c$.length - 1];
        },
        configurable: true
      },
      nextSibling: {
        get: function () {
          var c$ = this.parentNode && factory(this.parentNode).childNodes;
          if (c$) {
            return c$[Array.prototype.indexOf.call(c$, this.node) + 1];
          }
        },
        configurable: true
      },
      previousSibling: {
        get: function () {
          var c$ = this.parentNode && factory(this.parentNode).childNodes;
          if (c$) {
            return c$[Array.prototype.indexOf.call(c$, this.node) - 1];
          }
        },
        configurable: true
      },
      firstElementChild: {
        get: function () {
          return this.children[0];
        },
        configurable: true
      },
      lastElementChild: {
        get: function () {
          var c$ = this.children;
          return c$[c$.length - 1];
        },
        configurable: true
      },
      nextElementSibling: {
        get: function () {
          var c$ = this.parentNode && factory(this.parentNode).children;
          if (c$) {
            return c$[Array.prototype.indexOf.call(c$, this.node) + 1];
          }
        },
        configurable: true
      },
      previousElementSibling: {
        get: function () {
          var c$ = this.parentNode && factory(this.parentNode).children;
          if (c$) {
            return c$[Array.prototype.indexOf.call(c$, this.node) - 1];
          }
        },
        configurable: true
      },
      textContent: {
        get: function () {
          if (this.node.nodeType === Node.TEXT_NODE) {
            return this.node.textContent;
          } else {
            return Array.prototype.map.call(this.childNodes, function (c) {
              return c.textContent;
            }).join("");
          }
        },
        set: function (text) {
          this._clear();
          if (text) {
            this.appendChild(document.createTextNode(text));
          }
        },
        configurable: true
      },
      innerHTML: {
        get: function () {
          if (this.node.nodeType === Node.TEXT_NODE) {
            return null;
          } else {
            return getInnerHTML(this.node);
          }
        },
        set: function (text) {
          if (this.node.nodeType !== Node.TEXT_NODE) {
            this._clear();
            var d = document.createElement("div");
            d.innerHTML = text;
            for (var e = d.firstChild; e; e = e.nextSibling) {
              this.appendChild(e);
            }
          }
        },
        configurable: true
      }
    });
    DomApi.prototype._getComposedInnerHTML = function () {
      return getInnerHTML(this.node, true);
    };
  } else {
    DomApi.prototype.querySelectorAll = function (selector) {
      return Array.prototype.slice.call(this.node.querySelectorAll(selector));
    };
    DomApi.prototype.getOwnerRoot = function () {
      var n = this.node;
      while (n) {
        if (n.nodeType === Node.DOCUMENT_FRAGMENT_NODE && n.host) {
          return n;
        }
        n = n.parentNode;
      }
    };
    DomApi.prototype.getDestinationInsertionPoints = function () {
      var n$ = this.node.getDestinationInsertionPoints();
      return n$ ? Array.prototype.slice.call(n$) : [];
    };
    DomApi.prototype.getDistributedNodes = function () {
      var n$ = this.node.getDistributedNodes();
      return n$ ? Array.prototype.slice.call(n$) : [];
    };
    DomApi.prototype._distributeParent = function () {
    };
    Object.defineProperties(DomApi.prototype, {
      childNodes: {
        get: function () {
          return Array.prototype.slice.call(this.node.childNodes);
        },
        configurable: true
      },
      children: {
        get: function () {
          return Array.prototype.slice.call(this.node.children);
        },
        configurable: true
      },
      textContent: {
        get: function () {
          return this.node.textContent;
        },
        set: function (value) {
          return this.node.textContent = value;
        },
        configurable: true
      },
      innerHTML: {
        get: function () {
          return this.node.innerHTML;
        },
        set: function (value) {
          return this.node.innerHTML = value;
        },
        configurable: true
      }
    });
    var forwards = ["parentNode", "firstChild", "lastChild", "nextSibling", "previousSibling", "firstElementChild", "lastElementChild", "nextElementSibling", "previousElementSibling"];
    forwards.forEach(function (name) {
      Object.defineProperty(DomApi.prototype, name, {
        get: function () {
          return this.node[name];
        },
        configurable: true
      });
    });
  }
  var CONTENT = "content";
  var factory = function (node, patch) {
    node = node || document;
    if (!node.__domApi) {
      node.__domApi = new DomApi(node, patch);
    }
    return node.__domApi;
  };
  Polymer.dom = function (obj, patch) {
    if (obj instanceof Event) {
      return Polymer.EventApi.factory(obj);
    } else {
      return factory(obj, patch);
    }
  };
  Polymer.dom.flush = DomApi.prototype.flush;
  function getLightChildren(node) {
    var children = node._lightChildren;
    return children ? children : node.childNodes;
  }

  function getComposedChildren(node) {
    if (!node._composedChildren) {
      node._composedChildren = Array.prototype.slice.call(node.childNodes);
    }
    return node._composedChildren;
  }

  function addToComposedParent(parent, node, ref_node) {
    var children = getComposedChildren(parent);
    var i = ref_node ? children.indexOf(ref_node) : -1;
    if (node.nodeType === Node.DOCUMENT_FRAGMENT_NODE) {
      var fragChildren = getComposedChildren(node);
      fragChildren.forEach(function (c) {
        addNodeToComposedChildren(c, parent, children, i);
      });
    } else {
      addNodeToComposedChildren(node, parent, children, i);
    }
  }

  function addNodeToComposedChildren(node, parent, children, i) {
    node._composedParent = parent;
    if (i >= 0) {
      children.splice(i, 0, node);
    } else {
      children.push(node);
    }
  }

  function removeFromComposedParent(parent, node) {
    node._composedParent = null;
    if (parent) {
      var children = getComposedChildren(parent);
      var i = children.indexOf(node);
      if (i >= 0) {
        children.splice(i, 1);
      }
    }
  }

  function saveLightChildrenIfNeeded(node) {
    if (!node._lightChildren) {
      var c$ = Array.prototype.slice.call(node.childNodes);
      for (var i = 0, l = c$.length, child; i < l && (child = c$[i]); i++) {
        child._lightParent = child._lightParent || node;
      }
      node._lightChildren = c$;
    }
  }

  function hasInsertionPoint(root) {
    return Boolean(root._insertionPoints.length);
  }

  var p = Element.prototype;
  var matchesSelector = p.matches || p.matchesSelector || p.mozMatchesSelector || p.msMatchesSelector || p.oMatchesSelector || p.webkitMatchesSelector;
  return {
    getLightChildren: getLightChildren,
    getComposedChildren: getComposedChildren,
    removeFromComposedParent: removeFromComposedParent,
    saveLightChildrenIfNeeded: saveLightChildrenIfNeeded,
    matchesSelector: matchesSelector,
    hasInsertionPoint: hasInsertionPoint,
    ctor: DomApi,
    factory: factory
  };
}();

(function () {
  Polymer.Base._addFeature({
    _prepShady: function () {
      this._useContent = this._useContent || Boolean(this._template);
      if (this._useContent) {
        this._template._hasInsertionPoint = this._template.content.querySelector("content");
      }
    },
    _poolContent: function () {
      if (this._useContent) {
        saveLightChildrenIfNeeded(this);
      }
    },
    _setupRoot: function () {
      if (this._useContent) {
        this._createLocalRoot();
        if (!this.dataHost) {
          upgradeLightChildren(this._lightChildren);
        }
      }
    },
    _createLocalRoot: function () {
      this.shadyRoot = this.root;
      this.shadyRoot._distributionClean = false;
      this.shadyRoot._isShadyRoot = true;
      this.shadyRoot._dirtyRoots = [];
      this.shadyRoot._insertionPoints = this._template._hasInsertionPoint ? this.shadyRoot.querySelectorAll("content") : [];
      saveLightChildrenIfNeeded(this.shadyRoot);
      this.shadyRoot.host = this;
    },
    get domHost() {
      var root = Polymer.dom(this).getOwnerRoot();
      return root && root.host;
    },
    distributeContent: function () {
      if (this.shadyRoot) {
        var host = getTopDistributingHost(this);
        Polymer.dom(this)._lazyDistribute(host);
      }
    },
    _distributeContent: function () {
      if (this._useContent && !this.shadyRoot._distributionClean) {
        this._beginDistribute();
        this._distributeDirtyRoots();
        this._finishDistribute();
      }
    },
    _beginDistribute: function () {
      if (this._useContent && hasInsertionPoint(this.shadyRoot)) {
        this._resetDistribution();
        this._distributePool(this.shadyRoot, this._collectPool());
      }
    },
    _distributeDirtyRoots: function () {
      var c$ = this.shadyRoot._dirtyRoots;
      for (var i = 0, l = c$.length, c; i < l && (c = c$[i]); i++) {
        c._distributeContent();
      }
      this.shadyRoot._dirtyRoots = [];
    },
    _finishDistribute: function () {
      if (this._useContent) {
        if (hasInsertionPoint(this.shadyRoot)) {
          this._composeTree();
        } else {
          if (!this.shadyRoot._hasDistributed) {
            this.textContent = "";
            this.appendChild(this.shadyRoot);
          } else {
            var children = this._composeNode(this);
            this._updateChildNodes(this, children);
          }
        }
        this.shadyRoot._hasDistributed = true;
        this.shadyRoot._distributionClean = true;
      }
    },
    elementMatches: function (selector, node) {
      node = node || this;
      return matchesSelector.call(node, selector);
    },
    _resetDistribution: function () {
      var children = getLightChildren(this);
      for (var i = 0; i < children.length; i++) {
        var child = children[i];
        if (child._destinationInsertionPoints) {
          child._destinationInsertionPoints = undefined;
        }
        if (isInsertionPoint(child)) {
          clearDistributedDestinationInsertionPoints(child);
        }
      }
      var root = this.shadyRoot;
      var p$ = root._insertionPoints;
      for (var j = 0; j < p$.length; j++) {
        p$[j]._distributedNodes = [];
      }
    },
    _collectPool: function () {
      var pool = [];
      var children = getLightChildren(this);
      for (var i = 0; i < children.length; i++) {
        var child = children[i];
        if (isInsertionPoint(child)) {
          pool.push.apply(pool, child._distributedNodes);
        } else {
          pool.push(child);
        }
      }
      return pool;
    },
    _distributePool: function (node, pool) {
      var p$ = node._insertionPoints;
      for (var i = 0, l = p$.length, p; i < l && (p = p$[i]); i++) {
        this._distributeInsertionPoint(p, pool);
        maybeRedistributeParent(p, this);
      }
    },
    _distributeInsertionPoint: function (content, pool) {
      var anyDistributed = false;
      for (var i = 0, l = pool.length, node; i < l; i++) {
        node = pool[i];
        if (!node) {
          continue;
        }
        if (this._matchesContentSelect(node, content)) {
          distributeNodeInto(node, content);
          pool[i] = undefined;
          anyDistributed = true;
        }
      }
      if (!anyDistributed) {
        var children = getLightChildren(content);
        for (var j = 0; j < children.length; j++) {
          distributeNodeInto(children[j], content);
        }
      }
    },
    _composeTree: function () {
      this._updateChildNodes(this, this._composeNode(this));
      var p$ = this.shadyRoot._insertionPoints;
      for (var i = 0, l = p$.length, p, parent; i < l && (p = p$[i]); i++) {
        parent = p._lightParent || p.parentNode;
        if (!parent._useContent && parent !== this && parent !== this.shadyRoot) {
          this._updateChildNodes(parent, this._composeNode(parent));
        }
      }
    },
    _composeNode: function (node) {
      var children = [];
      var c$ = getLightChildren(node.shadyRoot || node);
      for (var i = 0; i < c$.length; i++) {
        var child = c$[i];
        if (isInsertionPoint(child)) {
          var distributedNodes = child._distributedNodes;
          for (var j = 0; j < distributedNodes.length; j++) {
            var distributedNode = distributedNodes[j];
            if (isFinalDestination(child, distributedNode)) {
              children.push(distributedNode);
            }
          }
        } else {
          children.push(child);
        }
      }
      return children;
    },
    _updateChildNodes: function (container, children) {
      var composed = getComposedChildren(container);
      var splices = Polymer.ArraySplice.calculateSplices(children, composed);
      for (var i = 0, d = 0, s; i < splices.length && (s = splices[i]); i++) {
        for (var j = 0, n; j < s.removed.length && (n = s.removed[j]); j++) {
          remove(n);
          composed.splice(s.index + d, 1);
        }
        d -= s.addedCount;
      }
      for (var i = 0, s, next; i < splices.length && (s = splices[i]); i++) {
        next = composed[s.index];
        for (var j = s.index, n; j < s.index + s.addedCount; j++) {
          n = children[j];
          insertBefore(container, n, next);
          composed.splice(j, 0, n);
        }
      }
    },
    _matchesContentSelect: function (node, contentElement) {
      var select = contentElement.getAttribute("select");
      if (!select) {
        return true;
      }
      select = select.trim();
      if (!select) {
        return true;
      }
      if (!(node instanceof Element)) {
        return false;
      }
      var validSelectors = /^(:not\()?[*.#[a-zA-Z_|]/;
      if (!validSelectors.test(select)) {
        return false;
      }
      return this.elementMatches(select, node);
    },
    _elementAdd: function () {
    },
    _elementRemove: function () {
    }
  });
  var saveLightChildrenIfNeeded = Polymer.DomApi.saveLightChildrenIfNeeded;
  var getLightChildren = Polymer.DomApi.getLightChildren;
  var matchesSelector = Polymer.DomApi.matchesSelector;
  var hasInsertionPoint = Polymer.DomApi.hasInsertionPoint;
  var getComposedChildren = Polymer.DomApi.getComposedChildren;
  var removeFromComposedParent = Polymer.DomApi.removeFromComposedParent;

  function distributeNodeInto(child, insertionPoint) {
    insertionPoint._distributedNodes.push(child);
    var points = child._destinationInsertionPoints;
    if (!points) {
      child._destinationInsertionPoints = [insertionPoint];
    } else {
      points.push(insertionPoint);
    }
  }

  function clearDistributedDestinationInsertionPoints(content) {
    var e$ = content._distributedNodes;
    for (var i = 0; i < e$.length; i++) {
      var d = e$[i]._destinationInsertionPoints;
      if (d) {
        d.splice(d.indexOf(content) + 1, d.length);
      }
    }
  }

  function maybeRedistributeParent(content, host) {
    var parent = content._lightParent;
    if (parent && parent.shadyRoot && hasInsertionPoint(parent.shadyRoot) && parent.shadyRoot._distributionClean) {
      parent.shadyRoot._distributionClean = false;
      host.shadyRoot._dirtyRoots.push(parent);
    }
  }

  function isFinalDestination(insertionPoint, node) {
    var points = node._destinationInsertionPoints;
    return points && points[points.length - 1] === insertionPoint;
  }

  function isInsertionPoint(node) {
    return node.localName == "content";
  }

  var nativeInsertBefore = Element.prototype.insertBefore;
  var nativeRemoveChild = Element.prototype.removeChild;

  function insertBefore(parentNode, newChild, refChild) {
    var newChildParent = getComposedParent(newChild);
    if (newChildParent !== parentNode) {
      removeFromComposedParent(newChildParent, newChild);
    }
    remove(newChild);
    saveLightChildrenIfNeeded(parentNode);
    nativeInsertBefore.call(parentNode, newChild, refChild || null);
    newChild._composedParent = parentNode;
  }

  function remove(node) {
    var parentNode = getComposedParent(node);
    if (parentNode) {
      saveLightChildrenIfNeeded(parentNode);
      node._composedParent = null;
      nativeRemoveChild.call(parentNode, node);
    }
  }

  function getComposedParent(node) {
    return node.__patched ? node._composedParent : node.parentNode;
  }

  function getTopDistributingHost(host) {
    while (host && hostNeedsRedistribution(host)) {
      host = host.domHost;
    }
    return host;
  }

  function hostNeedsRedistribution(host) {
    var c$ = Polymer.dom(host).children;
    for (var i = 0, c; i < c$.length; i++) {
      c = c$[i];
      if (c.localName === "content") {
        return host.domHost;
      }
    }
  }

  var needsUpgrade = window.CustomElements && !CustomElements.useNative;

  function upgradeLightChildren(children) {
    if (needsUpgrade && children) {
      for (var i = 0; i < children.length; i++) {
        CustomElements.upgrade(children[i]);
      }
    }
  }
})();

if (Polymer.Settings.useShadow) {
  Polymer.Base._addFeature({
    _poolContent: function () {
    },
    _beginDistribute: function () {
    },
    distributeContent: function () {
    },
    _distributeContent: function () {
    },
    _finishDistribute: function () {
    },
    _createLocalRoot: function () {
      this.createShadowRoot();
      this.shadowRoot.appendChild(this.root);
      this.root = this.shadowRoot;
    }
  });
}

Polymer.DomModule = document.createElement("dom-module");

Polymer.Base._addFeature({
  _registerFeatures: function () {
    this._prepIs();
    this._prepAttributes();
    this._prepBehaviors();
    this._prepExtends();
    this._prepConstructor();
    this._prepTemplate();
    this._prepShady();
  },
  _prepBehavior: function (b) {
    this._addHostAttributes(b.hostAttributes);
  },
  _initFeatures: function () {
    this._poolContent();
    this._pushHost();
    this._stampTemplate();
    this._popHost();
    this._marshalHostAttributes();
    this._setupDebouncers();
    this._marshalBehaviors();
    this._tryReady();
  },
  _marshalBehavior: function (b) {
  }
});


Polymer.nar = [];

Polymer.Annotations = {
  parseAnnotations: function (template) {
    var list = [];
    var content = template._content || template.content;
    this._parseNodeAnnotations(content, list);
    return list;
  },
  _parseNodeAnnotations: function (node, list) {
    return node.nodeType === Node.TEXT_NODE ? this._parseTextNodeAnnotation(node, list) : this._parseElementAnnotations(node, list);
  },
  _testEscape: function (value) {
    var escape = value.slice(0, 2);
    if (escape === "{{" || escape === "[[") {
      return escape;
    }
  },
  _parseTextNodeAnnotation: function (node, list) {
    var v = node.textContent;
    var escape = this._testEscape(v);
    if (escape) {
      node.textContent = " ";
      var annote = {
        bindings: [{
          kind: "text",
          mode: escape[0],
          value: v.slice(2, -2).trim()
        }]
      };
      list.push(annote);
      return annote;
    }
  },
  _parseElementAnnotations: function (element, list) {
    var annote = {
      bindings: [],
      events: []
    };
    this._parseChildNodesAnnotations(element, annote, list);
    if (element.attributes) {
      this._parseNodeAttributeAnnotations(element, annote, list);
      if (this.prepElement) {
        this.prepElement(element);
      }
    }
    if (annote.bindings.length || annote.events.length || annote.id) {
      list.push(annote);
    }
    return annote;
  },
  _parseChildNodesAnnotations: function (root, annote, list, callback) {
    if (root.firstChild) {
      for (var i = 0, node = root.firstChild; node; node = node.nextSibling, i++) {
        if (node.localName === "template" && !node.hasAttribute("preserve-content")) {
          this._parseTemplate(node, i, list, annote);
        }
        var childAnnotation = this._parseNodeAnnotations(node, list, callback);
        if (childAnnotation) {
          childAnnotation.parent = annote;
          childAnnotation.index = i;
        }
      }
    }
  },
  _parseTemplate: function (node, index, list, parent) {
    var content = document.createDocumentFragment();
    content._notes = this.parseAnnotations(node);
    content.appendChild(node.content);
    list.push({
      bindings: Polymer.nar,
      events: Polymer.nar,
      templateContent: content,
      parent: parent,
      index: index
    });
  },
  _parseNodeAttributeAnnotations: function (node, annotation) {
    for (var i = node.attributes.length - 1, a; a = node.attributes[i]; i--) {
      var n = a.name, v = a.value;
      if (n === "id" && !this._testEscape(v)) {
        annotation.id = v;
      } else if (n.slice(0, 3) === "on-") {
        node.removeAttribute(n);
        annotation.events.push({
          name: n.slice(3),
          value: v
        });
      } else {
        var b = this._parseNodeAttributeAnnotation(node, n, v);
        if (b) {
          annotation.bindings.push(b);
        }
      }
    }
  },
  _parseNodeAttributeAnnotation: function (node, n, v) {
    var escape = this._testEscape(v);
    if (escape) {
      var customEvent;
      var name = n;
      var mode = escape[0];
      v = v.slice(2, -2).trim();
      var not = false;
      if (v[0] == "!") {
        v = v.substring(1);
        not = true;
      }
      var kind = "property";
      if (n[n.length - 1] == "$") {
        name = n.slice(0, -1);
        kind = "attribute";
      }
      var notifyEvent, colon;
      if (mode == "{" && (colon = v.indexOf("::")) > 0) {
        notifyEvent = v.substring(colon + 2);
        v = v.substring(0, colon);
        customEvent = true;
      }
      if (node.localName == "input" && n == "value") {
        node.setAttribute(n, "");
      }
      node.removeAttribute(n);
      if (kind === "property") {
        name = Polymer.CaseMap.dashToCamelCase(name);
      }
      return {
        kind: kind,
        mode: mode,
        name: name,
        value: v,
        negate: not,
        event: notifyEvent,
        customEvent: customEvent
      };
    }
  },
  _localSubTree: function (node, host) {
    return node === host ? node.childNodes : node._lightChildren || node.childNodes;
  },
  findAnnotatedNode: function (root, annote) {
    var parent = annote.parent && Polymer.Annotations.findAnnotatedNode(root, annote.parent);
    return !parent ? root : Polymer.Annotations._localSubTree(parent, root)[annote.index];
  }
};

(function () {
  function resolveCss(cssText, ownerDocument) {
    return cssText.replace(CSS_URL_RX, function (m, pre, url, post) {
      return pre + "'" + resolve(url.replace(/["']/g, ""), ownerDocument) + "'" + post;
    });
  }

  function resolveAttrs(element, ownerDocument) {
    for (var name in URL_ATTRS) {
      var a$ = URL_ATTRS[name];
      for (var i = 0, l = a$.length, a, at, v; i < l && (a = a$[i]); i++) {
        if (name === "*" || element.localName === name) {
          at = element.attributes[a];
          v = at && at.value;
          if (v && v.search(BINDING_RX) < 0) {
            at.value = a === "style" ? resolveCss(v, ownerDocument) : resolve(v, ownerDocument);
          }
        }
      }
    }
  }

  function resolve(url, ownerDocument) {
    var resolver = getUrlResolver(ownerDocument);
    resolver.href = url;
    return resolver.href || url;
  }

  var tempDoc;
  var tempDocBase;

  function resolveUrl(url, baseUri) {
    if (!tempDoc) {
      tempDoc = document.implementation.createHTMLDocument("temp");
      tempDocBase = tempDoc.createElement("base");
      tempDoc.head.appendChild(tempDocBase);
    }
    tempDocBase.href = baseUri;
    return resolve(url, tempDoc);
  }

  function getUrlResolver(ownerDocument) {
    return ownerDocument.__urlResolver || (ownerDocument.__urlResolver = ownerDocument.createElement("a"));
  }

  var CSS_URL_RX = /(url\()([^)]*)(\))/g;
  var URL_ATTRS = {
    "*": ["href", "src", "style", "url"],
    form: ["action"]
  };
  var BINDING_RX = /\{\{|\[\[/;
  Polymer.ResolveUrl = {
    resolveCss: resolveCss,
    resolveAttrs: resolveAttrs,
    resolveUrl: resolveUrl
  };
})();

Polymer.Base._addFeature({
  _prepAnnotations: function () {
    if (!this._template) {
      this._notes = [];
    } else {
      Polymer.Annotations.prepElement = this._prepElement.bind(this);
      this._notes = Polymer.Annotations.parseAnnotations(this._template);
      this._processAnnotations(this._notes);
      Polymer.Annotations.prepElement = null;
    }
  },
  _processAnnotations: function (notes) {
    for (var i = 0; i < notes.length; i++) {
      var note = notes[i];
      for (var j = 0; j < note.bindings.length; j++) {
        var b = note.bindings[j];
        b.signature = this._parseMethod(b.value);
        if (!b.signature) {
          b.model = this._modelForPath(b.value);
        }
      }
      if (note.templateContent) {
        this._processAnnotations(note.templateContent._notes);
        var pp = note.templateContent._parentProps = this._discoverTemplateParentProps(note.templateContent._notes);
        var bindings = [];
        for (var prop in pp) {
          bindings.push({
            index: note.index,
            kind: "property",
            mode: "{",
            name: "_parent_" + prop,
            model: prop,
            value: prop
          });
        }
        note.bindings = note.bindings.concat(bindings);
      }
    }
  },
  _discoverTemplateParentProps: function (notes) {
    var pp = {};
    notes.forEach(function (n) {
      n.bindings.forEach(function (b) {
        if (b.signature) {
          var args = b.signature.args;
          for (var k = 0; k < args.length; k++) {
            pp[args[k].model] = true;
          }
        } else {
          pp[b.model] = true;
        }
      });
      if (n.templateContent) {
        var tpp = n.templateContent._parentProps;
        Polymer.Base.mixin(pp, tpp);
      }
    });
    return pp;
  },
  _prepElement: function (element) {
    Polymer.ResolveUrl.resolveAttrs(element, this._template.ownerDocument);
  },
  _findAnnotatedNode: Polymer.Annotations.findAnnotatedNode,
  _marshalAnnotationReferences: function () {
    if (this._template) {
      this._marshalIdNodes();
      this._marshalAnnotatedNodes();
      this._marshalAnnotatedListeners();
    }
  },
  _configureAnnotationReferences: function () {
    this._configureTemplateContent();
  },
  _configureTemplateContent: function () {
    this._notes.forEach(function (note, i) {
      if (note.templateContent) {
        this._nodes[i]._content = note.templateContent;
      }
    }, this);
  },
  _marshalIdNodes: function () {
    this.$ = {};
    this._notes.forEach(function (a) {
      if (a.id) {
        this.$[a.id] = this._findAnnotatedNode(this.root, a);
      }
    }, this);
  },
  _marshalAnnotatedNodes: function () {
    if (this._nodes) {
      this._nodes = this._nodes.map(function (a) {
        return this._findAnnotatedNode(this.root, a);
      }, this);
    }
  },
  _marshalAnnotatedListeners: function () {
    this._notes.forEach(function (a) {
      if (a.events && a.events.length) {
        var node = this._findAnnotatedNode(this.root, a);
        a.events.forEach(function (e) {
          this.listen(node, e.name, e.value);
        }, this);
      }
    }, this);
  }
});

Polymer.Base._addFeature({
  listeners: {},
  _listenListeners: function (listeners) {
    var node, name, key;
    for (key in listeners) {
      if (key.indexOf(".") < 0) {
        node = this;
        name = key;
      } else {
        name = key.split(".");
        node = this.$[name[0]];
        name = name[1];
      }
      this.listen(node, name, listeners[key]);
    }
  },
  listen: function (node, eventName, methodName) {
    this._listen(node, eventName, this._createEventHandler(node, eventName, methodName));
  },
  _createEventHandler: function (node, eventName, methodName) {
    var host = this;
    return function (e) {
      if (host[methodName]) {
        host[methodName](e, e.detail);
      } else {
        host._warn(host._logf("_createEventHandler", "listener method `" + methodName + "` not defined"));
      }
    };
  },
  _listen: function (node, eventName, handler) {
    node.addEventListener(eventName, handler);
  }
});

(function () {
  "use strict";
  var HAS_NATIVE_TA = typeof document.head.style.touchAction === "string";
  var GESTURE_KEY = "__polymerGestures";
  var HANDLED_OBJ = "__polymerGesturesHandled";
  var TOUCH_ACTION = "__polymerGesturesTouchAction";
  var TAP_DISTANCE = 25;
  var TRACK_DISTANCE = 5;
  var TRACK_LENGTH = 2;
  var MOUSE_TIMEOUT = 500;
  var MOUSE_EVENTS = ["mousedown", "mousemove", "mouseup", "click"];
  var mouseCanceller = function (mouseEvent) {
    mouseEvent[HANDLED_OBJ] = {
      skip: true
    };
    if (mouseEvent.type === "click") {
      var path = Polymer.dom(mouseEvent).path;
      for (var i = 0; i < path.length; i++) {
        if (path[i] === POINTERSTATE.mouse.target) {
          return;
        }
      }
      mouseEvent.preventDefault();
      mouseEvent.stopPropagation();
    }
  };

  function ignoreMouse(set) {
    if (set && POINTERSTATE.touch.mouseIgnoreId !== -1) {
      return;
    }
    for (var i = 0, en; i < MOUSE_EVENTS.length; i++) {
      en = MOUSE_EVENTS[i];
      if (set) {
        document.addEventListener(en, mouseCanceller, true);
      } else {
        document.removeEventListener(en, mouseCanceller, true);
      }
    }
    if (set) {
      POINTERSTATE.mouse.mouseIgnoreId = setTimeout(ignoreMouse, MOUSE_TIMEOUT);
    } else {
      POINTERSTATE.mouse.target = null;
      POINTERSTATE.touch.mouseIgnoreId = -1;
    }
  }

  var POINTERSTATE = {
    tapPrevented: false,
    mouse: {
      target: null
    },
    touch: {
      x: 0,
      y: 0,
      id: -1,
      scrollDecided: false,
      mouseIgnoreId: -1
    }
  };

  function firstTouchAction(ev) {
    var path = Polymer.dom(ev).path;
    var ta = "auto";
    for (var i = 0, n; i < path.length; i++) {
      n = path[i];
      if (n[TOUCH_ACTION]) {
        ta = n[TOUCH_ACTION];
        break;
      }
    }
    return ta;
  }

  var Gestures = {
    gestures: {},
    recognizers: [],
    deepTargetFind: function (x, y) {
      var node = document.elementFromPoint(x, y);
      var next = node;
      while (next && next.shadowRoot) {
        next = next.shadowRoot.elementFromPoint(x, y);
        if (next) {
          node = next;
        }
      }
      return node;
    },
    handleNative: function (ev) {
      var handled;
      var type = ev.type;
      var node = ev.currentTarget;
      var gobj = node[GESTURE_KEY];
      var gs = gobj[type];
      if (!gs) {
        return;
      }
      if (!ev[HANDLED_OBJ]) {
        ev[HANDLED_OBJ] = {};
        if (type.slice(0, 5) === "touch") {
          var t = ev.changedTouches[0];
          if (type === "touchstart") {
            if (ev.touches.length === 1) {
              POINTERSTATE.touch.id = t.identifier;
            }
          }
          if (POINTERSTATE.touch.id !== t.identifier) {
            return;
          }
          if (!HAS_NATIVE_TA) {
            if (type === "touchstart" || type === "touchmove") {
              Gestures.handleTouchAction(ev);
            }
          }
          if (type === "touchend") {
            POINTERSTATE.mouse.target = Polymer.dom(ev).rootTarget;
            ignoreMouse(true);
          }
        }
      }
      handled = ev[HANDLED_OBJ];
      if (handled.skip) {
        return;
      }
      var recognizers = Gestures.recognizers;
      for (var i = 0, r; i < recognizers.length; i++) {
        r = recognizers[i];
        if (gs[r.name] && !handled[r.name]) {
          handled[r.name] = true;
          r[type](ev);
        }
      }
    },
    handleTouchAction: function (ev) {
      var t = ev.changedTouches[0];
      var type = ev.type;
      if (type === "touchstart") {
        POINTERSTATE.touch.x = t.clientX;
        POINTERSTATE.touch.y = t.clientY;
        POINTERSTATE.touch.scrollDecided = false;
      } else if (type === "touchmove") {
        if (POINTERSTATE.touch.scrollDecided) {
          return;
        }
        POINTERSTATE.touch.scrollDecided = true;
        var ta = firstTouchAction(ev);
        var prevent = false;
        var dx = Math.abs(POINTERSTATE.touch.x - t.clientX);
        var dy = Math.abs(POINTERSTATE.touch.y - t.clientY);
        if (!ev.cancelable) {
        } else if (ta === "none") {
          prevent = true;
        } else if (ta === "pan-x") {
          prevent = dy > dx;
        } else if (ta === "pan-y") {
          prevent = dx > dy;
        }
        if (prevent) {
          ev.preventDefault();
        }
      }
    },
    add: function (node, evType, handler) {
      var recognizer = this.gestures[evType];
      var deps = recognizer.deps;
      var name = recognizer.name;
      var gobj = node[GESTURE_KEY];
      if (!gobj) {
        node[GESTURE_KEY] = gobj = {};
      }
      for (var i = 0, dep, gd; i < deps.length; i++) {
        dep = deps[i];
        gd = gobj[dep];
        if (!gd) {
          gobj[dep] = gd = {};
          node.addEventListener(dep, this.handleNative);
        }
        gd[name] = (gd[name] || 0) + 1;
      }
      node.addEventListener(evType, handler);
      if (recognizer.touchAction) {
        this.setTouchAction(node, recognizer.touchAction);
      }
    },
    register: function (recog) {
      this.recognizers.push(recog);
      for (var i = 0; i < recog.emits.length; i++) {
        this.gestures[recog.emits[i]] = recog;
      }
    },
    setTouchAction: function (node, value) {
      if (HAS_NATIVE_TA) {
        node.style.touchAction = value;
      }
      node[TOUCH_ACTION] = value;
    },
    fire: function (target, type, detail) {
      var ev = new CustomEvent(type, {
        detail: detail,
        bubbles: true,
        cancelable: true
      });
      target.dispatchEvent(ev);
    }
  };
  Gestures.register({
    name: "downup",
    deps: ["mousedown", "touchstart", "touchend"],
    emits: ["down", "up"],
    mousedown: function (e) {
      var t = e.currentTarget;
      var self = this;
      var upfn = function upfn(e) {
        self.fire("up", t, e);
        document.removeEventListener("mouseup", upfn);
      };
      document.addEventListener("mouseup", upfn);
      this.fire("down", t, e);
    },
    touchstart: function (e) {
      this.fire("down", e.currentTarget, e.changedTouches[0]);
    },
    touchend: function (e) {
      this.fire("up", e.currentTarget, e.changedTouches[0]);
    },
    fire: function (type, target, event) {
      Gestures.fire(target, type, {
        x: event.clientX,
        y: event.clientY,
        sourceEvent: event
      });
    }
  });
  Gestures.register({
    name: "track",
    touchAction: "none",
    deps: ["mousedown", "touchstart", "touchmove", "touchend"],
    emits: ["track"],
    info: {
      x: 0,
      y: 0,
      state: "start",
      started: false,
      moves: [],
      addMove: function (move) {
        if (this.moves.length > TRACK_LENGTH) {
          this.moves.shift();
        }
        this.moves.push(move);
      }
    },
    clearInfo: function () {
      this.info.state = "start";
      this.info.started = false;
      this.info.moves = [];
      this.info.x = 0;
      this.info.y = 0;
    },
    hasMovedEnough: function (x, y) {
      if (this.info.started) {
        return true;
      }
      var dx = Math.abs(this.info.x - x);
      var dy = Math.abs(this.info.y - y);
      return dx >= TRACK_DISTANCE || dy >= TRACK_DISTANCE;
    },
    mousedown: function (e) {
      var t = e.currentTarget;
      var self = this;
      var movefn = function movefn(e) {
        var x = e.clientX, y = e.clientY;
        if (self.hasMovedEnough(x, y)) {
          self.info.state = self.info.started ? e.type === "mouseup" ? "end" : "track" : "start";
          self.info.addMove({
            x: x,
            y: y
          });
          self.fire(t, e);
          e.preventDefault();
          self.info.started = true;
        }
      };
      var upfn = function upfn(e) {
        if (self.info.started) {
          POINTERSTATE.tapPrevented = true;
          movefn(e);
        }
        self.clearInfo();
        document.removeEventListener("mousemove", movefn);
        document.removeEventListener("mouseup", upfn);
      };
      document.addEventListener("mousemove", movefn);
      document.addEventListener("mouseup", upfn);
      this.info.x = e.clientX;
      this.info.y = e.clientY;
    },
    touchstart: function (e) {
      var ct = e.changedTouches[0];
      this.info.x = ct.clientX;
      this.info.y = ct.clientY;
    },
    touchmove: function (e) {
      var t = e.currentTarget;
      var ct = e.changedTouches[0];
      var x = ct.clientX, y = ct.clientY;
      if (this.hasMovedEnough(x, y)) {
        this.info.addMove({
          x: x,
          y: y
        });
        this.fire(t, ct);
        this.info.state = "track";
        this.info.started = true;
      }
    },
    touchend: function (e) {
      var t = e.currentTarget;
      var ct = e.changedTouches[0];
      if (this.info.started) {
        POINTERSTATE.tapPrevented = true;
        this.info.state = "end";
        this.info.addMove({
          x: ct.clientX,
          y: ct.clientY
        });
        this.fire(t, ct);
      }
      this.clearInfo();
    },
    fire: function (target, touch) {
      var secondlast = this.info.moves[this.info.moves.length - 2];
      var lastmove = this.info.moves[this.info.moves.length - 1];
      var dx = lastmove.x - this.info.x;
      var dy = lastmove.y - this.info.y;
      var ddx, ddy = 0;
      if (secondlast) {
        ddx = lastmove.x - secondlast.x;
        ddy = lastmove.y - secondlast.y;
      }
      return Gestures.fire(target, "track", {
        state: this.info.state,
        x: touch.clientX,
        y: touch.clientY,
        dx: dx,
        dy: dy,
        ddx: ddx,
        ddy: ddy,
        hover: function () {
          return Gestures.deepTargetFind(touch.clientX, touch.clientY);
        }
      });
    }
  });
  Gestures.register({
    name: "tap",
    deps: ["mousedown", "click", "touchstart", "touchend"],
    emits: ["tap"],
    start: {
      x: 0,
      y: 0
    },
    reset: function () {
      this.start.x = 0;
      this.start.y = 0;
    },
    save: function (e) {
      this.start.x = e.clientX;
      this.start.y = e.clientY;
    },
    mousedown: function (e) {
      POINTERSTATE.tapPrevented = false;
      this.save(e);
    },
    click: function (e) {
      this.forward(e);
    },
    touchstart: function (e) {
      POINTERSTATE.tapPrevented = false;
      this.save(e.changedTouches[0]);
    },
    touchend: function (e) {
      this.forward(e.changedTouches[0]);
    },
    forward: function (e) {
      var dx = Math.abs(e.clientX - this.start.x);
      var dy = Math.abs(e.clientY - this.start.y);
      if (isNaN(dx) || isNaN(dy) || dx <= TAP_DISTANCE || dy <= TAP_DISTANCE) {
        if (!POINTERSTATE.tapPrevented) {
          Gestures.fire(e.target, "tap", {
            x: e.clientX,
            y: e.clientY,
            sourceEvent: e
          });
        }
      }
      this.reset();
    }
  });
  var DIRECTION_MAP = {
    x: "pan-x",
    y: "pan-y",
    none: "none",
    all: "auto"
  };
  Polymer.Base._addFeature({
    _listen: function (node, eventName, handler) {
      if (Gestures.gestures[eventName]) {
        Gestures.add(node, eventName, handler);
      } else {
        node.addEventListener(eventName, handler);
      }
    },
    setScrollDirection: function (direction, node) {
      node = node || this;
      Gestures.setTouchAction(node, DIRECTION_MAP[direction] || "auto");
    }
  });
  Polymer.Gestures = Gestures;
})();

Polymer.Async = function () {
  var currVal = 0;
  var lastVal = 0;
  var callbacks = [];
  var twiddle = document.createTextNode("");

  function runAsync(callback, waitTime) {
    if (waitTime > 0) {
      return ~setTimeout(callback, waitTime);
    } else {
      twiddle.textContent = currVal++;
      callbacks.push(callback);
      return currVal - 1;
    }
  }

  function cancelAsync(handle) {
    if (handle < 0) {
      clearTimeout(~handle);
    } else {
      var idx = handle - lastVal;
      if (idx >= 0) {
        if (!callbacks[idx]) {
          throw "invalid async handle: " + handle;
        }
        callbacks[idx] = null;
      }
    }
  }

  function atEndOfMicrotask() {
    var len = callbacks.length;
    for (var i = 0; i < len; i++) {
      var cb = callbacks[i];
      if (cb) {
        cb();
      }
    }
    callbacks.splice(0, len);
    lastVal += len;
  }

  new (window.MutationObserver || JsMutationObserver)(atEndOfMicrotask).observe(twiddle, {
    characterData: true
  });
  return {
    run: runAsync,
    cancel: cancelAsync
  };
}();

Polymer.Debounce = function () {
  var Async = Polymer.Async;
  var Debouncer = function (context) {
    this.context = context;
    this.boundComplete = this.complete.bind(this);
  };
  Debouncer.prototype = {
    go: function (callback, wait) {
      var h;
      this.finish = function () {
        Async.cancel(h);
      };
      h = Async.run(this.boundComplete, wait);
      this.callback = callback;
    },
    stop: function () {
      if (this.finish) {
        this.finish();
        this.finish = null;
      }
    },
    complete: function () {
      if (this.finish) {
        this.stop();
        this.callback.call(this.context);
      }
    }
  };
  function debounce(debouncer, callback, wait) {
    if (debouncer) {
      debouncer.stop();
    } else {
      debouncer = new Debouncer(this);
    }
    debouncer.go(callback, wait);
    return debouncer;
  }

  return debounce;
}();

Polymer.Base._addFeature({
  $$: function (slctr) {
    return Polymer.dom(this.root).querySelector(slctr);
  },
  toggleClass: function (name, bool, node) {
    node = node || this;
    if (arguments.length == 1) {
      bool = !node.classList.contains(name);
    }
    if (bool) {
      Polymer.dom(node).classList.add(name);
    } else {
      Polymer.dom(node).classList.remove(name);
    }
  },
  toggleAttribute: function (name, bool, node) {
    node = node || this;
    if (arguments.length == 1) {
      bool = !node.hasAttribute(name);
    }
    if (bool) {
      Polymer.dom(node).setAttribute(name, "");
    } else {
      Polymer.dom(node).removeAttribute(name);
    }
  },
  classFollows: function (name, toElement, fromElement) {
    if (fromElement) {
      Polymer.dom(fromElement).classList.remove(name);
    }
    if (toElement) {
      Polymer.dom(toElement).classList.add(name);
    }
  },
  attributeFollows: function (name, toElement, fromElement) {
    if (fromElement) {
      Polymer.dom(fromElement).removeAttribute(name);
    }
    if (toElement) {
      Polymer.dom(toElement).setAttribute(name, "");
    }
  },
  getContentChildNodes: function (slctr) {
    return Polymer.dom(Polymer.dom(this.root).querySelector(slctr || "content")).getDistributedNodes();
  },
  getContentChildren: function (slctr) {
    return this.getContentChildNodes(slctr).filter(function (n) {
      return n.nodeType === Node.ELEMENT_NODE;
    });
  },
  fire: function (type, detail, options) {
    options = options || Polymer.nob;
    var node = options.node || this;
    var detail = detail === null || detail === undefined ? Polymer.nob : detail;
    var bubbles = options.bubbles === undefined ? true : options.bubbles;
    var event = new CustomEvent(type, {
      bubbles: Boolean(bubbles),
      cancelable: Boolean(options.cancelable),
      detail: detail
    });
    node.dispatchEvent(event);
    return event;
  },
  async: function (callback, waitTime) {
    return Polymer.Async.run(callback.bind(this), waitTime);
  },
  cancelAsync: function (handle) {
    Polymer.Async.cancel(handle);
  },
  arrayDelete: function (path, item) {
    var index;
    if (Array.isArray(path)) {
      index = path.indexOf(item);
      if (index >= 0) {
        return path.splice(index, 1);
      }
    } else {
      var arr = this.get(path);
      index = arr.indexOf(item);
      if (index >= 0) {
        return this.splice(path, index, 1);
      }
    }
  },
  transform: function (transform, node) {
    node = node || this;
    node.style.webkitTransform = transform;
    node.style.transform = transform;
  },
  translate3d: function (x, y, z, node) {
    node = node || this;
    this.transform("translate3d(" + x + "," + y + "," + z + ")", node);
  },
  importHref: function (href, onload, onerror) {
    var l = document.createElement("link");
    l.rel = "import";
    l.href = href;
    if (onload) {
      l.onload = onload.bind(this);
    }
    if (onerror) {
      l.onerror = onerror.bind(this);
    }
    document.head.appendChild(l);
    return l;
  },
  create: function (tag, props) {
    var elt = document.createElement(tag);
    if (props) {
      for (var n in props) {
        elt[n] = props[n];
      }
    }
    return elt;
  },
  mixin: function (target, source) {
    for (var i in source) {
      target[i] = source[i];
    }
  }
});

Polymer.Bind = {
  prepareModel: function (model) {
    model._propertyEffects = {};
    model._bindListeners = [];
    var api = this._modelApi;
    for (var n in api) {
      model[n] = api[n];
    }
  },
  _modelApi: {
    _notifyChange: function (property) {
      var eventName = Polymer.CaseMap.camelToDashCase(property) + "-changed";
      this.fire(eventName, {
        value: this[property]
      }, {
        bubbles: false
      });
    },
    _propertySet: function (property, value, effects) {
      var old = this.__data__[property];
      if (old !== value) {
        this.__data__[property] = value;
        if (typeof value == "object") {
          this._clearPath(property);
        }
        if (this._propertyChanged) {
          this._propertyChanged(property, value, old);
        }
        if (effects) {
          this._effectEffects(property, value, effects, old);
        }
      }
      return old;
    },
    _effectEffects: function (property, value, effects, old) {
      effects.forEach(function (fx) {
        var fn = Polymer.Bind["_" + fx.kind + "Effect"];
        if (fn) {
          fn.call(this, property, value, fx.effect, old);
        }
      }, this);
    },
    _clearPath: function (path) {
      for (var prop in this.__data__) {
        if (prop.indexOf(path + ".") === 0) {
          this.__data__[prop] = undefined;
        }
      }
    }
  },
  ensurePropertyEffects: function (model, property) {
    var fx = model._propertyEffects[property];
    if (!fx) {
      fx = model._propertyEffects[property] = [];
    }
    return fx;
  },
  addPropertyEffect: function (model, property, kind, effect) {
    var fx = this.ensurePropertyEffects(model, property);
    fx.push({
      kind: kind,
      effect: effect
    });
  },
  createBindings: function (model) {
    var fx$ = model._propertyEffects;
    if (fx$) {
      for (var n in fx$) {
        var fx = fx$[n];
        fx.sort(this._sortPropertyEffects);
        this._createAccessors(model, n, fx);
      }
    }
  },
  _sortPropertyEffects: function () {
    var EFFECT_ORDER = {
      compute: 0,
      annotation: 1,
      computedAnnotation: 2,
      reflect: 3,
      notify: 4,
      observer: 5,
      complexObserver: 6,
      "function": 7
    };
    return function (a, b) {
      return EFFECT_ORDER[a.kind] - EFFECT_ORDER[b.kind];
    };
  }(),
  _createAccessors: function (model, property, effects) {
    var defun = {
      get: function () {
        return this.__data__[property];
      }
    };
    var setter = function (value) {
      this._propertySet(property, value, effects);
    };
    if (model.getPropertyInfo && model.getPropertyInfo(property).readOnly) {
      model["_set" + this.upper(property)] = setter;
    } else {
      defun.set = setter;
    }
    Object.defineProperty(model, property, defun);
  },
  upper: function (name) {
    return name[0].toUpperCase() + name.substring(1);
  },
  _addAnnotatedListener: function (model, index, property, path, event) {
    var fn = this._notedListenerFactory(property, path, this._isStructured(path), this._isEventBogus);
    var eventName = event || Polymer.CaseMap.camelToDashCase(property) + "-changed";
    model._bindListeners.push({
      index: index,
      property: property,
      path: path,
      changedFn: fn,
      event: eventName
    });
  },
  _isStructured: function (path) {
    return path.indexOf(".") > 0;
  },
  _isEventBogus: function (e, target) {
    return e.path && e.path[0] !== target;
  },
  _notedListenerFactory: function (property, path, isStructured, bogusTest) {
    return function (e, target) {
      if (!bogusTest(e, target)) {
        if (e.detail && e.detail.path) {
          this.notifyPath(this._fixPath(path, property, e.detail.path), e.detail.value);
        } else {
          var value = target[property];
          if (!isStructured) {
            this[path] = target[property];
          } else {
            if (this.__data__[path] != value) {
              this.set(path, value);
            }
          }
        }
      }
    };
  },
  prepareInstance: function (inst) {
    inst.__data__ = Object.create(null);
  },
  setupBindListeners: function (inst) {
    inst._bindListeners.forEach(function (info) {
      var node = inst._nodes[info.index];
      node.addEventListener(info.event, inst._notifyListener.bind(inst, info.changedFn));
    });
  }
};

Polymer.Base.extend(Polymer.Bind, {
  _shouldAddListener: function (effect) {
    return effect.name && effect.mode === "{" && !effect.negate && effect.kind != "attribute";
  },
  _annotationEffect: function (source, value, effect) {
    if (source != effect.value) {
      value = this.get(effect.value);
      this.__data__[effect.value] = value;
    }
    var calc = effect.negate ? !value : value;
    if (!effect.customEvent || this._nodes[effect.index][effect.name] !== calc) {
      return this._applyEffectValue(calc, effect);
    }
  },
  _reflectEffect: function (source) {
    this.reflectPropertyToAttribute(source);
  },
  _notifyEffect: function (source) {
    this._notifyChange(source);
  },
  _functionEffect: function (source, value, fn, old) {
    fn.call(this, source, value, old);
  },
  _observerEffect: function (source, value, effect, old) {
    var fn = this[effect.method];
    if (fn) {
      fn.call(this, value, old);
    } else {
      this._warn(this._logf("_observerEffect", "observer method `" + effect.method + "` not defined"));
    }
  },
  _complexObserverEffect: function (source, value, effect) {
    var fn = this[effect.method];
    if (fn) {
      var args = Polymer.Bind._marshalArgs(this.__data__, effect, source, value);
      if (args) {
        fn.apply(this, args);
      }
    } else {
      this._warn(this._logf("_complexObserverEffect", "observer method `" + effect.method + "` not defined"));
    }
  },
  _computeEffect: function (source, value, effect) {
    var args = Polymer.Bind._marshalArgs(this.__data__, effect, source, value);
    if (args) {
      var fn = this[effect.method];
      if (fn) {
        this[effect.property] = fn.apply(this, args);
      } else {
        this._warn(this._logf("_computeEffect", "compute method `" + effect.method + "` not defined"));
      }
    }
  },
  _annotatedComputationEffect: function (source, value, effect) {
    var computedHost = this._rootDataHost || this;
    var fn = computedHost[effect.method];
    if (fn) {
      var args = Polymer.Bind._marshalArgs(this.__data__, effect, source, value);
      if (args) {
        var computedvalue = fn.apply(computedHost, args);
        if (effect.negate) {
          computedvalue = !computedvalue;
        }
        this._applyEffectValue(computedvalue, effect);
      }
    } else {
      computedHost._warn(computedHost._logf("_annotatedComputationEffect", "compute method `" + effect.method + "` not defined"));
    }
  },
  _marshalArgs: function (model, effect, path, value) {
    var values = [];
    var args = effect.args;
    for (var i = 0, l = args.length; i < l; i++) {
      var arg = args[i];
      var name = arg.name;
      if (arg.literal) {
        v = arg.value;
      } else if (arg.structured) {
        v = Polymer.Base.get(name, model);
      } else {
        v = model[name];
      }
      if (args.length > 1 && v === undefined) {
        return;
      }
      if (arg.wildcard) {
        var baseChanged = name.indexOf(path + ".") === 0;
        var matches = effect.trigger.name.indexOf(name) === 0 && !baseChanged;
        values[i] = {
          path: matches ? path : name,
          value: matches ? value : v,
          base: v
        };
      } else {
        values[i] = v;
      }
    }
    return values;
  }
});

Polymer.Base._addFeature({
  _addPropertyEffect: function (property, kind, effect) {
    Polymer.Bind.addPropertyEffect(this, property, kind, effect);
  },
  _prepEffects: function () {
    Polymer.Bind.prepareModel(this);
    this._addAnnotationEffects(this._notes);
  },
  _prepBindings: function () {
    Polymer.Bind.createBindings(this);
  },
  _addPropertyEffects: function (properties) {
    if (properties) {
      for (var p in properties) {
        var prop = properties[p];
        if (prop.observer) {
          this._addObserverEffect(p, prop.observer);
        }
        if (prop.computed) {
          this._addComputedEffect(p, prop.computed);
        }
        if (prop.notify) {
          this._addPropertyEffect(p, "notify");
        }
        if (prop.reflectToAttribute) {
          this._addPropertyEffect(p, "reflect");
        }
        if (prop.readOnly) {
          Polymer.Bind.ensurePropertyEffects(this, p);
        }
      }
    }
  },
  _addComputedEffect: function (name, expression) {
    var sig = this._parseMethod(expression);
    sig.args.forEach(function (arg) {
      this._addPropertyEffect(arg.model, "compute", {
        method: sig.method,
        args: sig.args,
        trigger: arg,
        property: name
      });
    }, this);
  },
  _addObserverEffect: function (property, observer) {
    this._addPropertyEffect(property, "observer", {
      method: observer,
      property: property
    });
  },
  _addComplexObserverEffects: function (observers) {
    if (observers) {
      observers.forEach(function (observer) {
        this._addComplexObserverEffect(observer);
      }, this);
    }
  },
  _addComplexObserverEffect: function (observer) {
    var sig = this._parseMethod(observer);
    sig.args.forEach(function (arg) {
      this._addPropertyEffect(arg.model, "complexObserver", {
        method: sig.method,
        args: sig.args,
        trigger: arg
      });
    }, this);
  },
  _addAnnotationEffects: function (notes) {
    this._nodes = [];
    notes.forEach(function (note) {
      var index = this._nodes.push(note) - 1;
      note.bindings.forEach(function (binding) {
        this._addAnnotationEffect(binding, index);
      }, this);
    }, this);
  },
  _addAnnotationEffect: function (note, index) {
    if (Polymer.Bind._shouldAddListener(note)) {
      Polymer.Bind._addAnnotatedListener(this, index, note.name, note.value, note.event);
    }
    if (note.signature) {
      this._addAnnotatedComputationEffect(note, index);
    } else {
      note.index = index;
      this._addPropertyEffect(note.model, "annotation", note);
    }
  },
  _addAnnotatedComputationEffect: function (note, index) {
    var sig = note.signature;
    if (sig.static) {
      this.__addAnnotatedComputationEffect("__static__", index, note, sig, null);
    } else {
      sig.args.forEach(function (arg) {
        if (!arg.literal) {
          this.__addAnnotatedComputationEffect(arg.model, index, note, sig, arg);
        }
      }, this);
    }
  },
  __addAnnotatedComputationEffect: function (property, index, note, sig, trigger) {
    this._addPropertyEffect(property, "annotatedComputation", {
      index: index,
      kind: note.kind,
      property: note.name,
      negate: note.negate,
      method: sig.method,
      args: sig.args,
      trigger: trigger
    });
  },
  _parseMethod: function (expression) {
    var m = expression.match(/(\w*)\((.*)\)/);
    if (m) {
      var sig = {
        method: m[1],
        "static": true
      };
      if (m[2].trim()) {
        var args = m[2].replace(/\\,/g, "&comma;").split(",");
        return this._parseArgs(args, sig);
      } else {
        sig.args = Polymer.nar;
        return sig;
      }
    }
  },
  _parseArgs: function (argList, sig) {
    sig.args = argList.map(function (rawArg) {
      var arg = this._parseArg(rawArg);
      if (!arg.literal) {
        sig.static = false;
      }
      return arg;
    }, this);
    return sig;
  },
  _parseArg: function (rawArg) {
    var arg = rawArg.trim().replace(/&comma;/g, ",").replace(/\\(.)/g, "$1");
    var a = {
      name: arg,
      model: this._modelForPath(arg)
    };
    var fc = arg[0];
    if (fc >= "0" && fc <= "9") {
      fc = "#";
    }
    switch (fc) {
      case "'":
      case '"':
        a.value = arg.slice(1, -1);
        a.literal = true;
        break;

      case "#":
        a.value = Number(arg);
        a.literal = true;
        break;
    }
    if (!a.literal) {
      a.structured = arg.indexOf(".") > 0;
      if (a.structured) {
        a.wildcard = arg.slice(-2) == ".*";
        if (a.wildcard) {
          a.name = arg.slice(0, -2);
        }
      }
    }
    return a;
  },
  _marshalInstanceEffects: function () {
    Polymer.Bind.prepareInstance(this);
    Polymer.Bind.setupBindListeners(this);
  },
  _applyEffectValue: function (value, info) {
    var node = this._nodes[info.index];
    var property = info.property || info.name || "textContent";
    if (info.kind == "attribute") {
      this.serializeValueToAttribute(value, property, node);
    } else {
      if (property === "className") {
        value = this._scopeElementClass(node, value);
      }
      if (property === "textContent" || node.localName == "input" && property == "value") {
        value = value == undefined ? "" : value;
      }
      return node[property] = value;
    }
  },
  _executeStaticEffects: function () {
    if (this._propertyEffects.__static__) {
      this._effectEffects("__static__", null, this._propertyEffects.__static__);
    }
  }
});

Polymer.Base._addFeature({
  _setupConfigure: function (initialConfig) {
    this._config = initialConfig || {};
    this._handlers = [];
  },
  _marshalAttributes: function () {
    this._takeAttributesToModel(this._config);
  },
  _configValue: function (name, value) {
    this._config[name] = value;
  },
  _beforeClientsReady: function () {
    this._configure();
  },
  _configure: function () {
    this._configureAnnotationReferences();
    var config = {};
    this.behaviors.forEach(function (b) {
      this._configureProperties(b.properties, config);
    }, this);
    this._configureProperties(this.properties, config);
    this._mixinConfigure(config, this._config);
    this._config = config;
    this._distributeConfig(this._config);
  },
  _configureProperties: function (properties, config) {
    for (var i in properties) {
      var c = properties[i];
      if (c.value !== undefined) {
        var value = c.value;
        if (typeof value == "function") {
          value = value.call(this, this._config);
        }
        config[i] = value;
      }
    }
  },
  _mixinConfigure: function (a, b) {
    for (var prop in b) {
      if (!this.getPropertyInfo(prop).readOnly) {
        a[prop] = b[prop];
      }
    }
  },
  _distributeConfig: function (config) {
    var fx$ = this._propertyEffects;
    if (fx$) {
      for (var p in config) {
        var fx = fx$[p];
        if (fx) {
          for (var i = 0, l = fx.length, x; i < l && (x = fx[i]); i++) {
            if (x.kind === "annotation") {
              var node = this._nodes[x.effect.index];
              if (node._configValue) {
                var value = p === x.effect.value ? config[p] : this.get(x.effect.value, config);
                node._configValue(x.effect.name, value);
              }
            }
          }
        }
      }
    }
  },
  _afterClientsReady: function () {
    this._executeStaticEffects();
    this._applyConfig(this._config);
    this._flushHandlers();
  },
  _applyConfig: function (config) {
    for (var n in config) {
      if (this[n] === undefined) {
        var effects = this._propertyEffects[n];
        if (effects) {
          this._propertySet(n, config[n], effects);
        } else {
          this[n] = config[n];
        }
      }
    }
  },
  _notifyListener: function (fn, e) {
    if (!this._clientsReadied) {
      this._queueHandler([fn, e, e.target]);
    } else {
      return fn.call(this, e, e.target);
    }
  },
  _queueHandler: function (args) {
    this._handlers.push(args);
  },
  _flushHandlers: function () {
    var h$ = this._handlers;
    for (var i = 0, l = h$.length, h; i < l && (h = h$[i]); i++) {
      h[0].call(this, h[1], h[2]);
    }
  }
});

(function () {
  "use strict";
  Polymer.Base._addFeature({
    notifyPath: function (path, value, fromAbove) {
      var old = this._propertySet(path, value);
      if (old !== value) {
        this._pathEffector(path, value);
        if (!fromAbove) {
          this._notifyPath(path, value);
        }
      }
    },
    _getPathParts: function (path) {
      if (Array.isArray(path)) {
        var parts = [];
        for (var i = 0; i < path.length; i++) {
          var args = path[i].toString().split(".");
          for (var j = 0; j < args.length; j++) {
            parts.push(args[j]);
          }
        }
        return parts;
      } else {
        return path.toString().split(".");
      }
    },
    set: function (path, value, root) {
      var prop = root || this;
      var parts = this._getPathParts(path);
      var array;
      var last = parts[parts.length - 1];
      if (parts.length > 1) {
        for (var i = 0; i < parts.length - 1; i++) {
          prop = prop[parts[i]];
          if (array) {
            parts[i] = Polymer.Collection.get(array).getKey(prop);
          }
          if (!prop) {
            return;
          }
          array = Array.isArray(prop) ? prop : null;
        }
        prop[last] = value;
        if (!root) {
          this.notifyPath(parts.join("."), value);
        }
      } else {
        prop[path] = value;
      }
    },
    get: function (path, root) {
      var prop = root || this;
      var parts = this._getPathParts(path);
      var last = parts.pop();
      while (parts.length) {
        prop = prop[parts.shift()];
        if (!prop) {
          return;
        }
      }
      return prop[last];
    },
    _pathEffector: function (path, value) {
      var model = this._modelForPath(path);
      var fx$ = this._propertyEffects[model];
      if (fx$) {
        fx$.forEach(function (fx) {
          var fxFn = this["_" + fx.kind + "PathEffect"];
          if (fxFn) {
            fxFn.call(this, path, value, fx.effect);
          }
        }, this);
      }
      if (this._boundPaths) {
        this._notifyBoundPaths(path, value);
      }
    },
    _annotationPathEffect: function (path, value, effect) {
      if (effect.value === path || effect.value.indexOf(path + ".") === 0) {
        Polymer.Bind._annotationEffect.call(this, path, value, effect);
      } else if (path.indexOf(effect.value + ".") === 0 && !effect.negate) {
        var node = this._nodes[effect.index];
        if (node && node.notifyPath) {
          var p = this._fixPath(effect.name, effect.value, path);
          node.notifyPath(p, value, true);
        }
      }
    },
    _complexObserverPathEffect: function (path, value, effect) {
      if (this._pathMatchesEffect(path, effect)) {
        Polymer.Bind._complexObserverEffect.call(this, path, value, effect);
      }
    },
    _computePathEffect: function (path, value, effect) {
      if (this._pathMatchesEffect(path, effect)) {
        Polymer.Bind._computeEffect.call(this, path, value, effect);
      }
    },
    _annotatedComputationPathEffect: function (path, value, effect) {
      if (this._pathMatchesEffect(path, effect)) {
        Polymer.Bind._annotatedComputationEffect.call(this, path, value, effect);
      }
    },
    _pathMatchesEffect: function (path, effect) {
      var effectArg = effect.trigger.name;
      return effectArg == path || effectArg.indexOf(path + ".") === 0 || effect.trigger.wildcard && path.indexOf(effectArg) === 0;
    },
    linkPaths: function (to, from) {
      this._boundPaths = this._boundPaths || {};
      if (from) {
        this._boundPaths[to] = from;
      } else {
        this.unbindPath(to);
      }
    },
    unlinkPaths: function (path) {
      if (this._boundPaths) {
        delete this._boundPaths[path];
      }
    },
    _notifyBoundPaths: function (path, value) {
      var from, to;
      for (var a in this._boundPaths) {
        var b = this._boundPaths[a];
        if (path.indexOf(a + ".") == 0) {
          from = a;
          to = b;
          break;
        }
        if (path.indexOf(b + ".") == 0) {
          from = b;
          to = a;
          break;
        }
      }
      if (from && to) {
        var p = this._fixPath(to, from, path);
        this.notifyPath(p, value);
      }
    },
    _fixPath: function (property, root, path) {
      return property + path.slice(root.length);
    },
    _notifyPath: function (path, value) {
      var rootName = this._modelForPath(path);
      var dashCaseName = Polymer.CaseMap.camelToDashCase(rootName);
      var eventName = dashCaseName + this._EVENT_CHANGED;
      this.fire(eventName, {
        path: path,
        value: value
      }, {
        bubbles: false
      });
    },
    _modelForPath: function (path) {
      var dot = path.indexOf(".");
      return dot < 0 ? path : path.slice(0, dot);
    },
    _EVENT_CHANGED: "-changed",
    _notifySplice: function (array, path, index, added, removed) {
      var splices = [{
        index: index,
        addedCount: added,
        removed: removed,
        object: array,
        type: "splice"
      }];
      var change = {
        keySplices: Polymer.Collection.get(array).applySplices(splices),
        indexSplices: splices
      };
      this.set(path + ".splices", change);
      if (added != removed.length) {
        this.notifyPath(path + ".length", array.length);
      }
      change.keySplices = null;
      change.indexSplices = null;
    },
    push: function (path) {
      var array = this.get(path);
      var args = Array.prototype.slice.call(arguments, 1);
      var len = array.length;
      var ret = array.push.apply(array, args);
      this._notifySplice(array, path, len, args.length, []);
      return ret;
    },
    pop: function (path) {
      var array = this.get(path);
      var args = Array.prototype.slice.call(arguments, 1);
      var rem = array.slice(-1);
      var ret = array.pop.apply(array, args);
      this._notifySplice(array, path, array.length, 0, rem);
      return ret;
    },
    splice: function (path, start, deleteCount) {
      var array = this.get(path);
      var args = Array.prototype.slice.call(arguments, 1);
      var rem = array.slice(start, start + deleteCount);
      var ret = array.splice.apply(array, args);
      this._notifySplice(array, path, start, args.length - 2, rem);
      return ret;
    },
    shift: function (path) {
      var array = this.get(path);
      var args = Array.prototype.slice.call(arguments, 1);
      var ret = array.shift.apply(array, args);
      this._notifySplice(array, path, 0, 0, [ret]);
      return ret;
    },
    unshift: function (path) {
      var array = this.get(path);
      var args = Array.prototype.slice.call(arguments, 1);
      var ret = array.unshift.apply(array, args);
      this._notifySplice(array, path, 0, args.length, []);
      return ret;
    }
  });
})();

Polymer.Base._addFeature({
  resolveUrl: function (url) {
    var module = Polymer.DomModule.import(this.is);
    var root = "";
    if (module) {
      var assetPath = module.getAttribute("assetpath") || "";
      root = Polymer.ResolveUrl.resolveUrl(assetPath, module.ownerDocument.baseURI);
    }
    return Polymer.ResolveUrl.resolveUrl(url, root);
  }
});

Polymer.CssParse = function () {
  var api = {
    parse: function (text) {
      text = this._clean(text);
      return this._parseCss(this._lex(text), text);
    },
    _clean: function (cssText) {
      return cssText.replace(rx.comments, "").replace(rx.port, "");
    },
    _lex: function (text) {
      var root = {
        start: 0,
        end: text.length
      };
      var n = root;
      for (var i = 0, s = 0, l = text.length; i < l; i++) {
        switch (text[i]) {
          case this.OPEN_BRACE:
            if (!n.rules) {
              n.rules = [];
            }
            var p = n;
            var previous = p.rules[p.rules.length - 1];
            n = {
              start: i + 1,
              parent: p,
              previous: previous
            };
            p.rules.push(n);
            break;

          case this.CLOSE_BRACE:
            n.end = i + 1;
            n = n.parent || root;
            break;
        }
      }
      return root;
    },
    _parseCss: function (node, text) {
      var t = text.substring(node.start, node.end - 1);
      node.parsedCssText = node.cssText = t.trim();
      if (node.parent) {
        var ss = node.previous ? node.previous.end : node.parent.start;
        t = text.substring(ss, node.start - 1);
        t = t.substring(t.lastIndexOf(";") + 1);
        var s = node.parsedSelector = node.selector = t.trim();
        node.atRule = s.indexOf(AT_START) === 0;
        if (node.atRule) {
          if (s.indexOf(MEDIA_START) === 0) {
            node.type = this.types.MEDIA_RULE;
          } else if (s.match(rx.keyframesRule)) {
            node.type = this.types.KEYFRAMES_RULE;
          }
        } else {
          if (s.indexOf(VAR_START) === 0) {
            node.type = this.types.MIXIN_RULE;
          } else {
            node.type = this.types.STYLE_RULE;
          }
        }
      }
      var r$ = node.rules;
      if (r$) {
        for (var i = 0, l = r$.length, r; i < l && (r = r$[i]); i++) {
          this._parseCss(r, text);
        }
      }
      return node;
    },
    stringify: function (node, preserveProperties, text) {
      text = text || "";
      var cssText = "";
      if (node.cssText || node.rules) {
        var r$ = node.rules;
        if (r$ && (preserveProperties || !hasMixinRules(r$))) {
          for (var i = 0, l = r$.length, r; i < l && (r = r$[i]); i++) {
            cssText = this.stringify(r, preserveProperties, cssText);
          }
        } else {
          cssText = preserveProperties ? node.cssText : removeCustomProps(node.cssText);
          cssText = cssText.trim();
          if (cssText) {
            cssText = "  " + cssText + "\n";
          }
        }
      }
      if (cssText) {
        if (node.selector) {
          text += node.selector + " " + this.OPEN_BRACE + "\n";
        }
        text += cssText;
        if (node.selector) {
          text += this.CLOSE_BRACE + "\n\n";
        }
      }
      return text;
    },
    types: {
      STYLE_RULE: 1,
      KEYFRAMES_RULE: 7,
      MEDIA_RULE: 4,
      MIXIN_RULE: 1e3
    },
    OPEN_BRACE: "{",
    CLOSE_BRACE: "}"
  };

  function hasMixinRules(rules) {
    return rules[0].selector.indexOf(VAR_START) >= 0;
  }

  function removeCustomProps(cssText) {
    return cssText.replace(rx.customProp, "").replace(rx.mixinProp, "").replace(rx.mixinApply, "").replace(rx.varApply, "");
  }

  var VAR_START = "--";
  var MEDIA_START = "@media";
  var AT_START = "@";
  var rx = {
    comments: /\/\*[^*]*\*+([^\/*][^*]*\*+)*\//gim,
    port: /@import[^;]*;/gim,
    customProp: /(?:^|[\s;])--[^;{]*?:[^{};]*?;/gim,
    mixinProp: /(?:^|[\s;])--[^;{]*?:[^{;]*?{[^}]*?};?/gim,
    mixinApply: /@apply[\s]*\([^)]*?\)[\s]*;/gim,
    varApply: /[^;:]*?:[^;]*var[^;]*;/gim,
    keyframesRule: /^@[^\s]*keyframes/
  };
  return api;
}();

Polymer.StyleUtil = function () {
  return {
    MODULE_STYLES_SELECTOR: "style, link[rel=import][type~=css]",
    toCssText: function (rules, callback, preserveProperties) {
      if (typeof rules === "string") {
        rules = this.parser.parse(rules);
      }
      if (callback) {
        this.forEachStyleRule(rules, callback);
      }
      return this.parser.stringify(rules, preserveProperties);
    },
    forRulesInStyles: function (styles, callback) {
      for (var i = 0, l = styles.length, s; i < l && (s = styles[i]); i++) {
        this.forEachStyleRule(this.rulesForStyle(s), callback);
      }
    },
    rulesForStyle: function (style) {
      if (!style.__cssRules) {
        style.__cssRules = this.parser.parse(style.textContent);
      }
      return style.__cssRules;
    },
    clearStyleRules: function (style) {
      style.__cssRules = null;
    },
    forEachStyleRule: function (node, callback) {
      var s = node.selector;
      var skipRules = false;
      if (node.type === this.ruleTypes.STYLE_RULE) {
        callback(node);
      } else if (node.type === this.ruleTypes.KEYFRAMES_RULE || node.type === this.ruleTypes.MIXIN_RULE) {
        skipRules = true;
      }
      var r$ = node.rules;
      if (r$ && !skipRules) {
        for (var i = 0, l = r$.length, r; i < l && (r = r$[i]); i++) {
          this.forEachStyleRule(r, callback);
        }
      }
    },
    applyCss: function (cssText, moniker, target, afterNode) {
      var style = document.createElement("style");
      if (moniker) {
        style.setAttribute("scope", moniker);
      }
      style.textContent = cssText;
      target = target || document.head;
      if (!afterNode) {
        var n$ = target.querySelectorAll("style[scope]");
        afterNode = n$[n$.length - 1];
      }
      target.insertBefore(style, afterNode && afterNode.nextSibling || target.firstChild);
      return style;
    },
    cssFromModule: function (moduleId) {
      var m = Polymer.DomModule.import(moduleId);
      if (m && !m._cssText) {
        var cssText = "";
        var e$ = Array.prototype.slice.call(m.querySelectorAll(this.MODULE_STYLES_SELECTOR));
        for (var i = 0, e; i < e$.length; i++) {
          e = e$[i];
          if (e.localName === "style") {
            e = e.__appliedElement || e;
            e.parentNode.removeChild(e);
          } else {
            e = e.import && e.import.body;
          }
          if (e) {
            cssText += Polymer.ResolveUrl.resolveCss(e.textContent, e.ownerDocument);
          }
        }
        m._cssText = cssText;
      }
      return m && m._cssText || "";
    },
    parser: Polymer.CssParse,
    ruleTypes: Polymer.CssParse.types
  };
}();

Polymer.StyleTransformer = function () {
  var nativeShadow = Polymer.Settings.useNativeShadow;
  var styleUtil = Polymer.StyleUtil;
  var api = {
    dom: function (node, scope, useAttr, shouldRemoveScope) {
      this._transformDom(node, scope || "", useAttr, shouldRemoveScope);
    },
    _transformDom: function (node, selector, useAttr, shouldRemoveScope) {
      if (node.setAttribute) {
        this.element(node, selector, useAttr, shouldRemoveScope);
      }
      var c$ = Polymer.dom(node).childNodes;
      for (var i = 0; i < c$.length; i++) {
        this._transformDom(c$[i], selector, useAttr, shouldRemoveScope);
      }
    },
    element: function (element, scope, useAttr, shouldRemoveScope) {
      if (useAttr) {
        if (shouldRemoveScope) {
          element.removeAttribute(SCOPE_NAME);
        } else {
          element.setAttribute(SCOPE_NAME, scope);
        }
      } else {
        if (scope) {
          if (element.classList) {
            if (shouldRemoveScope) {
              element.classList.remove(SCOPE_NAME);
              element.classList.remove(scope);
            } else {
              element.classList.add(SCOPE_NAME);
              element.classList.add(scope);
            }
          } else if (element.getAttribute) {
            var c = element.getAttribute(CLASS);
            if (shouldRemoveScope) {
              if (c) {
                element.setAttribute(CLASS, c.replace(SCOPE_NAME, "").replace(scope, ""));
              }
            } else {
              element.setAttribute(CLASS, c + (c ? " " : "") + SCOPE_NAME + " " + scope);
            }
          }
        }
      }
    },
    elementStyles: function (element, callback) {
      var styles = element._styles;
      var cssText = "";
      for (var i = 0, l = styles.length, s, text; i < l && (s = styles[i]); i++) {
        var rules = styleUtil.rulesForStyle(s);
        cssText += nativeShadow ? styleUtil.toCssText(rules, callback) : this.css(rules, element.is, element.extends, callback, element._scopeCssViaAttr) + "\n\n";
      }
      return cssText.trim();
    },
    css: function (rules, scope, ext, callback, useAttr) {
      var hostScope = this._calcHostScope(scope, ext);
      scope = this._calcElementScope(scope, useAttr);
      var self = this;
      return styleUtil.toCssText(rules, function (rule) {
        if (!rule.isScoped) {
          self.rule(rule, scope, hostScope);
          rule.isScoped = true;
        }
        if (callback) {
          callback(rule, scope, hostScope);
        }
      });
    },
    _calcElementScope: function (scope, useAttr) {
      if (scope) {
        return useAttr ? CSS_ATTR_PREFIX + scope + CSS_ATTR_SUFFIX : CSS_CLASS_PREFIX + scope;
      } else {
        return "";
      }
    },
    _calcHostScope: function (scope, ext) {
      return ext ? "[is=" + scope + "]" : scope;
    },
    rule: function (rule, scope, hostScope) {
      this._transformRule(rule, this._transformComplexSelector, scope, hostScope);
    },
    _transformRule: function (rule, transformer, scope, hostScope) {
      var p$ = rule.selector.split(COMPLEX_SELECTOR_SEP);
      for (var i = 0, l = p$.length, p; i < l && (p = p$[i]); i++) {
        p$[i] = transformer.call(this, p, scope, hostScope);
      }
      rule.selector = p$.join(COMPLEX_SELECTOR_SEP);
    },
    _transformComplexSelector: function (selector, scope, hostScope) {
      var stop = false;
      var self = this;
      selector = selector.replace(SIMPLE_SELECTOR_SEP, function (m, c, s) {
        if (!stop) {
          var o = self._transformCompoundSelector(s, c, scope, hostScope);
          if (o.stop) {
            stop = true;
          }
          c = o.combinator;
          s = o.value;
        }
        return c + s;
      });
      return selector;
    },
    _transformCompoundSelector: function (selector, combinator, scope, hostScope) {
      var jumpIndex = selector.search(SCOPE_JUMP);
      if (selector.indexOf(HOST) >= 0) {
        selector = selector.replace(HOST_PAREN, function (m, host, paren) {
          return hostScope + paren;
        });
        selector = selector.replace(HOST, hostScope);
      } else if (jumpIndex !== 0) {
        selector = scope ? this._transformSimpleSelector(selector, scope) : selector;
      }
      if (selector.indexOf(CONTENT) >= 0) {
        combinator = "";
      }
      var stop;
      if (jumpIndex >= 0) {
        selector = selector.replace(SCOPE_JUMP, " ");
        stop = true;
      }
      return {
        value: selector,
        combinator: combinator,
        stop: stop
      };
    },
    _transformSimpleSelector: function (selector, scope) {
      var p$ = selector.split(PSEUDO_PREFIX);
      p$[0] += scope;
      return p$.join(PSEUDO_PREFIX);
    },
    rootRule: function (rule) {
      this._transformRule(rule, this._transformRootSelector);
    },
    _transformRootSelector: function (selector) {
      return selector.match(SCOPE_JUMP) ? this._transformComplexSelector(selector) : selector.trim() + SCOPE_ROOT_SELECTOR;
    },
    SCOPE_NAME: "style-scope"
  };
  var SCOPE_NAME = api.SCOPE_NAME;
  var SCOPE_ROOT_SELECTOR = ":not([" + SCOPE_NAME + "])" + ":not(." + SCOPE_NAME + ")";
  var COMPLEX_SELECTOR_SEP = ",";
  var SIMPLE_SELECTOR_SEP = /(^|[\s>+~]+)([^\s>+~]+)/g;
  var HOST = ":host";
  var HOST_PAREN = /(\:host)(?:\(((?:\([^)(]*\)|[^)(]*)+?)\))/g;
  var CONTENT = "::content";
  var SCOPE_JUMP = /\:\:content|\:\:shadow|\/deep\//;
  var CSS_CLASS_PREFIX = ".";
  var CSS_ATTR_PREFIX = "[" + SCOPE_NAME + "~=";
  var CSS_ATTR_SUFFIX = "]";
  var PSEUDO_PREFIX = ":";
  var CLASS = "class";
  return api;
}();

Polymer.StyleExtends = function () {
  var styleUtil = Polymer.StyleUtil;
  return {
    hasExtends: function (cssText) {
      return Boolean(cssText.match(this.rx.EXTEND));
    },
    transform: function (style) {
      var rules = styleUtil.rulesForStyle(style);
      var self = this;
      styleUtil.forEachStyleRule(rules, function (rule) {
        var map = self._mapRule(rule);
        if (rule.parent) {
          var m;
          while (m = self.rx.EXTEND.exec(rule.cssText)) {
            var extend = m[1];
            var extendor = self._findExtendor(extend, rule);
            if (extendor) {
              self._extendRule(rule, extendor);
            }
          }
        }
        rule.cssText = rule.cssText.replace(self.rx.EXTEND, "");
      });
      return styleUtil.toCssText(rules, function (rule) {
        if (rule.selector.match(self.rx.STRIP)) {
          rule.cssText = "";
        }
      }, true);
    },
    _mapRule: function (rule) {
      if (rule.parent) {
        var map = rule.parent.map || (rule.parent.map = {});
        var parts = rule.selector.split(",");
        for (var i = 0, p; i < parts.length; i++) {
          p = parts[i];
          map[p.trim()] = rule;
        }
        return map;
      }
    },
    _findExtendor: function (extend, rule) {
      return rule.parent && rule.parent.map && rule.parent.map[extend] || this._findExtendor(extend, rule.parent);
    },
    _extendRule: function (target, source) {
      if (target.parent !== source.parent) {
        this._cloneAndAddRuleToParent(source, target.parent);
      }
      target.extends = target.extends || (target.extends = []);
      target.extends.push(source);
      source.selector = source.selector.replace(this.rx.STRIP, "");
      source.selector = (source.selector && source.selector + ",\n") + target.selector;
      if (source.extends) {
        source.extends.forEach(function (e) {
          this._extendRule(target, e);
        }, this);
      }
    },
    _cloneAndAddRuleToParent: function (rule, parent) {
      rule = Object.create(rule);
      rule.parent = parent;
      if (rule.extends) {
        rule.extends = rule.extends.slice();
      }
      parent.rules.push(rule);
    },
    rx: {
      EXTEND: /@extends\(([^)]*)\)\s*?;/gim,
      STRIP: /%[^,]*$/
    }
  };
}();

(function () {
  var prepElement = Polymer.Base._prepElement;
  var nativeShadow = Polymer.Settings.useNativeShadow;
  var styleUtil = Polymer.StyleUtil;
  var styleTransformer = Polymer.StyleTransformer;
  var styleExtends = Polymer.StyleExtends;
  Polymer.Base._addFeature({
    _prepElement: function (element) {
      if (this._encapsulateStyle) {
        styleTransformer.element(element, this.is, this._scopeCssViaAttr);
      }
      prepElement.call(this, element);
    },
    _prepStyles: function () {
      if (this._encapsulateStyle === undefined) {
        this._encapsulateStyle = !nativeShadow && Boolean(this._template);
      }
      this._styles = this._collectStyles();
      var cssText = styleTransformer.elementStyles(this);
      if (cssText && this._template) {
        var style = styleUtil.applyCss(cssText, this.is, nativeShadow ? this._template.content : null);
        if (!nativeShadow) {
          this._scopeStyle = style;
        }
      }
    },
    _collectStyles: function () {
      var styles = [];
      var cssText = "", m$ = this.styleModules;
      if (m$) {
        for (var i = 0, l = m$.length, m; i < l && (m = m$[i]); i++) {
          cssText += styleUtil.cssFromModule(m);
        }
      }
      cssText += styleUtil.cssFromModule(this.is);
      if (cssText) {
        var style = document.createElement("style");
        style.textContent = cssText;
        if (styleExtends.hasExtends(style.textContent)) {
          cssText = styleExtends.transform(style);
        }
        styles.push(style);
      }
      return styles;
    },
    _elementAdd: function (node) {
      if (this._encapsulateStyle && !node.__styleScoped) {
        styleTransformer.dom(node, this.is, this._scopeCssViaAttr);
      }
    },
    _elementRemove: function (node) {
      if (this._encapsulateStyle) {
        styleTransformer.dom(node, this.is, this._scopeCssViaAttr, true);
      }
    },
    scopeSubtree: function (container, shouldObserve) {
      if (nativeShadow) {
        return;
      }
      var self = this;
      var scopify = function (node) {
        if (node.nodeType === Node.ELEMENT_NODE) {
          node.className = self._scopeElementClass(node, node.className);
          var n$ = node.querySelectorAll("*");
          Array.prototype.forEach.call(n$, function (n) {
            n.className = self._scopeElementClass(n, n.className);
          });
        }
      };
      scopify(container);
      if (shouldObserve) {
        var mo = new MutationObserver(function (mxns) {
          mxns.forEach(function (m) {
            if (m.addedNodes) {
              for (var i = 0; i < m.addedNodes.length; i++) {
                scopify(m.addedNodes[i]);
              }
            }
          });
        });
        mo.observe(container, {
          childList: true,
          subtree: true
        });
        return mo;
      }
    }
  });
})();

Polymer.StyleProperties = function () {
  var nativeShadow = Polymer.Settings.useNativeShadow;
  var matchesSelector = Polymer.DomApi.matchesSelector;
  var styleUtil = Polymer.StyleUtil;
  var styleTransformer = Polymer.StyleTransformer;
  return {
    decorateStyles: function (styles) {
      var self = this, props = {};
      styleUtil.forRulesInStyles(styles, function (rule) {
        self.decorateRule(rule);
        self.collectPropertiesInCssText(rule.propertyInfo.cssText, props);
      });
      var names = [];
      for (var i in props) {
        names.push(i);
      }
      return names;
    },
    decorateRule: function (rule) {
      if (rule.propertyInfo) {
        return rule.propertyInfo;
      }
      var info = {}, properties = {};
      var hasProperties = this.collectProperties(rule, properties);
      if (hasProperties) {
        info.properties = properties;
        rule.rules = null;
      }
      info.cssText = this.collectCssText(rule);
      rule.propertyInfo = info;
      return info;
    },
    collectProperties: function (rule, properties) {
      var info = rule.propertyInfo;
      if (info) {
        if (info.properties) {
          Polymer.Base.mixin(properties, info.properties);
          return true;
        }
      } else {
        var m, rx = this.rx.VAR_ASSIGN;
        var cssText = rule.parsedCssText;
        var any;
        while (m = rx.exec(cssText)) {
          properties[m[1]] = (m[2] || m[3]).trim();
          any = true;
        }
        return any;
      }
    },
    collectCssText: function (rule) {
      var customCssText = "";
      var cssText = rule.parsedCssText;
      cssText = cssText.replace(this.rx.BRACKETED, "").replace(this.rx.VAR_ASSIGN, "");
      var parts = cssText.split(";");
      for (var i = 0, p; i < parts.length; i++) {
        p = parts[i];
        if (p.match(this.rx.MIXIN_MATCH) || p.match(this.rx.VAR_MATCH)) {
          customCssText += p + ";\n";
        }
      }
      return customCssText;
    },
    collectPropertiesInCssText: function (cssText, props) {
      var m;
      while (m = this.rx.VAR_CAPTURE.exec(cssText)) {
        props[m[1]] = true;
      }
    },
    reify: function (props) {
      var names = Object.getOwnPropertyNames(props);
      for (var i = 0, n; i < names.length; i++) {
        n = names[i];
        props[n] = this.valueForProperty(props[n], props);
      }
    },
    valueForProperty: function (property, props) {
      if (property) {
        if (property.indexOf(";") >= 0) {
          property = this.valueForProperties(property, props);
        } else {
          var self = this;
          var fn = function (all, prefix, value, fallback) {
            var propertyValue = self.valueForProperty(props[value], props) || (props[fallback] ? self.valueForProperty(props[fallback], props) : fallback);
            return prefix + (propertyValue || "");
          };
          property = property.replace(this.rx.VAR_MATCH, fn);
        }
      }
      return property && property.trim() || "";
    },
    valueForProperties: function (property, props) {
      var parts = property.split(";");
      for (var i = 0, p, m; i < parts.length && (p = parts[i]); i++) {
        m = p.match(this.rx.MIXIN_MATCH);
        if (m) {
          p = this.valueForProperty(props[m[1]], props);
        } else {
          var pp = p.split(":");
          if (pp[1]) {
            pp[1] = pp[1].trim();
            pp[1] = this.valueForProperty(pp[1], props) || pp[1];
          }
          p = pp.join(":");
        }
        parts[i] = p && p.lastIndexOf(";") === p.length - 1 ? p.slice(0, -1) : p || "";
      }
      return parts.join(";");
    },
    applyProperties: function (rule, props) {
      var output = "";
      if (!rule.properties) {
        this.decorateRule(rule);
      }
      if (rule.propertyInfo.cssText) {
        output = this.valueForProperties(rule.propertyInfo.cssText, props);
      }
      rule.cssText = output;
    },
    propertyDataFromStyles: function (styles, element) {
      var props = {}, self = this;
      var o = [], i = 0;
      styleUtil.forRulesInStyles(styles, function (rule) {
        if (!rule.propertyInfo) {
          self.decorateRule(rule);
        }
        if (element && rule.propertyInfo.properties && matchesSelector.call(element, rule.selector)) {
          self.collectProperties(rule, props);
          addToBitMask(i, o);
        }
        i++;
      });
      return {
        properties: props,
        key: o
      };
    },
    scopePropertiesFromStyles: function (styles) {
      if (!styles._scopeStyleProperties) {
        styles._scopeStyleProperties = this.selectedPropertiesFromStyles(styles, this.SCOPE_SELECTORS);
      }
      return styles._scopeStyleProperties;
    },
    hostPropertiesFromStyles: function (styles) {
      if (!styles._hostStyleProperties) {
        styles._hostStyleProperties = this.selectedPropertiesFromStyles(styles, this.HOST_SELECTORS);
      }
      return styles._hostStyleProperties;
    },
    selectedPropertiesFromStyles: function (styles, selectors) {
      var props = {}, self = this;
      styleUtil.forRulesInStyles(styles, function (rule) {
        if (!rule.propertyInfo) {
          self.decorateRule(rule);
        }
        for (var i = 0; i < selectors.length; i++) {
          if (rule.parsedSelector === selectors[i]) {
            self.collectProperties(rule, props);
            return;
          }
        }
      });
      return props;
    },
    transformStyles: function (element, properties, scopeSelector) {
      var self = this;
      var hostRx = new RegExp(this.rx.HOST_PREFIX + element.is + this.rx.HOST_SUFFIX);
      return styleTransformer.elementStyles(element, function (rule) {
        self.applyProperties(rule, properties);
        if (rule.cssText && !nativeShadow) {
          self._scopeSelector(rule, hostRx, element.is, element._scopeCssViaAttr, scopeSelector);
        }
      });
    },
    _scopeSelector: function (rule, hostRx, is, viaAttr, scopeId) {
      rule.transformedSelector = rule.transformedSelector || rule.selector;
      var selector = rule.transformedSelector;
      var scope = viaAttr ? "[" + styleTransformer.SCOPE_NAME + "~=" + scopeId + "]" : "." + scopeId;
      var parts = selector.split(",");
      for (var i = 0, l = parts.length, p; i < l && (p = parts[i]); i++) {
        parts[i] = p.match(hostRx) ? p.replace(is, is + scope) : scope + " " + p;
      }
      rule.selector = parts.join(",");
    },
    applyElementScopeSelector: function (element, selector, old, viaAttr) {
      var c = viaAttr ? element.getAttribute(styleTransformer.SCOPE_NAME) : element.className;
      v = old ? c.replace(old, selector) : (c ? c + " " : "") + this.XSCOPE_NAME + " " + selector;
      if (c !== v) {
        if (viaAttr) {
          element.setAttribute(styleTransformer.SCOPE_NAME, v);
        } else {
          element.className = v;
        }
      }
    },
    applyElementStyle: function (element, properties, selector, style) {
      var cssText = style ? style.textContent || "" : this.transformStyles(element, properties, selector);
      var s = element._customStyle;
      if (s && !nativeShadow && s !== style) {
        s._useCount--;
        if (s._useCount <= 0) {
          s.parentNode.removeChild(s);
        }
      }
      if (nativeShadow || (!style || !style.parentNode)) {
        if (nativeShadow && element._customStyle) {
          element._customStyle.textContent = cssText;
          style = element._customStyle;
        } else if (cssText) {
          style = styleUtil.applyCss(cssText, selector, nativeShadow ? element.root : null, element._scopeStyle);
        }
      }
      if (style) {
        style._useCount = style._useCount || 0;
        if (element._customStyle != style) {
          style._useCount++;
        }
        element._customStyle = style;
      }
      return style;
    },
    rx: {
      VAR_ASSIGN: /(?:^|;\s*)(--[^\:;]*?):\s*?(?:([^;{]*?)|{([^}]*)})(?=;)/gim,
      MIXIN_MATCH: /(?:^|\W+)@apply[\s]*\(([^)]*)\);?/im,
      VAR_MATCH: /(^|\W+)var\([\s]*([^,)]*)[\s]*,?[\s]*((?:[^,)]*)|(?:[^;]*\([^;)]*\)))[\s]*?\)/gim,
      VAR_CAPTURE: /\([\s]*(--[^,\s)]*)(?:,[\s]*(--[^,\s)]*))?(?:\)|,)/gim,
      BRACKETED: /\{[^}]*\}/g,
      HOST_PREFIX: "(?:^|[^.])",
      HOST_SUFFIX: "($|[.:[\\s>+~])"
    },
    HOST_SELECTORS: [":host"],
    SCOPE_SELECTORS: [":root"],
    XSCOPE_NAME: "x-scope"
  };
  function addToBitMask(n, bits) {
    var o = parseInt(n / 32);
    var v = 1 << n % 32;
    bits[o] = (bits[o] || 0) | v;
  }
}();

Polymer.StyleDefaults = function () {
  var styleProperties = Polymer.StyleProperties;
  var styleUtil = Polymer.StyleUtil;
  var style = document.createElement("style");
  var api = {
    style: style,
    _styles: [style],
    _properties: null,
    applyCss: function (cssText) {
      this.style.textContent += cssText;
      styleUtil.clearStyleRules(this.style);
      this._properties = null;
    },
    get _styleProperties() {
      if (!this._properties) {
        styleProperties.decorateStyles(this._styles);
        this._styles._scopeStyleProperties = null;
        this._properties = styleProperties.scopePropertiesFromStyles(this._styles);
      }
      return this._properties;
    },
    _needsStyleProperties: function () {
    },
    _computeStyleProperties: function () {
      return this._styleProperties;
    }
  };
  return api;
}();

(function () {
  Polymer.StyleCache = function () {
    this.cache = {};
  };
  Polymer.StyleCache.prototype = {
    MAX: 100,
    store: function (is, data, keyValues, keyStyles) {
      data.keyValues = keyValues;
      data.styles = keyStyles;
      var s$ = this.cache[is] = this.cache[is] || [];
      s$.push(data);
      if (s$.length > this.MAX) {
        s$.shift();
      }
    },
    retrieve: function (is, keyValues, keyStyles) {
      var cache = this.cache[is];
      if (cache) {
        for (var i = cache.length - 1, data; i >= 0; i--) {
          data = cache[i];
          if (keyStyles === data.styles && this._objectsEqual(keyValues, data.keyValues)) {
            return data;
          }
        }
      }
    },
    clear: function () {
      this.cache = {};
    },
    _objectsEqual: function (target, source) {
      for (var i in target) {
        if (target[i] !== source[i]) {
          return false;
        }
      }
      if (Array.isArray(target)) {
        return target.length === source.length;
      }
      return true;
    }
  };
})();

(function () {
  var serializeValueToAttribute = Polymer.Base.serializeValueToAttribute;
  var propertyUtils = Polymer.StyleProperties;
  var styleTransformer = Polymer.StyleTransformer;
  var styleUtil = Polymer.StyleUtil;
  var styleDefaults = Polymer.StyleDefaults;
  var nativeShadow = Polymer.Settings.useNativeShadow;
  Polymer.Base._addFeature({
    _prepStyleProperties: function () {
      this._ownStylePropertyNames = this._styles ? propertyUtils.decorateStyles(this._styles) : [];
    },
    _setupStyleProperties: function () {
      this.customStyle = {};
    },
    _needsStyleProperties: function () {
      return Boolean(this._ownStylePropertyNames && this._ownStylePropertyNames.length);
    },
    _beforeAttached: function () {
      if (!this._scopeSelector && this._needsStyleProperties()) {
        this._updateStyleProperties();
      }
    },
    _updateStyleProperties: function () {
      var info, scope = this.domHost || styleDefaults;
      if (!scope._styleCache) {
        scope._styleCache = new Polymer.StyleCache();
      }
      var scopeData = propertyUtils.propertyDataFromStyles(scope._styles, this);
      info = scope._styleCache.retrieve(this.is, scopeData.key, this._styles);
      var scopeCached = Boolean(info);
      if (scopeCached) {
        this._styleProperties = info._styleProperties;
      } else {
        this._computeStyleProperties(scopeData.properties);
      }
      this._computeOwnStyleProperties();
      if (!scopeCached) {
        info = styleCache.retrieve(this.is, this._ownStyleProperties, this._styles);
      }
      var globalCached = Boolean(info) && !scopeCached;
      style = this._applyStyleProperties(info);
      if (!scopeCached) {
        var cacheableStyle = style;
        if (nativeShadow) {
          cacheableStyle = style.cloneNode ? style.cloneNode(true) : Object.create(style || null);
        }
        info = {
          style: cacheableStyle,
          _scopeSelector: this._scopeSelector,
          _styleProperties: this._styleProperties
        };
        scope._styleCache.store(this.is, info, scopeData.key, this._styles);
        if (!globalCached) {
          styleCache.store(this.is, Object.create(info), this._ownStyleProperties, this._styles);
        }
      }
    },
    _computeStyleProperties: function (scopeProps) {
      var scope = this.domHost || styleDefaults;
      if (!scope._styleProperties) {
        scope._computeStyleProperties();
      }
      var props = Object.create(scope._styleProperties);
      this.mixin(props, propertyUtils.hostPropertiesFromStyles(this._styles));
      scopeProps = scopeProps || propertyUtils.propertyDataFromStyles(scope._styles, this).properties;
      this.mixin(props, scopeProps);
      this.mixin(props, propertyUtils.scopePropertiesFromStyles(this._styles));
      this.mixin(props, this.customStyle);
      propertyUtils.reify(props);
      this._styleProperties = props;
    },
    _computeOwnStyleProperties: function () {
      var props = {};
      for (var i = 0, n; i < this._ownStylePropertyNames.length; i++) {
        n = this._ownStylePropertyNames[i];
        props[n] = this._styleProperties[n];
      }
      this._ownStyleProperties = props;
    },
    _scopeCount: 0,
    _applyStyleProperties: function (info) {
      var oldScopeSelector = this._scopeSelector;
      this._scopeSelector = info ? info._scopeSelector : this.is + "-" + this.__proto__._scopeCount++;
      style = propertyUtils.applyElementStyle(this, this._styleProperties, this._scopeSelector, info && info.style);
      if ((style || oldScopeSelector) && !nativeShadow) {
        propertyUtils.applyElementScopeSelector(this, this._scopeSelector, oldScopeSelector, this._scopeCssViaAttr);
      }
      return style || {};
    },
    serializeValueToAttribute: function (value, attribute, node) {
      node = node || this;
      if (attribute === "class") {
        var host = node === this ? this.domHost || this.dataHost : this;
        if (host) {
          value = host._scopeElementClass(node, value);
        }
      }
      node = Polymer.dom(node);
      serializeValueToAttribute.call(this, value, attribute, node);
    },
    _scopeElementClass: function (element, selector) {
      if (!nativeShadow && !this._scopeCssViaAttr) {
        selector += (selector ? " " : "") + SCOPE_NAME + " " + this.is + (element._scopeSelector ? " " + XSCOPE_NAME + " " + element._scopeSelector : "");
      }
      return selector;
    },
    updateStyles: function () {
      if (this.isAttached) {
        if (this._needsStyleProperties()) {
          this._updateStyleProperties();
        } else {
          this._styleProperties = null;
        }
        if (this._styleCache) {
          this._styleCache.clear();
        }
        this._updateRootStyles();
      }
    },
    _updateRootStyles: function (root) {
      root = root || this.root;
      var c$ = Polymer.dom(root)._query(function (e) {
        return e.shadyRoot || e.shadowRoot;
      });
      for (var i = 0, l = c$.length, c; i < l && (c = c$[i]); i++) {
        if (c.updateStyles) {
          c.updateStyles();
        }
      }
    }
  });
  Polymer.updateStyles = function () {
    styleDefaults._styleCache.clear();
    Polymer.Base._updateRootStyles(document);
  };
  var styleCache = new Polymer.StyleCache();
  Polymer.customStyleCache = styleCache;
  var SCOPE_NAME = styleTransformer.SCOPE_NAME;
  var XSCOPE_NAME = propertyUtils.XSCOPE_NAME;
})();

Polymer.Base._addFeature({
  _registerFeatures: function () {
    this._prepIs();
    this._prepAttributes();
    this._prepExtends();
    this._prepConstructor();
    this._prepTemplate();
    this._prepStyles();
    this._prepStyleProperties();
    this._prepAnnotations();
    this._prepEffects();
    this._prepBehaviors();
    this._prepBindings();
    this._prepShady();
  },
  _prepBehavior: function (b) {
    this._addPropertyEffects(b.properties);
    this._addComplexObserverEffects(b.observers);
    this._addHostAttributes(b.hostAttributes);
  },
  _initFeatures: function () {
    this._poolContent();
    this._setupConfigure();
    this._setupStyleProperties();
    this._pushHost();
    this._stampTemplate();
    this._popHost();
    this._marshalAnnotationReferences();
    this._marshalHostAttributes();
    this._setupDebouncers();
    this._marshalInstanceEffects();
    this._marshalBehaviors();
    this._marshalAttributes();
    this._tryReady();
  },
  _marshalBehavior: function (b) {
    this._listenListeners(b.listeners);
  }
});

(function () {
  var nativeShadow = Polymer.Settings.useNativeShadow;
  var propertyUtils = Polymer.StyleProperties;
  var styleUtil = Polymer.StyleUtil;
  var styleDefaults = Polymer.StyleDefaults;
  Polymer({
    is: "custom-style",
    "extends": "style",
    created: function () {
      this._appliesToDocument = this.parentNode.localName !== "dom-module";
      if (this._appliesToDocument) {
        var e = this.__appliedElement || this;
        var rules = styleUtil.rulesForStyle(e);
        propertyUtils.decorateStyles([e]);
        this._rulesToDefaultProperties(rules);
        this.async(this._applyStyle);
      }
    },
    _applyStyle: function () {
      var e = this.__appliedElement || this;
      this._computeStyleProperties();
      var props = this._styleProperties;
      var self = this;
      e.textContent = styleUtil.toCssText(styleUtil.rulesForStyle(e), function (rule) {
        if (rule.selector === ":root") {
          rule.selector = "body";
        }
        var css = rule.cssText = rule.parsedCssText;
        if (rule.propertyInfo.cssText) {
          css = css.replace(propertyUtils.rx.VAR_ASSIGN, "");
          rule.cssText = propertyUtils.valueForProperties(css, props);
        }
        if (!nativeShadow) {
          Polymer.StyleTransformer.rootRule(rule);
        }
      });
    },
    _rulesToDefaultProperties: function (rules) {
      styleUtil.forEachStyleRule(rules, function (rule) {
        if (!rule.propertyInfo.properties) {
          rule.cssText = "";
        }
      });
      var cssText = styleUtil.parser.stringify(rules, true);
      if (cssText) {
        styleDefaults.applyCss(cssText);
      }
    }
  });
})();

Polymer.Templatizer = {
  properties: {
    _hideTemplateChildren: {
      observer: "_hideTemplateChildrenChanged"
    }
  },
  _templatizerStatic: {
    count: 0,
    callbacks: {},
    debouncer: null
  },
  _instanceProps: Polymer.nob,
  created: function () {
    this._templatizerId = this._templatizerStatic.count++;
  },
  templatize: function (template) {
    if (!template._content) {
      template._content = template.content;
    }
    if (template._content._ctor) {
      this.ctor = template._content._ctor;
      this._prepParentProperties(this.ctor.prototype, template);
      return;
    }
    var archetype = Object.create(Polymer.Base);
    this._customPrepAnnotations(archetype, template);
    archetype._prepEffects();
    this._customPrepEffects(archetype);
    archetype._prepBehaviors();
    archetype._prepBindings();
    this._prepParentProperties(archetype, template);
    archetype._notifyPath = this._notifyPathImpl;
    archetype._scopeElementClass = this._scopeElementClassImpl;
    archetype.listen = this._listenImpl;
    var _constructor = this._constructorImpl;
    var ctor = function TemplateInstance(model, host) {
      _constructor.call(this, model, host);
    };
    ctor.prototype = archetype;
    archetype.constructor = ctor;
    template._content._ctor = ctor;
    this.ctor = ctor;
  },
  _getRootDataHost: function () {
    return this.dataHost && this.dataHost._rootDataHost || this.dataHost;
  },
  _hideTemplateChildrenChanged: function (hidden) {
    if (this._hideChildren) {
      this._hideChildren(hidden);
    }
  },
  _debounceTemplate: function (fn) {
    this._templatizerStatic.callbacks[this._templatizerId] = fn.bind(this);
    this._templatizerStatic.debouncer = Polymer.Debounce(this._templatizerStatic.debouncer, this._flushTemplates.bind(this, true));
  },
  _flushTemplates: function (debouncerExpired) {
    var db = this._templatizerStatic.debouncer;
    while (debouncerExpired || db && db.finish) {
      db.stop();
      var cbs = this._templatizerStatic.callbacks;
      this._templatizerStatic.callbacks = {};
      for (var id in cbs) {
        cbs[id]();
      }
      debouncerExpired = false;
    }
  },
  _customPrepEffects: function (archetype) {
    var parentProps = archetype._parentProps;
    for (var prop in parentProps) {
      archetype._addPropertyEffect(prop, "function", this._createHostPropEffector(prop));
    }
  },
  _customPrepAnnotations: function (archetype, template) {
    archetype._template = template;
    var c = template._content;
    if (!c._notes) {
      var rootDataHost = archetype._rootDataHost;
      if (rootDataHost) {
        Polymer.Annotations.prepElement = rootDataHost._prepElement.bind(rootDataHost);
      }
      c._notes = Polymer.Annotations.parseAnnotations(template);
      Polymer.Annotations.prepElement = null;
      this._processAnnotations(c._notes);
    }
    archetype._notes = c._notes;
    archetype._parentProps = c._parentProps;
  },
  _prepParentProperties: function (archetype, template) {
    var parentProps = this._parentProps = archetype._parentProps;
    if (this._forwardParentProp && parentProps) {
      var proto = archetype._parentPropProto;
      var prop;
      if (!proto) {
        for (prop in this._instanceProps) {
          delete parentProps[prop];
        }
        proto = archetype._parentPropProto = Object.create(null);
        if (template != this) {
          Polymer.Bind.prepareModel(proto);
        }
        for (prop in parentProps) {
          var parentProp = "_parent_" + prop;
          var effects = [{
            kind: "function",
            effect: this._createForwardPropEffector(prop)
          }, {
            kind: "notify"
          }];
          Polymer.Bind._createAccessors(proto, parentProp, effects);
        }
      }
      if (template != this) {
        Polymer.Bind.prepareInstance(template);
        template._forwardParentProp = this._forwardParentProp.bind(this);
      }
      this._extendTemplate(template, proto);
    }
  },
  _createForwardPropEffector: function (prop) {
    return function (source, value) {
      this._forwardParentProp(prop, value);
    };
  },
  _createHostPropEffector: function (prop) {
    return function (source, value) {
      this.dataHost["_parent_" + prop] = value;
    };
  },
  _extendTemplate: function (template, proto) {
    Object.getOwnPropertyNames(proto).forEach(function (n) {
      var val = template[n];
      var pd = Object.getOwnPropertyDescriptor(proto, n);
      Object.defineProperty(template, n, pd);
      if (val !== undefined) {
        template._propertySet(n, val);
      }
    });
  },
  _forwardInstancePath: function (inst, path, value) {
  },
  _notifyPathImpl: function (path, value) {
    var dataHost = this.dataHost;
    var dot = path.indexOf(".");
    var root = dot < 0 ? path : path.slice(0, dot);
    dataHost._forwardInstancePath.call(dataHost, this, path, value);
    if (root in dataHost._parentProps) {
      dataHost.notifyPath("_parent_" + path, value);
    }
  },
  _pathEffector: function (path, value, fromAbove) {
    if (this._forwardParentPath) {
      if (path.indexOf("_parent_") === 0) {
        this._forwardParentPath(path.substring(8), value);
      }
    }
    Polymer.Base._pathEffector.apply(this, arguments);
  },
  _constructorImpl: function (model, host) {
    this._rootDataHost = host._getRootDataHost();
    this._setupConfigure(model);
    this._pushHost(host);
    this.root = this.instanceTemplate(this._template);
    this.root.__styleScoped = true;
    this._popHost();
    this._marshalAnnotatedNodes();
    this._marshalInstanceEffects();
    this._marshalAnnotatedListeners();
    var children = [];
    for (var n = this.root.firstChild; n; n = n.nextSibling) {
      children.push(n);
      n._templateInstance = this;
    }
    this._children = children;
    this._tryReady();
  },
  _listenImpl: function (node, eventName, methodName) {
    var model = this;
    var host = this._rootDataHost;
    var handler = host._createEventHandler(node, eventName, methodName);
    var decorated = function (e) {
      e.model = model;
      handler(e);
    };
    host._listen(node, eventName, decorated);
  },
  _scopeElementClassImpl: function (node, value) {
    var host = this._rootDataHost;
    if (host) {
      return host._scopeElementClass(node, value);
    }
  },
  stamp: function (model) {
    model = model || {};
    if (this._parentProps) {
      for (var prop in this._parentProps) {
        model[prop] = this["_parent_" + prop];
      }
    }
    return new this.ctor(model, this);
  }
};

Polymer({
  is: "dom-template",
  "extends": "template",
  behaviors: [Polymer.Templatizer],
  ready: function () {
    this.templatize(this);
  }
});

Polymer._collections = new WeakMap();

Polymer.Collection = function (userArray) {
  Polymer._collections.set(userArray, this);
  this.userArray = userArray;
  this.store = userArray.slice();
  this.initMap();
};

Polymer.Collection.prototype = {
  constructor: Polymer.Collection,
  initMap: function () {
    var omap = this.omap = new WeakMap();
    var pmap = this.pmap = {};
    var s = this.store;
    for (var i = 0; i < s.length; i++) {
      var item = s[i];
      if (item && typeof item == "object") {
        omap.set(item, i);
      } else {
        pmap[item] = i;
      }
    }
  },
  add: function (item) {
    var key = this.store.push(item) - 1;
    if (item && typeof item == "object") {
      this.omap.set(item, key);
    } else {
      this.pmap[item] = key;
    }
    return key;
  },
  removeKey: function (key) {
    this._removeFromMap(this.store[key]);
    delete this.store[key];
  },
  _removeFromMap: function (item) {
    if (typeof item == "object") {
      this.omap.delete(item);
    } else {
      delete this.pmap[item];
    }
  },
  remove: function (item) {
    var key = this.getKey(item);
    this.removeKey(key);
    return key;
  },
  getKey: function (item) {
    if (typeof item == "object") {
      return this.omap.get(item);
    } else {
      return this.pmap[item];
    }
  },
  getKeys: function () {
    return Object.keys(this.store);
  },
  setItem: function (key, value) {
    this.store[key] = value;
  },
  getItem: function (key) {
    return this.store[key];
  },
  getItems: function () {
    var items = [], store = this.store;
    for (var key in store) {
      items.push(store[key]);
    }
    return items;
  },
  applySplices: function (splices) {
    var keySplices = [];
    for (var i = 0; i < splices.length; i++) {
      var j, o, key, s = splices[i];
      var removed = [];
      for (j = 0; j < s.removed.length; j++) {
        o = s.removed[j];
        key = this.remove(o);
        removed.push(key);
      }
      var added = [];
      for (j = 0; j < s.addedCount; j++) {
        o = this.userArray[s.index + j];
        key = this.add(o);
        added.push(key);
      }
      keySplices.push({
        index: s.index,
        removed: removed,
        removedItems: s.removed,
        added: added
      });
    }
    return keySplices;
  }
};

Polymer.Collection.get = function (userArray) {
  return Polymer._collections.get(userArray) || new Polymer.Collection(userArray);
};

Polymer({
  is: "dom-repeat",
  "extends": "template",
  properties: {
    items: {
      type: Array
    },
    as: {
      type: String,
      value: "item"
    },
    indexAs: {
      type: String,
      value: "index"
    },
    sort: {
      type: Function,
      observer: "_sortChanged"
    },
    filter: {
      type: Function,
      observer: "_filterChanged"
    },
    observe: {
      type: String,
      observer: "_observeChanged"
    },
    delay: Number
  },
  behaviors: [Polymer.Templatizer],
  observers: ["_itemsChanged(items.*)"],
  detached: function () {
    if (this.rows) {
      for (var i = 0; i < this.rows.length; i++) {
        this._detachRow(i);
      }
    }
    this.rows = null;
  },
  ready: function () {
    this._instanceProps = {
      __key__: true
    };
    this._instanceProps[this.as] = true;
    this._instanceProps[this.indexAs] = true;
    if (!this.ctor) {
      this.templatize(this);
    }
  },
  _sortChanged: function () {
    var dataHost = this._getRootDataHost();
    var sort = this.sort;
    this._sortFn = sort && (typeof sort == "function" ? sort : function () {
        return dataHost[sort].apply(dataHost, arguments);
      });
    this._fullRefresh = true;
    if (this.items) {
      this._debounceTemplate(this._render);
    }
  },
  _filterChanged: function () {
    var dataHost = this._getRootDataHost();
    var filter = this.filter;
    this._filterFn = filter && (typeof filter == "function" ? filter : function () {
        return dataHost[filter].apply(dataHost, arguments);
      });
    this._fullRefresh = true;
    if (this.items) {
      this._debounceTemplate(this._render);
    }
  },
  _observeChanged: function () {
    this._observePaths = this.observe && this.observe.replace(".*", ".").split(" ");
  },
  _itemsChanged: function (change) {
    if (change.path == "items") {
      if (Array.isArray(this.items)) {
        this.collection = Polymer.Collection.get(this.items);
      } else if (!this.items) {
        this.collection = null;
      } else {
        this._error(this._logf("dom-repeat", "expected array for `items`," + " found", this.items));
      }
      this._splices = [];
      this._fullRefresh = true;
      this._debounceTemplate(this._render);
    } else if (change.path == "items.splices") {
      this._splices = this._splices.concat(change.value.keySplices);
      this._debounceTemplate(this._render);
    } else {
      var subpath = change.path.slice(6);
      this._forwardItemPath(subpath, change.value);
      this._checkObservedPaths(subpath);
    }
  },
  _checkObservedPaths: function (path) {
    if (this._observePaths) {
      path = path.substring(path.indexOf(".") + 1);
      var paths = this._observePaths;
      for (var i = 0; i < paths.length; i++) {
        if (path.indexOf(paths[i]) === 0) {
          this._fullRefresh = true;
          if (this.delay) {
            this.debounce("render", this._render, this.delay);
          } else {
            this._debounceTemplate(this._render);
          }
          return;
        }
      }
    }
  },
  render: function () {
    this._fullRefresh = true;
    this.debounce("render", this._render);
    this._flushTemplates();
  },
  _render: function () {
    var c = this.collection;
    if (!this._fullRefresh) {
      if (this._sortFn) {
        this._applySplicesViewSort(this._splices);
      } else {
        if (this._filterFn) {
          this._fullRefresh = true;
        } else {
          this._applySplicesArraySort(this._splices);
        }
      }
    }
    if (this._fullRefresh) {
      this._sortAndFilter();
      this._fullRefresh = false;
    }
    this._splices = [];
    var rowForKey = this._rowForKey = {};
    var keys = this._orderedKeys;
    this.rows = this.rows || [];
    for (var i = 0; i < keys.length; i++) {
      var key = keys[i];
      var item = c.getItem(key);
      var row = this.rows[i];
      rowForKey[key] = i;
      if (!row) {
        this.rows.push(row = this._insertRow(i, null, item));
      }
      row[this.as] = item;
      row.__key__ = key;
      row[this.indexAs] = i;
    }
    for (; i < this.rows.length; i++) {
      this._detachRow(i);
    }
    this.rows.splice(keys.length, this.rows.length - keys.length);
    this.fire("dom-change");
  },
  _sortAndFilter: function () {
    var c = this.collection;
    if (!this._sortFn) {
      this._orderedKeys = [];
      var items = this.items;
      if (items) {
        for (var i = 0; i < items.length; i++) {
          this._orderedKeys.push(c.getKey(items[i]));
        }
      }
    } else {
      this._orderedKeys = c ? c.getKeys() : [];
    }
    if (this._filterFn) {
      this._orderedKeys = this._orderedKeys.filter(function (a) {
        return this._filterFn(c.getItem(a));
      }, this);
    }
    if (this._sortFn) {
      this._orderedKeys.sort(function (a, b) {
        return this._sortFn(c.getItem(a), c.getItem(b));
      }.bind(this));
    }
  },
  _keySort: function (a, b) {
    return this.collection.getKey(a) - this.collection.getKey(b);
  },
  _applySplicesViewSort: function (splices) {
    var c = this.collection;
    var keys = this._orderedKeys;
    var rows = this.rows;
    var removedRows = [];
    var addedKeys = [];
    var pool = [];
    var sortFn = this._sortFn || this._keySort.bind(this);
    splices.forEach(function (s) {
      for (var i = 0; i < s.removed.length; i++) {
        var idx = this._rowForKey[s.removed[i]];
        if (idx != null) {
          removedRows.push(idx);
        }
      }
      for (var i = 0; i < s.added.length; i++) {
        addedKeys.push(s.added[i]);
      }
    }, this);
    if (removedRows.length) {
      removedRows.sort();
      for (var i = removedRows.length - 1; i >= 0; i--) {
        var idx = removedRows[i];
        pool.push(this._detachRow(idx));
        rows.splice(idx, 1);
        keys.splice(idx, 1);
      }
    }
    if (addedKeys.length) {
      if (this._filterFn) {
        addedKeys = addedKeys.filter(function (a) {
          return this._filterFn(c.getItem(a));
        }, this);
      }
      addedKeys.sort(function (a, b) {
        return this._sortFn(c.getItem(a), c.getItem(b));
      }.bind(this));
      var start = 0;
      for (var i = 0; i < addedKeys.length; i++) {
        start = this._insertRowIntoViewSort(start, addedKeys[i], pool);
      }
    }
  },
  _insertRowIntoViewSort: function (start, key, pool) {
    var c = this.collection;
    var item = c.getItem(key);
    var end = this.rows.length - 1;
    var idx = -1;
    var sortFn = this._sortFn || this._keySort.bind(this);
    while (start <= end) {
      var mid = start + end >> 1;
      var midKey = this._orderedKeys[mid];
      var cmp = sortFn(c.getItem(midKey), item);
      if (cmp < 0) {
        start = mid + 1;
      } else if (cmp > 0) {
        end = mid - 1;
      } else {
        idx = mid;
        break;
      }
    }
    if (idx < 0) {
      idx = end + 1;
    }
    this._orderedKeys.splice(idx, 0, key);
    this.rows.splice(idx, 0, this._insertRow(idx, pool, c.getItem(key)));
    return idx;
  },
  _applySplicesArraySort: function (splices) {
    var keys = this._orderedKeys;
    var pool = [];
    splices.forEach(function (s) {
      for (var i = 0; i < s.removed.length; i++) {
        pool.push(this._detachRow(s.index + i));
      }
      this.rows.splice(s.index, s.removed.length);
    }, this);
    var c = this.collection;
    splices.forEach(function (s) {
      var args = [s.index, s.removed.length].concat(s.added);
      keys.splice.apply(keys, args);
      for (var i = 0; i < s.added.length; i++) {
        var item = c.getItem(s.added[i]);
        var row = this._insertRow(s.index + i, pool, item);
        this.rows.splice(s.index + i, 0, row);
      }
    }, this);
  },
  _detachRow: function (idx) {
    var row = this.rows[idx];
    var parentNode = Polymer.dom(this).parentNode;
    for (var i = 0; i < row._children.length; i++) {
      var el = row._children[i];
      Polymer.dom(row.root).appendChild(el);
    }
    return row;
  },
  _insertRow: function (idx, pool, item) {
    var row = pool && pool.pop() || this._generateRow(idx, item);
    var beforeRow = this.rows[idx];
    var beforeNode = beforeRow ? beforeRow._children[0] : this;
    var parentNode = Polymer.dom(this).parentNode;
    Polymer.dom(parentNode).insertBefore(row.root, beforeNode);
    return row;
  },
  _generateRow: function (idx, item) {
    var model = {
      __key__: this.collection.getKey(item)
    };
    model[this.as] = item;
    model[this.indexAs] = idx;
    var row = this.stamp(model);
    return row;
  },
  _hideChildren: function (hidden) {
    if (this.rows) {
      for (var i = 0; i < this.rows.length; i++) {
        var c$ = this.rows[i]._children;
        for (var j = 0; j < c$.length; j++) {
          var c = c$[j];
          if (c.style) {
            c.style.display = hidden ? "none" : "";
          }
          c._hideTemplateChildren = hidden;
        }
      }
    }
  },
  _forwardInstancePath: function (row, path, value) {
    if (path.indexOf(this.as + ".") === 0) {
      this.notifyPath("items." + row.__key__ + "." + path.slice(this.as.length + 1), value);
      return true;
    }
  },
  _forwardParentProp: function (prop, value) {
    if (this.rows) {
      this.rows.forEach(function (row) {
        row[prop] = value;
      }, this);
    }
  },
  _forwardParentPath: function (path, value) {
    if (this.rows) {
      this.rows.forEach(function (row) {
        row.notifyPath(path, value, true);
      }, this);
    }
  },
  _forwardItemPath: function (path, value) {
    if (this._rowForKey) {
      var dot = path.indexOf(".");
      var key = path.substring(0, dot < 0 ? path.length : dot);
      var idx = this._rowForKey[key];
      var row = this.rows[idx];
      if (row) {
        if (dot >= 0) {
          path = this.as + "." + path.substring(dot + 1);
          row.notifyPath(path, value, true);
        } else {
          row[this.as] = value;
        }
      }
    }
  },
  modelForElement: function (el) {
    var model;
    while (el) {
      if (model = el._templateInstance) {
        if (model.dataHost != this) {
          el = model.dataHost;
        } else {
          return model;
        }
      } else {
        el = el.parentNode;
      }
    }
  },
  itemForElement: function (el) {
    var instance = this.modelForElement(el);
    return instance && instance[this.as];
  },
  keyForElement: function (el) {
    var instance = this.modelForElement(el);
    return instance && instance.__key__;
  },
  indexForElement: function (el) {
    var instance = this.modelForElement(el);
    return instance && instance[this.indexAs];
  }
});

Polymer({
  is: "array-selector",
  properties: {
    items: {
      type: Array,
      observer: "_itemsChanged"
    },
    selected: {
      type: Object,
      notify: true
    },
    toggle: Boolean,
    multi: Boolean
  },
  _itemsChanged: function () {
    if (Array.isArray(this.selected)) {
      for (var i = 0; i < this.selected.length; i++) {
        this.unlinkPaths("selected." + i);
      }
    } else {
      this.unlinkPaths("selected");
    }
    if (this.multi) {
      this.selected = [];
    } else {
      this.selected = null;
    }
  },
  deselect: function (item) {
    if (this.multi) {
      var scol = Polymer.Collection.get(this.selected);
      var sidx = this.selected.indexOf(item);
      if (sidx >= 0) {
        var skey = scol.getKey(item);
        this.splice("selected", sidx, 1);
        this.unlinkPaths("selected." + skey);
        return true;
      }
    } else {
      this.selected = null;
      this.unlinkPaths("selected");
    }
  },
  select: function (item) {
    var icol = Polymer.Collection.get(this.items);
    var key = icol.getKey(item);
    if (this.multi) {
      var scol = Polymer.Collection.get(this.selected);
      var skey = scol.getKey(item);
      if (skey >= 0) {
        this.deselect(item);
      } else if (this.toggle) {
        this.push("selected", item);
        this.async(function () {
          skey = scol.getKey(item);
          this.linkPaths("selected." + skey, "items." + key);
        });
      }
    } else {
      if (this.toggle && item == this.selected) {
        this.deselect();
      } else {
        this.linkPaths("selected", "items." + key);
        this.selected = item;
      }
    }
  }
});

Polymer({
  is: "dom-if",
  "extends": "template",
  properties: {
    "if": {
      type: Boolean,
      value: false
    },
    restamp: {
      type: Boolean,
      value: false
    }
  },
  behaviors: [Polymer.Templatizer],
  observers: ["_queueRender(if, restamp)"],
  _queueRender: function () {
    this._debounceTemplate(this._render);
  },
  detached: function () {
    this._teardownInstance();
  },
  attached: function () {
    if (this.if && this.ctor) {
      this.async(this._ensureInstance);
    }
  },
  render: function () {
    this._flushTemplates();
  },
  _render: function () {
    if (this.if) {
      if (!this.ctor) {
        this._wrapTextNodes(this._content || this.content);
        this.templatize(this);
      }
      this._ensureInstance();
      this._hideTemplateChildren = false;
    } else if (this.restamp) {
      this._teardownInstance();
    }
    if (!this.restamp && this._instance) {
      this._hideTemplateChildren = !this.if;
    }
    if (this.if != this._lastIf) {
      this.fire("dom-change");
      this._lastIf = this.if;
    }
  },
  _ensureInstance: function () {
    if (!this._instance) {
      this._instance = this.stamp();
      var root = this._instance.root;
      var parent = Polymer.dom(Polymer.dom(this).parentNode);
      parent.insertBefore(root, this);
    }
  },
  _teardownInstance: function () {
    if (this._instance) {
      var c = this._instance._children;
      if (c) {
        var parent = Polymer.dom(Polymer.dom(c[0]).parentNode);
        c.forEach(function (n) {
          parent.removeChild(n);
        });
      }
      this._instance = null;
    }
  },
  _wrapTextNodes: function (root) {
    for (var n = root.firstChild; n; n = n.nextSibling) {
      if (n.nodeType === Node.TEXT_NODE) {
        var s = document.createElement("span");
        root.insertBefore(s, n);
        s.appendChild(n);
        n = s;
      }
    }
  },
  _hideChildren: function (hidden) {
    if (this._instance) {
      var c$ = this._instance._children;
      for (var i = 0; i < c$.length; i++) {
        var c = c$[i];
        c.style.display = hidden ? "none" : "";
        c._hideTemplateChildren = hidden;
      }
    }
  },
  _forwardParentProp: function (prop, value) {
    if (this._instance) {
      this._instance[prop] = value;
    }
  },
  _forwardParentPath: function (path, value) {
    if (this._instance) {
      this._instance.notifyPath(path, value, true);
    }
  }
});

Polymer.ImportStatus = {
  _ready: false,
  _callbacks: [],
  whenLoaded: function (cb) {
    if (this._ready) {
      cb();
    } else {
      this._callbacks.push(cb);
    }
  },
  _importsLoaded: function () {
    this._ready = true;
    this._callbacks.forEach(function (cb) {
      cb();
    });
    this._callbacks = [];
  }
};

window.addEventListener("load", function () {
  Polymer.ImportStatus._importsLoaded();
});

if (window.HTMLImports) {
  HTMLImports.whenReady(function () {
    Polymer.ImportStatus._importsLoaded();
  });
}

Polymer({
  is: "dom-bind",
  "extends": "template",
  created: function () {
    Polymer.ImportStatus.whenLoaded(this._readySelf.bind(this));
  },
  _registerFeatures: function () {
    this._prepExtends();
    this._prepConstructor();
  },
  _insertChildren: function () {
    var parentDom = Polymer.dom(Polymer.dom(this).parentNode);
    parentDom.insertBefore(this.root, this);
  },
  _removeChildren: function () {
    if (this._children) {
      for (var i = 0; i < this._children.length; i++) {
        this.root.appendChild(this._children[i]);
      }
    }
  },
  _initFeatures: function () {
  },
  _scopeElementClass: function (element, selector) {
    if (this.dataHost) {
      return this.dataHost._scopeElementClass(element, selector);
    } else {
      return selector;
    }
  },
  _prepConfigure: function () {
    var config = {};
    for (var prop in this._propertyEffects) {
      config[prop] = this[prop];
    }
    this._setupConfigure = this._setupConfigure.bind(this, config);
  },
  attached: function () {
    if (!this._children) {
      this._template = this;
      this._prepAnnotations();
      this._prepEffects();
      this._prepBehaviors();
      this._prepConfigure();
      this._prepBindings();
      Polymer.Base._initFeatures.call(this);
      this._children = Array.prototype.slice.call(this.root.childNodes);
    }
    this._insertChildren();
    this.fire("dom-change");
  },
  detached: function () {
    this._removeChildren();
  }
});