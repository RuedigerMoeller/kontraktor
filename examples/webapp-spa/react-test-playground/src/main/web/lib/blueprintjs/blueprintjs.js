(function webpackUniversalModuleDefinition(root, factory) {
	if(typeof exports === 'object' && typeof module === 'object')
		module.exports = factory(require("React"), require("window"), require("ReactDOM"), require("classNames"), require("Tether"), require("React.addons.CSSTransitionGroup"));
	else if(typeof define === 'function' && define.amd)
		define(["React", "window", "ReactDOM", "classNames", "Tether", "React.addons.CSSTransitionGroup"], factory);
	else if(typeof exports === 'object')
		exports["Core"] = factory(require("React"), require("window"), require("ReactDOM"), require("classNames"), require("Tether"), require("React.addons.CSSTransitionGroup"));
	else
		root["Blueprint"] = root["Blueprint"] || {}, root["Blueprint"]["Core"] = factory(root["React"], root["window"], root["ReactDOM"], root["classNames"], root["Tether"], root["React.addons.CSSTransitionGroup"]);
})(this, function(__WEBPACK_EXTERNAL_MODULE_7__, __WEBPACK_EXTERNAL_MODULE_21__, __WEBPACK_EXTERNAL_MODULE_23__, __WEBPACK_EXTERNAL_MODULE_25__, __WEBPACK_EXTERNAL_MODULE_30__, __WEBPACK_EXTERNAL_MODULE_32__) {
return /******/ (function(modules) { // webpackBootstrap
/******/ 	// The module cache
/******/ 	var installedModules = {};
/******/
/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {
/******/
/******/ 		// Check if module is in cache
/******/ 		if(installedModules[moduleId])
/******/ 			return installedModules[moduleId].exports;
/******/
/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = installedModules[moduleId] = {
/******/ 			exports: {},
/******/ 			id: moduleId,
/******/ 			loaded: false
/******/ 		};
/******/
/******/ 		// Execute the module function
/******/ 		modules[moduleId].call(module.exports, module, module.exports, __webpack_require__);
/******/
/******/ 		// Flag the module as loaded
/******/ 		module.loaded = true;
/******/
/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}
/******/
/******/
/******/ 	// expose the modules object (__webpack_modules__)
/******/ 	__webpack_require__.m = modules;
/******/
/******/ 	// expose the module cache
/******/ 	__webpack_require__.c = installedModules;
/******/
/******/ 	// __webpack_public_path__
/******/ 	__webpack_require__.p = "";
/******/
/******/ 	// Load entry module and return exports
/******/ 	return __webpack_require__(0);
/******/ })
/************************************************************************/
/******/ ([
/* 0 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	function __export(m) {
	    for (var p in m) if (!exports.hasOwnProperty(p)) exports[p] = m[p];
	}
	Object.defineProperty(exports, "__esModule", { value: true });
	__export(__webpack_require__(1));
	__export(__webpack_require__(4));
	__export(__webpack_require__(20));
	
	//# sourceMappingURL=index.js.map


/***/ }),
/* 1 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	function __export(m) {
	    for (var p in m) if (!exports.hasOwnProperty(p)) exports[p] = m[p];
	}
	Object.defineProperty(exports, "__esModule", { value: true });
	__export(__webpack_require__(2));
	
	//# sourceMappingURL=index.js.map


/***/ }),
/* 2 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var interactionMode_1 = __webpack_require__(3);
	exports.FOCUS_DISABLED_CLASS = "pt-focus-disabled";
	/* istanbul ignore next */
	var fakeFocusEngine = {
	    isActive: function () { return true; },
	    start: function () { return true; },
	    stop: function () { return true; },
	};
	/* istanbul ignore next */
	var focusEngine = typeof document !== "undefined"
	    ? new interactionMode_1.InteractionModeEngine(document.documentElement, exports.FOCUS_DISABLED_CLASS)
	    : fakeFocusEngine;
	// this is basically meaningless to unit test; it requires manual UI testing
	/* istanbul ignore next */
	exports.FocusStyleManager = {
	    alwaysShowFocus: function () { return focusEngine.stop(); },
	    isActive: function () { return focusEngine.isActive(); },
	    onlyShowFocusOnTabs: function () { return focusEngine.start(); },
	};
	
	//# sourceMappingURL=focusStyleManager.js.map


/***/ }),
/* 3 */
/***/ (function(module, exports) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var TAB_KEY_CODE = 9;
	/* istanbul ignore next */
	/**
	 * A nifty little class that maintains event handlers to add a class to the container element
	 * when entering "mouse mode" (on a `mousedown` event) and remove it when entering "keyboard mode"
	 * (on a `tab` key `keydown` event).
	 */
	var InteractionModeEngine = (function () {
	    // tslint:disable-next-line:no-constructor-vars
	    function InteractionModeEngine(container, className) {
	        var _this = this;
	        this.container = container;
	        this.className = className;
	        this.isRunning = false;
	        this.handleKeyDown = function (e) {
	            if (e.which === TAB_KEY_CODE) {
	                _this.reset();
	                _this.container.addEventListener("mousedown", _this.handleMouseDown);
	            }
	        };
	        this.handleMouseDown = function () {
	            _this.reset();
	            _this.container.classList.add(_this.className);
	            _this.container.addEventListener("keydown", _this.handleKeyDown);
	        };
	    }
	    /** Returns whether the engine is currently running. */
	    InteractionModeEngine.prototype.isActive = function () {
	        return this.isRunning;
	    };
	    /** Enable behavior which applies the given className when in mouse mode. */
	    InteractionModeEngine.prototype.start = function () {
	        this.container.addEventListener("mousedown", this.handleMouseDown);
	        this.isRunning = true;
	    };
	    /** Disable interaction mode behavior and remove className from container. */
	    InteractionModeEngine.prototype.stop = function () {
	        this.reset();
	        this.isRunning = false;
	    };
	    InteractionModeEngine.prototype.reset = function () {
	        this.container.classList.remove(this.className);
	        this.container.removeEventListener("keydown", this.handleKeyDown);
	        this.container.removeEventListener("mousedown", this.handleMouseDown);
	    };
	    return InteractionModeEngine;
	}());
	exports.InteractionModeEngine = InteractionModeEngine;
	
	//# sourceMappingURL=interactionMode.js.map


/***/ }),
/* 4 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	function __export(m) {
	    for (var p in m) if (!exports.hasOwnProperty(p)) exports[p] = m[p];
	}
	Object.defineProperty(exports, "__esModule", { value: true });
	__export(__webpack_require__(5));
	__export(__webpack_require__(11));
	__export(__webpack_require__(12));
	__export(__webpack_require__(13));
	__export(__webpack_require__(14));
	__export(__webpack_require__(15));
	var classes = __webpack_require__(16);
	var keys = __webpack_require__(17);
	var utils = __webpack_require__(8);
	exports.Classes = classes;
	exports.Keys = keys;
	exports.Utils = utils;
	// NOTE: Errors is not exported in public API
	var iconClasses_1 = __webpack_require__(18);
	exports.IconClasses = iconClasses_1.IconClasses;
	var iconStrings_1 = __webpack_require__(19);
	exports.IconContents = iconStrings_1.IconContents;
	
	//# sourceMappingURL=index.js.map


/***/ }),
/* 5 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var React = __webpack_require__(7);
	var utils_1 = __webpack_require__(8);
	/**
	 * An abstract component that Blueprint components can extend
	 * in order to add some common functionality like runtime props validation.
	 */
	var AbstractComponent = (function (_super) {
	    tslib_1.__extends(AbstractComponent, _super);
	    function AbstractComponent(props, context) {
	        var _this = _super.call(this, props, context) || this;
	        // Not bothering to remove entries when their timeouts finish because clearing invalid ID is a no-op
	        _this.timeoutIds = [];
	        /**
	         * Clear all known timeouts.
	         */
	        _this.clearTimeouts = function () {
	            if (_this.timeoutIds.length > 0) {
	                for (var _i = 0, _a = _this.timeoutIds; _i < _a.length; _i++) {
	                    var timeoutId = _a[_i];
	                    clearTimeout(timeoutId);
	                }
	                _this.timeoutIds = [];
	            }
	        };
	        if (!utils_1.isNodeEnv("production")) {
	            _this.validateProps(_this.props);
	        }
	        return _this;
	    }
	    AbstractComponent.prototype.componentWillReceiveProps = function (nextProps) {
	        if (!utils_1.isNodeEnv("production")) {
	            this.validateProps(nextProps);
	        }
	    };
	    AbstractComponent.prototype.componentWillUnmount = function () {
	        this.clearTimeouts();
	    };
	    /**
	     * Set a timeout and remember its ID.
	     * All stored timeouts will be cleared when component unmounts.
	     * @returns a "cancel" function that will clear timeout when invoked.
	     */
	    AbstractComponent.prototype.setTimeout = function (callback, timeout) {
	        var handle = setTimeout(callback, timeout);
	        this.timeoutIds.push(handle);
	        return function () { return clearTimeout(handle); };
	    };
	    /**
	     * Ensures that the props specified for a component are valid.
	     * Implementations should check that props are valid and usually throw an Error if they are not.
	     * Implementations should not duplicate checks that the type system already guarantees.
	     *
	     * This method should be used instead of React's
	     * [propTypes](https://facebook.github.io/react/docs/reusable-components.html#prop-validation) feature.
	     * In contrast to propTypes, these runtime checks are _always_ run, not just in development mode.
	     */
	    AbstractComponent.prototype.validateProps = function (_) {
	        // implement in subclass
	    };
	    ;
	    return AbstractComponent;
	}(React.Component));
	exports.AbstractComponent = AbstractComponent;
	
	//# sourceMappingURL=abstractComponent.js.map


/***/ }),
/* 6 */
/***/ (function(module, exports, __webpack_require__) {

	var __WEBPACK_AMD_DEFINE_ARRAY__, __WEBPACK_AMD_DEFINE_RESULT__;/* WEBPACK VAR INJECTION */(function(global) {/*! *****************************************************************************
	Copyright (c) Microsoft Corporation. All rights reserved.
	Licensed under the Apache License, Version 2.0 (the "License"); you may not use
	this file except in compliance with the License. You may obtain a copy of the
	License at http://www.apache.org/licenses/LICENSE-2.0
	
	THIS CODE IS PROVIDED ON AN *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
	KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION ANY IMPLIED
	WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A PARTICULAR PURPOSE,
	MERCHANTABLITY OR NON-INFRINGEMENT.
	
	See the Apache Version 2.0 License for specific language governing permissions
	and limitations under the License.
	***************************************************************************** */
	/* global global, define, System, Reflect, Promise */
	var __extends;
	var __assign;
	var __rest;
	var __decorate;
	var __param;
	var __metadata;
	var __awaiter;
	var __generator;
	var __exportStar;
	var __values;
	var __read;
	var __spread;
	var __await;
	var __asyncGenerator;
	var __asyncDelegator;
	var __asyncValues;
	(function (factory) {
	    var root = typeof global === "object" ? global : typeof self === "object" ? self : typeof this === "object" ? this : {};
	    if (true) {
	        !(__WEBPACK_AMD_DEFINE_ARRAY__ = [exports], __WEBPACK_AMD_DEFINE_RESULT__ = function (exports) { factory(createExporter(root, createExporter(exports))); }.apply(exports, __WEBPACK_AMD_DEFINE_ARRAY__), __WEBPACK_AMD_DEFINE_RESULT__ !== undefined && (module.exports = __WEBPACK_AMD_DEFINE_RESULT__));
	    }
	    else if (typeof module === "object" && typeof module.exports === "object") {
	        factory(createExporter(root, createExporter(module.exports)));
	    }
	    else {
	        factory(createExporter(root));
	    }
	    function createExporter(exports, previous) {
	        return function (id, v) { return exports[id] = previous ? previous(id, v) : v; };
	    }
	})
	(function (exporter) {
	    var extendStatics = Object.setPrototypeOf ||
	        ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
	        function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
	
	    __extends = function (d, b) {
	        extendStatics(d, b);
	        function __() { this.constructor = d; }
	        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
	    };
	
	    __assign = Object.assign || function (t) {
	        for (var s, i = 1, n = arguments.length; i < n; i++) {
	            s = arguments[i];
	            for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p)) t[p] = s[p];
	        }
	        return t;
	    };
	
	    __rest = function (s, e) {
	        var t = {};
	        for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p) && e.indexOf(p) < 0)
	            t[p] = s[p];
	        if (s != null && typeof Object.getOwnPropertySymbols === "function")
	            for (var i = 0, p = Object.getOwnPropertySymbols(s); i < p.length; i++) if (e.indexOf(p[i]) < 0)
	                t[p[i]] = s[p[i]];
	        return t;
	    };
	
	    __decorate = function (decorators, target, key, desc) {
	        var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
	        if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
	        else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
	        return c > 3 && r && Object.defineProperty(target, key, r), r;
	    };
	
	    __param = function (paramIndex, decorator) {
	        return function (target, key) { decorator(target, key, paramIndex); }
	    };
	
	    __metadata = function (metadataKey, metadataValue) {
	        if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(metadataKey, metadataValue);
	    };
	
	    __awaiter = function (thisArg, _arguments, P, generator) {
	        return new (P || (P = Promise))(function (resolve, reject) {
	            function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
	            function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
	            function step(result) { result.done ? resolve(result.value) : new P(function (resolve) { resolve(result.value); }).then(fulfilled, rejected); }
	            step((generator = generator.apply(thisArg, _arguments || [])).next());
	        });
	    };
	
	    __generator = function (thisArg, body) {
	        var _ = { label: 0, sent: function() { if (t[0] & 1) throw t[1]; return t[1]; }, trys: [], ops: [] }, f, y, t, g;
	        return g = { next: verb(0), "throw": verb(1), "return": verb(2) }, typeof Symbol === "function" && (g[Symbol.iterator] = function() { return this; }), g;
	        function verb(n) { return function (v) { return step([n, v]); }; }
	        function step(op) {
	            if (f) throw new TypeError("Generator is already executing.");
	            while (_) try {
	                if (f = 1, y && (t = y[op[0] & 2 ? "return" : op[0] ? "throw" : "next"]) && !(t = t.call(y, op[1])).done) return t;
	                if (y = 0, t) op = [0, t.value];
	                switch (op[0]) {
	                    case 0: case 1: t = op; break;
	                    case 4: _.label++; return { value: op[1], done: false };
	                    case 5: _.label++; y = op[1]; op = [0]; continue;
	                    case 7: op = _.ops.pop(); _.trys.pop(); continue;
	                    default:
	                        if (!(t = _.trys, t = t.length > 0 && t[t.length - 1]) && (op[0] === 6 || op[0] === 2)) { _ = 0; continue; }
	                        if (op[0] === 3 && (!t || (op[1] > t[0] && op[1] < t[3]))) { _.label = op[1]; break; }
	                        if (op[0] === 6 && _.label < t[1]) { _.label = t[1]; t = op; break; }
	                        if (t && _.label < t[2]) { _.label = t[2]; _.ops.push(op); break; }
	                        if (t[2]) _.ops.pop();
	                        _.trys.pop(); continue;
	                }
	                op = body.call(thisArg, _);
	            } catch (e) { op = [6, e]; y = 0; } finally { f = t = 0; }
	            if (op[0] & 5) throw op[1]; return { value: op[0] ? op[1] : void 0, done: true };
	        }
	    };
	
	    __exportStar = function (m, exports) {
	        for (var p in m) if (!exports.hasOwnProperty(p)) exports[p] = m[p];
	    };
	
	    __values = function (o) {
	        var m = typeof Symbol === "function" && o[Symbol.iterator], i = 0;
	        if (m) return m.call(o);
	        return {
	            next: function () {
	                if (o && i >= o.length) o = void 0;
	                return { value: o && o[i++], done: !o };
	            }
	        };
	    };
	
	    __read = function (o, n) {
	        var m = typeof Symbol === "function" && o[Symbol.iterator];
	        if (!m) return o;
	        var i = m.call(o), r, ar = [], e;
	        try {
	            while ((n === void 0 || n-- > 0) && !(r = i.next()).done) ar.push(r.value);
	        }
	        catch (error) { e = { error: error }; }
	        finally {
	            try {
	                if (r && !r.done && (m = i["return"])) m.call(i);
	            }
	            finally { if (e) throw e.error; }
	        }
	        return ar;
	    };
	
	    __spread = function () {
	        for (var ar = [], i = 0; i < arguments.length; i++)
	            ar = ar.concat(__read(arguments[i]));
	        return ar;
	    };
	
	    __await = function (v) {
	        return this instanceof __await ? (this.v = v, this) : new __await(v);
	    };
	
	    __asyncGenerator = function (thisArg, _arguments, generator) {
	        if (!Symbol.asyncIterator) throw new TypeError("Symbol.asyncIterator is not defined.");
	        var g = generator.apply(thisArg, _arguments || []), i, q = [];
	        return i = {}, verb("next"), verb("throw"), verb("return"), i[Symbol.asyncIterator] = function () { return this; }, i;
	        function verb(n) { if (g[n]) i[n] = function (v) { return new Promise(function (a, b) { q.push([n, v, a, b]) > 1 || resume(n, v); }); }; }
	        function resume(n, v) { try { step(g[n](v)); } catch (e) { settle(q[0][3], e); } }
	        function step(r) { r.value instanceof __await ? Promise.resolve(r.value.v).then(fulfill, reject) : settle(q[0][2], r);  }
	        function fulfill(value) { resume("next", value); }
	        function reject(value) { resume("throw", value); }
	        function settle(f, v) { if (f(v), q.shift(), q.length) resume(q[0][0], q[0][1]); }
	    };
	
	    __asyncDelegator = function (o) {
	        var i, p;
	        return i = {}, verb("next"), verb("throw", function (e) { throw e; }), verb("return"), i[Symbol.iterator] = function () { return this; }, i;
	        function verb(n, f) { if (o[n]) i[n] = function (v) { return (p = !p) ? { value: __await(o[n](v)), done: n === "return" } : f ? f(v) : v; }; }
	    };
	
	    __asyncValues = function (o) {
	        if (!Symbol.asyncIterator) throw new TypeError("Symbol.asyncIterator is not defined.");
	        var m = o[Symbol.asyncIterator];
	        return m ? m.call(o) : typeof __values === "function" ? __values(o) : o[Symbol.iterator]();
	    };
	
	    exporter("__extends", __extends);
	    exporter("__assign", __assign);
	    exporter("__rest", __rest);
	    exporter("__decorate", __decorate);
	    exporter("__param", __param);
	    exporter("__metadata", __metadata);
	    exporter("__awaiter", __awaiter);
	    exporter("__generator", __generator);
	    exporter("__exportStar", __exportStar);
	    exporter("__values", __values);
	    exporter("__read", __read);
	    exporter("__spread", __spread);
	    exporter("__await", __await);
	    exporter("__asyncGenerator", __asyncGenerator);
	    exporter("__asyncDelegator", __asyncDelegator);
	    exporter("__asyncValues", __asyncValues);
	});
	/* WEBPACK VAR INJECTION */}.call(exports, (function() { return this; }())))

/***/ }),
/* 7 */
/***/ (function(module, exports) {

	module.exports = __WEBPACK_EXTERNAL_MODULE_7__;

/***/ }),
/* 8 */
/***/ (function(module, exports, __webpack_require__) {

	/* WEBPACK VAR INJECTION */(function(process) {/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var errors_1 = __webpack_require__(10);
	/** Returns whether `process.env.NODE_ENV` exists and equals `env`. */
	function isNodeEnv(env) {
	    return typeof process !== "undefined" && ({"NODE_ENV":"production"}) && ("production") === env;
	}
	exports.isNodeEnv = isNodeEnv;
	/** Returns whether the value is a function. Acts as a type guard. */
	// tslint:disable-next-line:ban-types
	function isFunction(value) {
	    return typeof value === "function";
	}
	exports.isFunction = isFunction;
	// tslint:disable-next-line:ban-types
	function safeInvoke(func) {
	    var args = [];
	    for (var _i = 1; _i < arguments.length; _i++) {
	        args[_i - 1] = arguments[_i];
	    }
	    if (isFunction(func)) {
	        return func.apply(void 0, args);
	    }
	}
	exports.safeInvoke = safeInvoke;
	function elementIsOrContains(element, testElement) {
	    return element === testElement || element.contains(testElement);
	}
	exports.elementIsOrContains = elementIsOrContains;
	/**
	 * Returns the difference in length between two arrays. A `null` argument is considered an empty list.
	 * The return value will be positive if `a` is longer than `b`, negative if the opposite is true,
	 * and zero if their lengths are equal.
	 */
	function arrayLengthCompare(a, b) {
	    if (a === void 0) { a = []; }
	    if (b === void 0) { b = []; }
	    return a.length - b.length;
	}
	exports.arrayLengthCompare = arrayLengthCompare;
	/**
	 * Returns true if the two numbers are within the given tolerance of each other.
	 * This is useful to correct for floating point precision issues, less useful for integers.
	 */
	function approxEqual(a, b, tolerance) {
	    if (tolerance === void 0) { tolerance = 0.00001; }
	    return Math.abs(a - b) <= tolerance;
	}
	exports.approxEqual = approxEqual;
	/** Clamps the given number between min and max values. Returns value if within range, or closest bound. */
	function clamp(val, min, max) {
	    if (val == null) {
	        return val;
	    }
	    if (max < min) {
	        throw new Error(errors_1.CLAMP_MIN_MAX);
	    }
	    return Math.min(Math.max(val, min), max);
	}
	exports.clamp = clamp;
	/** Returns the number of decimal places in the given number. */
	function countDecimalPlaces(num) {
	    if (typeof num !== "number" || Math.floor(num) === num) {
	        return 0;
	    }
	    return num.toString().split(".")[1].length;
	}
	exports.countDecimalPlaces = countDecimalPlaces;
	/**
	 * Throttle an event on an EventTarget by wrapping it in a `requestAnimationFrame` call.
	 * Returns the event handler that was bound to given eventName so you can clean up after yourself.
	 * @see https://developer.mozilla.org/en-US/docs/Web/Events/scroll
	 */
	function throttleEvent(target, eventName, newEventName) {
	    var throttledFunc = throttleHelper(undefined, undefined, function (event) {
	        target.dispatchEvent(new CustomEvent(newEventName, event));
	    });
	    target.addEventListener(eventName, throttledFunc);
	    return throttledFunc;
	}
	exports.throttleEvent = throttleEvent;
	;
	;
	/**
	 * Throttle a callback by wrapping it in a `requestAnimationFrame` call. Returns the throttled
	 * function.
	 * @see https://www.html5rocks.com/en/tutorials/speed/animations/
	 */
	function throttleReactEventCallback(callback, options) {
	    if (options === void 0) { options = {}; }
	    var throttledFunc = throttleHelper(function (event2) {
	        if (options.preventDefault) {
	            event2.preventDefault();
	        }
	    }, function (event2) {
	        // prevent React from reclaiming the event object before we reference it
	        event2.persist();
	    }, function (event2) {
	        var otherArgs2 = [];
	        for (var _i = 1; _i < arguments.length; _i++) {
	            otherArgs2[_i - 1] = arguments[_i];
	        }
	        callback.apply(void 0, [event2].concat(otherArgs2));
	    });
	    return throttledFunc;
	}
	exports.throttleReactEventCallback = throttleReactEventCallback;
	function throttleHelper(onBeforeIsRunningCheck, onAfterIsRunningCheck, onAnimationFrameRequested) {
	    var isRunning = false;
	    var func = function () {
	        var args = [];
	        for (var _i = 0; _i < arguments.length; _i++) {
	            args[_i] = arguments[_i];
	        }
	        // don't use safeInvoke, because we might have more than its max number of typed params
	        if (isFunction(onBeforeIsRunningCheck)) {
	            onBeforeIsRunningCheck.apply(void 0, args);
	        }
	        if (isRunning) {
	            return;
	        }
	        isRunning = true;
	        if (isFunction(onAfterIsRunningCheck)) {
	            onAfterIsRunningCheck.apply(void 0, args);
	        }
	        requestAnimationFrame(function () {
	            if (isFunction(onAnimationFrameRequested)) {
	                onAnimationFrameRequested.apply(void 0, args);
	            }
	            isRunning = false;
	        });
	    };
	    return func;
	}
	
	//# sourceMappingURL=utils.js.map
	
	/* WEBPACK VAR INJECTION */}.call(exports, __webpack_require__(9)))

/***/ }),
/* 9 */
/***/ (function(module, exports) {

	// shim for using process in browser
	var process = module.exports = {};
	
	// cached from whatever global is present so that test runners that stub it
	// don't break things.  But we need to wrap it in a try catch in case it is
	// wrapped in strict mode code which doesn't define any globals.  It's inside a
	// function because try/catches deoptimize in certain engines.
	
	var cachedSetTimeout;
	var cachedClearTimeout;
	
	function defaultSetTimout() {
	    throw new Error('setTimeout has not been defined');
	}
	function defaultClearTimeout () {
	    throw new Error('clearTimeout has not been defined');
	}
	(function () {
	    try {
	        if (typeof setTimeout === 'function') {
	            cachedSetTimeout = setTimeout;
	        } else {
	            cachedSetTimeout = defaultSetTimout;
	        }
	    } catch (e) {
	        cachedSetTimeout = defaultSetTimout;
	    }
	    try {
	        if (typeof clearTimeout === 'function') {
	            cachedClearTimeout = clearTimeout;
	        } else {
	            cachedClearTimeout = defaultClearTimeout;
	        }
	    } catch (e) {
	        cachedClearTimeout = defaultClearTimeout;
	    }
	} ())
	function runTimeout(fun) {
	    if (cachedSetTimeout === setTimeout) {
	        //normal enviroments in sane situations
	        return setTimeout(fun, 0);
	    }
	    // if setTimeout wasn't available but was latter defined
	    if ((cachedSetTimeout === defaultSetTimout || !cachedSetTimeout) && setTimeout) {
	        cachedSetTimeout = setTimeout;
	        return setTimeout(fun, 0);
	    }
	    try {
	        // when when somebody has screwed with setTimeout but no I.E. maddness
	        return cachedSetTimeout(fun, 0);
	    } catch(e){
	        try {
	            // When we are in I.E. but the script has been evaled so I.E. doesn't trust the global object when called normally
	            return cachedSetTimeout.call(null, fun, 0);
	        } catch(e){
	            // same as above but when it's a version of I.E. that must have the global object for 'this', hopfully our context correct otherwise it will throw a global error
	            return cachedSetTimeout.call(this, fun, 0);
	        }
	    }
	
	
	}
	function runClearTimeout(marker) {
	    if (cachedClearTimeout === clearTimeout) {
	        //normal enviroments in sane situations
	        return clearTimeout(marker);
	    }
	    // if clearTimeout wasn't available but was latter defined
	    if ((cachedClearTimeout === defaultClearTimeout || !cachedClearTimeout) && clearTimeout) {
	        cachedClearTimeout = clearTimeout;
	        return clearTimeout(marker);
	    }
	    try {
	        // when when somebody has screwed with setTimeout but no I.E. maddness
	        return cachedClearTimeout(marker);
	    } catch (e){
	        try {
	            // When we are in I.E. but the script has been evaled so I.E. doesn't  trust the global object when called normally
	            return cachedClearTimeout.call(null, marker);
	        } catch (e){
	            // same as above but when it's a version of I.E. that must have the global object for 'this', hopfully our context correct otherwise it will throw a global error.
	            // Some versions of I.E. have different rules for clearTimeout vs setTimeout
	            return cachedClearTimeout.call(this, marker);
	        }
	    }
	
	
	
	}
	var queue = [];
	var draining = false;
	var currentQueue;
	var queueIndex = -1;
	
	function cleanUpNextTick() {
	    if (!draining || !currentQueue) {
	        return;
	    }
	    draining = false;
	    if (currentQueue.length) {
	        queue = currentQueue.concat(queue);
	    } else {
	        queueIndex = -1;
	    }
	    if (queue.length) {
	        drainQueue();
	    }
	}
	
	function drainQueue() {
	    if (draining) {
	        return;
	    }
	    var timeout = runTimeout(cleanUpNextTick);
	    draining = true;
	
	    var len = queue.length;
	    while(len) {
	        currentQueue = queue;
	        queue = [];
	        while (++queueIndex < len) {
	            if (currentQueue) {
	                currentQueue[queueIndex].run();
	            }
	        }
	        queueIndex = -1;
	        len = queue.length;
	    }
	    currentQueue = null;
	    draining = false;
	    runClearTimeout(timeout);
	}
	
	process.nextTick = function (fun) {
	    var args = new Array(arguments.length - 1);
	    if (arguments.length > 1) {
	        for (var i = 1; i < arguments.length; i++) {
	            args[i - 1] = arguments[i];
	        }
	    }
	    queue.push(new Item(fun, args));
	    if (queue.length === 1 && !draining) {
	        runTimeout(drainQueue);
	    }
	};
	
	// v8 likes predictible objects
	function Item(fun, array) {
	    this.fun = fun;
	    this.array = array;
	}
	Item.prototype.run = function () {
	    this.fun.apply(null, this.array);
	};
	process.title = 'browser';
	process.browser = true;
	process.env = {};
	process.argv = [];
	process.version = ''; // empty string to avoid regexp issues
	process.versions = {};
	
	function noop() {}
	
	process.on = noop;
	process.addListener = noop;
	process.once = noop;
	process.off = noop;
	process.removeListener = noop;
	process.removeAllListeners = noop;
	process.emit = noop;
	process.prependListener = noop;
	process.prependOnceListener = noop;
	
	process.listeners = function (name) { return [] }
	
	process.binding = function (name) {
	    throw new Error('process.binding is not supported');
	};
	
	process.cwd = function () { return '/' };
	process.chdir = function (dir) {
	    throw new Error('process.chdir is not supported');
	};
	process.umask = function() { return 0; };


/***/ }),
/* 10 */
/***/ (function(module, exports) {

	/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var ns = "[Blueprint]";
	var deprec = ns + " DEPRECATION:";
	exports.CLAMP_MIN_MAX = ns + " clamp: max cannot be less than min";
	exports.ALERT_WARN_CANCEL_PROPS = ns + " <Alert> cancelButtonText and onCancel should be set together.";
	exports.COLLAPSIBLE_LIST_INVALID_CHILD = ns + " <CollapsibleList> children must be <MenuItem>s";
	exports.CONTEXTMENU_WARN_DECORATOR_NO_METHOD = ns + " @ContextMenuTarget-decorated class should implement renderContextMenu.";
	exports.HOTKEYS_HOTKEY_CHILDREN = ns + " <Hotkeys> only accepts <Hotkey> children.";
	exports.MENU_WARN_CHILDREN_SUBMENU_MUTEX = ns + " <MenuItem> children and submenu props are mutually exclusive, with children taking priority.";
	exports.NUMERIC_INPUT_MIN_MAX = ns + " <NumericInput> requires min to be strictly less than max if both are defined.";
	exports.NUMERIC_INPUT_MINOR_STEP_SIZE_BOUND = ns + " <NumericInput> requires minorStepSize to be strictly less than stepSize.";
	exports.NUMERIC_INPUT_MAJOR_STEP_SIZE_BOUND = ns + " <NumericInput> requires majorStepSize to be strictly greater than stepSize.";
	exports.NUMERIC_INPUT_MINOR_STEP_SIZE_NON_POSITIVE = ns + " <NumericInput> requires minorStepSize to be strictly greater than zero.";
	exports.NUMERIC_INPUT_MAJOR_STEP_SIZE_NON_POSITIVE = ns + " <NumericInput> requires majorStepSize to be strictly greater than zero.";
	exports.NUMERIC_INPUT_STEP_SIZE_NON_POSITIVE = ns + " <NumericInput> requires stepSize to be strictly greater than zero.";
	exports.NUMERIC_INPUT_STEP_SIZE_NULL = ns + " <NumericInput> requires stepSize to be defined.";
	exports.POPOVER_REQUIRES_TARGET = ns + " <Popover> requires target prop or at least one child element.";
	exports.POPOVER_MODAL_INTERACTION = ns + " <Popover isModal={true}> requires interactionKind={PopoverInteractionKind.CLICK}.";
	exports.POPOVER_WARN_TOO_MANY_CHILDREN = ns + " <Popover> supports one or two children; additional children are ignored."
	    + " First child is the target, second child is the content. You may instead supply these two as props.";
	exports.POPOVER_WARN_DOUBLE_CONTENT = ns + " <Popover> with two children ignores content prop; use either prop or children.";
	exports.POPOVER_WARN_DOUBLE_TARGET = ns + " <Popover> with children ignores target prop; use either prop or children.";
	exports.POPOVER_WARN_EMPTY_CONTENT = ns + " Disabling <Popover> with empty/whitespace content...";
	exports.POPOVER_WARN_MODAL_INLINE = ns + " <Popover inline={true}> ignores isModal";
	exports.POPOVER_WARN_DEPRECATED_CONSTRAINTS = deprec + " <Popover> constraints and useSmartPositioning are deprecated. Use tetherOptions directly.";
	exports.POPOVER_WARN_INLINE_NO_TETHER = ns + " <Popover inline={true}> ignores tetherOptions, constraints, and useSmartPositioning.";
	exports.POPOVER_WARN_UNCONTROLLED_ONINTERACTION = ns + " <Popover> onInteraction is ignored when uncontrolled.";
	exports.PORTAL_CONTEXT_CLASS_NAME_STRING = ns + " <Portal> context blueprintPortalClassName must be string";
	exports.RADIOGROUP_WARN_CHILDREN_OPTIONS_MUTEX = ns + " <RadioGroup> children and options prop are mutually exclusive, with options taking priority.";
	exports.SLIDER_ZERO_STEP = ns + " <Slider> stepSize must be greater than zero.";
	exports.SLIDER_ZERO_LABEL_STEP = ns + " <Slider> labelStepSize must be greater than zero.";
	exports.RANGESLIDER_NULL_VALUE = ns + " <RangeSlider> value prop must be an array of two non-null numbers.";
	exports.TABS_FIRST_CHILD = ns + " First child of <Tabs> component must be a <TabList>";
	exports.TABS_MISMATCH = ns + " Number of <Tab> components must equal number of <TabPanel> components";
	exports.TABS_WARN_DEPRECATED = deprec + " <Tabs> is deprecated since v1.11.0; consider upgrading to <Tabs2>."
	    + " https://blueprintjs.com/#components.tabs.js";
	exports.TOASTER_WARN_INLINE = ns + " Toaster.create() ignores inline prop as it always creates a new element.";
	exports.TOASTER_WARN_LEFT_RIGHT = ns + " Toaster does not support LEFT or RIGHT positions.";
	exports.DIALOG_WARN_NO_HEADER_ICON = ns + " <Dialog> iconName is ignored if title is omitted.";
	exports.DIALOG_WARN_NO_HEADER_CLOSE_BUTTON = ns + " <Dialog> isCloseButtonShown prop is ignored if title is omitted.";
	
	//# sourceMappingURL=errors.js.map


/***/ }),
/* 11 */
/***/ (function(module, exports) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	exports.Colors = {
	    BLACK: "#10161A",
	    BLUE1: "#0E5A8A",
	    BLUE2: "#106BA3",
	    BLUE3: "#137CBD",
	    BLUE4: "#2B95D6",
	    BLUE5: "#48AFF0",
	    COBALT1: "#1F4B99",
	    COBALT2: "#2458B3",
	    COBALT3: "#2965CC",
	    COBALT4: "#4580E6",
	    COBALT5: "#669EFF",
	    DARK_GRAY1: "#182026",
	    DARK_GRAY2: "#202B33",
	    DARK_GRAY3: "#293742",
	    DARK_GRAY4: "#30404D",
	    DARK_GRAY5: "#394B59",
	    FOREST1: "#1D7324",
	    FOREST2: "#238C2C",
	    FOREST3: "#29A634",
	    FOREST4: "#43BF4D",
	    FOREST5: "#62D96B",
	    GOLD1: "#A67908",
	    GOLD2: "#BF8C0A",
	    GOLD3: "#D99E0B",
	    GOLD4: "#F2B824",
	    GOLD5: "#FFC940",
	    GRAY1: "#5C7080",
	    GRAY2: "#738694",
	    GRAY3: "#8A9BA8",
	    GRAY4: "#A7B6C2",
	    GRAY5: "#BFCCD6",
	    GREEN1: "#0A6640",
	    GREEN2: "#0D8050",
	    GREEN3: "#0F9960",
	    GREEN4: "#15B371",
	    GREEN5: "#3DCC91",
	    INDIGO1: "#5642A6",
	    INDIGO2: "#634DBF",
	    INDIGO3: "#7157D9",
	    INDIGO4: "#9179F2",
	    INDIGO5: "#AD99FF",
	    LIGHT_GRAY1: "#CED9E0",
	    LIGHT_GRAY2: "#D8E1E8",
	    LIGHT_GRAY3: "#E1E8ED",
	    LIGHT_GRAY4: "#EBF1F5",
	    LIGHT_GRAY5: "#F5F8FA",
	    LIME1: "#728C23",
	    LIME2: "#87A629",
	    LIME3: "#9BBF30",
	    LIME4: "#B6D94C",
	    LIME5: "#D1F26D",
	    ORANGE1: "#A66321",
	    ORANGE2: "#BF7326",
	    ORANGE3: "#D9822B",
	    ORANGE4: "#F29D49",
	    ORANGE5: "#FFB366",
	    RED1: "#A82A2A",
	    RED2: "#C23030",
	    RED3: "#DB3737",
	    RED4: "#F55656",
	    RED5: "#FF7373",
	    ROSE1: "#A82255",
	    ROSE2: "#C22762",
	    ROSE3: "#DB2C6F",
	    ROSE4: "#F5498B",
	    ROSE5: "#FF66A1",
	    SEPIA1: "#63411E",
	    SEPIA2: "#7D5125",
	    SEPIA3: "#96622D",
	    SEPIA4: "#B07B46",
	    SEPIA5: "#C99765",
	    TURQUOISE1: "#008075",
	    TURQUOISE2: "#00998C",
	    TURQUOISE3: "#00B3A4",
	    TURQUOISE4: "#14CCBD",
	    TURQUOISE5: "#2EE6D6",
	    VERMILION1: "#9E2B0E",
	    VERMILION2: "#B83211",
	    VERMILION3: "#D13913",
	    VERMILION4: "#EB532D",
	    VERMILION5: "#FF6E4A",
	    VIOLET1: "#5C255C",
	    VIOLET2: "#752F75",
	    VIOLET3: "#8F398F",
	    VIOLET4: "#A854A8",
	    VIOLET5: "#C274C2",
	    WHITE: "#FFFFFF",
	};
	
	//# sourceMappingURL=colors.js.map


/***/ }),
/* 12 */
/***/ (function(module, exports) {

	/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	/**
	 * The four basic intents.
	 */
	var Intent;
	(function (Intent) {
	    Intent[Intent["NONE"] = -1] = "NONE";
	    Intent[Intent["PRIMARY"] = 0] = "PRIMARY";
	    Intent[Intent["SUCCESS"] = 1] = "SUCCESS";
	    Intent[Intent["WARNING"] = 2] = "WARNING";
	    Intent[Intent["DANGER"] = 3] = "DANGER";
	})(Intent = exports.Intent || (exports.Intent = {}));
	
	//# sourceMappingURL=intent.js.map


/***/ }),
/* 13 */
/***/ (function(module, exports) {

	/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var Position;
	(function (Position) {
	    Position[Position["TOP_LEFT"] = 0] = "TOP_LEFT";
	    Position[Position["TOP"] = 1] = "TOP";
	    Position[Position["TOP_RIGHT"] = 2] = "TOP_RIGHT";
	    Position[Position["RIGHT_TOP"] = 3] = "RIGHT_TOP";
	    Position[Position["RIGHT"] = 4] = "RIGHT";
	    Position[Position["RIGHT_BOTTOM"] = 5] = "RIGHT_BOTTOM";
	    Position[Position["BOTTOM_RIGHT"] = 6] = "BOTTOM_RIGHT";
	    Position[Position["BOTTOM"] = 7] = "BOTTOM";
	    Position[Position["BOTTOM_LEFT"] = 8] = "BOTTOM_LEFT";
	    Position[Position["LEFT_BOTTOM"] = 9] = "LEFT_BOTTOM";
	    Position[Position["LEFT"] = 10] = "LEFT";
	    Position[Position["LEFT_TOP"] = 11] = "LEFT_TOP";
	})(Position = exports.Position || (exports.Position = {}));
	function isPositionHorizontal(position) {
	    /* istanbul ignore next */
	    return position === Position.TOP || position === Position.TOP_LEFT || position === Position.TOP_RIGHT
	        || position === Position.BOTTOM || position === Position.BOTTOM_LEFT || position === Position.BOTTOM_RIGHT;
	}
	exports.isPositionHorizontal = isPositionHorizontal;
	function isPositionVertical(position) {
	    /* istanbul ignore next */
	    return position === Position.LEFT || position === Position.LEFT_TOP || position === Position.LEFT_BOTTOM
	        || position === Position.RIGHT || position === Position.RIGHT_TOP || position === Position.RIGHT_BOTTOM;
	}
	exports.isPositionVertical = isPositionVertical;
	
	//# sourceMappingURL=position.js.map


/***/ }),
/* 14 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	/** A collection of curated prop keys used across our Components which are not valid HTMLElement props. */
	var INVALID_PROPS = [
	    "active",
	    "containerRef",
	    "elementRef",
	    "iconName",
	    "inputRef",
	    "intent",
	    "inline",
	    "loading",
	    "leftIconName",
	    "onChildrenMount",
	    "onRemove",
	    "rightElement",
	    "rightIconName",
	    "text",
	];
	/**
	 * Typically applied to HTMLElements to filter out blacklisted props. When applied to a Component,
	 * can filter props from being passed down to the children. Can also filter by a combined list of
	 * supplied prop keys and the blacklist (only appropriate for HTMLElements).
	 * @param props The original props object to filter down.
	 * @param {string[]} invalidProps If supplied, overwrites the default blacklist.
	 * @param {boolean} shouldMerge If true, will merge supplied invalidProps and blacklist together.
	 */
	function removeNonHTMLProps(props, invalidProps, shouldMerge) {
	    if (invalidProps === void 0) { invalidProps = INVALID_PROPS; }
	    if (shouldMerge === void 0) { shouldMerge = false; }
	    if (shouldMerge) {
	        invalidProps = invalidProps.concat(INVALID_PROPS);
	    }
	    return invalidProps.reduce(function (prev, curr) {
	        if (prev.hasOwnProperty(curr)) {
	            delete prev[curr];
	        }
	        return prev;
	    }, tslib_1.__assign({}, props));
	}
	exports.removeNonHTMLProps = removeNonHTMLProps;
	
	//# sourceMappingURL=props.js.map


/***/ }),
/* 15 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var position_1 = __webpack_require__(13);
	// per https://github.com/HubSpot/tether/pull/204, Tether now exposes a `bodyElement` option that,
	// when present, gets the tethered element injected into *it* instead of into the document body.
	// but both approaches still cause React to freak out, because it loses its handle on the DOM
	// element. thus, we pass a fake HTML bodyElement to Tether, with a no-op `appendChild` function
	// (the only function the library uses from bodyElement).
	var fakeHtmlElement = {
	    appendChild: function () { },
	};
	/** @internal */
	function createTetherOptions(element, target, position, tetherOptions) {
	    if (tetherOptions === void 0) { tetherOptions = {}; }
	    return tslib_1.__assign({}, tetherOptions, { attachment: getPopoverAttachment(position), bodyElement: fakeHtmlElement, classPrefix: "pt-tether", element: element,
	        target: target, targetAttachment: getTargetAttachment(position) });
	}
	exports.createTetherOptions = createTetherOptions;
	/** @internal */
	function getTargetAttachment(position) {
	    var attachments = (_a = {},
	        _a[position_1.Position.TOP_LEFT] = "top left",
	        _a[position_1.Position.TOP] = "top center",
	        _a[position_1.Position.TOP_RIGHT] = "top right",
	        _a[position_1.Position.RIGHT_TOP] = "top right",
	        _a[position_1.Position.RIGHT] = "middle right",
	        _a[position_1.Position.RIGHT_BOTTOM] = "bottom right",
	        _a[position_1.Position.BOTTOM_RIGHT] = "bottom right",
	        _a[position_1.Position.BOTTOM] = "bottom center",
	        _a[position_1.Position.BOTTOM_LEFT] = "bottom left",
	        _a[position_1.Position.LEFT_BOTTOM] = "bottom left",
	        _a[position_1.Position.LEFT] = "middle left",
	        _a[position_1.Position.LEFT_TOP] = "top left",
	        _a);
	    return attachments[position];
	    var _a;
	}
	exports.getTargetAttachment = getTargetAttachment;
	/** @internal */
	function getPopoverAttachment(position) {
	    var attachments = (_a = {},
	        _a[position_1.Position.TOP_LEFT] = "bottom left",
	        _a[position_1.Position.TOP] = "bottom center",
	        _a[position_1.Position.TOP_RIGHT] = "bottom right",
	        _a[position_1.Position.RIGHT_TOP] = "top left",
	        _a[position_1.Position.RIGHT] = "middle left",
	        _a[position_1.Position.RIGHT_BOTTOM] = "bottom left",
	        _a[position_1.Position.BOTTOM_RIGHT] = "top right",
	        _a[position_1.Position.BOTTOM] = "top center",
	        _a[position_1.Position.BOTTOM_LEFT] = "top left",
	        _a[position_1.Position.LEFT_BOTTOM] = "bottom right",
	        _a[position_1.Position.LEFT] = "middle right",
	        _a[position_1.Position.LEFT_TOP] = "top right",
	        _a);
	    return attachments[position];
	    var _a;
	}
	exports.getPopoverAttachment = getPopoverAttachment;
	/** @internal */
	function getAttachmentClasses(position) {
	    // this essentially reimplements the Tether logic for attachment classes so the same styles
	    // can be reused outside of Tether-based popovers.
	    return expandAttachmentClasses(getPopoverAttachment(position), "pt-tether-element-attached").concat(expandAttachmentClasses(getTargetAttachment(position), "pt-tether-target-attached"));
	}
	exports.getAttachmentClasses = getAttachmentClasses;
	function expandAttachmentClasses(attachments, prefix) {
	    var _a = attachments.split(" "), verticalAlign = _a[0], horizontalAlign = _a[1];
	    return [prefix + "-" + verticalAlign, prefix + "-" + horizontalAlign];
	}
	
	//# sourceMappingURL=tetherUtils.js.map


/***/ }),
/* 16 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var intent_1 = __webpack_require__(12);
	// modifiers
	exports.DARK = "pt-dark";
	exports.ACTIVE = "pt-active";
	exports.MINIMAL = "pt-minimal";
	exports.DISABLED = "pt-disabled";
	exports.SMALL = "pt-small";
	exports.LARGE = "pt-large";
	exports.LOADING = "pt-loading";
	exports.INTERACTIVE = "pt-interactive";
	exports.ALIGN_LEFT = "pt-align-left";
	exports.ALIGN_RIGHT = "pt-align-right";
	exports.INLINE = "pt-inline";
	exports.FILL = "pt-fill";
	exports.FIXED = "pt-fixed";
	exports.FIXED_TOP = "pt-fixed-top";
	exports.VERTICAL = "pt-vertical";
	exports.ROUND = "pt-round";
	// text utilities
	exports.TEXT_MUTED = "pt-text-muted";
	exports.TEXT_OVERFLOW_ELLIPSIS = "pt-text-overflow-ellipsis";
	exports.UI_TEXT_LARGE = "pt-ui-text-large";
	// components
	exports.ALERT = "pt-alert";
	exports.ALERT_BODY = "pt-alert-body";
	exports.ALERT_CONTENTS = "pt-alert-contents";
	exports.ALERT_FOOTER = "pt-alert-footer";
	exports.BREADCRUMB = "pt-breadcrumb";
	exports.BREADCRUMB_CURRENT = "pt-breadcrumb-current";
	exports.BREADCRUMBS = "pt-breadcrumbs";
	exports.BREADCRUMBS_COLLAPSED = "pt-breadcrumbs-collapsed";
	exports.BUTTON = "pt-button";
	exports.BUTTON_GROUP = "pt-button-group";
	exports.CALLOUT = "pt-callout";
	exports.CARD = "pt-card";
	exports.COLLAPSE = "pt-collapse";
	exports.COLLAPSIBLE_LIST = "pt-collapse-list";
	exports.CONTEXT_MENU = "pt-context-menu";
	exports.CONTEXT_MENU_POPOVER_TARGET = "pt-context-menu-popover-target";
	exports.CONTROL = "pt-control";
	exports.CONTROL_GROUP = "pt-control-group";
	exports.CONTROL_INDICATOR = "pt-control-indicator";
	exports.DIALOG = "pt-dialog";
	exports.DIALOG_CONTAINER = "pt-dialog-container";
	exports.DIALOG_BODY = "pt-dialog-body";
	exports.DIALOG_CLOSE_BUTTON = "pt-dialog-close-button";
	exports.DIALOG_FOOTER = "pt-dialog-footer";
	exports.DIALOG_FOOTER_ACTIONS = "pt-dialog-footer-actions";
	exports.DIALOG_HEADER = "pt-dialog-header";
	exports.EDITABLE_TEXT = "pt-editable-text";
	exports.ELEVATION_0 = "pt-elevation-0";
	exports.ELEVATION_1 = "pt-elevation-1";
	exports.ELEVATION_2 = "pt-elevation-2";
	exports.ELEVATION_3 = "pt-elevation-3";
	exports.ELEVATION_4 = "pt-elevation-4";
	exports.INPUT = "pt-input";
	exports.INPUT_GROUP = "pt-input-group";
	exports.CHECKBOX = "pt-checkbox";
	exports.RADIO = "pt-radio";
	exports.SWITCH = "pt-switch";
	exports.FILE_UPLOAD = "pt-file-upload";
	exports.FILE_UPLOAD_INPUT = "pt-file-upload-input";
	exports.INTENT_PRIMARY = "pt-intent-primary";
	exports.INTENT_SUCCESS = "pt-intent-success";
	exports.INTENT_WARNING = "pt-intent-warning";
	exports.INTENT_DANGER = "pt-intent-danger";
	exports.LABEL = "pt-label";
	exports.FORM_GROUP = "pt-form-group";
	exports.FORM_CONTENT = "pt-form-content";
	exports.FORM_HELPER_TEXT = "pt-form-helper-text";
	exports.MENU = "pt-menu";
	exports.MENU_ITEM = "pt-menu-item";
	exports.MENU_ITEM_LABEL = "pt-menu-item-label";
	exports.MENU_SUBMENU = "pt-submenu";
	exports.MENU_DIVIDER = "pt-menu-divider";
	exports.MENU_HEADER = "pt-menu-header";
	exports.NAVBAR = "pt-navbar";
	exports.NAVBAR_GROUP = "pt-navbar-group";
	exports.NAVBAR_HEADING = "pt-navbar-heading";
	exports.NAVBAR_DIVIDER = "pt-navbar-divider";
	exports.NON_IDEAL_STATE = "pt-non-ideal-state";
	exports.NON_IDEAL_STATE_ACTION = "pt-non-ideal-state-action";
	exports.NON_IDEAL_STATE_DESCRIPTION = "pt-non-ideal-state-description";
	exports.NON_IDEAL_STATE_ICON = "pt-non-ideal-state-icon";
	exports.NON_IDEAL_STATE_TITLE = "pt-non-ideal-state-title";
	exports.NON_IDEAL_STATE_VISUAL = "pt-non-ideal-state-visual";
	exports.NUMERIC_INPUT = "pt-numeric-input";
	exports.OVERLAY = "pt-overlay";
	exports.OVERLAY_BACKDROP = "pt-overlay-backdrop";
	exports.OVERLAY_CONTENT = "pt-overlay-content";
	exports.OVERLAY_INLINE = "pt-overlay-inline";
	exports.OVERLAY_OPEN = "pt-overlay-open";
	exports.OVERLAY_SCROLL_CONTAINER = "pt-overlay-scroll-container";
	exports.POPOVER = "pt-popover";
	exports.POPOVER_ARROW = "pt-popover-arrow";
	exports.POPOVER_BACKDROP = "pt-popover-backdrop";
	exports.POPOVER_CONTENT = "pt-popover-content";
	exports.POPOVER_DISMISS = "pt-popover-dismiss";
	exports.POPOVER_DISMISS_OVERRIDE = "pt-popover-dismiss-override";
	exports.POPOVER_OPEN = "pt-popover-open";
	exports.POPOVER_TARGET = "pt-popover-target";
	exports.TRANSITION_CONTAINER = "pt-transition-container";
	exports.PROGRESS_BAR = "pt-progress-bar";
	exports.PROGRESS_METER = "pt-progress-meter";
	exports.PROGRESS_NO_STRIPES = "pt-no-stripes";
	exports.PROGRESS_NO_ANIMATION = "pt-no-animation";
	exports.PORTAL = "pt-portal";
	exports.SELECT = "pt-select";
	exports.SKELETON = "pt-skeleton";
	exports.SLIDER = "pt-slider";
	exports.SLIDER_HANDLE = exports.SLIDER + "-handle";
	exports.SLIDER_LABEL = exports.SLIDER + "-label";
	exports.RANGE_SLIDER = "pt-range-slider";
	exports.SPINNER = "pt-spinner";
	exports.SVG_SPINNER = "pt-svg-spinner";
	exports.TAB = "pt-tab";
	exports.TAB_LIST = "pt-tab-list";
	exports.TAB_PANEL = "pt-tab-panel";
	exports.TABS = "pt-tabs";
	exports.TABLE = "pt-table";
	exports.TABLE_CONDENSED = "pt-condensed";
	exports.TABLE_STRIPED = "pt-striped";
	exports.TABLE_BORDERED = "pt-bordered";
	exports.TAG = "pt-tag";
	exports.TAG_REMOVABLE = "pt-tag-removable";
	exports.TAG_REMOVE = "pt-tag-remove";
	exports.TOAST = "pt-toast";
	exports.TOAST_CONTAINER = "pt-toast-container";
	exports.TOAST_MESSAGE = "pt-toast-message";
	exports.TOOLTIP = "pt-tooltip";
	exports.TREE = "pt-tree";
	exports.TREE_NODE = "pt-tree-node";
	exports.TREE_NODE_CARET = "pt-tree-node-caret";
	exports.TREE_NODE_CARET_CLOSED = "pt-tree-node-caret-closed";
	exports.TREE_NODE_CARET_NONE = "pt-tree-node-caret-none";
	exports.TREE_NODE_CARET_OPEN = "pt-tree-node-caret-open";
	exports.TREE_NODE_CONTENT = "pt-tree-node-content";
	exports.TREE_NODE_EXPANDED = "pt-tree-node-expanded";
	exports.TREE_NODE_ICON = "pt-tree-node-icon";
	exports.TREE_NODE_LABEL = "pt-tree-node-label";
	exports.TREE_NODE_LIST = "pt-tree-node-list";
	exports.TREE_NODE_SECONDARY_LABEL = "pt-tree-node-secondary-label";
	exports.TREE_NODE_SELECTED = "pt-tree-node-selected";
	exports.TREE_ROOT = "pt-tree-root";
	exports.ICON = "pt-icon";
	exports.ICON_STANDARD = "pt-icon-standard";
	exports.ICON_LARGE = "pt-icon-large";
	/** Return CSS class for icon, whether or not 'pt-icon-' prefix is included */
	function iconClass(iconName) {
	    if (iconName == null) {
	        return undefined;
	    }
	    return iconName.indexOf("pt-icon-") === 0 ? iconName : "pt-icon-" + iconName;
	}
	exports.iconClass = iconClass;
	function intentClass(intent) {
	    if (intent === void 0) { intent = intent_1.Intent.NONE; }
	    if (intent === intent_1.Intent.NONE || intent_1.Intent[intent] == null) {
	        return undefined;
	    }
	    return "pt-intent-" + intent_1.Intent[intent].toLowerCase();
	}
	exports.intentClass = intentClass;
	
	//# sourceMappingURL=classes.js.map


/***/ }),
/* 17 */
/***/ (function(module, exports) {

	/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	exports.BACKSPACE = 8;
	exports.TAB = 9;
	exports.ENTER = 13;
	exports.SHIFT = 16;
	exports.ESCAPE = 27;
	exports.SPACE = 32;
	exports.ARROW_LEFT = 37;
	exports.ARROW_UP = 38;
	exports.ARROW_RIGHT = 39;
	exports.ARROW_DOWN = 40;
	exports.DELETE = 46;
	
	//# sourceMappingURL=keys.js.map


/***/ }),
/* 18 */
/***/ (function(module, exports) {

	/*
	 * Copyright 2015-present Palantir Technologies, Inc. All rights reserved.
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	// tslint:disable:object-literal-sort-keys
	exports.IconClasses = {
	    BLANK: "pt-icon-blank",
	    STYLE: "pt-icon-style",
	    ALIGN_LEFT: "pt-icon-align-left",
	    ALIGN_CENTER: "pt-icon-align-center",
	    ALIGN_RIGHT: "pt-icon-align-right",
	    ALIGN_JUSTIFY: "pt-icon-align-justify",
	    BOLD: "pt-icon-bold",
	    ITALIC: "pt-icon-italic",
	    UNDERLINE: "pt-icon-underline",
	    SEARCH_AROUND: "pt-icon-search-around",
	    GRAPH_REMOVE: "pt-icon-graph-remove",
	    GROUP_OBJECTS: "pt-icon-group-objects",
	    MERGE_LINKS: "pt-icon-merge-links",
	    LAYOUT: "pt-icon-layout",
	    LAYOUT_AUTO: "pt-icon-layout-auto",
	    LAYOUT_CIRCLE: "pt-icon-layout-circle",
	    LAYOUT_HIERARCHY: "pt-icon-layout-hierarchy",
	    LAYOUT_GRID: "pt-icon-layout-grid",
	    LAYOUT_GROUP_BY: "pt-icon-layout-group-by",
	    LAYOUT_SKEW_GRID: "pt-icon-layout-skew-grid",
	    GEOSEARCH: "pt-icon-geosearch",
	    HEATMAP: "pt-icon-heatmap",
	    DRIVE_TIME: "pt-icon-drive-time",
	    SELECT: "pt-icon-select",
	    PREDICTIVE_ANALYSIS: "pt-icon-predictive-analysis",
	    LAYERS: "pt-icon-layers",
	    LOCATE: "pt-icon-locate",
	    BOOKMARK: "pt-icon-bookmark",
	    CITATION: "pt-icon-citation",
	    TAG: "pt-icon-tag",
	    CLIPBOARD: "pt-icon-clipboard",
	    SELECTION: "pt-icon-selection",
	    TIMELINE_EVENTS: "pt-icon-timeline-events",
	    TIMELINE_LINE_CHART: "pt-icon-timeline-line-chart",
	    TIMELINE_BAR_CHART: "pt-icon-timeline-bar-chart",
	    APPLICATIONS: "pt-icon-applications",
	    PROJECTS: "pt-icon-projects",
	    CHANGES: "pt-icon-changes",
	    NOTIFICATIONS: "pt-icon-notifications",
	    LOCK: "pt-icon-lock",
	    UNLOCK: "pt-icon-unlock",
	    USER: "pt-icon-user",
	    SEARCH_TEMPLATE: "pt-icon-search-template",
	    INBOX: "pt-icon-inbox",
	    MORE: "pt-icon-more",
	    HELP: "pt-icon-help",
	    CALENDAR: "pt-icon-calendar",
	    MEDIA: "pt-icon-media",
	    LINK: "pt-icon-link",
	    SHARE: "pt-icon-share",
	    DOWNLOAD: "pt-icon-download",
	    DOCUMENT: "pt-icon-document",
	    PROPERTIES: "pt-icon-properties",
	    IMPORT: "pt-icon-import",
	    EXPORT: "pt-icon-export",
	    MINIMIZE: "pt-icon-minimize",
	    MAXIMIZE: "pt-icon-maximize",
	    TICK: "pt-icon-tick",
	    CROSS: "pt-icon-cross",
	    PLUS: "pt-icon-plus",
	    MINUS: "pt-icon-minus",
	    ARROW_LEFT: "pt-icon-arrow-left",
	    ARROW_RIGHT: "pt-icon-arrow-right",
	    EXCHANGE: "pt-icon-exchange",
	    COMPARISON: "pt-icon-comparison",
	    LIST: "pt-icon-list",
	    FILTER: "pt-icon-filter",
	    CONFIRM: "pt-icon-confirm",
	    FORK: "pt-icon-fork",
	    TRASH: "pt-icon-trash",
	    PERSON: "pt-icon-person",
	    PEOPLE: "pt-icon-people",
	    ADD: "pt-icon-add",
	    REMOVE: "pt-icon-remove",
	    GEOLOCATION: "pt-icon-geolocation",
	    ZOOM_IN: "pt-icon-zoom-in",
	    ZOOM_OUT: "pt-icon-zoom-out",
	    REFRESH: "pt-icon-refresh",
	    DELETE: "pt-icon-delete",
	    COG: "pt-icon-cog",
	    FLAG: "pt-icon-flag",
	    PIN: "pt-icon-pin",
	    WARNING_SIGN: "pt-icon-warning-sign",
	    ERROR: "pt-icon-error",
	    INFO_SIGN: "pt-icon-info-sign",
	    CREDIT_CARD: "pt-icon-credit-card",
	    EDIT: "pt-icon-edit",
	    HISTORY: "pt-icon-history",
	    SEARCH: "pt-icon-search",
	    LOG_OUT: "pt-icon-log-out",
	    STAR: "pt-icon-star",
	    STAR_EMPTY: "pt-icon-star-empty",
	    SORT_ALPHABETICAL: "pt-icon-sort-alphabetical",
	    SORT_NUMERICAL: "pt-icon-sort-numerical",
	    SORT: "pt-icon-sort",
	    FOLDER_OPEN: "pt-icon-folder-open",
	    FOLDER_CLOSE: "pt-icon-folder-close",
	    FOLDER_SHARED: "pt-icon-folder-shared",
	    CARET_UP: "pt-icon-caret-up",
	    CARET_RIGHT: "pt-icon-caret-right",
	    CARET_DOWN: "pt-icon-caret-down",
	    CARET_LEFT: "pt-icon-caret-left",
	    MENU_OPEN: "pt-icon-menu-open",
	    MENU_CLOSED: "pt-icon-menu-closed",
	    FEED: "pt-icon-feed",
	    TWO_COLUMNS: "pt-icon-two-columns",
	    ONE_COLUMN: "pt-icon-one-column",
	    DOT: "pt-icon-dot",
	    PROPERTY: "pt-icon-property",
	    TIME: "pt-icon-time",
	    DISABLE: "pt-icon-disable",
	    UNPIN: "pt-icon-unpin",
	    FLOWS: "pt-icon-flows",
	    NEW_TEXT_BOX: "pt-icon-new-text-box",
	    NEW_LINK: "pt-icon-new-link",
	    NEW_OBJECT: "pt-icon-new-object",
	    PATH_SEARCH: "pt-icon-path-search",
	    AUTOMATIC_UPDATES: "pt-icon-automatic-updates",
	    PAGE_LAYOUT: "pt-icon-page-layout",
	    CODE: "pt-icon-code",
	    MAP: "pt-icon-map",
	    SEARCH_TEXT: "pt-icon-search-text",
	    ENVELOPE: "pt-icon-envelope",
	    PAPERCLIP: "pt-icon-paperclip",
	    LABEL: "pt-icon-label",
	    GLOBE: "pt-icon-globe",
	    HOME: "pt-icon-home",
	    TH: "pt-icon-th",
	    TH_LIST: "pt-icon-th-list",
	    TH_DERIVED: "pt-icon-th-derived",
	    CIRCLE: "pt-icon-circle",
	    DRAW: "pt-icon-draw",
	    INSERT: "pt-icon-insert",
	    HELPER_MANAGEMENT: "pt-icon-helper-management",
	    SEND_TO: "pt-icon-send-to",
	    EYE_OPEN: "pt-icon-eye-open",
	    FOLDER_SHARED_OPEN: "pt-icon-folder-shared-open",
	    SOCIAL_MEDIA: "pt-icon-social-media",
	    ARROW_UP: "pt-icon-arrow-up",
	    ARROW_DOWN: "pt-icon-arrow-down",
	    ARROWS_HORIZONTAL: "pt-icon-arrows-horizontal",
	    ARROWS_VERTICAL: "pt-icon-arrows-vertical",
	    RESOLVE: "pt-icon-resolve",
	    GRAPH: "pt-icon-graph",
	    BRIEFCASE: "pt-icon-briefcase",
	    DOLLAR: "pt-icon-dollar",
	    NINJA: "pt-icon-ninja",
	    DELTA: "pt-icon-delta",
	    BARCODE: "pt-icon-barcode",
	    TORCH: "pt-icon-torch",
	    WIDGET: "pt-icon-widget",
	    UNRESOLVE: "pt-icon-unresolve",
	    OFFLINE: "pt-icon-offline",
	    ZOOM_TO_FIT: "pt-icon-zoom-to-fit",
	    ADD_TO_ARTIFACT: "pt-icon-add-to-artifact",
	    MAP_MARKER: "pt-icon-map-marker",
	    CHART: "pt-icon-chart",
	    CONTROL: "pt-icon-control",
	    MULTI_SELECT: "pt-icon-multi-select",
	    DIRECTION_LEFT: "pt-icon-direction-left",
	    DIRECTION_RIGHT: "pt-icon-direction-right",
	    DATABASE: "pt-icon-database",
	    PIE_CHART: "pt-icon-pie-chart",
	    FULL_CIRCLE: "pt-icon-full-circle",
	    SQUARE: "pt-icon-square",
	    PRINT: "pt-icon-print",
	    PRESENTATION: "pt-icon-presentation",
	    UNGROUP_OBJECTS: "pt-icon-ungroup-objects",
	    CHAT: "pt-icon-chat",
	    COMMENT: "pt-icon-comment",
	    CIRCLE_ARROW_RIGHT: "pt-icon-circle-arrow-right",
	    CIRCLE_ARROW_LEFT: "pt-icon-circle-arrow-left",
	    CIRCLE_ARROW_UP: "pt-icon-circle-arrow-up",
	    CIRCLE_ARROW_DOWN: "pt-icon-circle-arrow-down",
	    UPLOAD: "pt-icon-upload",
	    ASTERISK: "pt-icon-asterisk",
	    CLOUD: "pt-icon-cloud",
	    CLOUD_DOWNLOAD: "pt-icon-cloud-download",
	    CLOUD_UPLOAD: "pt-icon-cloud-upload",
	    REPEAT: "pt-icon-repeat",
	    MOVE: "pt-icon-move",
	    CHEVRON_LEFT: "pt-icon-chevron-left",
	    CHEVRON_RIGHT: "pt-icon-chevron-right",
	    CHEVRON_UP: "pt-icon-chevron-up",
	    CHEVRON_DOWN: "pt-icon-chevron-down",
	    RANDOM: "pt-icon-random",
	    FULLSCREEN: "pt-icon-fullscreen",
	    LOG_IN: "pt-icon-log-in",
	    HEART: "pt-icon-heart",
	    OFFICE: "pt-icon-office",
	    DUPLICATE: "pt-icon-duplicate",
	    BAN_CIRCLE: "pt-icon-ban-circle",
	    CAMERA: "pt-icon-camera",
	    MOBILE_VIDEO: "pt-icon-mobile-video",
	    VIDEO: "pt-icon-video",
	    FILM: "pt-icon-film",
	    SETTINGS: "pt-icon-settings",
	    VOLUME_OFF: "pt-icon-volume-off",
	    VOLUME_DOWN: "pt-icon-volume-down",
	    VOLUME_UP: "pt-icon-volume-up",
	    MUSIC: "pt-icon-music",
	    STEP_BACKWARD: "pt-icon-step-backward",
	    FAST_BACKWARD: "pt-icon-fast-backward",
	    PAUSE: "pt-icon-pause",
	    STOP: "pt-icon-stop",
	    PLAY: "pt-icon-play",
	    FAST_FORWARD: "pt-icon-fast-forward",
	    STEP_FORWARD: "pt-icon-step-forward",
	    EJECT: "pt-icon-eject",
	    RECORD: "pt-icon-record",
	    DESKTOP: "pt-icon-desktop",
	    PHONE: "pt-icon-phone",
	    LIGHTBULB: "pt-icon-lightbulb",
	    GLASS: "pt-icon-glass",
	    TINT: "pt-icon-tint",
	    FLASH: "pt-icon-flash",
	    FONT: "pt-icon-font",
	    HEADER: "pt-icon-header",
	    SAVED: "pt-icon-saved",
	    FLOPPY_DISK: "pt-icon-floppy-disk",
	    BOOK: "pt-icon-book",
	    HAND_RIGHT: "pt-icon-hand-right",
	    HAND_UP: "pt-icon-hand-up",
	    HAND_DOWN: "pt-icon-hand-down",
	    HAND_LEFT: "pt-icon-hand-left",
	    THUMBS_UP: "pt-icon-thumbs-up",
	    THUMBS_DOWN: "pt-icon-thumbs-down",
	    BOX: "pt-icon-box",
	    COMPRESSED: "pt-icon-compressed",
	    SHOPPING_CART: "pt-icon-shopping-cart",
	    SHOP: "pt-icon-shop",
	    LAYOUT_LINEAR: "pt-icon-layout-linear",
	    UNDO: "pt-icon-undo",
	    REDO: "pt-icon-redo",
	    CODE_BLOCK: "pt-icon-code-block",
	    DOUBLE_CARET_VERTICAL: "pt-icon-double-caret-vertical",
	    DOUBLE_CARET_HORIZONTAL: "pt-icon-double-caret-horizontal",
	    SORT_ALPHABETICAL_DESC: "pt-icon-sort-alphabetical-desc",
	    SORT_NUMERICAL_DESC: "pt-icon-sort-numerical-desc",
	    TAKE_ACTION: "pt-icon-take-action",
	    CONTRAST: "pt-icon-contrast",
	    EYE_OFF: "pt-icon-eye-off",
	    TIMELINE_AREA_CHART: "pt-icon-timeline-area-chart",
	    DOUGHNUT_CHART: "pt-icon-doughnut-chart",
	    LAYER: "pt-icon-layer",
	    GRID: "pt-icon-grid",
	    POLYGON_FILTER: "pt-icon-polygon-filter",
	    ADD_TO_FOLDER: "pt-icon-add-to-folder",
	    LAYOUT_BALLOON: "pt-icon-layout-balloon",
	    LAYOUT_SORTED_CLUSTERS: "pt-icon-layout-sorted-clusters",
	    SORT_ASC: "pt-icon-sort-asc",
	    SORT_DESC: "pt-icon-sort-desc",
	    SMALL_CROSS: "pt-icon-small-cross",
	    SMALL_TICK: "pt-icon-small-tick",
	    POWER: "pt-icon-power",
	    COLUMN_LAYOUT: "pt-icon-column-layout",
	    ARROW_TOP_LEFT: "pt-icon-arrow-top-left",
	    ARROW_TOP_RIGHT: "pt-icon-arrow-top-right",
	    ARROW_BOTTOM_RIGHT: "pt-icon-arrow-bottom-right",
	    ARROW_BOTTOM_LEFT: "pt-icon-arrow-bottom-left",
	    MUGSHOT: "pt-icon-mugshot",
	    HEADSET: "pt-icon-headset",
	    TEXT_HIGHLIGHT: "pt-icon-text-highlight",
	    HAND: "pt-icon-hand",
	    CHEVRON_BACKWARD: "pt-icon-chevron-backward",
	    CHEVRON_FORWARD: "pt-icon-chevron-forward",
	    ROTATE_DOCUMENT: "pt-icon-rotate-document",
	    ROTATE_PAGE: "pt-icon-rotate-page",
	    BADGE: "pt-icon-badge",
	    GRID_VIEW: "pt-icon-grid-view",
	    FUNCTION: "pt-icon-function",
	    WATERFALL_CHART: "pt-icon-waterfall-chart",
	    STACKED_CHART: "pt-icon-stacked-chart",
	    PULSE: "pt-icon-pulse",
	    NEW_PERSON: "pt-icon-new-person",
	    EXCLUDE_ROW: "pt-icon-exclude-row",
	    PIVOT_TABLE: "pt-icon-pivot-table",
	    SEGMENTED_CONTROL: "pt-icon-segmented-control",
	    HIGHLIGHT: "pt-icon-highlight",
	    FILTER_LIST: "pt-icon-filter-list",
	    CUT: "pt-icon-cut",
	    ANNOTATION: "pt-icon-annotation",
	    PIVOT: "pt-icon-pivot",
	    RING: "pt-icon-ring",
	    HEAT_GRID: "pt-icon-heat-grid",
	    GANTT_CHART: "pt-icon-gantt-chart",
	    VARIABLE: "pt-icon-variable",
	    MANUAL: "pt-icon-manual",
	    ADD_ROW_TOP: "pt-icon-add-row-top",
	    ADD_ROW_BOTTOM: "pt-icon-add-row-bottom",
	    ADD_COLUMN_LEFT: "pt-icon-add-column-left",
	    ADD_COLUMN_RIGHT: "pt-icon-add-column-right",
	    REMOVE_ROW_TOP: "pt-icon-remove-row-top",
	    REMOVE_ROW_BOTTOM: "pt-icon-remove-row-bottom",
	    REMOVE_COLUMN_LEFT: "pt-icon-remove-column-left",
	    REMOVE_COLUMN_RIGHT: "pt-icon-remove-column-right",
	    DOUBLE_CHEVRON_LEFT: "pt-icon-double-chevron-left",
	    DOUBLE_CHEVRON_RIGHT: "pt-icon-double-chevron-right",
	    DOUBLE_CHEVRON_UP: "pt-icon-double-chevron-up",
	    DOUBLE_CHEVRON_DOWN: "pt-icon-double-chevron-down",
	    KEY_CONTROL: "pt-icon-key-control",
	    KEY_COMMAND: "pt-icon-key-command",
	    KEY_SHIFT: "pt-icon-key-shift",
	    KEY_BACKSPACE: "pt-icon-key-backspace",
	    KEY_DELETE: "pt-icon-key-delete",
	    KEY_ESCAPE: "pt-icon-key-escape",
	    KEY_ENTER: "pt-icon-key-enter",
	    CALCULATOR: "pt-icon-calculator",
	    HORIZONTAL_BAR_CHART: "pt-icon-horizontal-bar-chart",
	    SMALL_PLUS: "pt-icon-small-plus",
	    SMALL_MINUS: "pt-icon-small-minus",
	    STEP_CHART: "pt-icon-step-chart",
	    EURO: "pt-icon-euro",
	    DRAG_HANDLE_VERTICAL: "pt-icon-drag-handle-vertical",
	    DRAG_HANDLE_HORIZONTAL: "pt-icon-drag-handle-horizontal",
	    MOBILE_PHONE: "pt-icon-mobile-phone",
	    SIM_CARD: "pt-icon-sim-card",
	    TRENDING_UP: "pt-icon-trending-up",
	    TRENDING_DOWN: "pt-icon-trending-down",
	    CURVED_RANGE_CHART: "pt-icon-curved-range-chart",
	    VERTICAL_BAR_CHART_DESC: "pt-icon-vertical-bar-chart-desc",
	    HORIZONTAL_BAR_CHART_DESC: "pt-icon-horizontal-bar-chart-desc",
	    DOCUMENT_OPEN: "pt-icon-document-open",
	    DOCUMENT_SHARE: "pt-icon-document-share",
	    HORIZONTAL_DISTRIBUTION: "pt-icon-horizontal-distribution",
	    VERTICAL_DISTRIBUTION: "pt-icon-vertical-distribution",
	    ALIGNMENT_LEFT: "pt-icon-alignment-left",
	    ALIGNMENT_VERTICAL_CENTER: "pt-icon-alignment-vertical-center",
	    ALIGNMENT_RIGHT: "pt-icon-alignment-right",
	    ALIGNMENT_TOP: "pt-icon-alignment-top",
	    ALIGNMENT_HORIZONTAL_CENTER: "pt-icon-alignment-horizontal-center",
	    ALIGNMENT_BOTTOM: "pt-icon-alignment-bottom",
	    GIT_PULL: "pt-icon-git-pull",
	    GIT_MERGE: "pt-icon-git-merge",
	    GIT_BRANCH: "pt-icon-git-branch",
	    GIT_COMMIT: "pt-icon-git-commit",
	    GIT_PUSH: "pt-icon-git-push",
	    BUILD: "pt-icon-build",
	    SYMBOL_CIRCLE: "pt-icon-symbol-circle",
	    SYMBOL_SQUARE: "pt-icon-symbol-square",
	    SYMBOL_DIAMOND: "pt-icon-symbol-diamond",
	    SYMBOL_CROSS: "pt-icon-symbol-cross",
	    SYMBOL_TRIANGLE_UP: "pt-icon-symbol-triangle-up",
	    SYMBOL_TRIANGLE_DOWN: "pt-icon-symbol-triangle-down",
	    WRENCH: "pt-icon-wrench",
	    APPLICATION: "pt-icon-application",
	    SEND_TO_GRAPH: "pt-icon-send-to-graph",
	    SEND_TO_MAP: "pt-icon-send-to-map",
	    JOIN_TABLE: "pt-icon-join-table",
	    DERIVE_COLUMN: "pt-icon-derive-column",
	    IMAGE_ROTATE_LEFT: "pt-icon-image-rotate-left",
	    IMAGE_ROTATE_RIGHT: "pt-icon-image-rotate-right",
	    KNOWN_VEHICLE: "pt-icon-known-vehicle",
	    UNKNOWN_VEHICLE: "pt-icon-unknown-vehicle",
	    SCATTER_PLOT: "pt-icon-scatter-plot",
	    OIL_FIELD: "pt-icon-oil-field",
	    RIG: "pt-icon-rig",
	    MAP_CREATE: "pt-icon-map-create",
	    KEY_OPTION: "pt-icon-key-option",
	    LIST_DETAIL_VIEW: "pt-icon-list-detail-view",
	    SWAP_VERTICAL: "pt-icon-swap-vertical",
	    SWAP_HORIZONTAL: "pt-icon-swap-horizontal",
	    NUMBERED_LIST: "pt-icon-numbered-list",
	    NEW_GRID_ITEM: "pt-icon-new-grid-item",
	    GIT_REPO: "pt-icon-git-repo",
	    GIT_NEW_BRANCH: "pt-icon-git-new-branch",
	    MANUALLY_ENTERED_DATA: "pt-icon-manually-entered-data",
	    AIRPLANE: "pt-icon-airplane",
	    MERGE_COLUMNS: "pt-icon-merge-columns",
	    SPLIT_COLUMNS: "pt-icon-split-columns",
	    DASHBOARD: "pt-icon-dashboard",
	    PUBLISH_FUNCTION: "pt-icon-publish-function",
	    PATH: "pt-icon-path",
	    MOON: "pt-icon-moon",
	    REMOVE_COLUMN: "pt-icon-remove-column",
	    NUMERICAL: "pt-icon-numerical",
	    KEY_TAB: "pt-icon-key-tab",
	    REGRESSION_CHART: "pt-icon-regression-chart",
	    TRANSLATE: "pt-icon-translate",
	    EYE_ON: "pt-icon-eye-on",
	    VERTICAL_BAR_CHART_ASC: "pt-icon-vertical-bar-chart-asc",
	    HORIZONTAL_BAR_CHART_ASC: "pt-icon-horizontal-bar-chart-asc",
	    GROUPED_BAR_CHART: "pt-icon-grouped-bar-chart",
	    FULL_STACKED_CHART: "pt-icon-full-stacked-chart",
	    ENDORSED: "pt-icon-endorsed",
	    FOLLOWER: "pt-icon-follower",
	    FOLLOWING: "pt-icon-following",
	    MENU: "pt-icon-menu",
	    COLLAPSE_ALL: "pt-icon-collapse-all",
	    EXPAND_ALL: "pt-icon-expand-all",
	    INTERSECTION: "pt-icon-intersection",
	    BLOCKED_PERSON: "pt-icon-blocked-person",
	    SLASH: "pt-icon-slash",
	    PERCENTAGE: "pt-icon-percentage",
	    SATELLITE: "pt-icon-satellite",
	    PARAGRAPH: "pt-icon-paragraph",
	    BANK_ACCOUNT: "pt-icon-bank-account",
	    CELL_TOWER: "pt-icon-cell-tower",
	    ID_NUMBER: "pt-icon-id-number",
	    IP_ADDRESS: "pt-icon-ip-address",
	    ERASER: "pt-icon-eraser",
	    ISSUE: "pt-icon-issue",
	    ISSUE_NEW: "pt-icon-issue-new",
	    ISSUE_CLOSED: "pt-icon-issue-closed",
	    PANEL_STATS: "pt-icon-panel-stats",
	    PANEL_TABLE: "pt-icon-panel-table",
	    TICK_CIRCLE: "pt-icon-tick-circle",
	    PRESCRIPTION: "pt-icon-prescription",
	    NEW_PRESCRIPTION: "pt-icon-new-prescription",
	};
	
	//# sourceMappingURL=iconClasses.js.map


/***/ }),
/* 19 */
/***/ (function(module, exports) {

	/*
	 * Copyright 2015-present Palantir Technologies, Inc. All rights reserved.
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	// tslint:disable:object-literal-sort-keys
	exports.IconContents = {
	    BLANK: "\ue900",
	    STYLE: "\ue601",
	    ALIGN_LEFT: "\ue602",
	    ALIGN_CENTER: "\ue603",
	    ALIGN_RIGHT: "\ue604",
	    ALIGN_JUSTIFY: "\ue605",
	    BOLD: "\ue606",
	    ITALIC: "\ue607",
	    UNDERLINE: "\u2381",
	    SEARCH_AROUND: "\ue608",
	    GRAPH_REMOVE: "\ue609",
	    GROUP_OBJECTS: "\ue60a",
	    MERGE_LINKS: "\ue60b",
	    LAYOUT: "\ue60c",
	    LAYOUT_AUTO: "\ue60d",
	    LAYOUT_CIRCLE: "\ue60e",
	    LAYOUT_HIERARCHY: "\ue60f",
	    LAYOUT_GRID: "\ue610",
	    LAYOUT_GROUP_BY: "\ue611",
	    LAYOUT_SKEW_GRID: "\ue612",
	    GEOSEARCH: "\ue613",
	    HEATMAP: "\ue614",
	    DRIVE_TIME: "\ue615",
	    SELECT: "\ue616",
	    PREDICTIVE_ANALYSIS: "\ue617",
	    LAYERS: "\ue618",
	    LOCATE: "\ue619",
	    BOOKMARK: "\ue61a",
	    CITATION: "\ue61b",
	    TAG: "\ue61c",
	    CLIPBOARD: "\ue61d",
	    SELECTION: "\u29bf",
	    TIMELINE_EVENTS: "\ue61e",
	    TIMELINE_LINE_CHART: "\ue61f",
	    TIMELINE_BAR_CHART: "\ue620",
	    APPLICATIONS: "\ue621",
	    PROJECTS: "\ue622",
	    CHANGES: "\ue623",
	    NOTIFICATIONS: "\ue624",
	    LOCK: "\ue625",
	    UNLOCK: "\ue626",
	    USER: "\ue627",
	    SEARCH_TEMPLATE: "\ue628",
	    INBOX: "\ue629",
	    MORE: "\ue62a",
	    HELP: "\u003F",
	    CALENDAR: "\ue62b",
	    MEDIA: "\ue62c",
	    LINK: "\ue62d",
	    SHARE: "\ue62e",
	    DOWNLOAD: "\ue62f",
	    DOCUMENT: "\ue630",
	    PROPERTIES: "\ue631",
	    IMPORT: "\ue632",
	    EXPORT: "\ue633",
	    MINIMIZE: "\ue634",
	    MAXIMIZE: "\ue635",
	    TICK: "\u2713",
	    CROSS: "\u2717",
	    PLUS: "\u002b",
	    MINUS: "\u2212",
	    ARROW_LEFT: "\u2190",
	    ARROW_RIGHT: "\u2192",
	    EXCHANGE: "\ue636",
	    COMPARISON: "\ue637",
	    LIST: "\u2630",
	    FILTER: "\ue638",
	    CONFIRM: "\ue639",
	    FORK: "\ue63a",
	    TRASH: "\ue63b",
	    PERSON: "\ue63c",
	    PEOPLE: "\ue63d",
	    ADD: "\ue63e",
	    REMOVE: "\ue63f",
	    GEOLOCATION: "\ue640",
	    ZOOM_IN: "\ue641",
	    ZOOM_OUT: "\ue642",
	    REFRESH: "\ue643",
	    DELETE: "\ue644",
	    COG: "\ue645",
	    FLAG: "\u2691",
	    PIN: "\ue646",
	    WARNING_SIGN: "\ue647",
	    ERROR: "\ue648",
	    INFO_SIGN: "\u2139",
	    CREDIT_CARD: "\ue649",
	    EDIT: "\u270E",
	    HISTORY: "\ue64a",
	    SEARCH: "\ue64b",
	    LOG_OUT: "\ue64c",
	    STAR: "\u2605",
	    STAR_EMPTY: "\u2606",
	    SORT_ALPHABETICAL: "\ue64d",
	    SORT_NUMERICAL: "\ue64e",
	    SORT: "\ue64f",
	    FOLDER_OPEN: "\ue651",
	    FOLDER_CLOSE: "\ue652",
	    FOLDER_SHARED: "\ue653",
	    CARET_UP: "\u2303",
	    CARET_RIGHT: "\u232A",
	    CARET_DOWN: "\u2304",
	    CARET_LEFT: "\u2329",
	    MENU_OPEN: "\ue654",
	    MENU_CLOSED: "\ue655",
	    FEED: "\ue656",
	    TWO_COLUMNS: "\ue657",
	    ONE_COLUMN: "\ue658",
	    DOT: "\u2022",
	    PROPERTY: "\ue65a",
	    TIME: "\u23F2",
	    DISABLE: "\ue600",
	    UNPIN: "\ue650",
	    FLOWS: "\ue659",
	    NEW_TEXT_BOX: "\ue65b",
	    NEW_LINK: "\ue65c",
	    NEW_OBJECT: "\ue65d",
	    PATH_SEARCH: "\ue65e",
	    AUTOMATIC_UPDATES: "\ue65f",
	    PAGE_LAYOUT: "\ue660",
	    CODE: "\ue661",
	    MAP: "\ue662",
	    SEARCH_TEXT: "\ue663",
	    ENVELOPE: "\u2709",
	    PAPERCLIP: "\ue664",
	    LABEL: "\ue665",
	    GLOBE: "\ue666",
	    HOME: "\u2302",
	    TH: "\ue667",
	    TH_LIST: "\ue668",
	    TH_DERIVED: "\ue669",
	    CIRCLE: "\ue66a",
	    DRAW: "\ue66b",
	    INSERT: "\ue66c",
	    HELPER_MANAGEMENT: "\ue66d",
	    SEND_TO: "\ue66e",
	    EYE_OPEN: "\ue66f",
	    FOLDER_SHARED_OPEN: "\ue670",
	    SOCIAL_MEDIA: "\ue671",
	    ARROW_UP: "\u2191 ",
	    ARROW_DOWN: "\u2193 ",
	    ARROWS_HORIZONTAL: "\u2194 ",
	    ARROWS_VERTICAL: "\u2195 ",
	    RESOLVE: "\ue672",
	    GRAPH: "\ue673",
	    BRIEFCASE: "\ue674",
	    DOLLAR: "\u0024",
	    NINJA: "\ue675",
	    DELTA: "\u0394",
	    BARCODE: "\ue676",
	    TORCH: "\ue677",
	    WIDGET: "\ue678",
	    UNRESOLVE: "\ue679",
	    OFFLINE: "\ue67a",
	    ZOOM_TO_FIT: "\ue67b",
	    ADD_TO_ARTIFACT: "\ue67c",
	    MAP_MARKER: "\ue67d",
	    CHART: "\ue67e",
	    CONTROL: "\ue67f",
	    MULTI_SELECT: "\ue680",
	    DIRECTION_LEFT: "\ue681",
	    DIRECTION_RIGHT: "\ue682",
	    DATABASE: "\ue683",
	    PIE_CHART: "\ue684",
	    FULL_CIRCLE: "\ue685",
	    SQUARE: "\ue686",
	    PRINT: "\u2399",
	    PRESENTATION: "\ue687",
	    UNGROUP_OBJECTS: "\ue688",
	    CHAT: "\ue689",
	    COMMENT: "\ue68a",
	    CIRCLE_ARROW_RIGHT: "\ue68b",
	    CIRCLE_ARROW_LEFT: "\ue68c",
	    CIRCLE_ARROW_UP: "\ue68d",
	    CIRCLE_ARROW_DOWN: "\ue68e",
	    UPLOAD: "\ue68f",
	    ASTERISK: "\u002a",
	    CLOUD: "\u2601",
	    CLOUD_DOWNLOAD: "\ue690",
	    CLOUD_UPLOAD: "\ue691",
	    REPEAT: "\ue692",
	    MOVE: "\ue693",
	    CHEVRON_LEFT: "\ue694",
	    CHEVRON_RIGHT: "\ue695",
	    CHEVRON_UP: "\ue696",
	    CHEVRON_DOWN: "\ue697",
	    RANDOM: "\ue698",
	    FULLSCREEN: "\ue699",
	    LOG_IN: "\ue69a",
	    HEART: "\u2665",
	    OFFICE: "\ue69b",
	    DUPLICATE: "\ue69c",
	    BAN_CIRCLE: "\ue69d",
	    CAMERA: "\ue69e",
	    MOBILE_VIDEO: "\ue69f",
	    VIDEO: "\ue6a0",
	    FILM: "\ue6a1",
	    SETTINGS: "\ue6a2",
	    VOLUME_OFF: "\ue6a3",
	    VOLUME_DOWN: "\ue6a4",
	    VOLUME_UP: "\ue6a5",
	    MUSIC: "\ue6a6",
	    STEP_BACKWARD: "\ue6a7",
	    FAST_BACKWARD: "\ue6a8",
	    PAUSE: "\ue6a9",
	    STOP: "\ue6aa",
	    PLAY: "\ue6ab",
	    FAST_FORWARD: "\ue6ac",
	    STEP_FORWARD: "\ue6ad",
	    EJECT: "\u23cf",
	    RECORD: "\ue6ae",
	    DESKTOP: "\ue6af",
	    PHONE: "\u260e",
	    LIGHTBULB: "\ue6b0",
	    GLASS: "\ue6b1",
	    TINT: "\ue6b2",
	    FLASH: "\ue6b3",
	    FONT: "\ue6b4",
	    HEADER: "\ue6b5",
	    SAVED: "\ue6b6",
	    FLOPPY_DISK: "\ue6b7",
	    BOOK: "\ue6b8",
	    HAND_RIGHT: "\ue6b9",
	    HAND_UP: "\ue6ba",
	    HAND_DOWN: "\ue6bb",
	    HAND_LEFT: "\ue6bc",
	    THUMBS_UP: "\ue6bd",
	    THUMBS_DOWN: "\ue6be",
	    BOX: "\ue6bf",
	    COMPRESSED: "\ue6c0",
	    SHOPPING_CART: "\ue6c1",
	    SHOP: "\ue6c2",
	    LAYOUT_LINEAR: "\ue6c3",
	    UNDO: "\u238c",
	    REDO: "\ue6c4",
	    CODE_BLOCK: "\ue6c5",
	    DOUBLE_CARET_VERTICAL: "\ue6c6",
	    DOUBLE_CARET_HORIZONTAL: "\ue6c7",
	    SORT_ALPHABETICAL_DESC: "\ue6c8",
	    SORT_NUMERICAL_DESC: "\ue6c9",
	    TAKE_ACTION: "\ue6ca",
	    CONTRAST: "\ue6cb",
	    EYE_OFF: "\ue6cc",
	    TIMELINE_AREA_CHART: "\ue6cd",
	    DOUGHNUT_CHART: "\ue6ce",
	    LAYER: "\ue6cf",
	    GRID: "\ue6d0",
	    POLYGON_FILTER: "\ue6d1",
	    ADD_TO_FOLDER: "\ue6d2",
	    LAYOUT_BALLOON: "\ue6d3",
	    LAYOUT_SORTED_CLUSTERS: "\ue6d4",
	    SORT_ASC: "\ue6d5",
	    SORT_DESC: "\ue6d6",
	    SMALL_CROSS: "\ue6d7",
	    SMALL_TICK: "\ue6d8",
	    POWER: "\ue6d9",
	    COLUMN_LAYOUT: "\ue6da",
	    ARROW_TOP_LEFT: "\u2196",
	    ARROW_TOP_RIGHT: "\u2197",
	    ARROW_BOTTOM_RIGHT: "\u2198",
	    ARROW_BOTTOM_LEFT: "\u2199",
	    MUGSHOT: "\ue6db",
	    HEADSET: "\ue6dc",
	    TEXT_HIGHLIGHT: "\ue6dd",
	    HAND: "\ue6de",
	    CHEVRON_BACKWARD: "\ue6df",
	    CHEVRON_FORWARD: "\ue6e0",
	    ROTATE_DOCUMENT: "\ue6e1",
	    ROTATE_PAGE: "\ue6e2",
	    BADGE: "\ue6e3",
	    GRID_VIEW: "\ue6e4",
	    FUNCTION: "\ue6e5",
	    WATERFALL_CHART: "\ue6e6",
	    STACKED_CHART: "\ue6e7",
	    PULSE: "\ue6e8",
	    NEW_PERSON: "\ue6e9",
	    EXCLUDE_ROW: "\ue6ea",
	    PIVOT_TABLE: "\ue6eb",
	    SEGMENTED_CONTROL: "\ue6ec",
	    HIGHLIGHT: "\ue6ed",
	    FILTER_LIST: "\ue6ee",
	    CUT: "\ue6ef",
	    ANNOTATION: "\ue6f0",
	    PIVOT: "\ue6f1",
	    RING: "\ue6f2",
	    HEAT_GRID: "\ue6f3",
	    GANTT_CHART: "\ue6f4",
	    VARIABLE: "\ue6f5",
	    MANUAL: "\ue6f6",
	    ADD_ROW_TOP: "\ue6f7",
	    ADD_ROW_BOTTOM: "\ue6f8",
	    ADD_COLUMN_LEFT: "\ue6f9",
	    ADD_COLUMN_RIGHT: "\ue6fa",
	    REMOVE_ROW_TOP: "\ue6fb",
	    REMOVE_ROW_BOTTOM: "\ue6fc",
	    REMOVE_COLUMN_LEFT: "\ue6fd",
	    REMOVE_COLUMN_RIGHT: "\ue6fe",
	    DOUBLE_CHEVRON_LEFT: "\ue6ff",
	    DOUBLE_CHEVRON_RIGHT: "\ue701",
	    DOUBLE_CHEVRON_UP: "\ue702",
	    DOUBLE_CHEVRON_DOWN: "\ue703",
	    KEY_CONTROL: "\ue704",
	    KEY_COMMAND: "\ue705",
	    KEY_SHIFT: "\ue706",
	    KEY_BACKSPACE: "\ue707",
	    KEY_DELETE: "\ue708",
	    KEY_ESCAPE: "\ue709",
	    KEY_ENTER: "\ue70a",
	    CALCULATOR: "\ue70b",
	    HORIZONTAL_BAR_CHART: "\ue70c",
	    SMALL_PLUS: "\ue70d",
	    SMALL_MINUS: "\ue70e",
	    STEP_CHART: "\ue70f",
	    EURO: "\u20ac",
	    DRAG_HANDLE_VERTICAL: "\ue715",
	    DRAG_HANDLE_HORIZONTAL: "\ue716",
	    MOBILE_PHONE: "\ue717",
	    SIM_CARD: "\ue718",
	    TRENDING_UP: "\ue719",
	    TRENDING_DOWN: "\ue71a",
	    CURVED_RANGE_CHART: "\ue71b",
	    VERTICAL_BAR_CHART_DESC: "\ue71c",
	    HORIZONTAL_BAR_CHART_DESC: "\ue71d",
	    DOCUMENT_OPEN: "\ue71e",
	    DOCUMENT_SHARE: "\ue71f",
	    HORIZONTAL_DISTRIBUTION: "\ue720",
	    VERTICAL_DISTRIBUTION: "\ue721",
	    ALIGNMENT_LEFT: "\ue722",
	    ALIGNMENT_VERTICAL_CENTER: "\ue723",
	    ALIGNMENT_RIGHT: "\ue724",
	    ALIGNMENT_TOP: "\ue725",
	    ALIGNMENT_HORIZONTAL_CENTER: "\ue726",
	    ALIGNMENT_BOTTOM: "\ue727",
	    GIT_PULL: "\ue728",
	    GIT_MERGE: "\ue729",
	    GIT_BRANCH: "\ue72a",
	    GIT_COMMIT: "\ue72b",
	    GIT_PUSH: "\ue72c",
	    BUILD: "\ue72d",
	    SYMBOL_CIRCLE: "\ue72e",
	    SYMBOL_SQUARE: "\ue72f",
	    SYMBOL_DIAMOND: "\ue730",
	    SYMBOL_CROSS: "\ue731",
	    SYMBOL_TRIANGLE_UP: "\ue732",
	    SYMBOL_TRIANGLE_DOWN: "\ue733",
	    WRENCH: "\ue734",
	    APPLICATION: "\ue735",
	    SEND_TO_GRAPH: "\ue736",
	    SEND_TO_MAP: "\ue737",
	    JOIN_TABLE: "\ue738",
	    DERIVE_COLUMN: "\ue739",
	    IMAGE_ROTATE_LEFT: "\ue73a",
	    IMAGE_ROTATE_RIGHT: "\ue73b",
	    KNOWN_VEHICLE: "\ue73c",
	    UNKNOWN_VEHICLE: "\ue73d",
	    SCATTER_PLOT: "\ue73e",
	    OIL_FIELD: "\ue73f",
	    RIG: "\ue740",
	    MAP_CREATE: "\ue741",
	    KEY_OPTION: "\ue742",
	    LIST_DETAIL_VIEW: "\ue743",
	    SWAP_VERTICAL: "\ue744",
	    SWAP_HORIZONTAL: "\ue745",
	    NUMBERED_LIST: "\ue746",
	    NEW_GRID_ITEM: "\ue747",
	    GIT_REPO: "\ue748",
	    GIT_NEW_BRANCH: "\ue749",
	    MANUALLY_ENTERED_DATA: "\ue74a",
	    AIRPLANE: "\ue74b",
	    MERGE_COLUMNS: "\ue74f",
	    SPLIT_COLUMNS: "\ue750",
	    DASHBOARD: "\ue751",
	    PUBLISH_FUNCTION: "\ue752",
	    PATH: "\ue753",
	    MOON: "\ue754",
	    REMOVE_COLUMN: "\ue755",
	    NUMERICAL: "\ue756",
	    KEY_TAB: "\ue757",
	    REGRESSION_CHART: "\ue758",
	    TRANSLATE: "\ue759",
	    EYE_ON: "\ue75a",
	    VERTICAL_BAR_CHART_ASC: "\ue75b",
	    HORIZONTAL_BAR_CHART_ASC: "\ue75c",
	    GROUPED_BAR_CHART: "\ue75d",
	    FULL_STACKED_CHART: "\ue75e",
	    ENDORSED: "\ue75f",
	    FOLLOWER: "\ue760",
	    FOLLOWING: "\ue761",
	    MENU: "\ue762",
	    COLLAPSE_ALL: "\ue763",
	    EXPAND_ALL: "\ue764",
	    INTERSECTION: "\ue765",
	    BLOCKED_PERSON: "\ue768",
	    SLASH: "\ue769",
	    PERCENTAGE: "\ue76a",
	    SATELLITE: "\ue76b",
	    PARAGRAPH: "\ue76c",
	    BANK_ACCOUNT: "\ue76f",
	    CELL_TOWER: "\ue770",
	    ID_NUMBER: "\ue771",
	    IP_ADDRESS: "\ue772",
	    ERASER: "\ue773",
	    ISSUE: "\ue774",
	    ISSUE_NEW: "\ue775",
	    ISSUE_CLOSED: "\ue776",
	    PANEL_STATS: "\ue777",
	    PANEL_TABLE: "\ue778",
	    TICK_CIRCLE: "\ue779",
	    PRESCRIPTION: "\ue78a",
	    NEW_PRESCRIPTION: "\ue78b",
	};
	
	//# sourceMappingURL=iconStrings.js.map


/***/ }),
/* 20 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	function __export(m) {
	    for (var p in m) if (!exports.hasOwnProperty(p)) exports[p] = m[p];
	}
	Object.defineProperty(exports, "__esModule", { value: true });
	if (typeof window !== "undefined" && typeof document !== "undefined") {
	    // tslint:disable-next-line:no-var-requires
	    __webpack_require__(21); // only import actual dom4 if we're in the browser (not server-compatible)
	    // we'll still need dom4 types for the TypeScript to compile, these are included in package.json
	}
	var contextMenu = __webpack_require__(22);
	exports.ContextMenu = contextMenu;
	__export(__webpack_require__(36));
	__export(__webpack_require__(42));
	__export(__webpack_require__(37));
	__export(__webpack_require__(43));
	__export(__webpack_require__(44));
	__export(__webpack_require__(47));
	__export(__webpack_require__(41));
	__export(__webpack_require__(48));
	__export(__webpack_require__(51));
	__export(__webpack_require__(52));
	__export(__webpack_require__(53));
	__export(__webpack_require__(54));
	__export(__webpack_require__(55));
	__export(__webpack_require__(39));
	__export(__webpack_require__(45));
	__export(__webpack_require__(62));
	__export(__webpack_require__(46));
	__export(__webpack_require__(63));
	__export(__webpack_require__(31));
	__export(__webpack_require__(64));
	__export(__webpack_require__(24));
	__export(__webpack_require__(65));
	__export(__webpack_require__(33));
	__export(__webpack_require__(66));
	__export(__webpack_require__(67));
	__export(__webpack_require__(68));
	__export(__webpack_require__(71));
	__export(__webpack_require__(40));
	__export(__webpack_require__(72));
	__export(__webpack_require__(73));
	__export(__webpack_require__(74));
	__export(__webpack_require__(75));
	__export(__webpack_require__(76));
	__export(__webpack_require__(77));
	__export(__webpack_require__(78));
	__export(__webpack_require__(80));
	__export(__webpack_require__(81));
	__export(__webpack_require__(82));
	__export(__webpack_require__(34));
	__export(__webpack_require__(83));
	__export(__webpack_require__(84));
	
	//# sourceMappingURL=index.js.map


/***/ }),
/* 21 */
/***/ (function(module, exports) {

	module.exports = __WEBPACK_EXTERNAL_MODULE_21__;

/***/ }),
/* 22 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var React = __webpack_require__(7);
	var ReactDOM = __webpack_require__(23);
	var abstractComponent_1 = __webpack_require__(5);
	var Classes = __webpack_require__(16);
	var position_1 = __webpack_require__(13);
	var utils_1 = __webpack_require__(8);
	var popover_1 = __webpack_require__(24);
	var TETHER_OPTIONS = {
	    constraints: [
	        { attachment: "together", pin: true, to: "window" },
	    ],
	};
	var TRANSITION_DURATION = 100;
	var ContextMenu = (function (_super) {
	    tslib_1.__extends(ContextMenu, _super);
	    function ContextMenu() {
	        var _this = _super !== null && _super.apply(this, arguments) || this;
	        _this.state = {
	            isOpen: false,
	        };
	        _this.cancelContextMenu = function (e) { return e.preventDefault(); };
	        _this.handleBackdropContextMenu = function (e) {
	            // React function to remove from the event pool, useful when using a event within a callback
	            e.persist();
	            e.preventDefault();
	            // wait for backdrop to disappear so we can find the "real" element at event coordinates.
	            // timeout duration is equivalent to transition duration so we know it's animated out.
	            _this.setTimeout(function () {
	                // retrigger context menu event at the element beneath the backdrop.
	                // if it has a `contextmenu` event handler then it'll be invoked.
	                // if it doesn't, no native menu will show (at least on OSX) :(
	                var newTarget = document.elementFromPoint(e.clientX, e.clientY);
	                newTarget.dispatchEvent(new MouseEvent("contextmenu", e));
	            }, TRANSITION_DURATION);
	        };
	        _this.handlePopoverInteraction = function (nextOpenState) {
	            if (!nextOpenState) {
	                // delay the actual hiding till the event queue clears
	                // to avoid flicker of opening twice
	                requestAnimationFrame(function () { return _this.hide(); });
	            }
	        };
	        return _this;
	    }
	    ContextMenu.prototype.render = function () {
	        // prevent right-clicking in a context menu
	        var content = React.createElement("div", { onContextMenu: this.cancelContextMenu }, this.state.menu);
	        return (React.createElement(popover_1.Popover, { backdropProps: { onContextMenu: this.handleBackdropContextMenu }, content: content, enforceFocus: false, isModal: true, isOpen: this.state.isOpen, onInteraction: this.handlePopoverInteraction, position: position_1.Position.RIGHT_TOP, popoverClassName: Classes.MINIMAL, useSmartArrowPositioning: false, tetherOptions: TETHER_OPTIONS, transitionDuration: TRANSITION_DURATION },
	            React.createElement("div", { className: Classes.CONTEXT_MENU_POPOVER_TARGET, style: this.state.offset })));
	    };
	    ContextMenu.prototype.show = function (menu, offset, onClose) {
	        this.setState({ isOpen: true, menu: menu, offset: offset, onClose: onClose });
	    };
	    ContextMenu.prototype.hide = function () {
	        utils_1.safeInvoke(this.state.onClose);
	        this.setState({ isOpen: false, onClose: undefined });
	    };
	    return ContextMenu;
	}(abstractComponent_1.AbstractComponent));
	var contextMenu;
	/**
	 * Show the given menu element at the given offset from the top-left corner of the viewport.
	 * The menu will appear below-right of this point and will flip to below-left if there is not enough
	 * room onscreen. The optional callback will be invoked when this menu closes.
	 */
	function show(menu, offset, onClose) {
	    if (contextMenu == null) {
	        var contextMenuElement = document.createElement("div");
	        contextMenuElement.classList.add(Classes.CONTEXT_MENU);
	        document.body.appendChild(contextMenuElement);
	        contextMenu = ReactDOM.render(React.createElement(ContextMenu, null), contextMenuElement);
	    }
	    contextMenu.show(menu, offset, onClose);
	}
	exports.show = show;
	/** Hide the open context menu. */
	function hide() {
	    if (contextMenu != null) {
	        contextMenu.hide();
	    }
	}
	exports.hide = hide;
	/** Return whether a context menu is currently open. */
	function isOpen() {
	    return contextMenu != null && contextMenu.state.isOpen;
	}
	exports.isOpen = isOpen;
	
	//# sourceMappingURL=contextMenu.js.map


/***/ }),
/* 23 */
/***/ (function(module, exports) {

	module.exports = __WEBPACK_EXTERNAL_MODULE_23__;

/***/ }),
/* 24 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var PureRender = __webpack_require__(26);
	var React = __webpack_require__(7);
	var react_dom_1 = __webpack_require__(23);
	var Tether = __webpack_require__(30);
	var abstractComponent_1 = __webpack_require__(5);
	var Classes = __webpack_require__(16);
	var Errors = __webpack_require__(10);
	var PosUtils = __webpack_require__(13);
	var TetherUtils = __webpack_require__(15);
	var Utils = __webpack_require__(8);
	var overlay_1 = __webpack_require__(31);
	var tooltip_1 = __webpack_require__(34);
	var Arrows = __webpack_require__(35);
	var SVG_SHADOW_PATH = "M8.11 6.302c1.015-.936 1.887-2.922 1.887-4.297v26c0-1.378" +
	    "-.868-3.357-1.888-4.297L.925 17.09c-1.237-1.14-1.233-3.034 0-4.17L8.11 6.302z";
	var SVG_ARROW_PATH = "M8.787 7.036c1.22-1.125 2.21-3.376 2.21-5.03V0v30-2.005" +
	    "c0-1.654-.983-3.9-2.21-5.03l-7.183-6.616c-.81-.746-.802-1.96 0-2.7l7.183-6.614z";
	var SMART_POSITIONING = {
	    attachment: "together",
	    to: "scrollParent",
	};
	var PopoverInteractionKind;
	(function (PopoverInteractionKind) {
	    PopoverInteractionKind[PopoverInteractionKind["CLICK"] = 0] = "CLICK";
	    PopoverInteractionKind[PopoverInteractionKind["CLICK_TARGET_ONLY"] = 1] = "CLICK_TARGET_ONLY";
	    PopoverInteractionKind[PopoverInteractionKind["HOVER"] = 2] = "HOVER";
	    PopoverInteractionKind[PopoverInteractionKind["HOVER_TARGET_ONLY"] = 3] = "HOVER_TARGET_ONLY";
	})(PopoverInteractionKind = exports.PopoverInteractionKind || (exports.PopoverInteractionKind = {}));
	var Popover = (function (_super) {
	    tslib_1.__extends(Popover, _super);
	    function Popover(props, context) {
	        var _this = _super.call(this, props, context) || this;
	        _this.hasDarkParent = false;
	        // a flag that is set to true while we are waiting for the underlying Portal to complete rendering
	        _this.isContentMounting = false;
	        _this.refHandlers = {
	            popover: function (ref) {
	                _this.popoverElement = ref;
	                _this.updateTether();
	                _this.updateArrowPosition();
	            },
	            target: function (ref) {
	                _this.targetElement = ref;
	            },
	        };
	        _this.handleContentMount = function () {
	            if (Utils.isFunction(_this.props.popoverDidOpen) && _this.isContentMounting) {
	                _this.props.popoverDidOpen();
	                _this.isContentMounting = false;
	            }
	        };
	        _this.handleTargetFocus = function (e) {
	            if (_this.props.openOnTargetFocus && _this.isHoverInteractionKind()) {
	                _this.handleMouseEnter(e);
	            }
	        };
	        _this.handleTargetBlur = function (e) {
	            if (_this.props.openOnTargetFocus && _this.isHoverInteractionKind()) {
	                // if the next element to receive focus is within the popover, we'll want to leave the
	                // popover open. we must do this check *after* the next element focuses, so we use a
	                // timeout of 0 to flush the rest of the event queue before proceeding.
	                requestAnimationFrame(function () {
	                    var popoverElement = _this.popoverElement;
	                    if (popoverElement == null || !popoverElement.contains(document.activeElement)) {
	                        _this.handleMouseLeave(e);
	                    }
	                });
	            }
	        };
	        _this.handleMouseEnter = function (e) {
	            // if we're entering the popover, and the mode is set to be HOVER_TARGET_ONLY, we want to manually
	            // trigger the mouse leave event, as hovering over the popover shouldn't count.
	            if (_this.props.inline
	                && _this.isElementInPopover(e.target)
	                && _this.props.interactionKind === PopoverInteractionKind.HOVER_TARGET_ONLY
	                && !_this.props.openOnTargetFocus) {
	                _this.handleMouseLeave(e);
	            }
	            else if (!_this.props.isDisabled) {
	                // only begin opening popover when it is enabled
	                _this.setOpenState(true, e, _this.props.hoverOpenDelay);
	            }
	        };
	        _this.handleMouseLeave = function (e) {
	            // user-configurable closing delay is helpful when moving mouse from target to popover
	            _this.setOpenState(false, e, _this.props.hoverCloseDelay);
	        };
	        _this.handlePopoverClick = function (e) {
	            var eventTarget = e.target;
	            var shouldDismiss = eventTarget.closest("." + Classes.POPOVER_DISMISS) != null;
	            var overrideDismiss = eventTarget.closest("." + Classes.POPOVER_DISMISS_OVERRIDE) != null;
	            if (shouldDismiss && !overrideDismiss) {
	                _this.setOpenState(false, e);
	            }
	        };
	        _this.handleOverlayClose = function (e) {
	            var eventTarget = e.target;
	            // if click was in target, target event listener will handle things, so don't close
	            if (!Utils.elementIsOrContains(_this.targetElement, eventTarget)
	                || e.nativeEvent instanceof KeyboardEvent) {
	                _this.setOpenState(false, e);
	            }
	        };
	        _this.handleTargetClick = function (e) {
	            // ensure click did not originate from within inline popover before closing
	            if (!_this.props.isDisabled && !_this.isElementInPopover(e.target)) {
	                if (_this.props.isOpen == null) {
	                    _this.setState(function (prevState) { return ({ isOpen: !prevState.isOpen }); });
	                }
	                else {
	                    _this.setOpenState(!_this.props.isOpen, e);
	                }
	            }
	        };
	        var isOpen = props.defaultIsOpen && !props.isDisabled;
	        if (props.isOpen != null) {
	            isOpen = props.isOpen;
	        }
	        _this.state = {
	            isOpen: isOpen,
	            ignoreTargetDimensions: false,
	            targetHeight: 0,
	            targetWidth: 0,
	        };
	        return _this;
	    }
	    Popover.prototype.render = function () {
	        var className = this.props.className;
	        var isOpen = this.state.isOpen;
	        var targetProps;
	        if (this.isHoverInteractionKind()) {
	            targetProps = {
	                onBlur: this.handleTargetBlur,
	                onFocus: this.handleTargetFocus,
	                onMouseEnter: this.handleMouseEnter,
	                onMouseLeave: this.handleMouseLeave,
	            };
	            // any one of the CLICK* values
	        }
	        else {
	            targetProps = {
	                onClick: this.handleTargetClick,
	            };
	        }
	        targetProps.className = classNames(Classes.POPOVER_TARGET, (_a = {},
	            _a[Classes.POPOVER_OPEN] = isOpen,
	            _a), className);
	        targetProps.ref = this.refHandlers.target;
	        var children = this.understandChildren();
	        var targetTabIndex = this.props.openOnTargetFocus && this.isHoverInteractionKind() ? 0 : undefined;
	        var target = React.cloneElement(children.target, 
	        // force disable single Tooltip child when popover is open (BLUEPRINT-552)
	        (isOpen && children.target.type === tooltip_1.Tooltip)
	            ? { isDisabled: true, tabIndex: targetTabIndex }
	            : { tabIndex: targetTabIndex });
	        var isContentEmpty = (children.content == null);
	        if (isContentEmpty && !this.props.isDisabled && isOpen !== false && !Utils.isNodeEnv("production")) {
	            console.warn(Errors.POPOVER_WARN_EMPTY_CONTENT);
	        }
	        return React.createElement(this.props.rootElementTag, targetProps, target, React.createElement(overlay_1.Overlay, { autoFocus: this.props.autoFocus, backdropClassName: Classes.POPOVER_BACKDROP, backdropProps: this.props.backdropProps, canEscapeKeyClose: this.props.canEscapeKeyClose, canOutsideClickClose: this.props.interactionKind === PopoverInteractionKind.CLICK, className: this.props.portalClassName, didOpen: this.handleContentMount, enforceFocus: this.props.enforceFocus, hasBackdrop: this.props.isModal, inline: this.props.inline, isOpen: isOpen && !isContentEmpty, lazy: this.props.lazy, onClose: this.handleOverlayClose, transitionDuration: this.props.transitionDuration, transitionName: Classes.POPOVER }, this.renderPopover(children.content)));
	        var _a;
	    };
	    Popover.prototype.componentDidMount = function () {
	        this.componentDOMChange();
	    };
	    Popover.prototype.componentWillReceiveProps = function (nextProps) {
	        _super.prototype.componentWillReceiveProps.call(this, nextProps);
	        if (nextProps.isOpen == null && nextProps.isDisabled && !this.props.isDisabled) {
	            // ok to use setOpenState here because isDisabled and isOpen are mutex.
	            this.setOpenState(false);
	        }
	        else if (nextProps.isOpen !== this.props.isOpen) {
	            // propagate isOpen prop directly to state, circumventing onInteraction callback
	            // (which would be invoked if this went through setOpenState)
	            this.setState({ isOpen: nextProps.isOpen });
	        }
	    };
	    Popover.prototype.componentWillUpdate = function (_, nextState) {
	        if (!this.state.isOpen && nextState.isOpen) {
	            this.isContentMounting = true;
	            Utils.safeInvoke(this.props.popoverWillOpen);
	        }
	        else if (this.state.isOpen && !nextState.isOpen) {
	            Utils.safeInvoke(this.props.popoverWillClose);
	        }
	    };
	    Popover.prototype.componentDidUpdate = function () {
	        this.componentDOMChange();
	    };
	    Popover.prototype.componentWillUnmount = function () {
	        _super.prototype.componentWillUnmount.call(this);
	        this.destroyTether();
	    };
	    Popover.prototype.validateProps = function (props) {
	        if (props.useSmartPositioning || props.constraints != null) {
	            console.warn(Errors.POPOVER_WARN_DEPRECATED_CONSTRAINTS);
	        }
	        if (props.isOpen == null && props.onInteraction != null) {
	            console.warn(Errors.POPOVER_WARN_UNCONTROLLED_ONINTERACTION);
	        }
	        if (props.inline && (props.useSmartPositioning || props.constraints != null || props.tetherOptions != null)) {
	            console.warn(Errors.POPOVER_WARN_INLINE_NO_TETHER);
	        }
	        if (props.isModal && props.inline) {
	            console.warn(Errors.POPOVER_WARN_MODAL_INLINE);
	        }
	        if (props.isModal && props.interactionKind !== PopoverInteractionKind.CLICK) {
	            throw new Error(Errors.POPOVER_MODAL_INTERACTION);
	        }
	        var childrenCount = React.Children.count(props.children);
	        var hasContentProp = props.content !== undefined;
	        var hasTargetProp = props.target !== undefined;
	        if (childrenCount === 0 && !hasTargetProp) {
	            throw new Error(Errors.POPOVER_REQUIRES_TARGET);
	        }
	        if (childrenCount > 2) {
	            console.warn(Errors.POPOVER_WARN_TOO_MANY_CHILDREN);
	        }
	        if (childrenCount > 0 && hasTargetProp) {
	            console.warn(Errors.POPOVER_WARN_DOUBLE_TARGET);
	        }
	        if (childrenCount === 2 && hasContentProp) {
	            console.warn(Errors.POPOVER_WARN_DOUBLE_CONTENT);
	        }
	    };
	    Popover.prototype.componentDOMChange = function () {
	        if (this.props.useSmartArrowPositioning) {
	            this.setState({
	                targetHeight: this.targetElement.clientHeight,
	                targetWidth: this.targetElement.clientWidth,
	            });
	        }
	        if (!this.props.inline) {
	            this.hasDarkParent = this.targetElement.closest("." + Classes.DARK) != null;
	            this.updateTether();
	        }
	    };
	    Popover.prototype.renderPopover = function (content) {
	        var _a = this.props, inline = _a.inline, interactionKind = _a.interactionKind;
	        var popoverHandlers = {
	            // always check popover clicks for dismiss class
	            onClick: this.handlePopoverClick,
	        };
	        if ((interactionKind === PopoverInteractionKind.HOVER)
	            || (inline && interactionKind === PopoverInteractionKind.HOVER_TARGET_ONLY)) {
	            popoverHandlers.onMouseEnter = this.handleMouseEnter;
	            popoverHandlers.onMouseLeave = this.handleMouseLeave;
	        }
	        var positionClasses = TetherUtils.getAttachmentClasses(this.props.position).join(" ");
	        var containerClasses = classNames(Classes.TRANSITION_CONTAINER, (_b = {}, _b[positionClasses] = inline, _b));
	        var popoverClasses = classNames(Classes.POPOVER, (_c = {},
	            _c[Classes.DARK] = this.props.inheritDarkTheme && this.hasDarkParent && !inline,
	            _c), this.props.popoverClassName);
	        var styles = this.getArrowPositionStyles();
	        var transform = { transformOrigin: this.getPopoverTransformOrigin() };
	        return (React.createElement("div", { className: containerClasses, ref: this.refHandlers.popover, style: styles.container },
	            React.createElement("div", tslib_1.__assign({ className: popoverClasses, style: transform }, popoverHandlers),
	                React.createElement("div", { className: Classes.POPOVER_ARROW, style: styles.arrow },
	                    React.createElement("svg", { viewBox: "0 0 30 30" },
	                        React.createElement("path", { className: Classes.POPOVER_ARROW + "-border", d: SVG_SHADOW_PATH }),
	                        React.createElement("path", { className: Classes.POPOVER_ARROW + "-fill", d: SVG_ARROW_PATH }))),
	                React.createElement("div", { className: Classes.POPOVER_CONTENT }, content))));
	        var _b, _c;
	    };
	    // content and target can be specified as props or as children.
	    // this method normalizes the two approaches, preferring child over prop.
	    Popover.prototype.understandChildren = function () {
	        var _a = this.props, children = _a.children, contentProp = _a.content, targetProp = _a.target;
	        // #validateProps asserts that 1 <= children.length <= 2 so content is optional
	        var _b = React.Children.toArray(children), targetChild = _b[0], contentChild = _b[1];
	        return {
	            content: ensureElement(contentChild == null ? contentProp : contentChild),
	            target: ensureElement(targetChild == null ? targetProp : targetChild),
	        };
	    };
	    Popover.prototype.getArrowPositionStyles = function () {
	        if (this.props.useSmartArrowPositioning) {
	            var dimensions = { height: this.state.targetHeight, width: this.state.targetWidth };
	            return Arrows.getArrowPositionStyles(this.props.position, this.props.arrowSize, this.state.ignoreTargetDimensions, dimensions, this.props.inline);
	        }
	        else {
	            return {};
	        }
	    };
	    Popover.prototype.getPopoverTransformOrigin = function () {
	        // if smart positioning is enabled then we must rely on CSS classes to put transform origin
	        // on the correct side and cannot override it in JS. (https://github.com/HubSpot/tether/issues/154)
	        if (this.props.useSmartArrowPositioning && !this.props.useSmartPositioning) {
	            var dimensions = { height: this.state.targetHeight, width: this.state.targetWidth };
	            return Arrows.getPopoverTransformOrigin(this.props.position, this.props.arrowSize, dimensions);
	        }
	        else {
	            return undefined;
	        }
	    };
	    Popover.prototype.updateArrowPosition = function () {
	        if (this.popoverElement != null) {
	            var arrow = this.popoverElement.getElementsByClassName(Classes.POPOVER_ARROW)[0];
	            var centerWidth = (this.state.targetWidth + arrow.clientWidth) / 2;
	            var centerHeight = (this.state.targetHeight + arrow.clientHeight) / 2;
	            var ignoreWidth = centerWidth > this.popoverElement.clientWidth
	                && PosUtils.isPositionHorizontal(this.props.position);
	            var ignoreHeight = centerHeight > this.popoverElement.clientHeight
	                && PosUtils.isPositionVertical(this.props.position);
	            if (!this.state.ignoreTargetDimensions && (ignoreWidth || ignoreHeight)) {
	                this.setState({ ignoreTargetDimensions: true });
	            }
	            else if (this.state.ignoreTargetDimensions && !ignoreWidth && !ignoreHeight) {
	                this.setState({ ignoreTargetDimensions: false });
	            }
	        }
	    };
	    Popover.prototype.updateTether = function () {
	        var _this = this;
	        if (this.state.isOpen && !this.props.inline && this.popoverElement != null) {
	            var _a = this.props, constraints = _a.constraints, position = _a.position, _b = _a.tetherOptions, tetherOptions = _b === void 0 ? {} : _b, useSmartPositioning = _a.useSmartPositioning;
	            // the .pt-popover-target span we wrap the children in won't always be as big as its children
	            // so instead, we'll position tether based off of its first child.
	            // NOTE: use findDOMNode(this) directly because this.targetElement may not exist yet
	            var target = react_dom_1.findDOMNode(this).childNodes[0];
	            // constraints is deprecated but must still be supported through tetherOptions until v2.0
	            var options = (constraints == null && !useSmartPositioning)
	                ? tetherOptions
	                : tslib_1.__assign({}, tetherOptions, { constraints: useSmartPositioning ? [SMART_POSITIONING] : constraints });
	            var finalTetherOptions = TetherUtils.createTetherOptions(this.popoverElement, target, position, options);
	            if (this.tether == null) {
	                this.tether = new Tether(finalTetherOptions);
	            }
	            else {
	                this.tether.setOptions(finalTetherOptions);
	            }
	            // if props.position has just changed, Tether unfortunately positions the popover based
	            // on the margins from the previous position. delay a frame for styles to catch up.
	            setTimeout(function () { return _this.tether.position(); });
	        }
	        else {
	            this.destroyTether();
	        }
	    };
	    Popover.prototype.destroyTether = function () {
	        if (this.tether != null) {
	            this.tether.destroy();
	        }
	    };
	    // a wrapper around setState({isOpen}) that will call props.onInteraction instead when in controlled mode.
	    // starts a timeout to delay changing the state if a non-zero duration is provided.
	    Popover.prototype.setOpenState = function (isOpen, e, timeout) {
	        var _this = this;
	        // cancel any existing timeout because we have new state
	        Utils.safeInvoke(this.cancelOpenTimeout);
	        if (timeout > 0) {
	            this.cancelOpenTimeout = this.setTimeout(function () { return _this.setOpenState(isOpen, e); }, timeout);
	        }
	        else {
	            if (this.props.isOpen == null) {
	                this.setState({ isOpen: isOpen });
	            }
	            else {
	                Utils.safeInvoke(this.props.onInteraction, isOpen);
	            }
	            if (!isOpen) {
	                Utils.safeInvoke(this.props.onClose, e);
	            }
	        }
	    };
	    Popover.prototype.isElementInPopover = function (element) {
	        return this.popoverElement != null && this.popoverElement.contains(element);
	    };
	    Popover.prototype.isHoverInteractionKind = function () {
	        return this.props.interactionKind === PopoverInteractionKind.HOVER
	            || this.props.interactionKind === PopoverInteractionKind.HOVER_TARGET_ONLY;
	    };
	    return Popover;
	}(abstractComponent_1.AbstractComponent));
	Popover.defaultProps = {
	    arrowSize: 30,
	    className: "",
	    defaultIsOpen: false,
	    hoverCloseDelay: 300,
	    hoverOpenDelay: 150,
	    inheritDarkTheme: true,
	    inline: false,
	    interactionKind: PopoverInteractionKind.CLICK,
	    isDisabled: false,
	    isModal: false,
	    openOnTargetFocus: true,
	    popoverClassName: "",
	    position: PosUtils.Position.RIGHT,
	    rootElementTag: "span",
	    transitionDuration: 300,
	    useSmartArrowPositioning: true,
	    useSmartPositioning: false,
	};
	Popover.displayName = "Blueprint.Popover";
	Popover = tslib_1.__decorate([
	    PureRender
	], Popover);
	exports.Popover = Popover;
	/**
	 * Converts a react child to an element: non-empty strings or numbers are wrapped in `<span>`;
	 * empty strings are discarded.
	 */
	function ensureElement(child) {
	    // wrap text in a <span> so children are always elements
	    if (typeof child === "string") {
	        // cull whitespace strings
	        return child.trim().length > 0 ? React.createElement("span", null, child) : undefined;
	    }
	    else if (typeof child === "number") {
	        return React.createElement("span", null, child);
	    }
	    else {
	        return child;
	    }
	}
	exports.PopoverFactory = React.createFactory(Popover);
	
	//# sourceMappingURL=popover.js.map


/***/ }),
/* 25 */
/***/ (function(module, exports) {

	module.exports = __WEBPACK_EXTERNAL_MODULE_25__;

/***/ }),
/* 26 */
/***/ (function(module, exports, __webpack_require__) {

	/**
	 * @author Félix Girault <felix.girault@gmail.com>
	 * @license MIT
	 */
	'use strict';
	
	var warning = __webpack_require__(27);
	var shallowEqual = __webpack_require__(29);
	
	
	
	/**
	 * Tells if a component should update given it's next props
	 * and state.
	 *
	 * @param object nextProps Next props.
	 * @param object nextState Next state.
	 */
	function shouldComponentUpdate(nextProps, nextState) {
	  return !shallowEqual(this.props, nextProps) || !shallowEqual(this.state, nextState);
	}
	
	/**
	 * Returns a text description of the component that can be
	 * used to identify it in error messages.
	 *
	 * @see https://github.com/facebook/react/blob/v15.4.0-rc.3/src/renderers/shared/stack/reconciler/ReactCompositeComponent.js#L1143
	 * @param {function} component The component.
	 * @return {string} The name of the component.
	 */
	function getComponentName(component) {
	  var constructor = component.prototype && component.prototype.constructor;
	
	  return (
	    component.displayName
	    || (constructor && constructor.displayName)
	    || component.name
	    || (constructor && constructor.name)
	    || 'a component'
	  );
	}
	
	/**
	 * Makes the given component "pure".
	 *
	 * @param object component Component.
	 */
	function pureRenderDecorator(component) {
	  if (component.prototype.shouldComponentUpdate !== undefined) {
	    // We're not using the condition mecanism of warning()
	    // here to avoid useless calls to getComponentName().
	    warning(
	      false,
	      'Cannot decorate `%s` with @pureRenderDecorator, '
	      + 'because it already implements `shouldComponentUpdate().',
	      getComponentName(component)
	    )
	  }
	
	  component.prototype.shouldComponentUpdate = shouldComponentUpdate;
	  return component;
	}
	
	
	
	module.exports = pureRenderDecorator;


/***/ }),
/* 27 */
/***/ (function(module, exports, __webpack_require__) {

	/**
	 * Copyright 2014-2015, Facebook, Inc.
	 * All rights reserved.
	 *
	 * This source code is licensed under the BSD-style license found in the
	 * LICENSE file in the root directory of this source tree. An additional grant
	 * of patent rights can be found in the PATENTS file in the same directory.
	 *
	 */
	
	'use strict';
	
	var emptyFunction = __webpack_require__(28);
	
	/**
	 * Similar to invariant but only logs a warning if the condition is not met.
	 * This can be used to log issues in development environments in critical
	 * paths. Removing the logging code for production environments will keep the
	 * same logic and follow the same code paths.
	 */
	
	var warning = emptyFunction;
	
	if (false) {
	  (function () {
	    var printWarning = function printWarning(format) {
	      for (var _len = arguments.length, args = Array(_len > 1 ? _len - 1 : 0), _key = 1; _key < _len; _key++) {
	        args[_key - 1] = arguments[_key];
	      }
	
	      var argIndex = 0;
	      var message = 'Warning: ' + format.replace(/%s/g, function () {
	        return args[argIndex++];
	      });
	      if (typeof console !== 'undefined') {
	        console.error(message);
	      }
	      try {
	        // --- Welcome to debugging React ---
	        // This error was thrown as a convenience so that you can use this stack
	        // to find the callsite that caused this warning to fire.
	        throw new Error(message);
	      } catch (x) {}
	    };
	
	    warning = function warning(condition, format) {
	      if (format === undefined) {
	        throw new Error('`warning(condition, format, ...args)` requires a warning ' + 'message argument');
	      }
	
	      if (format.indexOf('Failed Composite propType: ') === 0) {
	        return; // Ignore CompositeComponent proptype check.
	      }
	
	      if (!condition) {
	        for (var _len2 = arguments.length, args = Array(_len2 > 2 ? _len2 - 2 : 0), _key2 = 2; _key2 < _len2; _key2++) {
	          args[_key2 - 2] = arguments[_key2];
	        }
	
	        printWarning.apply(undefined, [format].concat(args));
	      }
	    };
	  })();
	}
	
	module.exports = warning;

/***/ }),
/* 28 */
/***/ (function(module, exports) {

	"use strict";
	
	/**
	 * Copyright (c) 2013-present, Facebook, Inc.
	 * All rights reserved.
	 *
	 * This source code is licensed under the BSD-style license found in the
	 * LICENSE file in the root directory of this source tree. An additional grant
	 * of patent rights can be found in the PATENTS file in the same directory.
	 *
	 * 
	 */
	
	function makeEmptyFunction(arg) {
	  return function () {
	    return arg;
	  };
	}
	
	/**
	 * This function accepts and discards inputs; it has no side effects. This is
	 * primarily useful idiomatically for overridable function endpoints which
	 * always need to be callable, since JS lacks a null-call idiom ala Cocoa.
	 */
	var emptyFunction = function emptyFunction() {};
	
	emptyFunction.thatReturns = makeEmptyFunction;
	emptyFunction.thatReturnsFalse = makeEmptyFunction(false);
	emptyFunction.thatReturnsTrue = makeEmptyFunction(true);
	emptyFunction.thatReturnsNull = makeEmptyFunction(null);
	emptyFunction.thatReturnsThis = function () {
	  return this;
	};
	emptyFunction.thatReturnsArgument = function (arg) {
	  return arg;
	};
	
	module.exports = emptyFunction;

/***/ }),
/* 29 */
/***/ (function(module, exports) {

	/**
	 * Copyright (c) 2013-present, Facebook, Inc.
	 * All rights reserved.
	 *
	 * This source code is licensed under the BSD-style license found in the
	 * LICENSE file in the root directory of this source tree. An additional grant
	 * of patent rights can be found in the PATENTS file in the same directory.
	 *
	 * @typechecks
	 * 
	 */
	
	/*eslint-disable no-self-compare */
	
	'use strict';
	
	var hasOwnProperty = Object.prototype.hasOwnProperty;
	
	/**
	 * inlined Object.is polyfill to avoid requiring consumers ship their own
	 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/is
	 */
	function is(x, y) {
	  // SameValue algorithm
	  if (x === y) {
	    // Steps 1-5, 7-10
	    // Steps 6.b-6.e: +0 != -0
	    // Added the nonzero y check to make Flow happy, but it is redundant
	    return x !== 0 || y !== 0 || 1 / x === 1 / y;
	  } else {
	    // Step 6.a: NaN == NaN
	    return x !== x && y !== y;
	  }
	}
	
	/**
	 * Performs equality by iterating through keys on an object and returning false
	 * when any key has values which are not strictly equal between the arguments.
	 * Returns true when the values of all keys are strictly equal.
	 */
	function shallowEqual(objA, objB) {
	  if (is(objA, objB)) {
	    return true;
	  }
	
	  if (typeof objA !== 'object' || objA === null || typeof objB !== 'object' || objB === null) {
	    return false;
	  }
	
	  var keysA = Object.keys(objA);
	  var keysB = Object.keys(objB);
	
	  if (keysA.length !== keysB.length) {
	    return false;
	  }
	
	  // Test for A's keys different from B.
	  for (var i = 0; i < keysA.length; i++) {
	    if (!hasOwnProperty.call(objB, keysA[i]) || !is(objA[keysA[i]], objB[keysA[i]])) {
	      return false;
	    }
	  }
	
	  return true;
	}
	
	module.exports = shallowEqual;

/***/ }),
/* 30 */
/***/ (function(module, exports) {

	module.exports = __WEBPACK_EXTERNAL_MODULE_30__;

/***/ }),
/* 31 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var PureRender = __webpack_require__(26);
	var React = __webpack_require__(7);
	var CSSTransitionGroup = __webpack_require__(32);
	var Classes = __webpack_require__(16);
	var Keys = __webpack_require__(17);
	var utils_1 = __webpack_require__(8);
	var portal_1 = __webpack_require__(33);
	var Overlay = Overlay_1 = (function (_super) {
	    tslib_1.__extends(Overlay, _super);
	    function Overlay(props, context) {
	        var _this = _super.call(this, props, context) || this;
	        _this.refHandlers = {
	            container: function (ref) { return _this.containerElement = ref; },
	        };
	        _this.handleBackdropMouseDown = function (e) {
	            var _a = _this.props, backdropProps = _a.backdropProps, canOutsideClickClose = _a.canOutsideClickClose, enforceFocus = _a.enforceFocus, onClose = _a.onClose;
	            if (canOutsideClickClose) {
	                utils_1.safeInvoke(onClose, e);
	            }
	            if (enforceFocus) {
	                // make sure document.activeElement is updated before bringing the focus back
	                _this.bringFocusInsideOverlay();
	            }
	            utils_1.safeInvoke(backdropProps.onMouseDown, e);
	        };
	        _this.handleDocumentClick = function (e) {
	            var _a = _this.props, isOpen = _a.isOpen, onClose = _a.onClose;
	            var eventTarget = e.target;
	            var isClickInOverlay = _this.containerElement != null
	                && _this.containerElement.contains(eventTarget);
	            if (isOpen && _this.props.canOutsideClickClose && !isClickInOverlay) {
	                // casting to any because this is a native event
	                utils_1.safeInvoke(onClose, e);
	            }
	        };
	        _this.handleContentMount = function () {
	            if (_this.props.isOpen) {
	                utils_1.safeInvoke(_this.props.didOpen);
	            }
	            if (_this.props.autoFocus) {
	                _this.bringFocusInsideOverlay();
	            }
	        };
	        _this.handleDocumentFocus = function (e) {
	            if (_this.props.enforceFocus
	                && _this.containerElement != null
	                && !_this.containerElement.contains(e.target)) {
	                // prevent default focus behavior (sometimes auto-scrolls the page)
	                e.preventDefault();
	                e.stopImmediatePropagation();
	                _this.bringFocusInsideOverlay();
	            }
	        };
	        _this.handleKeyDown = function (e) {
	            var _a = _this.props, canEscapeKeyClose = _a.canEscapeKeyClose, onClose = _a.onClose;
	            if (e.which === Keys.ESCAPE && canEscapeKeyClose) {
	                utils_1.safeInvoke(onClose, e);
	                // prevent browser-specific escape key behavior (Safari exits fullscreen)
	                e.preventDefault();
	            }
	        };
	        _this.state = { hasEverOpened: props.isOpen };
	        return _this;
	    }
	    Overlay.prototype.render = function () {
	        // oh snap! no reason to render anything at all if we're being truly lazy
	        if (this.props.lazy && !this.state.hasEverOpened) {
	            return null;
	        }
	        var _a = this.props, children = _a.children, className = _a.className, inline = _a.inline, isOpen = _a.isOpen, transitionDuration = _a.transitionDuration, transitionName = _a.transitionName;
	        // add a special class to each child that will automatically set the appropriate
	        // CSS position mode under the hood. also, make the container focusable so we can
	        // trap focus inside it (via `enforceFocus`).
	        var decoratedChildren = React.Children.map(children, function (child) {
	            return React.cloneElement(child, {
	                className: classNames(child.props.className, Classes.OVERLAY_CONTENT),
	                tabIndex: 0,
	            });
	        });
	        var transitionGroup = (React.createElement(CSSTransitionGroup, { transitionAppear: true, transitionAppearTimeout: transitionDuration, transitionEnterTimeout: transitionDuration, transitionLeaveTimeout: transitionDuration, transitionName: transitionName },
	            this.maybeRenderBackdrop(),
	            isOpen ? decoratedChildren : null));
	        var mergedClassName = classNames(Classes.OVERLAY, (_b = {},
	            _b[Classes.OVERLAY_OPEN] = isOpen,
	            _b[Classes.OVERLAY_INLINE] = inline,
	            _b), className);
	        var elementProps = {
	            className: mergedClassName,
	            onKeyDown: this.handleKeyDown,
	        };
	        if (inline) {
	            return (React.createElement("span", tslib_1.__assign({}, elementProps, { ref: this.refHandlers.container }), transitionGroup));
	        }
	        else {
	            return (React.createElement(portal_1.Portal, tslib_1.__assign({}, elementProps, { containerRef: this.refHandlers.container, onChildrenMount: this.handleContentMount }), transitionGroup));
	        }
	        var _b;
	    };
	    Overlay.prototype.componentDidMount = function () {
	        if (this.props.isOpen) {
	            this.overlayWillOpen();
	        }
	    };
	    Overlay.prototype.componentWillReceiveProps = function (nextProps) {
	        this.setState({ hasEverOpened: this.state.hasEverOpened || nextProps.isOpen });
	    };
	    Overlay.prototype.componentDidUpdate = function (prevProps) {
	        if (prevProps.isOpen && !this.props.isOpen) {
	            this.overlayWillClose();
	        }
	        else if (!prevProps.isOpen && this.props.isOpen) {
	            this.overlayWillOpen();
	        }
	    };
	    Overlay.prototype.componentWillUnmount = function () {
	        this.overlayWillClose();
	    };
	    Overlay.prototype.maybeRenderBackdrop = function () {
	        var _a = this.props, backdropClassName = _a.backdropClassName, backdropProps = _a.backdropProps, hasBackdrop = _a.hasBackdrop, isOpen = _a.isOpen;
	        if (hasBackdrop && isOpen) {
	            return (React.createElement("div", tslib_1.__assign({}, backdropProps, { className: classNames(Classes.OVERLAY_BACKDROP, backdropClassName, backdropProps.className), onMouseDown: this.handleBackdropMouseDown, tabIndex: this.props.canOutsideClickClose ? 0 : null })));
	        }
	        else {
	            return undefined;
	        }
	    };
	    Overlay.prototype.overlayWillClose = function () {
	        document.removeEventListener("focus", this.handleDocumentFocus, /* useCapture */ true);
	        document.removeEventListener("mousedown", this.handleDocumentClick);
	        var openStack = Overlay_1.openStack;
	        var stackIndex = openStack.indexOf(this);
	        if (stackIndex !== -1) {
	            openStack.splice(stackIndex, 1);
	            if (openStack.length > 0) {
	                var lastOpenedOverlay = Overlay_1.getLastOpened();
	                if (lastOpenedOverlay.props.enforceFocus) {
	                    document.addEventListener("focus", lastOpenedOverlay.handleDocumentFocus, /* useCapture */ true);
	                }
	            }
	            else {
	                document.body.classList.remove(Classes.OVERLAY_OPEN);
	            }
	        }
	    };
	    Overlay.prototype.overlayWillOpen = function () {
	        var openStack = Overlay_1.openStack;
	        if (openStack.length > 0) {
	            document.removeEventListener("focus", Overlay_1.getLastOpened().handleDocumentFocus, /* useCapture */ true);
	        }
	        openStack.push(this);
	        if (this.props.canOutsideClickClose && !this.props.hasBackdrop) {
	            document.addEventListener("mousedown", this.handleDocumentClick);
	        }
	        if (this.props.enforceFocus) {
	            document.addEventListener("focus", this.handleDocumentFocus, /* useCapture */ true);
	        }
	        if (this.props.inline) {
	            utils_1.safeInvoke(this.props.didOpen);
	            if (this.props.autoFocus) {
	                this.bringFocusInsideOverlay();
	            }
	        }
	        else if (this.props.hasBackdrop) {
	            // add a class to the body to prevent scrolling of content below the overlay
	            document.body.classList.add(Classes.OVERLAY_OPEN);
	        }
	    };
	    Overlay.prototype.bringFocusInsideOverlay = function () {
	        var _this = this;
	        // always delay focus manipulation to just before repaint to prevent scroll jumping
	        return requestAnimationFrame(function () {
	            var containerElement = _this.containerElement;
	            // container ref may be undefined between component mounting and Portal rendering
	            // activeElement may be undefined in some rare cases in IE
	            if (containerElement == null || document.activeElement == null || !_this.props.isOpen) {
	                return;
	            }
	            var isFocusOutsideModal = !containerElement.contains(document.activeElement);
	            if (isFocusOutsideModal) {
	                // element marked autofocus has higher priority than the other clowns
	                var autofocusElement = containerElement.query("[autofocus]");
	                var wrapperElement = containerElement.query("[tabindex]");
	                if (autofocusElement != null) {
	                    autofocusElement.focus();
	                }
	                else if (wrapperElement != null) {
	                    wrapperElement.focus();
	                }
	            }
	        });
	    };
	    return Overlay;
	}(React.Component));
	Overlay.displayName = "Blueprint.Overlay";
	Overlay.defaultProps = {
	    autoFocus: true,
	    backdropProps: {},
	    canEscapeKeyClose: true,
	    canOutsideClickClose: true,
	    enforceFocus: true,
	    hasBackdrop: true,
	    inline: false,
	    isOpen: false,
	    lazy: true,
	    transitionDuration: 300,
	    transitionName: "pt-overlay",
	};
	Overlay.openStack = [];
	Overlay.getLastOpened = function () { return Overlay_1.openStack[Overlay_1.openStack.length - 1]; };
	Overlay = Overlay_1 = tslib_1.__decorate([
	    PureRender
	], Overlay);
	exports.Overlay = Overlay;
	exports.OverlayFactory = React.createFactory(Overlay);
	var Overlay_1;
	
	//# sourceMappingURL=overlay.js.map


/***/ }),
/* 32 */
/***/ (function(module, exports) {

	module.exports = __WEBPACK_EXTERNAL_MODULE_32__;

/***/ }),
/* 33 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var React = __webpack_require__(7);
	var ReactDOM = __webpack_require__(23);
	var Classes = __webpack_require__(16);
	var Errors = __webpack_require__(10);
	var props_1 = __webpack_require__(14);
	var utils_1 = __webpack_require__(8);
	var REACT_CONTEXT_TYPES = {
	    blueprintPortalClassName: function (obj, key) {
	        if (obj[key] != null && typeof obj[key] !== "string") {
	            return new Error(Errors.PORTAL_CONTEXT_CLASS_NAME_STRING);
	        }
	        return undefined;
	    },
	};
	/**
	 * This component detaches its contents and re-attaches them to document.body.
	 * Use it when you need to circumvent DOM z-stacking (for dialogs, popovers, etc.).
	 * Any class names passed to this element will be propagated to the new container element on document.body.
	 */
	var Portal = (function (_super) {
	    tslib_1.__extends(Portal, _super);
	    function Portal() {
	        return _super !== null && _super.apply(this, arguments) || this;
	    }
	    Portal.prototype.render = function () {
	        return null;
	    };
	    Portal.prototype.componentDidMount = function () {
	        var targetElement = document.createElement("div");
	        targetElement.classList.add(Classes.PORTAL);
	        if (this.context.blueprintPortalClassName != null) {
	            targetElement.classList.add(this.context.blueprintPortalClassName);
	        }
	        document.body.appendChild(targetElement);
	        this.targetElement = targetElement;
	        this.componentDidUpdate();
	    };
	    Portal.prototype.componentDidUpdate = function () {
	        var _this = this;
	        // use special render function to preserve React context, in case children need it
	        ReactDOM.unstable_renderSubtreeIntoContainer(
	        /* parentComponent */ this, React.createElement("div", tslib_1.__assign({}, props_1.removeNonHTMLProps(this.props), { ref: this.props.containerRef }), this.props.children), this.targetElement, function () { return utils_1.safeInvoke(_this.props.onChildrenMount); });
	    };
	    Portal.prototype.componentWillUnmount = function () {
	        ReactDOM.unmountComponentAtNode(this.targetElement);
	        this.targetElement.remove();
	    };
	    return Portal;
	}(React.Component));
	Portal.displayName = "Blueprint.Portal";
	Portal.contextTypes = REACT_CONTEXT_TYPES;
	exports.Portal = Portal;
	
	//# sourceMappingURL=portal.js.map


/***/ }),
/* 34 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var PureRender = __webpack_require__(26);
	var React = __webpack_require__(7);
	var Classes = __webpack_require__(16);
	var position_1 = __webpack_require__(13);
	var popover_1 = __webpack_require__(24);
	var Tooltip = (function (_super) {
	    tslib_1.__extends(Tooltip, _super);
	    function Tooltip() {
	        return _super !== null && _super.apply(this, arguments) || this;
	    }
	    Tooltip.prototype.render = function () {
	        var _a = this.props, children = _a.children, intent = _a.intent, openOnTargetFocus = _a.openOnTargetFocus, tooltipClassName = _a.tooltipClassName;
	        var classes = classNames(Classes.TOOLTIP, Classes.intentClass(intent), tooltipClassName);
	        return (React.createElement(popover_1.Popover, tslib_1.__assign({}, this.props, { arrowSize: 22, autoFocus: false, canEscapeKeyClose: false, enforceFocus: false, interactionKind: popover_1.PopoverInteractionKind.HOVER_TARGET_ONLY, lazy: true, openOnTargetFocus: openOnTargetFocus, popoverClassName: classes }), children));
	    };
	    return Tooltip;
	}(React.Component));
	Tooltip.defaultProps = {
	    hoverCloseDelay: 0,
	    hoverOpenDelay: 100,
	    isDisabled: false,
	    openOnTargetFocus: true,
	    position: position_1.Position.TOP,
	    rootElementTag: "span",
	    transitionDuration: 100,
	    useSmartArrowPositioning: true,
	    useSmartPositioning: false,
	};
	Tooltip.displayName = "Blueprint.Tooltip";
	Tooltip = tslib_1.__decorate([
	    PureRender
	], Tooltip);
	exports.Tooltip = Tooltip;
	exports.TooltipFactory = React.createFactory(Tooltip);
	
	//# sourceMappingURL=tooltip.js.map


/***/ }),
/* 35 */
/***/ (function(module, exports, __webpack_require__) {

	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var position_1 = __webpack_require__(13);
	// this value causes popover and target edges to line up on 50px targets
	exports.MIN_ARROW_SPACING = 18;
	function computeArrowOffset(sideLength, arrowSize, minimum) {
	    if (minimum === void 0) { minimum = exports.MIN_ARROW_SPACING; }
	    return Math.max(Math.round((sideLength - arrowSize) / 2), minimum);
	}
	exports.computeArrowOffset = computeArrowOffset;
	function getPopoverTransformOrigin(position, arrowSize, targetDimensions) {
	    var offsetX = computeArrowOffset(targetDimensions.width, arrowSize);
	    var offsetY = computeArrowOffset(targetDimensions.height, arrowSize);
	    switch (position) {
	        case position_1.Position.TOP_LEFT:
	            return offsetX + "px bottom";
	        case position_1.Position.TOP_RIGHT:
	            return "calc(100% - " + offsetX + "px) bottom";
	        case position_1.Position.BOTTOM_LEFT:
	            return offsetX + "px top";
	        case position_1.Position.BOTTOM_RIGHT:
	            return "calc(100% - " + offsetX + "px) top";
	        case position_1.Position.LEFT_TOP:
	            return "right " + offsetY + "px";
	        case position_1.Position.LEFT_BOTTOM:
	            return "right calc(100% - " + offsetY + "px)";
	        case position_1.Position.RIGHT_TOP:
	            return "left " + offsetY + "px";
	        case position_1.Position.RIGHT_BOTTOM:
	            return "left calc(100% - " + offsetY + "px)";
	        default:
	            return undefined;
	    }
	}
	exports.getPopoverTransformOrigin = getPopoverTransformOrigin;
	function getArrowPositionStyles(position, arrowSize, ignoreTargetDimensions, targetDimensions, inline) {
	    // compute the offset to center an arrow with given hypotenuse in a side of the given length
	    var popoverOffset = function (sideLength) {
	        var offset = computeArrowOffset(sideLength, arrowSize, 0);
	        return offset < exports.MIN_ARROW_SPACING ? exports.MIN_ARROW_SPACING - offset : 0;
	    };
	    var popoverOffsetX = popoverOffset(targetDimensions.width);
	    var popoverOffsetY = popoverOffset(targetDimensions.height);
	    // TOP, RIGHT, BOTTOM, LEFT are handled purely in CSS because of centering transforms
	    switch (position) {
	        case position_1.Position.TOP_LEFT:
	        case position_1.Position.BOTTOM_LEFT:
	            return {
	                arrow: ignoreTargetDimensions ? {} : { left: computeArrowOffset(targetDimensions.width, arrowSize) },
	                container: { marginLeft: -popoverOffsetX },
	            };
	        case position_1.Position.TOP_RIGHT:
	        case position_1.Position.BOTTOM_RIGHT:
	            return {
	                arrow: ignoreTargetDimensions ? {} : { right: computeArrowOffset(targetDimensions.width, arrowSize) },
	                container: { marginLeft: popoverOffsetX },
	            };
	        case position_1.Position.RIGHT_TOP:
	        case position_1.Position.LEFT_TOP:
	            return {
	                arrow: ignoreTargetDimensions ? {} : { top: computeArrowOffset(targetDimensions.height, arrowSize) },
	                container: inline ? { top: -popoverOffsetY } : { marginTop: -popoverOffsetY },
	            };
	        case position_1.Position.RIGHT_BOTTOM:
	        case position_1.Position.LEFT_BOTTOM:
	            return {
	                arrow: ignoreTargetDimensions ? {} : { bottom: computeArrowOffset(targetDimensions.height, arrowSize) },
	                container: inline ? { bottom: -popoverOffsetY } : { marginTop: popoverOffsetY },
	            };
	        default:
	            return {};
	    }
	}
	exports.getArrowPositionStyles = getArrowPositionStyles;
	
	//# sourceMappingURL=arrows.js.map


/***/ }),
/* 36 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var React = __webpack_require__(7);
	var common_1 = __webpack_require__(4);
	var errors_1 = __webpack_require__(10);
	var buttons_1 = __webpack_require__(37);
	var dialog_1 = __webpack_require__(41);
	var icon_1 = __webpack_require__(39);
	var Alert = (function (_super) {
	    tslib_1.__extends(Alert, _super);
	    function Alert() {
	        return _super !== null && _super.apply(this, arguments) || this;
	    }
	    Alert.prototype.render = function () {
	        var _a = this.props, children = _a.children, className = _a.className, iconName = _a.iconName, intent = _a.intent, isOpen = _a.isOpen, confirmButtonText = _a.confirmButtonText, onConfirm = _a.onConfirm, style = _a.style;
	        return (React.createElement(dialog_1.Dialog, { className: classNames(common_1.Classes.ALERT, className), isOpen: isOpen, style: style },
	            React.createElement("div", { className: common_1.Classes.ALERT_BODY },
	                React.createElement(icon_1.Icon, { iconName: iconName, iconSize: "inherit", intent: common_1.Intent.DANGER }),
	                React.createElement("div", { className: common_1.Classes.ALERT_CONTENTS }, children)),
	            React.createElement("div", { className: common_1.Classes.ALERT_FOOTER },
	                React.createElement(buttons_1.Button, { intent: intent, text: confirmButtonText, onClick: onConfirm }),
	                this.maybeRenderSecondaryAction())));
	    };
	    Alert.prototype.validateProps = function (props) {
	        if (props.cancelButtonText != null && props.onCancel == null ||
	            props.cancelButtonText == null && props.onCancel != null) {
	            console.warn(errors_1.ALERT_WARN_CANCEL_PROPS);
	        }
	    };
	    Alert.prototype.maybeRenderSecondaryAction = function () {
	        if (this.props.cancelButtonText != null) {
	            return React.createElement(buttons_1.Button, { text: this.props.cancelButtonText, onClick: this.props.onCancel });
	        }
	        return undefined;
	    };
	    return Alert;
	}(common_1.AbstractComponent));
	Alert.defaultProps = {
	    confirmButtonText: "OK",
	    isOpen: false,
	    onConfirm: null,
	};
	Alert.displayName = "Blueprint.Alert";
	exports.Alert = Alert;
	
	//# sourceMappingURL=alert.js.map


/***/ }),
/* 37 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	// HACKHACK: these components should go in separate files
	// tslint:disable max-classes-per-file
	var React = __webpack_require__(7);
	var props_1 = __webpack_require__(14);
	var abstractButton_1 = __webpack_require__(38);
	var Button = (function (_super) {
	    tslib_1.__extends(Button, _super);
	    function Button() {
	        return _super !== null && _super.apply(this, arguments) || this;
	    }
	    Button.prototype.render = function () {
	        return (React.createElement("button", tslib_1.__assign({ type: "button" }, props_1.removeNonHTMLProps(this.props), this.getCommonButtonProps()), this.renderChildren()));
	    };
	    return Button;
	}(abstractButton_1.AbstractButton));
	Button.displayName = "Blueprint.Button";
	exports.Button = Button;
	exports.ButtonFactory = React.createFactory(Button);
	var AnchorButton = (function (_super) {
	    tslib_1.__extends(AnchorButton, _super);
	    function AnchorButton() {
	        return _super !== null && _super.apply(this, arguments) || this;
	    }
	    AnchorButton.prototype.render = function () {
	        var _a = this.props, href = _a.href, _b = _a.tabIndex, tabIndex = _b === void 0 ? 0 : _b;
	        var commonProps = this.getCommonButtonProps();
	        return (React.createElement("a", tslib_1.__assign({ role: "button" }, props_1.removeNonHTMLProps(this.props), commonProps, { href: commonProps.disabled ? undefined : href, tabIndex: commonProps.disabled ? undefined : tabIndex }), this.renderChildren()));
	    };
	    return AnchorButton;
	}(abstractButton_1.AbstractButton));
	AnchorButton.displayName = "Blueprint.AnchorButton";
	exports.AnchorButton = AnchorButton;
	exports.AnchorButtonFactory = React.createFactory(AnchorButton);
	
	//# sourceMappingURL=buttons.js.map


/***/ }),
/* 38 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var React = __webpack_require__(7);
	var Classes = __webpack_require__(16);
	var Keys = __webpack_require__(17);
	var utils_1 = __webpack_require__(8);
	var icon_1 = __webpack_require__(39);
	var spinner_1 = __webpack_require__(40);
	var AbstractButton = (function (_super) {
	    tslib_1.__extends(AbstractButton, _super);
	    function AbstractButton() {
	        var _this = _super !== null && _super.apply(this, arguments) || this;
	        _this.state = {
	            isActive: false,
	        };
	        _this.refHandlers = {
	            button: function (ref) {
	                _this.buttonRef = ref;
	                utils_1.safeInvoke(_this.props.elementRef, ref);
	            },
	        };
	        _this.currentKeyDown = null;
	        // we're casting as `any` to get around a somewhat opaque safeInvoke error
	        // that "Type argument candidate 'KeyboardEvent<T>' is not a valid type
	        // argument because it is not a supertype of candidate
	        // 'KeyboardEvent<HTMLElement>'."
	        _this.handleKeyDown = function (e) {
	            if (isKeyboardClick(e.which)) {
	                e.preventDefault();
	                if (e.which !== _this.currentKeyDown) {
	                    _this.setState({ isActive: true });
	                }
	            }
	            _this.currentKeyDown = e.which;
	            utils_1.safeInvoke(_this.props.onKeyDown, e);
	        };
	        _this.handleKeyUp = function (e) {
	            if (isKeyboardClick(e.which)) {
	                _this.setState({ isActive: false });
	                _this.buttonRef.click();
	            }
	            _this.currentKeyDown = null;
	            utils_1.safeInvoke(_this.props.onKeyUp, e);
	        };
	        return _this;
	    }
	    AbstractButton.prototype.getCommonButtonProps = function () {
	        var disabled = this.props.disabled || this.props.loading;
	        var className = classNames(Classes.BUTTON, (_a = {},
	            _a[Classes.ACTIVE] = this.state.isActive || this.props.active,
	            _a[Classes.DISABLED] = disabled,
	            _a[Classes.LOADING] = this.props.loading,
	            _a), Classes.iconClass(this.props.iconName), Classes.intentClass(this.props.intent), this.props.className);
	        return {
	            className: className,
	            disabled: disabled,
	            onClick: disabled ? undefined : this.props.onClick,
	            onKeyDown: this.handleKeyDown,
	            onKeyUp: this.handleKeyUp,
	            ref: this.refHandlers.button,
	        };
	        var _a;
	    };
	    AbstractButton.prototype.renderChildren = function () {
	        var _a = this.props, loading = _a.loading, rightIconName = _a.rightIconName, text = _a.text;
	        var children = React.Children.map(this.props.children, function (child, index) {
	            // must wrap string children in spans so loading prop can hide them
	            if (typeof child === "string") {
	                return React.createElement("span", { key: "text-" + index }, child);
	            }
	            return child;
	        });
	        return [
	            loading ? React.createElement(spinner_1.Spinner, { className: "pt-small pt-button-spinner", key: "spinner" }) : undefined,
	            text != null ? React.createElement("span", { key: "text" }, text) : undefined
	        ].concat(children, [
	            React.createElement(icon_1.Icon, { className: Classes.ALIGN_RIGHT, iconName: rightIconName, key: "icon" }),
	        ]);
	    };
	    return AbstractButton;
	}(React.Component));
	exports.AbstractButton = AbstractButton;
	function isKeyboardClick(keyCode) {
	    return keyCode === Keys.ENTER || keyCode === Keys.SPACE;
	}
	
	//# sourceMappingURL=abstractButton.js.map


/***/ }),
/* 39 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var PureRender = __webpack_require__(26);
	var React = __webpack_require__(7);
	var common_1 = __webpack_require__(4);
	var Icon = Icon_1 = (function (_super) {
	    tslib_1.__extends(Icon, _super);
	    function Icon() {
	        return _super !== null && _super.apply(this, arguments) || this;
	    }
	    Icon.prototype.render = function () {
	        if (this.props.iconName == null) {
	            return null;
	        }
	        var _a = this.props, className = _a.className, iconName = _a.iconName, intent = _a.intent, _b = _a.iconSize, iconSize = _b === void 0 ? Icon_1.SIZE_STANDARD : _b, restProps = tslib_1.__rest(_a, ["className", "iconName", "intent", "iconSize"]);
	        var classes = classNames(getSizeClass(iconSize), common_1.Classes.iconClass(iconName), common_1.Classes.intentClass(intent), className);
	        return React.createElement("span", tslib_1.__assign({ className: classes }, restProps));
	    };
	    return Icon;
	}(React.Component));
	Icon.displayName = "Blueprint.Icon";
	Icon.SIZE_STANDARD = 16;
	Icon.SIZE_LARGE = 20;
	Icon.SIZE_INHERIT = "inherit";
	Icon = Icon_1 = tslib_1.__decorate([
	    PureRender
	], Icon);
	exports.Icon = Icon;
	;
	// NOTE: not using a type alias here so the full union will appear in the interface docs
	function getSizeClass(size) {
	    switch (size) {
	        case Icon.SIZE_STANDARD: return common_1.Classes.ICON_STANDARD;
	        case Icon.SIZE_LARGE: return common_1.Classes.ICON_LARGE;
	        default: return common_1.Classes.ICON;
	    }
	}
	var Icon_1;
	
	//# sourceMappingURL=icon.js.map


/***/ }),
/* 40 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var PureRender = __webpack_require__(26);
	var React = __webpack_require__(7);
	var Classes = __webpack_require__(16);
	var utils_1 = __webpack_require__(8);
	// see http://stackoverflow.com/a/18473154/3124288 for calculating arc path
	var SPINNER_TRACK = "M 50,50 m 0,-44.5 a 44.5,44.5 0 1 1 0,89 a 44.5,44.5 0 1 1 0,-89";
	// unitless total length of SVG path, to which stroke-dash* properties are relative.
	// https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/pathLength
	// this value is the result of `<path d={SPINNER_TRACK} />.getTotalLength()` and works in all browsers:
	var PATH_LENGTH = 280;
	var Spinner = (function (_super) {
	    tslib_1.__extends(Spinner, _super);
	    function Spinner() {
	        return _super !== null && _super.apply(this, arguments) || this;
	    }
	    Spinner.prototype.render = function () {
	        var _a = this.props, className = _a.className, intent = _a.intent, value = _a.value;
	        var classes = classNames(Classes.SPINNER, Classes.intentClass(intent), {
	            "pt-no-spin": value != null,
	        }, className);
	        var style = {
	            strokeDasharray: PATH_LENGTH + " " + PATH_LENGTH,
	            // default to quarter-circle when indeterminate
	            // IE11: CSS transitions on SVG elements are Not Supported :(
	            strokeDashoffset: PATH_LENGTH - PATH_LENGTH * (value == null ? 0.25 : utils_1.clamp(value, 0, 1)),
	        };
	        // HACKHACK to temporarily squash error regarding React.SVGProps missing prop pathLength
	        var headElement = React.createElement("path", {
	            className: "pt-spinner-head",
	            d: SPINNER_TRACK,
	            pathLength: PATH_LENGTH,
	            style: style,
	        });
	        return this.renderContainer(classes, (React.createElement("svg", { viewBox: classes.indexOf(Classes.SMALL) >= 0 ? "-15 -15 130 130" : "0 0 100 100" },
	            React.createElement("path", { className: "pt-spinner-track", d: SPINNER_TRACK }),
	            headElement)));
	    };
	    // abstract away the container elements so SVGSpinner can do its own thing
	    Spinner.prototype.renderContainer = function (classes, content) {
	        return (React.createElement("div", { className: classes },
	            React.createElement("div", { className: "pt-spinner-svg-container" }, content)));
	    };
	    return Spinner;
	}(React.Component));
	Spinner.displayName = "Blueprint.Spinner";
	Spinner = tslib_1.__decorate([
	    PureRender
	], Spinner);
	exports.Spinner = Spinner;
	exports.SpinnerFactory = React.createFactory(Spinner);
	
	//# sourceMappingURL=spinner.js.map


/***/ }),
/* 41 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var React = __webpack_require__(7);
	var abstractComponent_1 = __webpack_require__(5);
	var Classes = __webpack_require__(16);
	var Errors = __webpack_require__(10);
	var utils_1 = __webpack_require__(8);
	var icon_1 = __webpack_require__(39);
	var overlay_1 = __webpack_require__(31);
	var Dialog = (function (_super) {
	    tslib_1.__extends(Dialog, _super);
	    function Dialog() {
	        var _this = _super !== null && _super.apply(this, arguments) || this;
	        _this.handleContainerMouseDown = function (evt) {
	            // quick re-implementation of canOutsideClickClose because .pt-dialog-container covers the backdrop
	            var isClickOutsideDialog = evt.target.closest("." + Classes.DIALOG) == null;
	            if (isClickOutsideDialog && _this.props.canOutsideClickClose) {
	                utils_1.safeInvoke(_this.props.onClose, evt);
	            }
	        };
	        return _this;
	    }
	    Dialog.prototype.render = function () {
	        return (React.createElement(overlay_1.Overlay, tslib_1.__assign({}, this.props, { className: Classes.OVERLAY_SCROLL_CONTAINER, hasBackdrop: true }),
	            React.createElement("div", { className: Classes.DIALOG_CONTAINER, onMouseDown: this.handleContainerMouseDown },
	                React.createElement("div", { className: classNames(Classes.DIALOG, this.props.className), style: this.props.style },
	                    this.maybeRenderHeader(),
	                    this.props.children))));
	    };
	    Dialog.prototype.validateProps = function (props) {
	        if (props.title == null) {
	            if (props.iconName != null) {
	                console.warn(Errors.DIALOG_WARN_NO_HEADER_ICON);
	            }
	            if (props.isCloseButtonShown != null) {
	                console.warn(Errors.DIALOG_WARN_NO_HEADER_CLOSE_BUTTON);
	            }
	        }
	    };
	    Dialog.prototype.maybeRenderCloseButton = function () {
	        // for now, show close button if prop is undefined or null
	        // this gives us a behavior as if the default value were `true`
	        if (this.props.isCloseButtonShown !== false) {
	            var classes = classNames(Classes.DIALOG_CLOSE_BUTTON, Classes.iconClass("small-cross"));
	            return React.createElement("button", { "aria-label": "Close", className: classes, onClick: this.props.onClose });
	        }
	        else {
	            return undefined;
	        }
	    };
	    Dialog.prototype.maybeRenderHeader = function () {
	        var _a = this.props, iconName = _a.iconName, title = _a.title;
	        if (title == null) {
	            return undefined;
	        }
	        return (React.createElement("div", { className: Classes.DIALOG_HEADER },
	            React.createElement(icon_1.Icon, { iconName: iconName, iconSize: 20 }),
	            React.createElement("h5", null, title),
	            this.maybeRenderCloseButton()));
	    };
	    return Dialog;
	}(abstractComponent_1.AbstractComponent));
	Dialog.defaultProps = {
	    canOutsideClickClose: true,
	    isOpen: false,
	};
	Dialog.displayName = "Blueprint.Dialog";
	exports.Dialog = Dialog;
	exports.DialogFactory = React.createFactory(Dialog);
	
	//# sourceMappingURL=dialog.js.map


/***/ }),
/* 42 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var classNames = __webpack_require__(25);
	var React = __webpack_require__(7);
	var Classes = __webpack_require__(16);
	exports.Breadcrumb = function (breadcrumbProps) {
	    var classes = classNames(Classes.BREADCRUMB, (_a = {},
	        _a[Classes.DISABLED] = breadcrumbProps.disabled,
	        _a), breadcrumbProps.className);
	    return (React.createElement("a", { className: classes, href: breadcrumbProps.href, onClick: breadcrumbProps.disabled ? null : breadcrumbProps.onClick, tabIndex: breadcrumbProps.disabled ? null : 0, target: breadcrumbProps.target }, breadcrumbProps.text));
	    var _a;
	};
	
	//# sourceMappingURL=breadcrumb.js.map


/***/ }),
/* 43 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var React = __webpack_require__(7);
	var abstractComponent_1 = __webpack_require__(5);
	var Classes = __webpack_require__(16);
	var AnimationStates;
	(function (AnimationStates) {
	    AnimationStates[AnimationStates["CLOSED"] = 0] = "CLOSED";
	    AnimationStates[AnimationStates["OPENING"] = 1] = "OPENING";
	    AnimationStates[AnimationStates["OPEN"] = 2] = "OPEN";
	    AnimationStates[AnimationStates["CLOSING_START"] = 3] = "CLOSING_START";
	    AnimationStates[AnimationStates["CLOSING_END"] = 4] = "CLOSING_END";
	})(AnimationStates = exports.AnimationStates || (exports.AnimationStates = {}));
	/*
	 * A collapse can be in one of 5 states:
	 * CLOSED
	 * When in this state, the contents of the collapse is not rendered, the collapse height is 0,
	 * and the body Y is at -height (so that the bottom of the body is at Y=0).
	 *
	 * OPEN
	 * When in this state, the collapse height is set to auto, and the body Y is set to 0 (so the element can be seen
	 * as normal).
	 *
	 * CLOSING_START
	 * When in this state, height has been changed from auto to the measured height of the body to prepare for the
	 * closing animation in CLOSING_END.
	 *
	 * CLOSING_END
	 * When in this state, the height is set to 0 and the body Y is at -height. Both of these properties are transformed,
	 * and then after the animation is complete, the state changes to CLOSED.
	 *
	 * OPENING
	 * When in this state, the body is re-rendered, height is set to the measured body height and the body Y is set to 0.
	 * This is all animated, and on complete, the state changes to OPEN.
	 *
	 * When changing the isOpen prop, the following happens to the states:
	 * isOpen = true : CLOSED -> OPENING -> OPEN
	 * isOpen = false: OPEN -> CLOSING_START -> CLOSING_END -> CLOSED
	 * These are all animated.
	 */
	var Collapse = (function (_super) {
	    tslib_1.__extends(Collapse, _super);
	    function Collapse() {
	        var _this = _super !== null && _super.apply(this, arguments) || this;
	        _this.state = {
	            animationState: AnimationStates.OPEN,
	            height: "0px",
	        };
	        // The most recent non-0 height (once a height has been measured - is 0 until then)
	        _this.height = 0;
	        _this.contentsRefHandler = function (el) {
	            _this.contents = el;
	            if (el != null) {
	                _this.height = _this.contents.clientHeight;
	                _this.setState({
	                    animationState: _this.props.isOpen ? AnimationStates.OPEN : AnimationStates.CLOSED,
	                    height: _this.height + "px",
	                });
	            }
	        };
	        return _this;
	    }
	    Collapse.prototype.componentWillReceiveProps = function (nextProps) {
	        var _this = this;
	        if (this.contents != null && this.contents.clientHeight !== 0) {
	            this.height = this.contents.clientHeight;
	        }
	        if (this.props.isOpen !== nextProps.isOpen) {
	            this.clearTimeouts();
	            if (this.state.animationState !== AnimationStates.CLOSED && !nextProps.isOpen) {
	                this.setState({
	                    animationState: AnimationStates.CLOSING_START,
	                    height: this.height + "px",
	                });
	            }
	            else if (this.state.animationState !== AnimationStates.OPEN && nextProps.isOpen) {
	                this.setState({
	                    animationState: AnimationStates.OPENING,
	                    height: this.height + "px",
	                });
	                this.setTimeout(function () { return _this.onDelayedStateChange(); }, this.props.transitionDuration);
	            }
	        }
	    };
	    Collapse.prototype.render = function () {
	        var showContents = this.state.animationState !== AnimationStates.CLOSED;
	        var displayWithTransform = showContents && this.state.animationState !== AnimationStates.CLOSING_END;
	        var isAutoHeight = this.state.height === "auto";
	        var containerStyle = {
	            height: showContents ? this.state.height : undefined,
	            overflowY: (isAutoHeight ? "visible" : undefined),
	            transition: isAutoHeight ? "none" : undefined,
	        };
	        var contentsStyle = {
	            transform: displayWithTransform ? "translateY(0)" : "translateY(-" + this.height + "px)",
	            transition: isAutoHeight ? "none" : undefined,
	        };
	        // HACKHACK: type cast because there's no single overload that supports all
	        // three ReactTypes (string | ComponentClass | StatelessComponent)
	        return React.createElement(this.props.component, {
	            className: classNames(Classes.COLLAPSE, this.props.className),
	            style: containerStyle,
	        }, (React.createElement("div", { className: "pt-collapse-body", ref: this.contentsRefHandler, style: contentsStyle }, showContents ? this.props.children : null)));
	    };
	    Collapse.prototype.componentDidMount = function () {
	        this.forceUpdate();
	        if (this.props.isOpen) {
	            this.setState({ animationState: AnimationStates.OPEN, height: "auto" });
	        }
	        else {
	            this.setState({ animationState: AnimationStates.CLOSED });
	        }
	    };
	    Collapse.prototype.componentDidUpdate = function () {
	        var _this = this;
	        if (this.state.animationState === AnimationStates.CLOSING_START) {
	            this.setTimeout(function () { return _this.setState({
	                animationState: AnimationStates.CLOSING_END,
	                height: "0px",
	            }); });
	            this.setTimeout(function () { return _this.onDelayedStateChange(); }, this.props.transitionDuration);
	        }
	    };
	    Collapse.prototype.onDelayedStateChange = function () {
	        switch (this.state.animationState) {
	            case AnimationStates.OPENING:
	                this.setState({ animationState: AnimationStates.OPEN, height: "auto" });
	                break;
	            case AnimationStates.CLOSING_END:
	                this.setState({ animationState: AnimationStates.CLOSED });
	                break;
	            default:
	                break;
	        }
	    };
	    return Collapse;
	}(abstractComponent_1.AbstractComponent));
	Collapse.displayName = "Blueprint.Collapse";
	Collapse.defaultProps = {
	    component: "div",
	    isOpen: false,
	    transitionDuration: 200,
	};
	exports.Collapse = Collapse;
	
	//# sourceMappingURL=collapse.js.map


/***/ }),
/* 44 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var React = __webpack_require__(7);
	var Classes = __webpack_require__(16);
	var Errors = __webpack_require__(10);
	var position_1 = __webpack_require__(13);
	var menu_1 = __webpack_require__(45);
	var menuItem_1 = __webpack_require__(46);
	var popover_1 = __webpack_require__(24);
	var CollapseFrom;
	(function (CollapseFrom) {
	    CollapseFrom[CollapseFrom["START"] = 0] = "START";
	    CollapseFrom[CollapseFrom["END"] = 1] = "END";
	})(CollapseFrom = exports.CollapseFrom || (exports.CollapseFrom = {}));
	var CollapsibleList = (function (_super) {
	    tslib_1.__extends(CollapsibleList, _super);
	    function CollapsibleList() {
	        return _super !== null && _super.apply(this, arguments) || this;
	    }
	    CollapsibleList.prototype.render = function () {
	        var _this = this;
	        var collapseFrom = this.props.collapseFrom;
	        var childrenLength = React.Children.count(this.props.children);
	        var _a = this.partitionChildren(), visibleChildren = _a[0], collapsedChildren = _a[1];
	        var visibleItems = visibleChildren.map(function (child, index) {
	            var absoluteIndex = (collapseFrom === CollapseFrom.START ? childrenLength - 1 - index : index);
	            return (React.createElement("li", { className: _this.props.visibleItemClassName, key: absoluteIndex }, _this.props.renderVisibleItem(child.props, absoluteIndex)));
	        });
	        if (collapseFrom === CollapseFrom.START) {
	            // reverse START list so separators appear before items
	            visibleItems.reverse();
	        }
	        // construct dropdown menu for collapsed items
	        var collapsedPopover;
	        if (collapsedChildren.length > 0) {
	            var position = (collapseFrom === CollapseFrom.END ? position_1.Position.BOTTOM_RIGHT : position_1.Position.BOTTOM_LEFT);
	            collapsedPopover = (React.createElement("li", { className: this.props.visibleItemClassName },
	                React.createElement(popover_1.Popover, tslib_1.__assign({ content: React.createElement(menu_1.Menu, null, collapsedChildren), position: position }, this.props.dropdownProps), this.props.dropdownTarget)));
	        }
	        return (React.createElement("ul", { className: classNames(Classes.COLLAPSIBLE_LIST, this.props.className) },
	            collapseFrom === CollapseFrom.START ? collapsedPopover : null,
	            visibleItems,
	            collapseFrom === CollapseFrom.END ? collapsedPopover : null));
	    };
	    // splits the list of children into two arrays: visible and collapsed
	    CollapsibleList.prototype.partitionChildren = function () {
	        if (this.props.children == null) {
	            return [[], []];
	        }
	        var childrenArray = React.Children.map(this.props.children, function (child, index) {
	            if (child.type !== menuItem_1.MenuItem) {
	                throw new Error(Errors.COLLAPSIBLE_LIST_INVALID_CHILD);
	            }
	            return React.cloneElement(child, { key: "visible-" + index });
	        });
	        if (this.props.collapseFrom === CollapseFrom.START) {
	            // reverse START list so we can always slice visible items from the front of the list
	            childrenArray.reverse();
	        }
	        var visibleItemCount = this.props.visibleItemCount;
	        return [
	            childrenArray.slice(0, visibleItemCount),
	            childrenArray.slice(visibleItemCount),
	        ];
	    };
	    return CollapsibleList;
	}(React.Component));
	CollapsibleList.displayName = "Blueprint.CollapsibleList";
	CollapsibleList.defaultProps = {
	    collapseFrom: CollapseFrom.START,
	    dropdownTarget: null,
	    renderVisibleItem: null,
	    visibleItemCount: 3,
	};
	exports.CollapsibleList = CollapsibleList;
	exports.CollapsibleListFactory = React.createFactory(CollapsibleList);
	
	//# sourceMappingURL=collapsibleList.js.map


/***/ }),
/* 45 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var React = __webpack_require__(7);
	var Classes = __webpack_require__(16);
	var Menu = (function (_super) {
	    tslib_1.__extends(Menu, _super);
	    function Menu() {
	        return _super !== null && _super.apply(this, arguments) || this;
	    }
	    Menu.prototype.render = function () {
	        return (React.createElement("ul", { className: classNames(Classes.MENU, this.props.className), ref: this.props.ulRef }, this.props.children));
	    };
	    return Menu;
	}(React.Component));
	Menu.displayName = "Blueprint.Menu";
	exports.Menu = Menu;
	exports.MenuFactory = React.createFactory(Menu);
	
	//# sourceMappingURL=menu.js.map


/***/ }),
/* 46 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var React = __webpack_require__(7);
	var ReactDOM = __webpack_require__(23);
	var abstractComponent_1 = __webpack_require__(5);
	var Classes = __webpack_require__(16);
	var Errors = __webpack_require__(10);
	var position_1 = __webpack_require__(13);
	var popover_1 = __webpack_require__(24);
	var menu_1 = __webpack_require__(45);
	var REACT_CONTEXT_TYPES = {
	    alignLeft: function (obj, key) {
	        if (obj[key] != null && typeof obj[key] !== "boolean") {
	            return new Error("[Blueprint] MenuItem context alignLeft must be boolean");
	        }
	        return undefined;
	    },
	};
	var MenuItem = (function (_super) {
	    tslib_1.__extends(MenuItem, _super);
	    function MenuItem() {
	        var _this = _super !== null && _super.apply(this, arguments) || this;
	        _this.state = {
	            alignLeft: false,
	        };
	        _this.liRefHandler = function (r) { return _this.liElement = r; };
	        _this.measureSubmenu = function (el) {
	            if (el != null) {
	                var submenuRect = ReactDOM.findDOMNode(el).getBoundingClientRect();
	                var parentWidth = _this.liElement.parentElement.getBoundingClientRect().width;
	                var adjustmentWidth = submenuRect.width + parentWidth;
	                // this ensures that the left and right measurements represent a submenu opened to the right
	                var submenuLeft = submenuRect.left;
	                var submenuRight = submenuRect.right;
	                if (_this.state.alignLeft) {
	                    submenuLeft += adjustmentWidth;
	                    submenuRight += adjustmentWidth;
	                }
	                var _a = _this.props.submenuViewportMargin.left, left = _a === void 0 ? 0 : _a;
	                var _b = _this.props.submenuViewportMargin.right, right = _b === void 0 ? 0 : _b;
	                if (typeof document !== "undefined"
	                    && typeof document.documentElement !== "undefined"
	                    && Number(document.documentElement.clientWidth)) {
	                    // we're in a browser context and the clientWidth is available,
	                    // use it to set calculate 'right'
	                    right = document.documentElement.clientWidth - right;
	                }
	                // uses context to prioritize the previous positioning
	                var alignLeft = _this.context.alignLeft || false;
	                if (alignLeft) {
	                    if ((submenuLeft - adjustmentWidth) <= left) {
	                        alignLeft = false;
	                    }
	                }
	                else {
	                    if (submenuRight >= right) {
	                        alignLeft = true;
	                    }
	                }
	                _this.setState({ alignLeft: alignLeft });
	            }
	        };
	        _this.renderChildren = function () {
	            var _a = _this.props, children = _a.children, submenu = _a.submenu;
	            if (children != null) {
	                var childProps_1 = _this.cascadeProps();
	                if (Object.keys(childProps_1).length === 0) {
	                    return children;
	                }
	                else {
	                    return React.Children.map(children, function (child) {
	                        return React.cloneElement(child, childProps_1);
	                    });
	                }
	            }
	            else if (submenu != null) {
	                return submenu.map(_this.cascadeProps).map(renderMenuItem);
	            }
	            else {
	                return undefined;
	            }
	        };
	        /**
	         * Evalutes this.props and cascades prop values into new props when:
	         * - submenuViewportMargin is defined, but is undefined for the supplied input.
	         * - useSmartPositioning is false, but is undefined for the supplied input.
	         * @param {IMenuItemProps} newProps If supplied, object will be modified, otherwise, defaults to an empty object.
	         * @returns An object to be used as child props.
	         */
	        _this.cascadeProps = function (newProps) {
	            if (newProps === void 0) { newProps = {}; }
	            var _a = _this.props, submenuViewportMargin = _a.submenuViewportMargin, useSmartPositioning = _a.useSmartPositioning;
	            if (submenuViewportMargin != null && newProps.submenuViewportMargin == null) {
	                newProps.submenuViewportMargin = submenuViewportMargin;
	            }
	            if (useSmartPositioning === false && newProps.useSmartPositioning == null) {
	                newProps.useSmartPositioning = useSmartPositioning;
	            }
	            return newProps;
	        };
	        return _this;
	    }
	    MenuItem.prototype.render = function () {
	        var _a = this.props, children = _a.children, disabled = _a.disabled, label = _a.label, submenu = _a.submenu;
	        var hasSubmenu = children != null || submenu != null;
	        var liClasses = classNames((_b = {},
	            _b[Classes.MENU_SUBMENU] = hasSubmenu,
	            _b));
	        var anchorClasses = classNames(Classes.MENU_ITEM, Classes.intentClass(this.props.intent), (_c = {},
	            _c[Classes.DISABLED] = disabled,
	            // prevent popover from closing when clicking on submenu trigger or disabled item
	            _c[Classes.POPOVER_DISMISS] = this.props.shouldDismissPopover && !disabled && !hasSubmenu,
	            _c), Classes.iconClass(this.props.iconName), this.props.className);
	        var labelElement;
	        if (label != null) {
	            labelElement = React.createElement("span", { className: "pt-menu-item-label" }, label);
	        }
	        var content = (React.createElement("a", { className: anchorClasses, href: disabled ? undefined : this.props.href, onClick: disabled ? undefined : this.props.onClick, tabIndex: disabled ? undefined : 0, target: this.props.target },
	            labelElement,
	            this.props.text));
	        if (hasSubmenu) {
	            var measureSubmenu = (this.props.useSmartPositioning) ? this.measureSubmenu : null;
	            var submenuElement = React.createElement(menu_1.Menu, { ref: measureSubmenu }, this.renderChildren());
	            var popoverClasses = classNames((_d = {},
	                _d[Classes.ALIGN_LEFT] = this.state.alignLeft,
	                _d));
	            content = (React.createElement(popover_1.Popover, { content: submenuElement, isDisabled: disabled, enforceFocus: false, hoverCloseDelay: 0, inline: true, interactionKind: popover_1.PopoverInteractionKind.HOVER, position: this.state.alignLeft ? position_1.Position.LEFT_TOP : position_1.Position.RIGHT_TOP, popoverClassName: classNames(Classes.MINIMAL, Classes.MENU_SUBMENU, popoverClasses), useSmartArrowPositioning: false }, content));
	        }
	        return (React.createElement("li", { className: liClasses, ref: this.liRefHandler }, content));
	        var _b, _c, _d;
	    };
	    MenuItem.prototype.getChildContext = function () {
	        return { alignLeft: this.state.alignLeft };
	    };
	    MenuItem.prototype.validateProps = function (props) {
	        if (props.children != null && props.submenu != null) {
	            console.warn(Errors.MENU_WARN_CHILDREN_SUBMENU_MUTEX);
	        }
	    };
	    return MenuItem;
	}(abstractComponent_1.AbstractComponent));
	MenuItem.defaultProps = {
	    disabled: false,
	    shouldDismissPopover: true,
	    submenuViewportMargin: {},
	    text: "",
	    useSmartPositioning: true,
	};
	MenuItem.displayName = "Blueprint.MenuItem";
	MenuItem.contextTypes = REACT_CONTEXT_TYPES;
	MenuItem.childContextTypes = REACT_CONTEXT_TYPES;
	exports.MenuItem = MenuItem;
	function renderMenuItem(props, key) {
	    return React.createElement(MenuItem, tslib_1.__assign({ key: key }, props));
	}
	exports.renderMenuItem = renderMenuItem;
	exports.MenuItemFactory = React.createFactory(MenuItem);
	
	//# sourceMappingURL=menuItem.js.map


/***/ }),
/* 47 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var React = __webpack_require__(7);
	var errors_1 = __webpack_require__(10);
	var utils_1 = __webpack_require__(8);
	var ContextMenu = __webpack_require__(22);
	function ContextMenuTarget(constructor) {
	    var _a = constructor.prototype, render = _a.render, renderContextMenu = _a.renderContextMenu, onContextMenuClose = _a.onContextMenuClose;
	    if (!utils_1.isFunction(renderContextMenu)) {
	        console.warn(errors_1.CONTEXTMENU_WARN_DECORATOR_NO_METHOD);
	    }
	    // patching classes like this requires preserving function context
	    // tslint:disable-next-line only-arrow-functions
	    constructor.prototype.render = function () {
	        var _this = this;
	        /* tslint:disable:no-invalid-this */
	        var element = render.call(this);
	        if (element == null) {
	            // always return `element` in case caller is distinguishing between `null` and `undefined`
	            return element;
	        }
	        var oldOnContextMenu = element.props.onContextMenu;
	        var onContextMenu = function (e) {
	            // support nested menus (inner menu target would have called preventDefault())
	            if (e.defaultPrevented) {
	                return;
	            }
	            if (utils_1.isFunction(_this.renderContextMenu)) {
	                var menu = _this.renderContextMenu(e);
	                if (menu != null) {
	                    e.preventDefault();
	                    ContextMenu.show(menu, { left: e.clientX, top: e.clientY }, onContextMenuClose);
	                }
	            }
	            utils_1.safeInvoke(oldOnContextMenu, e);
	        };
	        return React.cloneElement(element, { onContextMenu: onContextMenu });
	        /* tslint:enable:no-invalid-this */
	    };
	}
	exports.ContextMenuTarget = ContextMenuTarget;
	;
	
	//# sourceMappingURL=contextMenuTarget.js.map


/***/ }),
/* 48 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var PureRender = __webpack_require__(26);
	var React = __webpack_require__(7);
	var abstractComponent_1 = __webpack_require__(5);
	var Classes = __webpack_require__(16);
	var Keys = __webpack_require__(17);
	var utils_1 = __webpack_require__(8);
	var compatibility_1 = __webpack_require__(49);
	var BUFFER_WIDTH_EDGE = 5;
	var BUFFER_WIDTH_IE = 30;
	var EditableText = (function (_super) {
	    tslib_1.__extends(EditableText, _super);
	    function EditableText(props, context) {
	        var _this = _super.call(this, props, context) || this;
	        _this.refHandlers = {
	            content: function (spanElement) {
	                _this.valueElement = spanElement;
	            },
	            input: function (input) {
	                if (input != null) {
	                    input.focus();
	                    var length_1 = input.value.length;
	                    input.setSelectionRange(_this.props.selectAllOnFocus ? 0 : length_1, length_1);
	                }
	            },
	        };
	        _this.cancelEditing = function () {
	            var _a = _this.state, lastValue = _a.lastValue, value = _a.value;
	            _this.setState({ isEditing: false, value: lastValue });
	            if (value !== lastValue) {
	                utils_1.safeInvoke(_this.props.onChange, lastValue);
	            }
	            utils_1.safeInvoke(_this.props.onCancel, lastValue);
	        };
	        _this.toggleEditing = function () {
	            if (_this.state.isEditing) {
	                var value = _this.state.value;
	                _this.setState({ isEditing: false, lastValue: value });
	                utils_1.safeInvoke(_this.props.onConfirm, value);
	            }
	            else if (!_this.props.disabled) {
	                _this.setState({ isEditing: true });
	            }
	        };
	        _this.handleFocus = function () {
	            if (!_this.props.disabled) {
	                _this.setState({ isEditing: true });
	            }
	        };
	        _this.handleTextChange = function (event) {
	            var value = event.target.value;
	            // state value should be updated only when uncontrolled
	            if (_this.props.value == null) {
	                _this.setState({ value: value });
	            }
	            utils_1.safeInvoke(_this.props.onChange, value);
	        };
	        _this.handleKeyEvent = function (event) {
	            var altKey = event.altKey, ctrlKey = event.ctrlKey, metaKey = event.metaKey, shiftKey = event.shiftKey, which = event.which;
	            if (which === Keys.ESCAPE) {
	                _this.cancelEditing();
	                return;
	            }
	            var hasModifierKey = altKey || ctrlKey || metaKey || shiftKey;
	            if (which === Keys.ENTER) {
	                // prevent IE11 from full screening with alt + enter
	                // shift + enter adds a newline by default
	                if (altKey || shiftKey) {
	                    event.preventDefault();
	                }
	                if (_this.props.confirmOnEnterKey && _this.props.multiline) {
	                    if (event.target != null && hasModifierKey) {
	                        insertAtCaret(event.target, "\n");
	                        _this.handleTextChange(event);
	                    }
	                    else {
	                        _this.toggleEditing();
	                    }
	                }
	                else if (!_this.props.multiline || hasModifierKey) {
	                    _this.toggleEditing();
	                }
	            }
	        };
	        var value = (props.value == null) ? props.defaultValue : props.value;
	        _this.state = {
	            inputHeight: 0,
	            inputWidth: 0,
	            isEditing: props.isEditing === true && props.disabled === false,
	            lastValue: value,
	            value: value,
	        };
	        return _this;
	    }
	    EditableText.prototype.render = function () {
	        var _a = this.props, disabled = _a.disabled, multiline = _a.multiline;
	        var value = (this.props.value == null ? this.state.value : this.props.value);
	        var hasValue = (value != null && value !== "");
	        var classes = classNames(Classes.EDITABLE_TEXT, Classes.intentClass(this.props.intent), (_b = {},
	            _b[Classes.DISABLED] = disabled,
	            _b["pt-editable-editing"] = this.state.isEditing,
	            _b["pt-editable-placeholder"] = !hasValue,
	            _b["pt-multiline"] = multiline,
	            _b), this.props.className);
	        var contentStyle;
	        if (multiline) {
	            // set height only in multiline mode when not editing
	            // otherwise we're measuring this element to determine appropriate height of text
	            contentStyle = { height: !this.state.isEditing ? this.state.inputHeight : null };
	        }
	        else {
	            // minWidth only applies in single line mode (multiline == width 100%)
	            contentStyle = {
	                height: this.state.inputHeight,
	                lineHeight: this.state.inputHeight != null ? this.state.inputHeight + "px" : null,
	                minWidth: this.props.minWidth,
	            };
	        }
	        // make enclosing div focusable when not editing, so it can still be tabbed to focus
	        // (when editing, input itself is focusable so div doesn't need to be)
	        var tabIndex = this.state.isEditing || disabled ? null : 0;
	        return (React.createElement("div", { className: classes, onFocus: this.handleFocus, tabIndex: tabIndex },
	            this.maybeRenderInput(value),
	            React.createElement("span", { className: "pt-editable-content", ref: this.refHandlers.content, style: contentStyle }, hasValue ? value : this.props.placeholder)));
	        var _b;
	    };
	    EditableText.prototype.componentDidMount = function () {
	        this.updateInputDimensions();
	    };
	    EditableText.prototype.componentDidUpdate = function (_, prevState) {
	        if (this.state.isEditing && !prevState.isEditing) {
	            utils_1.safeInvoke(this.props.onEdit);
	        }
	        this.updateInputDimensions();
	    };
	    EditableText.prototype.componentWillReceiveProps = function (nextProps) {
	        var state = {};
	        if (nextProps.value != null) {
	            state.value = nextProps.value;
	        }
	        if (nextProps.isEditing != null) {
	            state.isEditing = nextProps.isEditing;
	        }
	        if (nextProps.disabled || (nextProps.disabled == null && this.props.disabled)) {
	            state.isEditing = false;
	        }
	        this.setState(state);
	    };
	    EditableText.prototype.maybeRenderInput = function (value) {
	        var _a = this.props, maxLength = _a.maxLength, multiline = _a.multiline;
	        if (!this.state.isEditing) {
	            return undefined;
	        }
	        var props = {
	            className: "pt-editable-input",
	            maxLength: maxLength,
	            onBlur: this.toggleEditing,
	            onChange: this.handleTextChange,
	            onKeyDown: this.handleKeyEvent,
	            ref: this.refHandlers.input,
	            style: {
	                height: this.state.inputHeight,
	                lineHeight: !multiline && this.state.inputHeight != null ? this.state.inputHeight + "px" : null,
	                width: multiline ? "100%" : this.state.inputWidth,
	            },
	            value: value,
	        };
	        return multiline ? React.createElement("textarea", tslib_1.__assign({}, props)) : React.createElement("input", tslib_1.__assign({ type: "text" }, props));
	    };
	    EditableText.prototype.updateInputDimensions = function () {
	        if (this.valueElement != null) {
	            var _a = this.props, maxLines = _a.maxLines, minLines = _a.minLines, minWidth = _a.minWidth, multiline = _a.multiline;
	            var _b = this.valueElement, parentElement_1 = _b.parentElement, textContent = _b.textContent;
	            var _c = this.valueElement, scrollHeight_1 = _c.scrollHeight, scrollWidth = _c.scrollWidth;
	            var lineHeight = getLineHeight(this.valueElement);
	            // add one line to computed <span> height if text ends in newline
	            // because <span> collapses that trailing whitespace but <textarea> shows it
	            if (multiline && this.state.isEditing && /\n$/.test(textContent)) {
	                scrollHeight_1 += lineHeight;
	            }
	            if (lineHeight > 0) {
	                // line height could be 0 if the isNaN block from getLineHeight kicks in
	                scrollHeight_1 = utils_1.clamp(scrollHeight_1, minLines * lineHeight, maxLines * lineHeight);
	            }
	            // Chrome's input caret height misaligns text so the line-height must be larger than font-size.
	            // The computed scrollHeight must also account for a larger inherited line-height from the parent.
	            scrollHeight_1 = Math.max(scrollHeight_1, getFontSize(this.valueElement) + 1, getLineHeight(parentElement_1));
	            // IE11 & Edge needs a small buffer so text does not shift prior to resizing
	            if (compatibility_1.Browser.isEdge()) {
	                scrollWidth += BUFFER_WIDTH_EDGE;
	            }
	            else if (compatibility_1.Browser.isInternetExplorer()) {
	                scrollWidth += BUFFER_WIDTH_IE;
	            }
	            this.setState({
	                inputHeight: scrollHeight_1,
	                inputWidth: Math.max(scrollWidth, minWidth),
	            });
	            // synchronizes the ::before pseudo-element's height while editing for Chrome 53
	            if (multiline && this.state.isEditing) {
	                this.setTimeout(function () { return parentElement_1.style.height = scrollHeight_1 + "px"; });
	            }
	        }
	    };
	    return EditableText;
	}(abstractComponent_1.AbstractComponent));
	EditableText.defaultProps = {
	    confirmOnEnterKey: false,
	    defaultValue: "",
	    disabled: false,
	    maxLines: Infinity,
	    minLines: 1,
	    minWidth: 80,
	    multiline: false,
	    placeholder: "Click to Edit",
	};
	EditableText = tslib_1.__decorate([
	    PureRender
	], EditableText);
	exports.EditableText = EditableText;
	function getFontSize(element) {
	    var fontSize = getComputedStyle(element).fontSize;
	    return fontSize === "" ? 0 : parseInt(fontSize.slice(0, -2), 10);
	}
	function getLineHeight(element) {
	    // getComputedStyle() => 18.0001px => 18
	    var lineHeight = parseInt(getComputedStyle(element).lineHeight.slice(0, -2), 10);
	    // this check will be true if line-height is a keyword like "normal"
	    if (isNaN(lineHeight)) {
	        // @see http://stackoverflow.com/a/18430767/6342931
	        var line = document.createElement("span");
	        line.innerHTML = "<br>";
	        element.appendChild(line);
	        var singleLineHeight = element.offsetHeight;
	        line.innerHTML = "<br><br>";
	        var doubleLineHeight = element.offsetHeight;
	        element.removeChild(line);
	        // this can return 0 in edge cases
	        lineHeight = doubleLineHeight - singleLineHeight;
	    }
	    return lineHeight;
	}
	function insertAtCaret(el, text) {
	    var selectionEnd = el.selectionEnd, selectionStart = el.selectionStart, value = el.value;
	    if (selectionStart >= 0) {
	        var before_1 = value.substring(0, selectionStart);
	        var after_1 = value.substring(selectionEnd, value.length);
	        var len = text.length;
	        el.value = "" + before_1 + text + after_1;
	        el.selectionStart = selectionStart + len;
	        el.selectionEnd = selectionStart + len;
	    }
	}
	exports.EditableTextFactory = React.createFactory(EditableText);
	
	//# sourceMappingURL=editableText.js.map


/***/ }),
/* 49 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	function __export(m) {
	    for (var p in m) if (!exports.hasOwnProperty(p)) exports[p] = m[p];
	}
	Object.defineProperty(exports, "__esModule", { value: true });
	__export(__webpack_require__(50));
	
	//# sourceMappingURL=index.js.map


/***/ }),
/* 50 */
/***/ (function(module, exports) {

	/*
	 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var userAgent = typeof navigator !== "undefined" ? navigator.userAgent : "";
	var browser = {
	    isEdge: !!userAgent.match(/Edge/),
	    isInternetExplorer: (!!userAgent.match(/Trident/) || !!userAgent.match(/rv:11/)),
	    isWebkit: !!userAgent.match(/AppleWebKit/),
	};
	exports.Browser = {
	    isEdge: function () { return browser.isEdge; },
	    isInternetExplorer: function () { return browser.isInternetExplorer; },
	    isWebkit: function () { return browser.isWebkit; },
	};
	
	//# sourceMappingURL=browser.js.map


/***/ }),
/* 51 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	// HACKHACK: these components should go in separate files
	// tslint:disable max-classes-per-file
	// we need some empty interfaces to show up in docs
	// tslint:disable no-empty-interface
	var classNames = __webpack_require__(25);
	var React = __webpack_require__(7);
	var Classes = __webpack_require__(16);
	var props_1 = __webpack_require__(14);
	var utils_1 = __webpack_require__(8);
	var INVALID_PROPS = [
	    // we spread props to `<input>` but render `children` as its sibling
	    "children",
	    "defaultIndeterminate",
	    "indeterminate",
	    "labelElement",
	];
	/** Base Component class for all Controls */
	var Control = (function (_super) {
	    tslib_1.__extends(Control, _super);
	    function Control() {
	        return _super !== null && _super.apply(this, arguments) || this;
	    }
	    // generates control markup for given input type.
	    // optional inputRef in case the component needs reference for itself (don't forget to invoke the prop!).
	    Control.prototype.renderControl = function (type, typeClassName, inputRef) {
	        if (inputRef === void 0) { inputRef = this.props.inputRef; }
	        var className = classNames(Classes.CONTROL, typeClassName, (_a = {},
	            _a[Classes.DISABLED] = this.props.disabled,
	            _a[Classes.INLINE] = this.props.inline,
	            _a), this.props.className);
	        var inputProps = props_1.removeNonHTMLProps(this.props, INVALID_PROPS, true);
	        return (React.createElement("label", { className: className, style: this.props.style },
	            React.createElement("input", tslib_1.__assign({}, inputProps, { ref: inputRef, type: type })),
	            React.createElement("span", { className: Classes.CONTROL_INDICATOR }),
	            this.props.label,
	            this.props.labelElement,
	            this.props.children));
	        var _a;
	    };
	    return Control;
	}(React.Component));
	exports.Control = Control;
	var Checkbox = (function (_super) {
	    tslib_1.__extends(Checkbox, _super);
	    function Checkbox() {
	        var _this = _super !== null && _super.apply(this, arguments) || this;
	        _this.handleInputRef = function (ref) {
	            _this.input = ref;
	            utils_1.safeInvoke(_this.props.inputRef, ref);
	        };
	        return _this;
	    }
	    Checkbox.prototype.render = function () {
	        return this.renderControl("checkbox", "pt-checkbox", this.handleInputRef);
	    };
	    Checkbox.prototype.componentDidMount = function () {
	        if (this.props.defaultIndeterminate != null) {
	            this.input.indeterminate = this.props.defaultIndeterminate;
	        }
	        this.updateIndeterminate();
	    };
	    Checkbox.prototype.componentDidUpdate = function () {
	        this.updateIndeterminate();
	    };
	    Checkbox.prototype.updateIndeterminate = function () {
	        if (this.props.indeterminate != null) {
	            this.input.indeterminate = this.props.indeterminate;
	        }
	    };
	    return Checkbox;
	}(Control));
	Checkbox.displayName = "Blueprint.Checkbox";
	exports.Checkbox = Checkbox;
	var Switch = (function (_super) {
	    tslib_1.__extends(Switch, _super);
	    function Switch() {
	        return _super !== null && _super.apply(this, arguments) || this;
	    }
	    Switch.prototype.render = function () {
	        return this.renderControl("checkbox", "pt-switch");
	    };
	    return Switch;
	}(Control));
	Switch.displayName = "Blueprint.Switch";
	exports.Switch = Switch;
	var Radio = (function (_super) {
	    tslib_1.__extends(Radio, _super);
	    function Radio() {
	        return _super !== null && _super.apply(this, arguments) || this;
	    }
	    Radio.prototype.render = function () {
	        return this.renderControl("radio", "pt-radio");
	    };
	    return Radio;
	}(Control));
	Radio.displayName = "Blueprint.Radio";
	exports.Radio = Radio;
	exports.CheckboxFactory = React.createFactory(Checkbox);
	exports.SwitchFactory = React.createFactory(Switch);
	exports.RadioFactory = React.createFactory(Radio);
	
	//# sourceMappingURL=controls.js.map


/***/ }),
/* 52 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var PureRender = __webpack_require__(26);
	var React = __webpack_require__(7);
	var Classes = __webpack_require__(16);
	var props_1 = __webpack_require__(14);
	var icon_1 = __webpack_require__(39);
	var InputGroup = (function (_super) {
	    tslib_1.__extends(InputGroup, _super);
	    function InputGroup() {
	        var _this = _super !== null && _super.apply(this, arguments) || this;
	        _this.state = {
	            rightElementWidth: 30,
	        };
	        _this.refHandlers = {
	            rightElement: function (ref) { return _this.rightElement = ref; },
	        };
	        return _this;
	    }
	    InputGroup.prototype.render = function () {
	        var _a = this.props, className = _a.className, intent = _a.intent, leftIconName = _a.leftIconName;
	        var classes = classNames(Classes.INPUT_GROUP, Classes.intentClass(intent), (_b = {},
	            _b[Classes.DISABLED] = this.props.disabled,
	            _b), className);
	        var style = { paddingRight: this.state.rightElementWidth };
	        return (React.createElement("div", { className: classes },
	            React.createElement(icon_1.Icon, { iconName: leftIconName, iconSize: "inherit" }),
	            React.createElement("input", tslib_1.__assign({ type: "text" }, props_1.removeNonHTMLProps(this.props), { className: Classes.INPUT, ref: this.props.inputRef, style: style })),
	            this.maybeRenderRightElement()));
	        var _b;
	    };
	    InputGroup.prototype.componentDidMount = function () {
	        this.updateInputWidth();
	    };
	    InputGroup.prototype.componentDidUpdate = function () {
	        this.updateInputWidth();
	    };
	    InputGroup.prototype.maybeRenderRightElement = function () {
	        var rightElement = this.props.rightElement;
	        if (rightElement == null) {
	            return undefined;
	        }
	        return React.createElement("span", { className: "pt-input-action", ref: this.refHandlers.rightElement }, rightElement);
	    };
	    InputGroup.prototype.updateInputWidth = function () {
	        if (this.rightElement != null) {
	            var clientWidth = this.rightElement.clientWidth;
	            // small threshold to prevent infinite loops
	            if (Math.abs(clientWidth - this.state.rightElementWidth) > 2) {
	                this.setState({ rightElementWidth: clientWidth });
	            }
	        }
	        else {
	            this.setState({ rightElementWidth: 0 });
	        }
	    };
	    return InputGroup;
	}(React.Component));
	InputGroup.displayName = "Blueprint.InputGroup";
	InputGroup = tslib_1.__decorate([
	    PureRender
	], InputGroup);
	exports.InputGroup = InputGroup;
	exports.InputGroupFactory = React.createFactory(InputGroup);
	
	//# sourceMappingURL=inputGroup.js.map


/***/ }),
/* 53 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var PureRender = __webpack_require__(26);
	var React = __webpack_require__(7);
	var common_1 = __webpack_require__(4);
	var Errors = __webpack_require__(10);
	var buttons_1 = __webpack_require__(37);
	var inputGroup_1 = __webpack_require__(52);
	var IncrementDirection;
	(function (IncrementDirection) {
	    IncrementDirection[IncrementDirection["DOWN"] = -1] = "DOWN";
	    IncrementDirection[IncrementDirection["UP"] = 1] = "UP";
	})(IncrementDirection || (IncrementDirection = {}));
	var NumericInput = NumericInput_1 = (function (_super) {
	    tslib_1.__extends(NumericInput, _super);
	    function NumericInput(props, context) {
	        var _this = _super.call(this, props, context) || this;
	        // updating these flags need not trigger re-renders, so don't include them in this.state.
	        _this.didPasteEventJustOccur = false;
	        _this.shouldSelectAfterUpdate = false;
	        _this.inputRef = function (input) {
	            _this.inputElement = input;
	        };
	        // Callbacks - Buttons
	        // ===================
	        _this.handleDecrementButtonClick = function (e) {
	            var delta = _this.getIncrementDelta(IncrementDirection.DOWN, e.shiftKey, e.altKey);
	            var nextValue = _this.incrementValue(delta);
	            _this.invokeValueCallback(nextValue, _this.props.onButtonClick);
	        };
	        _this.handleIncrementButtonClick = function (e) {
	            var delta = _this.getIncrementDelta(IncrementDirection.UP, e.shiftKey, e.altKey);
	            var nextValue = _this.incrementValue(delta);
	            _this.invokeValueCallback(nextValue, _this.props.onButtonClick);
	        };
	        _this.handleButtonFocus = function () {
	            _this.setState({ isButtonGroupFocused: true });
	        };
	        _this.handleButtonBlur = function () {
	            _this.setState({ isButtonGroupFocused: false });
	        };
	        _this.handleButtonKeyUp = function (e, onClick) {
	            if (e.keyCode === common_1.Keys.SPACE || e.keyCode === common_1.Keys.ENTER) {
	                // prevent the page from scrolling (this is the default browser
	                // behavior for shift + space or alt + space).
	                e.preventDefault();
	                // trigger a click event to update the input value appropriately,
	                // based on the active modifier keys.
	                var fakeClickEvent = {
	                    altKey: e.altKey,
	                    currentTarget: e.currentTarget,
	                    shiftKey: e.shiftKey,
	                    target: e.target,
	                };
	                onClick(fakeClickEvent);
	            }
	        };
	        // Callbacks - Input
	        // =================
	        _this.handleInputFocus = function (e) {
	            _this.shouldSelectAfterUpdate = _this.props.selectAllOnFocus;
	            _this.setState({ isInputGroupFocused: true });
	            common_1.Utils.safeInvoke(_this.props.onFocus, e);
	        };
	        _this.handleInputBlur = function (e) {
	            // explicitly set `shouldSelectAfterUpdate` to `false` to prevent focus
	            // hoarding on IE11 (#704)
	            _this.shouldSelectAfterUpdate = false;
	            var baseStateChange = { isInputGroupFocused: false };
	            if (_this.props.clampValueOnBlur) {
	                var value = e.target.value;
	                var sanitizedValue = _this.getSanitizedValue(value);
	                _this.setState(tslib_1.__assign({}, baseStateChange, { value: sanitizedValue }));
	                if (value !== sanitizedValue) {
	                    _this.invokeValueCallback(sanitizedValue, _this.props.onValueChange);
	                }
	            }
	            else {
	                _this.setState(baseStateChange);
	            }
	            common_1.Utils.safeInvoke(_this.props.onBlur, e);
	        };
	        _this.handleInputKeyDown = function (e) {
	            if (_this.props.disabled || _this.props.readOnly) {
	                return;
	            }
	            var keyCode = e.keyCode;
	            var direction;
	            if (keyCode === common_1.Keys.ARROW_UP) {
	                direction = IncrementDirection.UP;
	            }
	            else if (keyCode === common_1.Keys.ARROW_DOWN) {
	                direction = IncrementDirection.DOWN;
	            }
	            if (direction != null) {
	                // when the input field has focus, some key combinations will modify
	                // the field's selection range. we'll actually want to select all
	                // text in the field after we modify the value on the following
	                // lines. preventing the default selection behavior lets us do that
	                // without interference.
	                e.preventDefault();
	                var delta = _this.getIncrementDelta(direction, e.shiftKey, e.altKey);
	                _this.incrementValue(delta);
	            }
	            common_1.Utils.safeInvoke(_this.props.onKeyDown, e);
	        };
	        _this.handleInputKeyPress = function (e) {
	            // we prohibit keystrokes in onKeyPress instead of onKeyDown, because
	            // e.key is not trustworthy in onKeyDown in all browsers.
	            if (_this.props.allowNumericCharactersOnly && _this.isKeyboardEventDisabledForBasicNumericEntry(e)) {
	                e.preventDefault();
	            }
	            common_1.Utils.safeInvoke(_this.props.onKeyPress, e);
	        };
	        _this.handleInputPaste = function (e) {
	            _this.didPasteEventJustOccur = true;
	            common_1.Utils.safeInvoke(_this.props.onPaste, e);
	        };
	        _this.handleInputChange = function (e) {
	            var value = e.target.value;
	            var nextValue;
	            if (_this.props.allowNumericCharactersOnly && _this.didPasteEventJustOccur) {
	                _this.didPasteEventJustOccur = false;
	                var valueChars = value.split("");
	                var sanitizedValueChars = valueChars.filter(_this.isFloatingPointNumericCharacter);
	                var sanitizedValue = sanitizedValueChars.join("");
	                nextValue = sanitizedValue;
	            }
	            else {
	                nextValue = value;
	            }
	            _this.shouldSelectAfterUpdate = false;
	            _this.setState({ value: nextValue });
	            _this.invokeValueCallback(nextValue, _this.props.onValueChange);
	        };
	        _this.state = {
	            stepMaxPrecision: _this.getStepMaxPrecision(props),
	            value: _this.getValueOrEmptyValue(props.value),
	        };
	        return _this;
	    }
	    NumericInput.prototype.componentWillReceiveProps = function (nextProps) {
	        _super.prototype.componentWillReceiveProps.call(this, nextProps);
	        var value = this.getValueOrEmptyValue(nextProps.value);
	        var didMinChange = nextProps.min !== this.props.min;
	        var didMaxChange = nextProps.max !== this.props.max;
	        var didBoundsChange = didMinChange || didMaxChange;
	        var sanitizedValue = (value !== NumericInput_1.VALUE_EMPTY)
	            ? this.getSanitizedValue(value, /* delta */ 0, nextProps.min, nextProps.max)
	            : NumericInput_1.VALUE_EMPTY;
	        var stepMaxPrecision = this.getStepMaxPrecision(nextProps);
	        // if a new min and max were provided that cause the existing value to fall
	        // outside of the new bounds, then clamp the value to the new valid range.
	        if (didBoundsChange && sanitizedValue !== this.state.value) {
	            this.setState({ stepMaxPrecision: stepMaxPrecision, value: sanitizedValue });
	            this.invokeValueCallback(sanitizedValue, this.props.onValueChange);
	        }
	        else {
	            this.setState({ stepMaxPrecision: stepMaxPrecision, value: value });
	        }
	    };
	    NumericInput.prototype.render = function () {
	        var _a = this.props, buttonPosition = _a.buttonPosition, className = _a.className, large = _a.large;
	        var inputGroupHtmlProps = common_1.removeNonHTMLProps(this.props, [
	            "allowNumericCharactersOnly",
	            "buttonPosition",
	            "clampValueOnBlur",
	            "className",
	            "large",
	            "majorStepSize",
	            "minorStepSize",
	            "onButtonClick",
	            "onValueChange",
	            "selectAllOnFocus",
	            "selectAllOnIncrement",
	            "stepSize",
	        ], true);
	        var inputGroup = (React.createElement(inputGroup_1.InputGroup, tslib_1.__assign({ autoComplete: "off" }, inputGroupHtmlProps, { className: classNames((_b = {}, _b[common_1.Classes.LARGE] = large, _b)), intent: this.props.intent, inputRef: this.inputRef, key: "input-group", leftIconName: this.props.leftIconName, onFocus: this.handleInputFocus, onBlur: this.handleInputBlur, onChange: this.handleInputChange, onKeyDown: this.handleInputKeyDown, onKeyPress: this.handleInputKeyPress, onPaste: this.handleInputPaste, value: this.state.value })));
	        // the strict null check here is intentional; an undefined value should
	        // fall back to the default button position on the right side.
	        if (buttonPosition === "none" || buttonPosition === null) {
	            // If there are no buttons, then the control group will render the
	            // text field with squared border-radii on the left side, causing it
	            // to look weird. This problem goes away if we simply don't nest within
	            // a control group.
	            return (React.createElement("div", { className: className }, inputGroup));
	        }
	        else {
	            var incrementButton = this.renderButton(NumericInput_1.INCREMENT_KEY, NumericInput_1.INCREMENT_ICON_NAME, this.handleIncrementButtonClick);
	            var decrementButton = this.renderButton(NumericInput_1.DECREMENT_KEY, NumericInput_1.DECREMENT_ICON_NAME, this.handleDecrementButtonClick);
	            var buttonGroup = (React.createElement("div", { key: "button-group", className: classNames(common_1.Classes.BUTTON_GROUP, common_1.Classes.VERTICAL, common_1.Classes.FIXED) },
	                incrementButton,
	                decrementButton));
	            var inputElems = (buttonPosition === common_1.Position.LEFT)
	                ? [buttonGroup, inputGroup]
	                : [inputGroup, buttonGroup];
	            var classes = classNames(common_1.Classes.NUMERIC_INPUT, common_1.Classes.CONTROL_GROUP, (_c = {},
	                _c[common_1.Classes.LARGE] = large,
	                _c), className);
	            return (React.createElement("div", { className: classes }, inputElems));
	        }
	        var _b, _c;
	    };
	    NumericInput.prototype.componentDidUpdate = function () {
	        if (this.shouldSelectAfterUpdate) {
	            this.inputElement.setSelectionRange(0, this.state.value.length);
	        }
	    };
	    NumericInput.prototype.validateProps = function (nextProps) {
	        var majorStepSize = nextProps.majorStepSize, max = nextProps.max, min = nextProps.min, minorStepSize = nextProps.minorStepSize, stepSize = nextProps.stepSize;
	        if (min != null && max != null && min >= max) {
	            throw new Error(Errors.NUMERIC_INPUT_MIN_MAX);
	        }
	        if (stepSize == null) {
	            throw new Error(Errors.NUMERIC_INPUT_STEP_SIZE_NULL);
	        }
	        if (stepSize <= 0) {
	            throw new Error(Errors.NUMERIC_INPUT_STEP_SIZE_NON_POSITIVE);
	        }
	        if (minorStepSize && minorStepSize <= 0) {
	            throw new Error(Errors.NUMERIC_INPUT_MINOR_STEP_SIZE_NON_POSITIVE);
	        }
	        if (majorStepSize && majorStepSize <= 0) {
	            throw new Error(Errors.NUMERIC_INPUT_MAJOR_STEP_SIZE_NON_POSITIVE);
	        }
	        if (minorStepSize && minorStepSize > stepSize) {
	            throw new Error(Errors.NUMERIC_INPUT_MINOR_STEP_SIZE_BOUND);
	        }
	        if (majorStepSize && majorStepSize < stepSize) {
	            throw new Error(Errors.NUMERIC_INPUT_MAJOR_STEP_SIZE_BOUND);
	        }
	    };
	    // Render Helpers
	    // ==============
	    NumericInput.prototype.renderButton = function (key, iconName, onClick) {
	        var _this = this;
	        // respond explicitly on key *up*, because onKeyDown triggers multiple
	        // times and doesn't always receive modifier-key flags, leading to an
	        // unintuitive/out-of-control incrementing experience.
	        var onKeyUp = function (e) {
	            _this.handleButtonKeyUp(e, onClick);
	        };
	        return (React.createElement(buttons_1.Button, { disabled: this.props.disabled || this.props.readOnly, iconName: iconName, intent: this.props.intent, key: key, onBlur: this.handleButtonBlur, onClick: onClick, onFocus: this.handleButtonFocus, onKeyUp: onKeyUp }));
	    };
	    NumericInput.prototype.invokeValueCallback = function (value, callback) {
	        common_1.Utils.safeInvoke(callback, +value, value);
	    };
	    // Value Helpers
	    // =============
	    NumericInput.prototype.incrementValue = function (delta) {
	        // pretend we're incrementing from 0 if currValue is empty
	        var currValue = this.state.value || NumericInput_1.VALUE_ZERO;
	        var nextValue = this.getSanitizedValue(currValue, delta);
	        this.shouldSelectAfterUpdate = this.props.selectAllOnIncrement;
	        this.setState({ value: nextValue });
	        this.invokeValueCallback(nextValue, this.props.onValueChange);
	        return nextValue;
	    };
	    NumericInput.prototype.getIncrementDelta = function (direction, isShiftKeyPressed, isAltKeyPressed) {
	        var _a = this.props, majorStepSize = _a.majorStepSize, minorStepSize = _a.minorStepSize, stepSize = _a.stepSize;
	        if (isShiftKeyPressed && majorStepSize != null) {
	            return direction * majorStepSize;
	        }
	        else if (isAltKeyPressed && minorStepSize != null) {
	            return direction * minorStepSize;
	        }
	        else {
	            return direction * stepSize;
	        }
	    };
	    NumericInput.prototype.getSanitizedValue = function (value, delta, min, max) {
	        if (delta === void 0) { delta = 0; }
	        if (min === void 0) { min = this.props.min; }
	        if (max === void 0) { max = this.props.max; }
	        if (!this.isValueNumeric(value)) {
	            return NumericInput_1.VALUE_EMPTY;
	        }
	        var nextValue = this.toMaxPrecision(parseFloat(value) + delta);
	        // defaultProps won't work if the user passes in null, so just default
	        // to +/- infinity here instead, as a catch-all.
	        var adjustedMin = (min != null) ? min : -Infinity;
	        var adjustedMax = (max != null) ? max : Infinity;
	        nextValue = common_1.Utils.clamp(nextValue, adjustedMin, adjustedMax);
	        return nextValue.toString();
	    };
	    NumericInput.prototype.getValueOrEmptyValue = function (value) {
	        return (value != null) ? value.toString() : NumericInput_1.VALUE_EMPTY;
	    };
	    NumericInput.prototype.isValueNumeric = function (value) {
	        // checking if a string is numeric in Typescript is a big pain, because
	        // we can't simply toss a string parameter to isFinite. below is the
	        // essential approach that jQuery uses, which involves subtracting a
	        // parsed numeric value from the string representation of the value. we
	        // need to cast the value to the `any` type to allow this operation
	        // between dissimilar types.
	        return value != null && (value - parseFloat(value) + 1) >= 0;
	    };
	    NumericInput.prototype.isKeyboardEventDisabledForBasicNumericEntry = function (e) {
	        // unit tests may not include e.key. don't bother disabling those events.
	        if (e.key == null) {
	            return false;
	        }
	        // allow modified key strokes that may involve letters and other
	        // non-numeric/invalid characters (Cmd + A, Cmd + C, Cmd + V, Cmd + X).
	        if (e.ctrlKey || e.altKey || e.metaKey) {
	            return false;
	        }
	        // keys that print a single character when pressed have a `key` name of
	        // length 1. every other key has a longer `key` name (e.g. "Backspace",
	        // "ArrowUp", "Shift"). since none of those keys can print a character
	        // to the field--and since they may have important native behaviors
	        // beyond printing a character--we don't want to disable their effects.
	        var isSingleCharKey = e.key.length === 1;
	        if (!isSingleCharKey) {
	            return false;
	        }
	        // now we can simply check that the single character that wants to be printed
	        // is a floating-point number character that we're allowed to print.
	        return !this.isFloatingPointNumericCharacter(e.key);
	    };
	    NumericInput.prototype.isFloatingPointNumericCharacter = function (char) {
	        return NumericInput_1.FLOATING_POINT_NUMBER_CHARACTER_REGEX.test(char);
	    };
	    NumericInput.prototype.getStepMaxPrecision = function (props) {
	        if (props.minorStepSize != null) {
	            return common_1.Utils.countDecimalPlaces(props.minorStepSize);
	        }
	        else {
	            return common_1.Utils.countDecimalPlaces(props.stepSize);
	        }
	    };
	    NumericInput.prototype.toMaxPrecision = function (value) {
	        // round the value to have the specified maximum precision (toFixed is the wrong choice,
	        // because it would show trailing zeros in the decimal part out to the specified precision)
	        // source: http://stackoverflow.com/a/18358056/5199574
	        var scaleFactor = Math.pow(10, this.state.stepMaxPrecision);
	        return Math.round(value * scaleFactor) / scaleFactor;
	    };
	    return NumericInput;
	}(common_1.AbstractComponent));
	NumericInput.displayName = "Blueprint.NumericInput";
	NumericInput.VALUE_EMPTY = "";
	NumericInput.VALUE_ZERO = "0";
	NumericInput.defaultProps = {
	    allowNumericCharactersOnly: true,
	    buttonPosition: common_1.Position.RIGHT,
	    clampValueOnBlur: false,
	    large: false,
	    majorStepSize: 10,
	    minorStepSize: 0.1,
	    selectAllOnFocus: false,
	    selectAllOnIncrement: false,
	    stepSize: 1,
	    value: NumericInput_1.VALUE_EMPTY,
	};
	NumericInput.DECREMENT_KEY = "decrement";
	NumericInput.INCREMENT_KEY = "increment";
	NumericInput.DECREMENT_ICON_NAME = "chevron-down";
	NumericInput.INCREMENT_ICON_NAME = "chevron-up";
	/**
	 * A regex that matches a string of length 1 (i.e. a standalone character)
	 * if and only if it is a floating-point number character as defined by W3C:
	 * https://www.w3.org/TR/2012/WD-html-markup-20120329/datatypes.html#common.data.float
	 *
	 * Floating-point number characters are the only characters that can be
	 * printed within a default input[type="number"]. This component should
	 * behave the same way when this.props.allowNumericCharactersOnly = true.
	 * See here for the input[type="number"].value spec:
	 * https://www.w3.org/TR/2012/WD-html-markup-20120329/input.number.html#input.number.attrs.value
	 */
	NumericInput.FLOATING_POINT_NUMBER_CHARACTER_REGEX = /^[Ee0-9\+\-\.]$/;
	NumericInput = NumericInput_1 = tslib_1.__decorate([
	    PureRender
	], NumericInput);
	exports.NumericInput = NumericInput;
	exports.NumericInputFactory = React.createFactory(NumericInput);
	var NumericInput_1;
	
	//# sourceMappingURL=numericInput.js.map


/***/ }),
/* 54 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var React = __webpack_require__(7);
	var abstractComponent_1 = __webpack_require__(5);
	var Classes = __webpack_require__(16);
	var Errors = __webpack_require__(10);
	var controls_1 = __webpack_require__(51);
	var counter = 0;
	function nextName() { return RadioGroup.displayName + "-" + counter++; }
	var RadioGroup = (function (_super) {
	    tslib_1.__extends(RadioGroup, _super);
	    function RadioGroup() {
	        var _this = _super !== null && _super.apply(this, arguments) || this;
	        // a unique name for this group, which can be overridden by `name` prop.
	        _this.autoGroupName = nextName();
	        return _this;
	    }
	    RadioGroup.prototype.render = function () {
	        var label = this.props.label;
	        return (React.createElement("div", { className: this.props.className },
	            label == null ? null : React.createElement("label", { className: Classes.LABEL }, label),
	            Array.isArray(this.props.options) ? this.renderOptions() : this.renderChildren()));
	    };
	    RadioGroup.prototype.validateProps = function () {
	        if (this.props.children != null && this.props.options != null) {
	            console.warn(Errors.RADIOGROUP_WARN_CHILDREN_OPTIONS_MUTEX);
	        }
	    };
	    RadioGroup.prototype.renderChildren = function () {
	        var _this = this;
	        return React.Children.map(this.props.children, function (child) {
	            if (isRadio(child)) {
	                return React.cloneElement(child, _this.getRadioProps(child.props));
	            }
	            else {
	                return child;
	            }
	        });
	    };
	    RadioGroup.prototype.renderOptions = function () {
	        var _this = this;
	        return this.props.options.map(function (option) { return (React.createElement(controls_1.Radio, tslib_1.__assign({}, option, _this.getRadioProps(option), { key: option.value }))); });
	    };
	    RadioGroup.prototype.getRadioProps = function (optionProps) {
	        var name = this.props.name;
	        var value = optionProps.value, disabled = optionProps.disabled;
	        return {
	            checked: value === this.props.selectedValue,
	            disabled: disabled || this.props.disabled,
	            inline: this.props.inline,
	            name: name == null ? this.autoGroupName : name,
	            onChange: this.props.onChange,
	        };
	    };
	    return RadioGroup;
	}(abstractComponent_1.AbstractComponent));
	RadioGroup.displayName = "Blueprint.RadioGroup";
	exports.RadioGroup = RadioGroup;
	;
	function isRadio(child) {
	    return child != null && child.type === controls_1.Radio;
	}
	
	//# sourceMappingURL=radioGroup.js.map


/***/ }),
/* 55 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var react_1 = __webpack_require__(7);
	var React = __webpack_require__(7);
	var common_1 = __webpack_require__(4);
	var hotkey_1 = __webpack_require__(56);
	var hotkey_2 = __webpack_require__(56);
	exports.Hotkey = hotkey_2.Hotkey;
	var keyCombo_1 = __webpack_require__(57);
	exports.KeyCombo = keyCombo_1.KeyCombo;
	var hotkeysTarget_1 = __webpack_require__(59);
	exports.HotkeysTarget = hotkeysTarget_1.HotkeysTarget;
	var hotkeyParser_1 = __webpack_require__(58);
	exports.comboMatches = hotkeyParser_1.comboMatches;
	exports.getKeyCombo = hotkeyParser_1.getKeyCombo;
	exports.getKeyComboString = hotkeyParser_1.getKeyComboString;
	exports.parseKeyCombo = hotkeyParser_1.parseKeyCombo;
	var hotkeysDialog_1 = __webpack_require__(61);
	exports.hideHotkeysDialog = hotkeysDialog_1.hideHotkeysDialog;
	exports.setHotkeysDialogProps = hotkeysDialog_1.setHotkeysDialogProps;
	var errors_1 = __webpack_require__(10);
	var Hotkeys = (function (_super) {
	    tslib_1.__extends(Hotkeys, _super);
	    function Hotkeys() {
	        return _super !== null && _super.apply(this, arguments) || this;
	    }
	    Hotkeys.prototype.render = function () {
	        var hotkeys = react_1.Children.map(this.props.children, function (child) { return child.props; });
	        // sort by group label alphabetically, globals first
	        hotkeys.sort(function (a, b) {
	            if (a.global) {
	                return b.global ? 0 : -1;
	            }
	            if (b.global) {
	                return 1;
	            }
	            return a.group.localeCompare(b.group);
	        });
	        var lastGroup = null;
	        var elems = [];
	        for (var _i = 0, hotkeys_1 = hotkeys; _i < hotkeys_1.length; _i++) {
	            var hotkey = hotkeys_1[_i];
	            var groupLabel = hotkey.group;
	            if (groupLabel !== lastGroup) {
	                elems.push(React.createElement("h4", { key: "group-" + elems.length, className: "pt-hotkey-group" }, groupLabel));
	                lastGroup = groupLabel;
	            }
	            elems.push(React.createElement(hotkey_1.Hotkey, tslib_1.__assign({ key: elems.length }, hotkey)));
	        }
	        return React.createElement("div", { className: "pt-hotkey-column" }, elems);
	    };
	    Hotkeys.prototype.validateProps = function (props) {
	        react_1.Children.forEach(props.children, function (child) {
	            if (!hotkey_1.Hotkey.isInstance(child)) {
	                throw new Error(errors_1.HOTKEYS_HOTKEY_CHILDREN);
	            }
	        });
	    };
	    return Hotkeys;
	}(common_1.AbstractComponent));
	Hotkeys.defaultProps = {
	    tabIndex: 0,
	};
	exports.Hotkeys = Hotkeys;
	
	//# sourceMappingURL=hotkeys.js.map


/***/ }),
/* 56 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var React = __webpack_require__(7);
	var common_1 = __webpack_require__(4);
	var keyCombo_1 = __webpack_require__(57);
	var Hotkey = (function (_super) {
	    tslib_1.__extends(Hotkey, _super);
	    function Hotkey() {
	        return _super !== null && _super.apply(this, arguments) || this;
	    }
	    Hotkey.isInstance = function (element) {
	        return element != null && element.type === Hotkey;
	    };
	    Hotkey.prototype.render = function () {
	        var _a = this.props, label = _a.label, spreadableProps = tslib_1.__rest(_a, ["label"]);
	        return React.createElement("div", { className: "pt-hotkey" },
	            React.createElement("div", { className: "pt-hotkey-label" }, label),
	            React.createElement(keyCombo_1.KeyCombo, tslib_1.__assign({}, spreadableProps)));
	    };
	    Hotkey.prototype.validateProps = function (props) {
	        if (props.global !== true && props.group == null) {
	            throw new Error("non-global <Hotkey>s must define a group");
	        }
	    };
	    return Hotkey;
	}(common_1.AbstractComponent));
	Hotkey.defaultProps = {
	    allowInInput: false,
	    disabled: false,
	    global: false,
	    preventDefault: false,
	    stopPropagation: false,
	};
	exports.Hotkey = Hotkey;
	
	//# sourceMappingURL=hotkey.js.map


/***/ }),
/* 57 */
/***/ (function(module, exports, __webpack_require__) {

	/**
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var React = __webpack_require__(7);
	var hotkeyParser_1 = __webpack_require__(58);
	var KeyIcons = {
	    alt: "pt-icon-key-option",
	    cmd: "pt-icon-key-command",
	    ctrl: "pt-icon-key-control",
	    delete: "pt-icon-key-delete",
	    down: "pt-icon-arrow-down",
	    enter: "pt-icon-key-enter",
	    left: "pt-icon-arrow-left",
	    meta: "pt-icon-key-command",
	    right: "pt-icon-arrow-right",
	    shift: "pt-icon-key-shift",
	    up: "pt-icon-arrow-up",
	};
	var KeyCombo = (function (_super) {
	    tslib_1.__extends(KeyCombo, _super);
	    function KeyCombo() {
	        return _super !== null && _super.apply(this, arguments) || this;
	    }
	    KeyCombo.prototype.render = function () {
	        var keys = hotkeyParser_1.normalizeKeyCombo(this.props.combo);
	        var components = [];
	        for (var i = 0; i < keys.length; i++) {
	            var key = keys[i];
	            var icon = KeyIcons[key];
	            if (icon != null) {
	                components.push(React.createElement("kbd", { className: "pt-key pt-modifier-key", key: "key-" + i },
	                    React.createElement("span", { className: "pt-icon-standard " + icon }),
	                    key));
	            }
	            else {
	                if (key.length === 1) {
	                    key = key.toUpperCase();
	                }
	                components.push(React.createElement("kbd", { className: "pt-key", key: "key-" + i }, key));
	            }
	        }
	        return React.createElement("div", { className: "pt-key-combo" }, components);
	    };
	    return KeyCombo;
	}(React.Component));
	exports.KeyCombo = KeyCombo;
	
	//# sourceMappingURL=keyCombo.js.map


/***/ }),
/* 58 */
/***/ (function(module, exports) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	exports.KeyCodes = {
	    8: "backspace",
	    9: "tab",
	    13: "enter",
	    20: "capslock",
	    27: "esc",
	    32: "space",
	    33: "pageup",
	    34: "pagedown",
	    35: "end",
	    36: "home",
	    37: "left",
	    38: "up",
	    39: "right",
	    40: "down",
	    45: "ins",
	    46: "del",
	    // number keys
	    48: "0",
	    49: "1",
	    50: "2",
	    51: "3",
	    52: "4",
	    53: "5",
	    54: "6",
	    55: "7",
	    56: "8",
	    57: "9",
	    // alphabet
	    65: "a",
	    66: "b",
	    67: "c",
	    68: "d",
	    69: "e",
	    70: "f",
	    71: "g",
	    72: "h",
	    73: "i",
	    74: "j",
	    75: "k",
	    76: "l",
	    77: "m",
	    78: "n",
	    79: "o",
	    80: "p",
	    81: "q",
	    82: "r",
	    83: "s",
	    84: "t",
	    85: "u",
	    86: "v",
	    87: "w",
	    88: "x",
	    89: "y",
	    90: "z",
	    // punctuation
	    106: "*",
	    107: "+",
	    109: "-",
	    110: ".",
	    111: "/",
	    186: ";",
	    187: "=",
	    188: ",",
	    189: "-",
	    190: ".",
	    191: "/",
	    192: "`",
	    219: "[",
	    220: "\\",
	    221: "]",
	    222: "\'",
	};
	exports.Modifiers = {
	    16: "shift",
	    17: "ctrl",
	    18: "alt",
	    91: "meta",
	    93: "meta",
	    224: "meta",
	};
	exports.ModifierBitMasks = {
	    alt: 1,
	    ctrl: 2,
	    meta: 4,
	    shift: 8,
	};
	exports.Aliases = {
	    cmd: "meta",
	    command: "meta",
	    escape: "esc",
	    minus: "-",
	    mod: isMac() ? "meta" : "ctrl",
	    option: "alt",
	    plus: "+",
	    return: "enter",
	    win: "meta",
	};
	// alph sorting is unintuitive here
	// tslint:disable object-literal-sort-keys
	exports.ShiftKeys = {
	    "~": "`",
	    "!": "1",
	    "@": "2",
	    "#": "3",
	    "$": "4",
	    "%": "5",
	    "^": "6",
	    "&": "7",
	    "*": "8",
	    "(": "9",
	    ")": "0",
	    "_": "-",
	    "+": "=",
	    "{": "[",
	    "}": "]",
	    "|": "\\",
	    ":": ";",
	    "\"": "\'",
	    "<": ",",
	    ">": ".",
	    "?": "/",
	};
	// tslint:enable object-literal-sort-keys
	/* tslint:enable:object-literal-key-quotes */
	// Function keys
	for (var i = 1; i <= 12; ++i) {
	    exports.KeyCodes[111 + i] = "f" + i;
	}
	// Numpad
	for (var i = 0; i <= 9; ++i) {
	    exports.KeyCodes[96 + i] = "num" + i.toString();
	}
	function comboMatches(a, b) {
	    return a.modifiers === b.modifiers && a.key === b.key;
	}
	exports.comboMatches = comboMatches;
	/**
	 * Converts a key combo string into a key combo object. Key combos include
	 * zero or more modifier keys, such as `shift` or `alt`, and exactly one
	 * action key, such as `A`, `enter`, or `left`.
	 *
	 * For action keys that require a shift, e.g. `@` or `|`, we inlude the
	 * necessary `shift` modifier and automatically convert the action key to the
	 * unshifted version. For example, `@` is equivalent to `shift+2`.
	 */
	exports.parseKeyCombo = function (combo) {
	    var pieces = combo.replace(/\s/g, "").toLowerCase().split("+");
	    var modifiers = 0;
	    var key = null;
	    for (var _i = 0, pieces_1 = pieces; _i < pieces_1.length; _i++) {
	        var piece = pieces_1[_i];
	        if (piece === "") {
	            throw new Error("Failed to parse key combo \"" + combo + "\".\n                Valid key combos look like \"cmd + plus\", \"shift+p\", or \"!\"");
	        }
	        if (exports.Aliases[piece] != null) {
	            piece = exports.Aliases[piece];
	        }
	        if (exports.ModifierBitMasks[piece] != null) {
	            modifiers += exports.ModifierBitMasks[piece];
	        }
	        else if (exports.ShiftKeys[piece] != null) {
	            // tslint:disable-next-line no-string-literal
	            modifiers += exports.ModifierBitMasks["shift"];
	            key = exports.ShiftKeys[piece];
	        }
	        else {
	            key = piece.toLowerCase();
	        }
	    }
	    return { modifiers: modifiers, key: key };
	};
	/**
	 * PhantomJS's webkit totally messes up keyboard events, so we have do this
	 * fancy little dance with the event data to determine which key was pressed
	 * for unit tests.
	 */
	var normalizeKeyCode = function (e) {
	    return (e.which === 0 && e.key != null) ? e.key.charCodeAt(0) : e.which;
	};
	/**
	 * Converts a keyboard event into a valid combo prop string
	 */
	exports.getKeyComboString = function (e) {
	    var keys = [];
	    // modifiers first
	    if (e.ctrlKey) {
	        keys.push("ctrl");
	    }
	    if (e.altKey) {
	        keys.push("alt");
	    }
	    if (e.shiftKey) {
	        keys.push("shift");
	    }
	    if (e.metaKey) {
	        keys.push("meta");
	    }
	    var which = normalizeKeyCode(e);
	    if (exports.Modifiers[which] != null) {
	        // no action key
	    }
	    else if (exports.KeyCodes[which] != null) {
	        keys.push(exports.KeyCodes[which]);
	    }
	    else {
	        keys.push(String.fromCharCode(which).toLowerCase());
	    }
	    // join keys with plusses
	    return keys.join(" + ");
	};
	/**
	 * Determines the key combo object from the given keyboard event. Again, a key
	 * combo includes zero or more modifiers (represented by a bitmask) and one
	 * action key, which we determine from the `e.which` property of the keyboard
	 * event.
	 */
	exports.getKeyCombo = function (e) {
	    var key = null;
	    var which = normalizeKeyCode(e);
	    if (exports.Modifiers[which] != null) {
	        // keep key null
	    }
	    else if (exports.KeyCodes[which] != null) {
	        key = exports.KeyCodes[which];
	    }
	    else {
	        key = String.fromCharCode(which).toLowerCase();
	    }
	    var modifiers = 0;
	    // tslint:disable no-string-literal
	    if (e.altKey) {
	        modifiers += exports.ModifierBitMasks["alt"];
	    }
	    if (e.ctrlKey) {
	        modifiers += exports.ModifierBitMasks["ctrl"];
	    }
	    if (e.metaKey) {
	        modifiers += exports.ModifierBitMasks["meta"];
	    }
	    if (e.shiftKey) {
	        modifiers += exports.ModifierBitMasks["shift"];
	    }
	    // tslint:enable
	    return { modifiers: modifiers, key: key };
	};
	/**
	 * Splits a key combo string into its constituent key values and looks up
	 * aliases, such as `return` -> `enter`.
	 *
	 * Unlike the parseKeyCombo method, this method does NOT convert shifted
	 * action keys. So `"@"` will NOT be converted to `["shift", "2"]`).
	 */
	exports.normalizeKeyCombo = function (combo, platformOverride) {
	    var keys = combo.replace(/\s/g, "").split("+");
	    return keys.map(function (key) {
	        var keyName = (exports.Aliases[key] != null) ? exports.Aliases[key] : key;
	        return (keyName === "meta")
	            ? (isMac(platformOverride) ? "cmd" : "ctrl")
	            : keyName;
	    });
	};
	/* tslint:enable:no-string-literal */
	function isMac(platformOverride) {
	    var platform = platformOverride != null
	        ? platformOverride
	        : (typeof navigator !== "undefined" ? navigator.platform : undefined);
	    return platform == null
	        ? false
	        : /Mac|iPod|iPhone|iPad/.test(platform);
	}
	
	//# sourceMappingURL=hotkeyParser.js.map


/***/ }),
/* 59 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var React = __webpack_require__(7);
	var utils_1 = __webpack_require__(8);
	var hotkeysEvents_1 = __webpack_require__(60);
	function HotkeysTarget(constructor) {
	    var _a = constructor.prototype, componentWillMount = _a.componentWillMount, componentDidMount = _a.componentDidMount, componentWillUnmount = _a.componentWillUnmount, render = _a.render, renderHotkeys = _a.renderHotkeys;
	    if (!utils_1.isFunction(renderHotkeys)) {
	        throw new Error("@HotkeysTarget-decorated class must implement `renderHotkeys`. " + constructor);
	    }
	    // tslint:disable no-invalid-this only-arrow-functions
	    constructor.prototype.componentWillMount = function () {
	        this.localHotkeysEvents = new hotkeysEvents_1.HotkeysEvents(hotkeysEvents_1.HotkeyScope.LOCAL);
	        this.globalHotkeysEvents = new hotkeysEvents_1.HotkeysEvents(hotkeysEvents_1.HotkeyScope.GLOBAL);
	        if (componentWillMount != null) {
	            componentWillMount.call(this);
	        }
	    };
	    constructor.prototype.componentDidMount = function () {
	        // attach global key event listeners
	        document.addEventListener("keydown", this.globalHotkeysEvents.handleKeyDown);
	        document.addEventListener("keyup", this.globalHotkeysEvents.handleKeyUp);
	        if (componentDidMount != null) {
	            componentDidMount.call(this);
	        }
	    };
	    constructor.prototype.componentWillUnmount = function () {
	        // detach global key event listeners
	        document.removeEventListener("keydown", this.globalHotkeysEvents.handleKeyDown);
	        document.removeEventListener("keyup", this.globalHotkeysEvents.handleKeyUp);
	        this.globalHotkeysEvents.clear();
	        this.localHotkeysEvents.clear();
	        if (componentWillUnmount != null) {
	            componentWillUnmount.call(this);
	        }
	    };
	    constructor.prototype.render = function () {
	        var _this = this;
	        var element = render.call(this);
	        var hotkeys = renderHotkeys.call(this);
	        this.localHotkeysEvents.setHotkeys(hotkeys.props);
	        this.globalHotkeysEvents.setHotkeys(hotkeys.props);
	        // attach local key event listeners
	        if (element != null && this.localHotkeysEvents.count() > 0) {
	            var tabIndex = hotkeys.props.tabIndex === undefined ? 0 : hotkeys.props.tabIndex;
	            var existingKeyDown_1 = element.props.onKeyDown;
	            var onKeyDown = function (e) {
	                _this.localHotkeysEvents.handleKeyDown(e.nativeEvent);
	                utils_1.safeInvoke(existingKeyDown_1, e);
	            };
	            var existingKeyUp_1 = element.props.onKeyUp;
	            var onKeyUp = function (e) {
	                _this.localHotkeysEvents.handleKeyUp(e.nativeEvent);
	                utils_1.safeInvoke(existingKeyUp_1, e);
	            };
	            return React.cloneElement(element, { tabIndex: tabIndex, onKeyDown: onKeyDown, onKeyUp: onKeyUp });
	        }
	        else {
	            return element;
	        }
	    };
	    // tslint:enable
	}
	exports.HotkeysTarget = HotkeysTarget;
	;
	
	//# sourceMappingURL=hotkeysTarget.js.map


/***/ }),
/* 60 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var react_1 = __webpack_require__(7);
	var utils_1 = __webpack_require__(8);
	var hotkey_1 = __webpack_require__(56);
	var hotkeyParser_1 = __webpack_require__(58);
	var hotkeysDialog_1 = __webpack_require__(61);
	var SHOW_DIALOG_KEY = "?";
	var HotkeyScope;
	(function (HotkeyScope) {
	    HotkeyScope[HotkeyScope["LOCAL"] = 0] = "LOCAL";
	    HotkeyScope[HotkeyScope["GLOBAL"] = 1] = "GLOBAL";
	})(HotkeyScope = exports.HotkeyScope || (exports.HotkeyScope = {}));
	var HotkeysEvents = (function () {
	    function HotkeysEvents(scope) {
	        var _this = this;
	        this.scope = scope;
	        this.actions = [];
	        this.handleKeyDown = function (e) {
	            var combo = hotkeyParser_1.getKeyCombo(e);
	            var isTextInput = _this.isTextInput(e);
	            if (!isTextInput && hotkeyParser_1.comboMatches(hotkeyParser_1.parseKeyCombo(SHOW_DIALOG_KEY), combo)) {
	                if (hotkeysDialog_1.isHotkeysDialogShowing()) {
	                    hotkeysDialog_1.hideHotkeysDialogAfterDelay();
	                }
	                else {
	                    hotkeysDialog_1.showHotkeysDialog(_this.actions.map(function (action) { return action.props; }));
	                }
	                return;
	            }
	            else if (hotkeysDialog_1.isHotkeysDialogShowing()) {
	                return;
	            }
	            _this.invokeNamedCallbackIfComboRecognized(combo, "onKeyDown", e);
	        };
	        this.handleKeyUp = function (e) {
	            if (hotkeysDialog_1.isHotkeysDialogShowing()) {
	                return;
	            }
	            _this.invokeNamedCallbackIfComboRecognized(hotkeyParser_1.getKeyCombo(e), "onKeyUp", e);
	        };
	    }
	    HotkeysEvents.prototype.count = function () {
	        return this.actions.length;
	    };
	    HotkeysEvents.prototype.clear = function () {
	        this.actions = [];
	    };
	    HotkeysEvents.prototype.setHotkeys = function (props) {
	        var _this = this;
	        var actions = [];
	        react_1.Children.forEach(props.children, function (child) {
	            if (hotkey_1.Hotkey.isInstance(child) && _this.isScope(child.props)) {
	                actions.push({
	                    combo: hotkeyParser_1.parseKeyCombo(child.props.combo),
	                    props: child.props,
	                });
	            }
	        });
	        this.actions = actions;
	    };
	    HotkeysEvents.prototype.invokeNamedCallbackIfComboRecognized = function (combo, callbackName, e) {
	        var isTextInput = this.isTextInput(e);
	        for (var _i = 0, _a = this.actions; _i < _a.length; _i++) {
	            var action = _a[_i];
	            var shouldIgnore = (isTextInput && !action.props.allowInInput) || action.props.disabled;
	            if (!shouldIgnore && hotkeyParser_1.comboMatches(action.combo, combo)) {
	                if (action.props.preventDefault) {
	                    e.preventDefault();
	                }
	                if (action.props.stopPropagation) {
	                    // set a flag just for unit testing. not meant to be referenced in feature work.
	                    e.isPropagationStopped = true;
	                    e.stopPropagation();
	                }
	                utils_1.safeInvoke(action.props[callbackName], e);
	            }
	        }
	    };
	    HotkeysEvents.prototype.isScope = function (props) {
	        return (props.global ? HotkeyScope.GLOBAL : HotkeyScope.LOCAL) === this.scope;
	    };
	    HotkeysEvents.prototype.isTextInput = function (e) {
	        var elem = e.target;
	        // we check these cases for unit testing, but this should not happen
	        // during normal operation
	        if (elem == null || elem.closest == null) {
	            return false;
	        }
	        var editable = elem.closest("input, textarea, [contenteditable=true]");
	        if (editable == null) {
	            return false;
	        }
	        // don't let checkboxes, switches, and radio buttons prevent hotkey behavior
	        if (editable.tagName.toLowerCase() === "input") {
	            var inputType = editable.type;
	            if (inputType === "checkbox" || inputType === "radio") {
	                return false;
	            }
	        }
	        // don't let read-only fields prevent hotkey behavior
	        if (editable.readOnly) {
	            return false;
	        }
	        return true;
	    };
	    return HotkeysEvents;
	}());
	exports.HotkeysEvents = HotkeysEvents;
	
	//# sourceMappingURL=hotkeysEvents.js.map


/***/ }),
/* 61 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var React = __webpack_require__(7);
	var ReactDOM = __webpack_require__(23);
	var common_1 = __webpack_require__(4);
	var components_1 = __webpack_require__(20);
	var hotkey_1 = __webpack_require__(56);
	var hotkeys_1 = __webpack_require__(55);
	/**
	 * The delay before showing or hiding the dialog. Should be long enough to
	 * allow all registered hotkey listeners to execute first.
	 */
	var DELAY_IN_MS = 10;
	var HotkeysDialog = (function () {
	    function HotkeysDialog() {
	        var _this = this;
	        this.componentProps = {
	            globalHotkeysGroup: "Global hotkeys",
	        };
	        this.hotkeysQueue = [];
	        this.isDialogShowing = false;
	        this.show = function () {
	            _this.isDialogShowing = true;
	            _this.render();
	        };
	        this.hide = function () {
	            _this.isDialogShowing = false;
	            _this.render();
	        };
	    }
	    HotkeysDialog.prototype.render = function () {
	        if (this.container == null) {
	            this.container = this.getContainer();
	        }
	        ReactDOM.render(this.renderComponent(), this.container);
	    };
	    HotkeysDialog.prototype.unmount = function () {
	        if (this.container != null) {
	            ReactDOM.unmountComponentAtNode(this.container);
	            this.container.remove();
	            delete this.container;
	        }
	    };
	    /**
	     * Because hotkeys can be registered globally and locally and because
	     * event ordering cannot be guaranteed, we use this debouncing method to
	     * allow all hotkey listeners to fire and add their hotkeys to the dialog.
	     *
	     * 10msec after the last listener adds their hotkeys, we render the dialog
	     * and clear the queue.
	     */
	    HotkeysDialog.prototype.enqueueHotkeysForDisplay = function (hotkeys) {
	        this.hotkeysQueue.push(hotkeys);
	        // reset timeout for debounce
	        clearTimeout(this.showTimeoutToken);
	        this.showTimeoutToken = setTimeout(this.show, DELAY_IN_MS);
	    };
	    HotkeysDialog.prototype.hideAfterDelay = function () {
	        clearTimeout(this.hideTimeoutToken);
	        this.hideTimeoutToken = setTimeout(this.hide, DELAY_IN_MS);
	    };
	    HotkeysDialog.prototype.isShowing = function () {
	        return this.isDialogShowing;
	    };
	    HotkeysDialog.prototype.getContainer = function () {
	        if (this.container == null) {
	            this.container = document.createElement("div");
	            this.container.classList.add(common_1.Classes.PORTAL);
	            document.body.appendChild(this.container);
	        }
	        return this.container;
	    };
	    HotkeysDialog.prototype.renderComponent = function () {
	        return (React.createElement(components_1.Dialog, tslib_1.__assign({}, this.componentProps, { className: classNames(this.componentProps.className, "pt-hotkey-dialog"), isOpen: this.isDialogShowing, onClose: this.hide }),
	            React.createElement("div", { className: common_1.Classes.DIALOG_BODY }, this.renderHotkeys())));
	    };
	    HotkeysDialog.prototype.renderHotkeys = function () {
	        var _this = this;
	        var hotkeys = this.emptyHotkeyQueue();
	        var elements = hotkeys.map(function (hotkey, index) {
	            var group = (hotkey.global === true && hotkey.group == null) ?
	                _this.componentProps.globalHotkeysGroup : hotkey.group;
	            return React.createElement(hotkey_1.Hotkey, tslib_1.__assign({ key: index }, hotkey, { group: group }));
	        });
	        return React.createElement(hotkeys_1.Hotkeys, null, elements);
	    };
	    HotkeysDialog.prototype.emptyHotkeyQueue = function () {
	        // flatten then empty the hotkeys queue
	        var hotkeys = this.hotkeysQueue.reduce((function (arr, queued) { return arr.concat(queued); }), []);
	        this.hotkeysQueue.length = 0;
	        return hotkeys;
	    };
	    return HotkeysDialog;
	}());
	// singleton instance
	var HOTKEYS_DIALOG = new HotkeysDialog();
	function isHotkeysDialogShowing() {
	    return HOTKEYS_DIALOG.isShowing();
	}
	exports.isHotkeysDialogShowing = isHotkeysDialogShowing;
	function setHotkeysDialogProps(props) {
	    for (var key in props) {
	        if (props.hasOwnProperty(key)) {
	            HOTKEYS_DIALOG.componentProps[key] = props[key];
	        }
	    }
	}
	exports.setHotkeysDialogProps = setHotkeysDialogProps;
	function showHotkeysDialog(hotkeys) {
	    HOTKEYS_DIALOG.enqueueHotkeysForDisplay(hotkeys);
	}
	exports.showHotkeysDialog = showHotkeysDialog;
	function hideHotkeysDialog() {
	    HOTKEYS_DIALOG.hide();
	}
	exports.hideHotkeysDialog = hideHotkeysDialog;
	/**
	 * Use this function instead of `hideHotkeysDialog` if you need to ensure that all hotkey listeners
	 * have time to execute with the dialog in a consistent open state. This can avoid flickering the
	 * dialog between open and closed states as successive listeners fire.
	 */
	function hideHotkeysDialogAfterDelay() {
	    HOTKEYS_DIALOG.hideAfterDelay();
	}
	exports.hideHotkeysDialogAfterDelay = hideHotkeysDialogAfterDelay;
	
	//# sourceMappingURL=hotkeysDialog.js.map


/***/ }),
/* 62 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var React = __webpack_require__(7);
	var Classes = __webpack_require__(16);
	var MenuDivider = (function (_super) {
	    tslib_1.__extends(MenuDivider, _super);
	    function MenuDivider() {
	        return _super !== null && _super.apply(this, arguments) || this;
	    }
	    MenuDivider.prototype.render = function () {
	        var _a = this.props, className = _a.className, title = _a.title;
	        if (title == null) {
	            // simple divider
	            return React.createElement("li", { className: classNames(Classes.MENU_DIVIDER, className) });
	        }
	        else {
	            // section header with title
	            return React.createElement("li", { className: classNames(Classes.MENU_HEADER, className) },
	                React.createElement("h6", null, title));
	        }
	    };
	    return MenuDivider;
	}(React.Component));
	MenuDivider.displayName = "Blueprint.MenuDivider";
	exports.MenuDivider = MenuDivider;
	exports.MenuDividerFactory = React.createFactory(MenuDivider);
	
	//# sourceMappingURL=menuDivider.js.map


/***/ }),
/* 63 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var PureRender = __webpack_require__(26);
	var React = __webpack_require__(7);
	var Classes = __webpack_require__(16);
	var icon_1 = __webpack_require__(39);
	var NonIdealState = (function (_super) {
	    tslib_1.__extends(NonIdealState, _super);
	    function NonIdealState() {
	        return _super !== null && _super.apply(this, arguments) || this;
	    }
	    NonIdealState.prototype.render = function () {
	        return (React.createElement("div", { className: classNames(Classes.NON_IDEAL_STATE, this.props.className) },
	            this.maybeRenderVisual(),
	            this.maybeRenderTitle(),
	            this.maybeRenderDescription(),
	            this.maybeRenderAction()));
	    };
	    NonIdealState.prototype.maybeRenderAction = function () {
	        if (this.props.action == null) {
	            return undefined;
	        }
	        return React.createElement("div", { className: Classes.NON_IDEAL_STATE_ACTION }, this.props.action);
	    };
	    NonIdealState.prototype.maybeRenderDescription = function () {
	        if (this.props.description == null) {
	            return undefined;
	        }
	        return React.createElement("div", { className: Classes.NON_IDEAL_STATE_DESCRIPTION }, this.props.description);
	    };
	    NonIdealState.prototype.maybeRenderTitle = function () {
	        if (this.props.title == null) {
	            return undefined;
	        }
	        return React.createElement("h4", { className: Classes.NON_IDEAL_STATE_TITLE }, this.props.title);
	    };
	    NonIdealState.prototype.maybeRenderVisual = function () {
	        var visual = this.props.visual;
	        if (visual == null) {
	            return undefined;
	        }
	        else if (typeof visual === "string") {
	            return (React.createElement("div", { className: classNames(Classes.NON_IDEAL_STATE_VISUAL, Classes.NON_IDEAL_STATE_ICON) },
	                React.createElement(icon_1.Icon, { iconName: visual, iconSize: "inherit" })));
	        }
	        else {
	            return (React.createElement("div", { className: Classes.NON_IDEAL_STATE_VISUAL }, visual));
	        }
	    };
	    return NonIdealState;
	}(React.Component));
	NonIdealState = tslib_1.__decorate([
	    PureRender
	], NonIdealState);
	exports.NonIdealState = NonIdealState;
	exports.NonIdealStateFactory = React.createFactory(NonIdealState);
	
	//# sourceMappingURL=nonIdealState.js.map


/***/ }),
/* 64 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var PureRender = __webpack_require__(26);
	var React = __webpack_require__(7);
	var Classes = __webpack_require__(16);
	var Text = (function (_super) {
	    tslib_1.__extends(Text, _super);
	    function Text() {
	        var _this = _super !== null && _super.apply(this, arguments) || this;
	        _this.state = {
	            isContentOverflowing: false,
	            textContent: "",
	        };
	        _this.refHandlers = {
	            text: function (overflowElement) { return _this.textRef = overflowElement; },
	        };
	        return _this;
	    }
	    Text.prototype.componentDidMount = function () {
	        this.update();
	    };
	    Text.prototype.componentDidUpdate = function () {
	        this.update();
	    };
	    Text.prototype.render = function () {
	        var classes = classNames((_a = {},
	            _a[Classes.TEXT_OVERFLOW_ELLIPSIS] = this.props.ellipsize,
	            _a), this.props.className);
	        return (React.createElement("div", { className: classes, ref: this.refHandlers.text, title: this.state.isContentOverflowing ? this.state.textContent : undefined }, this.props.children));
	        var _a;
	    };
	    Text.prototype.update = function () {
	        var newState = {
	            isContentOverflowing: this.props.ellipsize && this.textRef.scrollWidth > this.textRef.clientWidth,
	            textContent: this.textRef.textContent,
	        };
	        this.setState(newState);
	    };
	    return Text;
	}(React.Component));
	Text = tslib_1.__decorate([
	    PureRender
	], Text);
	exports.Text = Text;
	
	//# sourceMappingURL=text.js.map


/***/ }),
/* 65 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var React = __webpack_require__(7);
	var popover_1 = __webpack_require__(24);
	var SVGPopover = (function (_super) {
	    tslib_1.__extends(SVGPopover, _super);
	    function SVGPopover() {
	        return _super !== null && _super.apply(this, arguments) || this;
	    }
	    SVGPopover.prototype.render = function () {
	        return React.createElement(popover_1.Popover, tslib_1.__assign({ rootElementTag: "g" }, this.props), this.props.children);
	    };
	    return SVGPopover;
	}(React.Component));
	exports.SVGPopover = SVGPopover;
	exports.SVGPopoverFactory = React.createFactory(SVGPopover);
	
	//# sourceMappingURL=svgPopover.js.map


/***/ }),
/* 66 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var PureRender = __webpack_require__(26);
	var React = __webpack_require__(7);
	var Classes = __webpack_require__(16);
	var utils_1 = __webpack_require__(8);
	var ProgressBar = (function (_super) {
	    tslib_1.__extends(ProgressBar, _super);
	    function ProgressBar() {
	        return _super !== null && _super.apply(this, arguments) || this;
	    }
	    ProgressBar.prototype.render = function () {
	        var _a = this.props, className = _a.className, intent = _a.intent, value = _a.value;
	        var classes = classNames("pt-progress-bar", Classes.intentClass(intent), className);
	        // don't set width if value is null (rely on default CSS value)
	        var width = (value == null ? null : 100 * utils_1.clamp(value, 0, 1) + "%");
	        return (React.createElement("div", { className: classes },
	            React.createElement("div", { className: "pt-progress-meter", style: { width: width } })));
	    };
	    return ProgressBar;
	}(React.Component));
	ProgressBar.displayName = "Blueprint.ProgressBar";
	ProgressBar = tslib_1.__decorate([
	    PureRender
	], ProgressBar);
	exports.ProgressBar = ProgressBar;
	exports.ProgressBarFactory = React.createFactory(ProgressBar);
	
	//# sourceMappingURL=progressBar.js.map


/***/ }),
/* 67 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var React = __webpack_require__(7);
	var tooltip_1 = __webpack_require__(34);
	var SVGTooltip = (function (_super) {
	    tslib_1.__extends(SVGTooltip, _super);
	    function SVGTooltip() {
	        return _super !== null && _super.apply(this, arguments) || this;
	    }
	    SVGTooltip.prototype.render = function () {
	        return React.createElement(tooltip_1.Tooltip, tslib_1.__assign({ rootElementTag: "g" }, this.props), this.props.children);
	    };
	    return SVGTooltip;
	}(React.Component));
	exports.SVGTooltip = SVGTooltip;
	exports.SVGTooltipFactory = React.createFactory(SVGTooltip);
	
	//# sourceMappingURL=svgTooltip.js.map


/***/ }),
/* 68 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var React = __webpack_require__(7);
	var Classes = __webpack_require__(16);
	var Errors = __webpack_require__(10);
	var utils_1 = __webpack_require__(8);
	var coreSlider_1 = __webpack_require__(69);
	var handle_1 = __webpack_require__(70);
	var RangeEnd;
	(function (RangeEnd) {
	    RangeEnd[RangeEnd["LEFT"] = 0] = "LEFT";
	    RangeEnd[RangeEnd["RIGHT"] = 1] = "RIGHT";
	})(RangeEnd || (RangeEnd = {}));
	var RangeSlider = (function (_super) {
	    tslib_1.__extends(RangeSlider, _super);
	    function RangeSlider() {
	        var _this = _super !== null && _super.apply(this, arguments) || this;
	        _this.className = classNames(Classes.SLIDER, Classes.RANGE_SLIDER);
	        _this.handles = [];
	        _this.addHandleRef = function (ref) {
	            if (ref != null) {
	                _this.handles.push(ref);
	            }
	        };
	        _this.getHandlerForIndex = function (index, callback) { return function (newValue) {
	            if (utils_1.isFunction(callback)) {
	                var _a = _this.props.value, leftValue = _a[0], rightValue = _a[1];
	                if (index === RangeEnd.LEFT) {
	                    callback([Math.min(newValue, rightValue), rightValue]);
	                }
	                else {
	                    callback([leftValue, Math.max(newValue, leftValue)]);
	                }
	            }
	        }; };
	        _this.handleChange = function (newValue) {
	            var _a = _this.props.value, leftValue = _a[0], rightValue = _a[1];
	            var newLeftValue = newValue[0], newRightValue = newValue[1];
	            if ((leftValue !== newLeftValue || rightValue !== newRightValue) && utils_1.isFunction(_this.props.onChange)) {
	                _this.props.onChange(newValue);
	            }
	        };
	        return _this;
	    }
	    RangeSlider.prototype.renderFill = function () {
	        var _a = this.props.value, leftValue = _a[0], rightValue = _a[1];
	        if (leftValue === rightValue) {
	            return undefined;
	        }
	        // expand by 1px in each direction so it sits under the handle border
	        var left = Math.round((leftValue - this.props.min) * this.state.tickSize) - 1;
	        var width = Math.round((rightValue - leftValue) * this.state.tickSize) + 2;
	        if (width < 0) {
	            left += width;
	            width = Math.abs(width);
	        }
	        return React.createElement("div", { className: Classes.SLIDER + "-progress", style: { left: left, width: width } });
	    };
	    RangeSlider.prototype.renderHandles = function () {
	        var _this = this;
	        var _a = this.props, disabled = _a.disabled, max = _a.max, min = _a.min, onRelease = _a.onRelease, stepSize = _a.stepSize, value = _a.value;
	        return value.map(function (val, index) { return (React.createElement(handle_1.Handle, { disabled: disabled, key: index, label: _this.formatLabel(val), max: max, min: min, onChange: _this.getHandlerForIndex(index, _this.handleChange), onRelease: _this.getHandlerForIndex(index, onRelease), ref: _this.addHandleRef, stepSize: stepSize, tickSize: _this.state.tickSize, value: val })); });
	    };
	    RangeSlider.prototype.handleTrackClick = function (event) {
	        var _this = this;
	        this.handles.reduce(function (min, handle) {
	            // find closest handle to the mouse position
	            var value = handle.clientToValue(event.clientX);
	            return _this.nearestHandleForValue(value, min, handle);
	        }).beginHandleMovement(event);
	    };
	    RangeSlider.prototype.handleTrackTouch = function (event) {
	        var _this = this;
	        this.handles.reduce(function (min, handle) {
	            // find closest handle to the touch position
	            var value = handle.clientToValue(handle.touchEventClientX(event));
	            return _this.nearestHandleForValue(value, min, handle);
	        }).beginHandleTouchMovement(event);
	    };
	    RangeSlider.prototype.nearestHandleForValue = function (value, firstHandle, secondHandle) {
	        var firstDistance = Math.abs(value - firstHandle.props.value);
	        var secondDistance = Math.abs(value - secondHandle.props.value);
	        return secondDistance < firstDistance ? secondHandle : firstHandle;
	    };
	    RangeSlider.prototype.validateProps = function (props) {
	        var value = props.value;
	        if (value == null || value[RangeEnd.LEFT] == null || value[RangeEnd.RIGHT] == null) {
	            throw new Error(Errors.RANGESLIDER_NULL_VALUE);
	        }
	    };
	    return RangeSlider;
	}(coreSlider_1.CoreSlider));
	RangeSlider.defaultProps = {
	    disabled: false,
	    labelStepSize: 1,
	    max: 10,
	    min: 0,
	    showTrackFill: true,
	    stepSize: 1,
	    value: [0, 10],
	};
	RangeSlider.displayName = "Blueprint.RangeSlider";
	exports.RangeSlider = RangeSlider;
	exports.RangeSliderFactory = React.createFactory(RangeSlider);
	
	//# sourceMappingURL=rangeSlider.js.map


/***/ }),
/* 69 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var PureRender = __webpack_require__(26);
	var React = __webpack_require__(7);
	var abstractComponent_1 = __webpack_require__(5);
	var Classes = __webpack_require__(16);
	var Errors = __webpack_require__(10);
	var utils_1 = __webpack_require__(8);
	var CoreSlider = (function (_super) {
	    tslib_1.__extends(CoreSlider, _super);
	    function CoreSlider(props) {
	        var _this = _super.call(this, props) || this;
	        _this.className = Classes.SLIDER;
	        _this.refHandlers = {
	            track: function (el) { return _this.trackElement = el; },
	        };
	        _this.maybeHandleTrackClick = function (event) {
	            if (_this.canHandleTrackEvent(event)) {
	                _this.handleTrackClick(event);
	            }
	        };
	        _this.maybeHandleTrackTouch = function (event) {
	            if (_this.canHandleTrackEvent(event)) {
	                _this.handleTrackTouch(event);
	            }
	        };
	        _this.canHandleTrackEvent = function (event) {
	            var target = event.target;
	            // ensure event does not come from inside the handle
	            return !_this.props.disabled && target.closest("." + Classes.SLIDER_HANDLE) == null;
	        };
	        _this.state = {
	            labelPrecision: _this.getLabelPrecision(props),
	            tickSize: 0,
	        };
	        return _this;
	    }
	    CoreSlider.prototype.render = function () {
	        var disabled = this.props.disabled;
	        var classes = classNames(this.className, (_a = {},
	            _a[Classes.DISABLED] = disabled,
	            _a[Classes.SLIDER + "-unlabeled"] = this.props.renderLabel === false,
	            _a), this.props.className);
	        return (React.createElement("div", { className: classes, onMouseDown: this.maybeHandleTrackClick, onTouchStart: this.maybeHandleTrackTouch },
	            React.createElement("div", { className: Classes.SLIDER + "-track", ref: this.refHandlers.track }),
	            this.maybeRenderFill(),
	            this.maybeRenderAxis(),
	            this.renderHandles()));
	        var _a;
	    };
	    CoreSlider.prototype.componentDidMount = function () {
	        this.updateTickSize();
	    };
	    CoreSlider.prototype.componentDidUpdate = function () {
	        this.updateTickSize();
	    };
	    CoreSlider.prototype.componentWillReceiveProps = function (props) {
	        _super.prototype.componentWillReceiveProps.call(this, props);
	        this.setState({ labelPrecision: this.getLabelPrecision(props) });
	    };
	    CoreSlider.prototype.formatLabel = function (value) {
	        var renderLabel = this.props.renderLabel;
	        if (renderLabel === false) {
	            return undefined;
	        }
	        else if (utils_1.isFunction(renderLabel)) {
	            return renderLabel(value);
	        }
	        else {
	            return value.toFixed(this.state.labelPrecision);
	        }
	    };
	    CoreSlider.prototype.validateProps = function (props) {
	        if (props.stepSize <= 0) {
	            throw new Error(Errors.SLIDER_ZERO_STEP);
	        }
	        if (props.labelStepSize <= 0) {
	            throw new Error(Errors.SLIDER_ZERO_LABEL_STEP);
	        }
	    };
	    CoreSlider.prototype.maybeRenderAxis = function () {
	        // explicit typedefs are required because tsc (rightly) assumes that props might be overriden with different
	        // types in subclasses
	        var max = this.props.max;
	        var min = this.props.min;
	        var labelStepSize = this.props.labelStepSize;
	        if (this.props.renderLabel === false) {
	            return undefined;
	        }
	        var stepSize = Math.round(this.state.tickSize * labelStepSize);
	        var labels = [];
	        // tslint:disable-next-line:one-variable-per-declaration
	        for (var i = min, left = 0; i < max || utils_1.approxEqual(i, max); i += labelStepSize, left += stepSize) {
	            labels.push(React.createElement("div", { className: Classes.SLIDER + "-label", key: i, style: { left: left } }, this.formatLabel(i)));
	        }
	        return React.createElement("div", { className: Classes.SLIDER + "-axis" }, labels);
	    };
	    CoreSlider.prototype.maybeRenderFill = function () {
	        if (this.props.showTrackFill && this.trackElement != null) {
	            return this.renderFill();
	        }
	        return undefined;
	    };
	    CoreSlider.prototype.getLabelPrecision = function (_a) {
	        var labelPrecision = _a.labelPrecision, stepSize = _a.stepSize;
	        // infer default label precision from stepSize because that's how much the handle moves.
	        return (labelPrecision == null)
	            ? utils_1.countDecimalPlaces(stepSize)
	            : labelPrecision;
	    };
	    CoreSlider.prototype.updateTickSize = function () {
	        if (this.trackElement != null) {
	            var tickSize = this.trackElement.clientWidth / (this.props.max - this.props.min);
	            this.setState({ tickSize: tickSize });
	        }
	    };
	    return CoreSlider;
	}(abstractComponent_1.AbstractComponent));
	CoreSlider = tslib_1.__decorate([
	    PureRender
	], CoreSlider);
	exports.CoreSlider = CoreSlider;
	
	//# sourceMappingURL=coreSlider.js.map


/***/ }),
/* 70 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var PureRender = __webpack_require__(26);
	var React = __webpack_require__(7);
	var abstractComponent_1 = __webpack_require__(5);
	var Classes = __webpack_require__(16);
	var Keys = __webpack_require__(17);
	var utils_1 = __webpack_require__(8);
	// props that require number values, for validation
	var NUMBER_PROPS = ["max", "min", "stepSize", "tickSize", "value"];
	var Handle = (function (_super) {
	    tslib_1.__extends(Handle, _super);
	    function Handle() {
	        var _this = _super !== null && _super.apply(this, arguments) || this;
	        _this.state = {
	            isMoving: false,
	        };
	        _this.refHandlers = {
	            handle: function (el) { return _this.handleElement = el; },
	        };
	        _this.beginHandleMovement = function (event) {
	            document.addEventListener("mousemove", _this.handleHandleMovement);
	            document.addEventListener("mouseup", _this.endHandleMovement);
	            _this.setState({ isMoving: true });
	            _this.changeValue(_this.clientToValue(event.clientX));
	        };
	        _this.beginHandleTouchMovement = function (event) {
	            document.addEventListener("touchmove", _this.handleHandleTouchMovement);
	            document.addEventListener("touchend", _this.endHandleTouchMovement);
	            document.addEventListener("touchcancel", _this.endHandleTouchMovement);
	            _this.setState({ isMoving: true });
	            _this.changeValue(_this.clientToValue(_this.touchEventClientX(event)));
	        };
	        _this.endHandleMovement = function (event) {
	            _this.handleMoveEndedAt(event.clientX);
	        };
	        _this.endHandleTouchMovement = function (event) {
	            _this.handleMoveEndedAt(_this.touchEventClientX(event));
	        };
	        _this.handleMoveEndedAt = function (clientPixel) {
	            _this.removeDocumentEventListeners();
	            _this.setState({ isMoving: false });
	            // not using changeValue because we want to invoke the handler regardless of current prop value
	            var onRelease = _this.props.onRelease;
	            var finalValue = _this.clamp(_this.clientToValue(clientPixel));
	            utils_1.safeInvoke(onRelease, finalValue);
	        };
	        _this.handleHandleMovement = function (event) {
	            _this.handleMovedTo(event.clientX);
	        };
	        _this.handleHandleTouchMovement = function (event) {
	            _this.handleMovedTo(_this.touchEventClientX(event));
	        };
	        _this.handleMovedTo = function (clientPixel) {
	            if (_this.state.isMoving && !_this.props.disabled) {
	                _this.changeValue(_this.clientToValue(clientPixel));
	            }
	        };
	        _this.handleKeyDown = function (event) {
	            var _a = _this.props, stepSize = _a.stepSize, value = _a.value;
	            var which = event.which;
	            if (which === Keys.ARROW_DOWN || which === Keys.ARROW_LEFT) {
	                _this.changeValue(value - stepSize);
	                // this key event has been handled! prevent browser scroll on up/down
	                event.preventDefault();
	            }
	            else if (which === Keys.ARROW_UP || which === Keys.ARROW_RIGHT) {
	                _this.changeValue(value + stepSize);
	                event.preventDefault();
	            }
	        };
	        _this.handleKeyUp = function (event) {
	            if ([Keys.ARROW_UP, Keys.ARROW_DOWN, Keys.ARROW_LEFT, Keys.ARROW_RIGHT].indexOf(event.which) >= 0) {
	                utils_1.safeInvoke(_this.props.onRelease, _this.props.value);
	            }
	        };
	        return _this;
	    }
	    Handle.prototype.render = function () {
	        var _a = this.props, className = _a.className, disabled = _a.disabled, label = _a.label, min = _a.min, tickSize = _a.tickSize, value = _a.value;
	        var isMoving = this.state.isMoving;
	        // getBoundingClientRect().height includes border size as opposed to clientHeight
	        var handleSize = (this.handleElement == null ? 0 : this.handleElement.getBoundingClientRect().height);
	        return (React.createElement("span", { className: classNames(Classes.SLIDER_HANDLE, (_b = {}, _b[Classes.ACTIVE] = isMoving, _b), className), onKeyDown: disabled ? null : this.handleKeyDown, onKeyUp: disabled ? null : this.handleKeyUp, onMouseDown: disabled ? null : this.beginHandleMovement, onTouchStart: disabled ? null : this.beginHandleTouchMovement, ref: this.refHandlers.handle, style: { left: Math.round((value - min) * tickSize - handleSize / 2) }, tabIndex: 0 }, label == null ? null : React.createElement("span", { className: Classes.SLIDER_LABEL }, label)));
	        var _b;
	    };
	    Handle.prototype.componentWillUnmount = function () {
	        this.removeDocumentEventListeners();
	    };
	    /** Convert client pixel to value between min and max. */
	    Handle.prototype.clientToValue = function (clientPixel) {
	        var _a = this.props, stepSize = _a.stepSize, tickSize = _a.tickSize, value = _a.value;
	        if (this.handleElement == null) {
	            return value;
	        }
	        var handleRect = this.handleElement.getBoundingClientRect();
	        var handleCenterPixel = handleRect.left + handleRect.width / 2;
	        var pixelDelta = clientPixel - handleCenterPixel;
	        // convert pixels to range value in increments of `stepSize`
	        var valueDelta = Math.round(pixelDelta / (tickSize * stepSize)) * stepSize;
	        return value + valueDelta;
	    };
	    Handle.prototype.touchEventClientX = function (event) {
	        return event.changedTouches[0].clientX;
	    };
	    Handle.prototype.validateProps = function (props) {
	        for (var _i = 0, NUMBER_PROPS_1 = NUMBER_PROPS; _i < NUMBER_PROPS_1.length; _i++) {
	            var prop = NUMBER_PROPS_1[_i];
	            if (typeof props[prop] !== "number") {
	                throw new Error("[Blueprint] <Handle> requires number value for " + prop + " prop");
	            }
	        }
	    };
	    /** Clamp value and invoke callback if it differs from current value */
	    Handle.prototype.changeValue = function (newValue, callback) {
	        if (callback === void 0) { callback = this.props.onChange; }
	        newValue = this.clamp(newValue);
	        if (!isNaN(newValue) && this.props.value !== newValue) {
	            utils_1.safeInvoke(callback, newValue);
	        }
	    };
	    /** Clamp value between min and max props */
	    Handle.prototype.clamp = function (value) {
	        return utils_1.clamp(value, this.props.min, this.props.max);
	    };
	    Handle.prototype.removeDocumentEventListeners = function () {
	        document.removeEventListener("mousemove", this.handleHandleMovement);
	        document.removeEventListener("mouseup", this.endHandleMovement);
	        document.removeEventListener("touchmove", this.handleHandleTouchMovement);
	        document.removeEventListener("touchend", this.endHandleTouchMovement);
	        document.removeEventListener("touchcancel", this.endHandleTouchMovement);
	    };
	    return Handle;
	}(abstractComponent_1.AbstractComponent));
	Handle.displayName = "Blueprint.SliderHandle";
	Handle = tslib_1.__decorate([
	    PureRender
	], Handle);
	exports.Handle = Handle;
	
	//# sourceMappingURL=handle.js.map


/***/ }),
/* 71 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var React = __webpack_require__(7);
	var Classes = __webpack_require__(16);
	var utils_1 = __webpack_require__(8);
	var coreSlider_1 = __webpack_require__(69);
	var handle_1 = __webpack_require__(70);
	var Slider = (function (_super) {
	    tslib_1.__extends(Slider, _super);
	    function Slider() {
	        var _this = _super !== null && _super.apply(this, arguments) || this;
	        _this.handleHandleRef = function (ref) {
	            _this.handle = ref;
	        };
	        return _this;
	    }
	    Slider.prototype.renderFill = function () {
	        var initialValue = utils_1.clamp(this.props.initialValue, this.props.min, this.props.max);
	        var left = Math.round((initialValue - this.props.min) * this.state.tickSize);
	        var width = Math.round((this.props.value - initialValue) * this.state.tickSize);
	        if (width < 0) {
	            left += width;
	            width = Math.abs(width);
	        }
	        return React.createElement("div", { className: Classes.SLIDER + "-progress", style: { left: left, width: width } });
	    };
	    Slider.prototype.renderHandles = function () {
	        // make sure to *not* pass this.props.className to handle
	        return (React.createElement(handle_1.Handle, tslib_1.__assign({}, this.props, this.state, { className: "", label: this.formatLabel(this.props.value), ref: this.handleHandleRef })));
	    };
	    Slider.prototype.handleTrackClick = function (event) {
	        if (this.handle != null) {
	            this.handle.beginHandleMovement(event);
	        }
	    };
	    Slider.prototype.handleTrackTouch = function (event) {
	        if (this.handle != null) {
	            this.handle.beginHandleTouchMovement(event);
	        }
	    };
	    return Slider;
	}(coreSlider_1.CoreSlider));
	Slider.defaultProps = {
	    disabled: false,
	    initialValue: 0,
	    labelStepSize: 1,
	    max: 10,
	    min: 0,
	    showTrackFill: true,
	    stepSize: 1,
	    value: 0,
	};
	exports.Slider = Slider;
	exports.SliderFactory = React.createFactory(Slider);
	
	//# sourceMappingURL=slider.js.map


/***/ }),
/* 72 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var React = __webpack_require__(7);
	var Classes = __webpack_require__(16);
	// import * to avoid "cannot be named" error on factory
	var spinner = __webpack_require__(40);
	var SVGSpinner = (function (_super) {
	    tslib_1.__extends(SVGSpinner, _super);
	    function SVGSpinner() {
	        return _super !== null && _super.apply(this, arguments) || this;
	    }
	    SVGSpinner.prototype.renderContainer = function (classes, content) {
	        return (React.createElement("g", { className: classNames(Classes.SVG_SPINNER, classes) },
	            React.createElement("g", { className: "pt-svg-spinner-transform-group" }, content)));
	    };
	    return SVGSpinner;
	}(spinner.Spinner));
	exports.SVGSpinner = SVGSpinner;
	exports.SVGSpinnerFactory = React.createFactory(SVGSpinner);
	
	//# sourceMappingURL=svgSpinner.js.map


/***/ }),
/* 73 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var PureRender = __webpack_require__(26);
	var React = __webpack_require__(7);
	var Classes = __webpack_require__(16);
	var Tab = (function (_super) {
	    tslib_1.__extends(Tab, _super);
	    function Tab() {
	        return _super !== null && _super.apply(this, arguments) || this;
	    }
	    Tab.prototype.render = function () {
	        return (React.createElement("li", { "aria-controls": this.props.panelId, "aria-disabled": this.props.isDisabled, "aria-expanded": this.props.isSelected, "aria-selected": this.props.isSelected, className: classNames(Classes.TAB, this.props.className), id: this.props.id, role: "tab", selected: this.props.isSelected ? true : null, tabIndex: this.props.isDisabled ? null : 0 }, this.props.children));
	    };
	    return Tab;
	}(React.Component));
	Tab.defaultProps = {
	    isDisabled: false,
	    isSelected: false,
	};
	Tab.displayName = "Blueprint.Tab";
	Tab = tslib_1.__decorate([
	    PureRender
	], Tab);
	exports.Tab = Tab;
	exports.TabFactory = React.createFactory(Tab);
	
	//# sourceMappingURL=tab.js.map


/***/ }),
/* 74 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var PureRender = __webpack_require__(26);
	var React = __webpack_require__(7);
	var react_dom_1 = __webpack_require__(23);
	var abstractComponent_1 = __webpack_require__(5);
	var Classes = __webpack_require__(16);
	var Errors = __webpack_require__(10);
	var Keys = __webpack_require__(17);
	var Utils = __webpack_require__(8);
	var tab_1 = __webpack_require__(73);
	var tabList_1 = __webpack_require__(75);
	var tabPanel_1 = __webpack_require__(76);
	var TAB_CSS_SELECTOR = "li[role=tab]";
	var Tabs = (function (_super) {
	    tslib_1.__extends(Tabs, _super);
	    function Tabs(props, context) {
	        var _this = _super.call(this, props, context) || this;
	        // state is initialized in the constructor but getStateFromProps needs state defined
	        _this.state = {};
	        _this.panelIds = [];
	        _this.tabIds = [];
	        _this.handleClick = function (e) {
	            _this.handleTabSelectingEvent(e);
	        };
	        _this.handleKeyPress = function (e) {
	            var insideTab = e.target.closest("." + Classes.TAB) != null;
	            if (insideTab && (e.which === Keys.SPACE || e.which === Keys.ENTER)) {
	                e.preventDefault();
	                _this.handleTabSelectingEvent(e);
	            }
	        };
	        _this.handleKeyDown = function (e) {
	            // don't want to handle keyDown events inside a tab panel
	            var insideTabList = e.target.closest("." + Classes.TAB_LIST) != null;
	            if (!insideTabList) {
	                return;
	            }
	            var focusedTabIndex = _this.getFocusedTabIndex();
	            if (focusedTabIndex === -1) {
	                return;
	            }
	            if (e.which === Keys.ARROW_LEFT) {
	                e.preventDefault();
	                // find previous tab that isn't disabled
	                var newTabIndex = focusedTabIndex - 1;
	                var tabIsDisabled = _this.isTabDisabled(newTabIndex);
	                while (tabIsDisabled && newTabIndex !== -1) {
	                    newTabIndex--;
	                    tabIsDisabled = _this.isTabDisabled(newTabIndex);
	                }
	                if (newTabIndex !== -1) {
	                    _this.focusTab(newTabIndex);
	                }
	            }
	            else if (e.which === Keys.ARROW_RIGHT) {
	                e.preventDefault();
	                // find next tab that isn't disabled
	                var tabsCount = _this.getTabsCount();
	                var newTabIndex = focusedTabIndex + 1;
	                var tabIsDisabled = _this.isTabDisabled(newTabIndex);
	                while (tabIsDisabled && newTabIndex !== tabsCount) {
	                    newTabIndex++;
	                    tabIsDisabled = _this.isTabDisabled(newTabIndex);
	                }
	                if (newTabIndex !== tabsCount) {
	                    _this.focusTab(newTabIndex);
	                }
	            }
	        };
	        _this.handleTabSelectingEvent = function (e) {
	            var tabElement = e.target.closest(TAB_CSS_SELECTOR);
	            // select only if Tab is one of us and is enabled
	            if (tabElement != null
	                && _this.tabIds.indexOf(tabElement.id) >= 0
	                && tabElement.getAttribute("aria-disabled") !== "true") {
	                var index = tabElement.parentElement.queryAll(TAB_CSS_SELECTOR).indexOf(tabElement);
	                _this.setSelectedTabIndex(index);
	            }
	        };
	        _this.state = _this.getStateFromProps(_this.props);
	        if (!Utils.isNodeEnv("production")) {
	            console.warn(Errors.TABS_WARN_DEPRECATED);
	        }
	        return _this;
	    }
	    Tabs.prototype.render = function () {
	        return (React.createElement("div", { className: classNames(Classes.TABS, this.props.className), onClick: this.handleClick, onKeyPress: this.handleKeyPress, onKeyDown: this.handleKeyDown }, this.getChildren()));
	    };
	    Tabs.prototype.componentWillReceiveProps = function (newProps) {
	        var newState = this.getStateFromProps(newProps);
	        this.setState(newState);
	    };
	    Tabs.prototype.componentDidMount = function () {
	        var _this = this;
	        var selectedTab = react_dom_1.findDOMNode(this.refs["tabs-" + this.state.selectedTabIndex]);
	        this.setTimeout(function () { return _this.moveIndicator(selectedTab); });
	    };
	    Tabs.prototype.componentDidUpdate = function (_, prevState) {
	        var _this = this;
	        var newIndex = this.state.selectedTabIndex;
	        if (newIndex !== prevState.selectedTabIndex) {
	            var tabElement_1 = react_dom_1.findDOMNode(this.refs["tabs-" + newIndex]);
	            // need to measure on the next frame in case the Tab children simultaneously change
	            this.setTimeout(function () { return _this.moveIndicator(tabElement_1); });
	        }
	    };
	    Tabs.prototype.validateProps = function (props) {
	        if (React.Children.count(props.children) > 0) {
	            var child = React.Children.toArray(props.children)[0];
	            if (child != null && child.type !== tabList_1.TabList) {
	                throw new Error(Errors.TABS_FIRST_CHILD);
	            }
	            if (this.getTabsCount() !== this.getPanelsCount()) {
	                throw new Error(Errors.TABS_MISMATCH);
	            }
	        }
	    };
	    /**
	     * Calculate the new height, width, and position of the tab indicator.
	     * Store the CSS values so the transition animation can start.
	     */
	    Tabs.prototype.moveIndicator = function (_a) {
	        var clientHeight = _a.clientHeight, clientWidth = _a.clientWidth, offsetLeft = _a.offsetLeft, offsetTop = _a.offsetTop;
	        var indicatorWrapperStyle = {
	            height: clientHeight,
	            transform: "translateX(" + Math.floor(offsetLeft) + "px) translateY(" + Math.floor(offsetTop) + "px)",
	            width: clientWidth,
	        };
	        this.setState({ indicatorWrapperStyle: indicatorWrapperStyle });
	    };
	    /**
	     * Most of the component logic lives here. We clone the children provided by the user to set up refs,
	     * accessibility attributes, and selection props correctly.
	     */
	    Tabs.prototype.getChildren = function () {
	        var _this = this;
	        for (var unassignedTabs = this.getTabsCount() - this.tabIds.length; unassignedTabs > 0; unassignedTabs--) {
	            this.tabIds.push(generateTabId());
	            this.panelIds.push(generatePanelId());
	        }
	        var childIndex = 0;
	        return React.Children.map(this.props.children, function (child) {
	            var result;
	            // can be null if conditionally rendering TabList / TabPanel
	            if (child == null) {
	                return null;
	            }
	            if (childIndex === 0) {
	                // clone TabList / Tab elements
	                result = _this.cloneTabList(child);
	            }
	            else {
	                var tabPanelIndex = childIndex - 1;
	                var shouldRenderTabPanel = _this.state.selectedTabIndex === tabPanelIndex;
	                result = shouldRenderTabPanel ? _this.cloneTabPanel(child, tabPanelIndex) : null;
	            }
	            childIndex++;
	            return result;
	        });
	    };
	    Tabs.prototype.cloneTabList = function (child) {
	        var _this = this;
	        var tabIndex = 0;
	        var tabs = React.Children.map(child.props.children, function (tab) {
	            // can be null if conditionally rendering Tab
	            if (tab == null) {
	                return null;
	            }
	            var clonedTab = React.cloneElement(tab, {
	                id: _this.tabIds[tabIndex],
	                isSelected: _this.state.selectedTabIndex === tabIndex,
	                panelId: _this.panelIds[tabIndex],
	                ref: "tabs-" + tabIndex,
	            });
	            tabIndex++;
	            return clonedTab;
	        });
	        return React.cloneElement(child, {
	            children: tabs,
	            indicatorWrapperStyle: this.state.indicatorWrapperStyle,
	            ref: "tablist",
	        });
	    };
	    Tabs.prototype.cloneTabPanel = function (child, tabIndex) {
	        return React.cloneElement(child, {
	            id: this.panelIds[tabIndex],
	            isSelected: this.state.selectedTabIndex === tabIndex,
	            ref: "panels-" + tabIndex,
	            tabId: this.tabIds[tabIndex],
	        });
	    };
	    Tabs.prototype.focusTab = function (index) {
	        var ref = "tabs-" + index;
	        var tab = react_dom_1.findDOMNode(this.refs[ref]);
	        tab.focus();
	    };
	    Tabs.prototype.getFocusedTabIndex = function () {
	        var focusedElement = document.activeElement;
	        if (focusedElement != null && focusedElement.classList.contains(Classes.TAB)) {
	            var tabId = focusedElement.id;
	            return this.tabIds.indexOf(tabId);
	        }
	        return -1;
	    };
	    Tabs.prototype.getTabs = function () {
	        if (this.props.children == null) {
	            return [];
	        }
	        var tabs = [];
	        if (React.Children.count(this.props.children) > 0) {
	            var firstChild = React.Children.toArray(this.props.children)[0];
	            if (firstChild != null) {
	                React.Children.forEach(firstChild.props.children, function (tabListChild) {
	                    if (tabListChild.type === tab_1.Tab) {
	                        tabs.push(tabListChild);
	                    }
	                });
	            }
	        }
	        return tabs;
	    };
	    Tabs.prototype.getTabsCount = function () {
	        return this.getTabs().length;
	    };
	    Tabs.prototype.getPanelsCount = function () {
	        if (this.props.children == null) {
	            return 0;
	        }
	        var index = 0;
	        var panelCount = 0;
	        React.Children.forEach(this.props.children, function (child) {
	            if (child.type === tabPanel_1.TabPanel) {
	                panelCount++;
	            }
	            index++;
	        });
	        return panelCount;
	    };
	    Tabs.prototype.getStateFromProps = function (props) {
	        var selectedTabIndex = props.selectedTabIndex, initialSelectedTabIndex = props.initialSelectedTabIndex;
	        if (this.isValidTabIndex(selectedTabIndex)) {
	            return { selectedTabIndex: selectedTabIndex };
	        }
	        else if (this.isValidTabIndex(initialSelectedTabIndex) && this.state.selectedTabIndex == null) {
	            return { selectedTabIndex: initialSelectedTabIndex };
	        }
	        else {
	            return this.state;
	        }
	    };
	    Tabs.prototype.isTabDisabled = function (index) {
	        var tab = this.getTabs()[index];
	        return tab != null && tab.props.isDisabled;
	    };
	    Tabs.prototype.isValidTabIndex = function (index) {
	        return index != null && index >= 0 && index < this.getTabsCount();
	    };
	    /**
	     * Updates the component's state if uncontrolled and calls onChange.
	     */
	    Tabs.prototype.setSelectedTabIndex = function (index) {
	        if (index === this.state.selectedTabIndex || !this.isValidTabIndex(index)) {
	            return;
	        }
	        var prevSelectedIndex = this.state.selectedTabIndex;
	        if (this.props.selectedTabIndex == null) {
	            this.setState({
	                selectedTabIndex: index,
	            });
	        }
	        if (Utils.isFunction(this.props.onChange)) {
	            this.props.onChange(index, prevSelectedIndex);
	        }
	    };
	    return Tabs;
	}(abstractComponent_1.AbstractComponent));
	Tabs.defaultProps = {
	    initialSelectedTabIndex: 0,
	};
	Tabs.displayName = "Blueprint.Tabs";
	Tabs = tslib_1.__decorate([
	    PureRender
	], Tabs);
	exports.Tabs = Tabs;
	var tabCount = 0;
	function generateTabId() {
	    return "pt-tab-" + tabCount++;
	}
	var panelCount = 0;
	function generatePanelId() {
	    return "pt-tab-panel-" + panelCount++;
	}
	exports.TabsFactory = React.createFactory(Tabs);
	
	//# sourceMappingURL=tabs.js.map


/***/ }),
/* 75 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var PureRender = __webpack_require__(26);
	var React = __webpack_require__(7);
	var abstractComponent_1 = __webpack_require__(5);
	var Classes = __webpack_require__(16);
	var TabList = (function (_super) {
	    tslib_1.__extends(TabList, _super);
	    function TabList() {
	        var _this = _super !== null && _super.apply(this, arguments) || this;
	        _this.state = {
	            shouldAnimate: false,
	        };
	        return _this;
	    }
	    TabList.prototype.render = function () {
	        return (React.createElement("ul", { className: classNames(Classes.TAB_LIST, this.props.className), role: "tablist" },
	            React.createElement("div", { className: classNames("pt-tab-indicator-wrapper", { "pt-no-animation": !this.state.shouldAnimate }), style: this.props.indicatorWrapperStyle },
	                React.createElement("div", { className: "pt-tab-indicator" })),
	            this.props.children));
	    };
	    TabList.prototype.componentDidUpdate = function (prevProps) {
	        var _this = this;
	        if (prevProps.indicatorWrapperStyle == null) {
	            this.setTimeout(function () { return _this.setState({ shouldAnimate: true }); });
	        }
	    };
	    return TabList;
	}(abstractComponent_1.AbstractComponent));
	TabList.displayName = "Blueprint.TabList";
	TabList = tslib_1.__decorate([
	    PureRender
	], TabList);
	exports.TabList = TabList;
	exports.TabListFactory = React.createFactory(TabList);
	
	//# sourceMappingURL=tabList.js.map


/***/ }),
/* 76 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var PureRender = __webpack_require__(26);
	var React = __webpack_require__(7);
	var Classes = __webpack_require__(16);
	var TabPanel = (function (_super) {
	    tslib_1.__extends(TabPanel, _super);
	    function TabPanel() {
	        return _super !== null && _super.apply(this, arguments) || this;
	    }
	    TabPanel.prototype.render = function () {
	        return (React.createElement("div", { "aria-labelledby": this.props._tabId, className: classNames(Classes.TAB_PANEL, this.props.className), id: this.props._id, role: "tabpanel" }, this.props.children));
	    };
	    return TabPanel;
	}(React.Component));
	TabPanel.displayName = "Blueprint.TabPanel";
	TabPanel = tslib_1.__decorate([
	    PureRender
	], TabPanel);
	exports.TabPanel = TabPanel;
	exports.TabPanelFactory = React.createFactory(TabPanel);
	
	//# sourceMappingURL=tabPanel.js.map


/***/ }),
/* 77 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var PureRender = __webpack_require__(26);
	var React = __webpack_require__(7);
	var Classes = __webpack_require__(16);
	var Tab2 = (function (_super) {
	    tslib_1.__extends(Tab2, _super);
	    function Tab2() {
	        return _super !== null && _super.apply(this, arguments) || this;
	    }
	    // this component is never rendered directly; see Tabs2#renderTabPanel()
	    /* istanbul ignore next */
	    Tab2.prototype.render = function () {
	        var _a = this.props, className = _a.className, panel = _a.panel;
	        return React.createElement("div", { className: classNames(Classes.TAB_PANEL, className), role: "tablist" }, panel);
	    };
	    return Tab2;
	}(React.Component));
	Tab2.defaultProps = {
	    disabled: false,
	    id: undefined,
	};
	Tab2.displayName = "Blueprint.Tab2";
	Tab2 = tslib_1.__decorate([
	    PureRender
	], Tab2);
	exports.Tab2 = Tab2;
	exports.Tab2Factory = React.createFactory(Tab2);
	
	//# sourceMappingURL=tab2.js.map


/***/ }),
/* 78 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var PureRender = __webpack_require__(26);
	var React = __webpack_require__(7);
	var abstractComponent_1 = __webpack_require__(5);
	var Classes = __webpack_require__(16);
	var Keys = __webpack_require__(17);
	var utils_1 = __webpack_require__(8);
	var tab2_1 = __webpack_require__(77);
	var tabTitle_1 = __webpack_require__(79);
	exports.Expander = function () { return React.createElement("div", { className: "pt-flex-expander" }); };
	var TAB_SELECTOR = "." + Classes.TAB;
	var Tabs2 = (function (_super) {
	    tslib_1.__extends(Tabs2, _super);
	    function Tabs2(props) {
	        var _this = _super.call(this, props) || this;
	        _this.refHandlers = {
	            tablist: function (tabElement) { return _this.tablistElement = tabElement; },
	        };
	        _this.handleKeyDown = function (e) {
	            var focusedElement = document.activeElement.closest(TAB_SELECTOR);
	            // rest of this is potentially expensive and futile, so bail if no tab is focused
	            if (focusedElement == null) {
	                return;
	            }
	            // must rely on DOM state because we have no way of mapping `focusedElement` to a JSX.Element
	            var enabledTabElements = _this.getTabElements()
	                .filter(function (el) { return el.getAttribute("aria-disabled") === "false"; });
	            var focusedIndex = enabledTabElements.indexOf(focusedElement);
	            var direction = _this.getKeyCodeDirection(e);
	            if (focusedIndex >= 0 && direction !== undefined) {
	                e.preventDefault();
	                var length_1 = enabledTabElements.length;
	                // auto-wrapping at 0 and `length`
	                var nextFocusedIndex = (focusedIndex + direction + length_1) % length_1;
	                enabledTabElements[nextFocusedIndex].focus();
	            }
	        };
	        _this.handleKeyPress = function (e) {
	            var targetTabElement = e.target.closest(TAB_SELECTOR);
	            if (targetTabElement != null && isEventKeyCode(e, Keys.SPACE, Keys.ENTER)) {
	                e.preventDefault();
	                targetTabElement.click();
	            }
	        };
	        _this.handleTabClick = function (newTabId, event) {
	            utils_1.safeInvoke(_this.props.onChange, newTabId, _this.state.selectedTabId, event);
	            if (_this.props.selectedTabId === undefined) {
	                _this.setState({ selectedTabId: newTabId });
	            }
	        };
	        _this.renderTabPanel = function (tab) {
	            var _a = tab.props, className = _a.className, panel = _a.panel, id = _a.id;
	            if (panel === undefined) {
	                return undefined;
	            }
	            return (React.createElement("div", { "aria-labelledby": tabTitle_1.generateTabTitleId(_this.props.id, id), "aria-hidden": id !== _this.state.selectedTabId, className: classNames(Classes.TAB_PANEL, className), id: tabTitle_1.generateTabPanelId(_this.props.id, id), key: id, role: "tabpanel" }, panel));
	        };
	        _this.renderTabTitle = function (tab) {
	            var id = tab.props.id;
	            return (React.createElement(tabTitle_1.TabTitle, tslib_1.__assign({}, tab.props, { parentId: _this.props.id, onClick: _this.handleTabClick, selected: id === _this.state.selectedTabId })));
	        };
	        var selectedTabId = _this.getInitialSelectedTabId();
	        _this.state = { selectedTabId: selectedTabId };
	        return _this;
	    }
	    Tabs2.prototype.render = function () {
	        var _this = this;
	        var _a = this.state, indicatorWrapperStyle = _a.indicatorWrapperStyle, selectedTabId = _a.selectedTabId;
	        var tabTitles = React.Children.map(this.props.children, function (child) { return (isTab(child) ? _this.renderTabTitle(child) : child); });
	        var tabPanels = this.getTabChildren()
	            .filter(this.props.renderActiveTabPanelOnly ? function (tab) { return tab.props.id === selectedTabId; } : function () { return true; })
	            .map(this.renderTabPanel);
	        var tabIndicator = (React.createElement("div", { className: "pt-tab-indicator-wrapper", style: indicatorWrapperStyle },
	            React.createElement("div", { className: "pt-tab-indicator" })));
	        var classes = classNames(Classes.TABS, (_b = {}, _b[Classes.VERTICAL] = this.props.vertical, _b), this.props.className);
	        return (React.createElement("div", { className: classes },
	            React.createElement("div", { className: Classes.TAB_LIST, onKeyDown: this.handleKeyDown, onKeyPress: this.handleKeyPress, ref: this.refHandlers.tablist, role: "tablist" },
	                this.props.animate ? tabIndicator : undefined,
	                tabTitles),
	            tabPanels));
	        var _b;
	    };
	    Tabs2.prototype.componentDidMount = function () {
	        this.moveSelectionIndicator();
	    };
	    Tabs2.prototype.componentWillReceiveProps = function (_a) {
	        var selectedTabId = _a.selectedTabId;
	        if (selectedTabId !== undefined) {
	            // keep state in sync with controlled prop, so state is canonical source of truth
	            this.setState({ selectedTabId: selectedTabId });
	        }
	    };
	    Tabs2.prototype.componentDidUpdate = function (_prevProps, prevState) {
	        if (this.state.selectedTabId !== prevState.selectedTabId) {
	            this.moveSelectionIndicator();
	        }
	    };
	    Tabs2.prototype.getInitialSelectedTabId = function () {
	        // NOTE: providing an unknown ID will hide the selection
	        var _a = this.props, defaultSelectedTabId = _a.defaultSelectedTabId, selectedTabId = _a.selectedTabId;
	        if (selectedTabId !== undefined) {
	            return selectedTabId;
	        }
	        else if (defaultSelectedTabId !== undefined) {
	            return defaultSelectedTabId;
	        }
	        else {
	            // select first tab in absence of user input
	            var tabs = this.getTabChildren();
	            return tabs.length === 0 ? undefined : tabs[0].props.id;
	        }
	    };
	    Tabs2.prototype.getKeyCodeDirection = function (e) {
	        if (isEventKeyCode(e, Keys.ARROW_LEFT, Keys.ARROW_UP)) {
	            return -1;
	        }
	        else if (isEventKeyCode(e, Keys.ARROW_RIGHT, Keys.ARROW_DOWN)) {
	            return 1;
	        }
	        return undefined;
	    };
	    /** Filters this.props.children to only `<Tab>`s */
	    Tabs2.prototype.getTabChildren = function () {
	        return React.Children.toArray(this.props.children).filter(isTab);
	    };
	    /** Queries root HTML element for all `.pt-tab`s with optional filter selector */
	    Tabs2.prototype.getTabElements = function (subselector) {
	        if (subselector === void 0) { subselector = ""; }
	        if (this.tablistElement == null) {
	            return [];
	        }
	        return this.tablistElement.queryAll(TAB_SELECTOR + subselector);
	    };
	    /**
	     * Calculate the new height, width, and position of the tab indicator.
	     * Store the CSS values so the transition animation can start.
	     */
	    Tabs2.prototype.moveSelectionIndicator = function () {
	        if (this.tablistElement === undefined) {
	            return;
	        }
	        var tabIdSelector = TAB_SELECTOR + "[data-tab-id=\"" + this.state.selectedTabId + "\"]";
	        var selectedTabElement = this.tablistElement.query(tabIdSelector);
	        var indicatorWrapperStyle = { display: "none" };
	        if (selectedTabElement != null) {
	            var clientHeight = selectedTabElement.clientHeight, clientWidth = selectedTabElement.clientWidth, offsetLeft = selectedTabElement.offsetLeft, offsetTop = selectedTabElement.offsetTop;
	            indicatorWrapperStyle = {
	                height: clientHeight,
	                transform: "translateX(" + Math.floor(offsetLeft) + "px) translateY(" + Math.floor(offsetTop) + "px)",
	                width: clientWidth,
	            };
	        }
	        this.setState({ indicatorWrapperStyle: indicatorWrapperStyle });
	    };
	    return Tabs2;
	}(abstractComponent_1.AbstractComponent));
	/** Insert a `Tabs2.Expander` between any two children to right-align all subsequent children. */
	Tabs2.Expander = exports.Expander;
	Tabs2.Tab = tab2_1.Tab2;
	Tabs2.defaultProps = {
	    animate: true,
	    renderActiveTabPanelOnly: false,
	    vertical: false,
	};
	Tabs2.displayName = "Blueprint.Tabs2";
	Tabs2 = tslib_1.__decorate([
	    PureRender
	], Tabs2);
	exports.Tabs2 = Tabs2;
	exports.Tabs2Factory = React.createFactory(Tabs2);
	function isEventKeyCode(e) {
	    var codes = [];
	    for (var _i = 1; _i < arguments.length; _i++) {
	        codes[_i - 1] = arguments[_i];
	    }
	    return codes.indexOf(e.which) >= 0;
	}
	function isTab(child) {
	    return child != null && child.type === tab2_1.Tab2;
	}
	
	//# sourceMappingURL=tabs2.js.map


/***/ }),
/* 79 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var PureRender = __webpack_require__(26);
	var React = __webpack_require__(7);
	var Classes = __webpack_require__(16);
	var TabTitle = (function (_super) {
	    tslib_1.__extends(TabTitle, _super);
	    function TabTitle() {
	        var _this = _super !== null && _super.apply(this, arguments) || this;
	        _this.handleClick = function (e) { return _this.props.onClick(_this.props.id, e); };
	        return _this;
	    }
	    TabTitle.prototype.render = function () {
	        var _a = this.props, disabled = _a.disabled, id = _a.id, parentId = _a.parentId, selected = _a.selected;
	        return (React.createElement("div", { "aria-controls": generateTabPanelId(parentId, id), "aria-disabled": disabled, "aria-expanded": selected, "aria-selected": selected, className: classNames(Classes.TAB, this.props.className), "data-tab-id": id, id: generateTabTitleId(parentId, id), onClick: disabled ? undefined : this.handleClick, role: "tab", selected: selected ? true : undefined, tabIndex: disabled ? undefined : 0 },
	            this.props.title,
	            this.props.children));
	    };
	    return TabTitle;
	}(React.Component));
	TabTitle.displayName = "Blueprint.TabTitle";
	TabTitle = tslib_1.__decorate([
	    PureRender
	], TabTitle);
	exports.TabTitle = TabTitle;
	function generateTabPanelId(parentId, tabId) {
	    return Classes.TAB_PANEL + "_" + parentId + "_" + tabId;
	}
	exports.generateTabPanelId = generateTabPanelId;
	function generateTabTitleId(parentId, tabId) {
	    return Classes.TAB + "-title_" + parentId + "_" + tabId;
	}
	exports.generateTabTitleId = generateTabTitleId;
	
	//# sourceMappingURL=tabTitle.js.map


/***/ }),
/* 80 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var PureRender = __webpack_require__(26);
	var React = __webpack_require__(7);
	var common_1 = __webpack_require__(4);
	var props_1 = __webpack_require__(14);
	var Classes = __webpack_require__(16);
	var Tag = (function (_super) {
	    tslib_1.__extends(Tag, _super);
	    function Tag() {
	        var _this = _super !== null && _super.apply(this, arguments) || this;
	        _this.onRemoveClick = function (e) {
	            common_1.Utils.safeInvoke(_this.props.onRemove, e, _this.props);
	        };
	        return _this;
	    }
	    Tag.prototype.render = function () {
	        var _a = this.props, active = _a.active, className = _a.className, intent = _a.intent, onRemove = _a.onRemove;
	        var tagClasses = classNames(Classes.TAG, Classes.intentClass(intent), (_b = {},
	            _b[Classes.TAG_REMOVABLE] = onRemove != null,
	            _b[Classes.ACTIVE] = active,
	            _b), className);
	        var button = common_1.Utils.isFunction(onRemove)
	            ? React.createElement("button", { type: "button", className: Classes.TAG_REMOVE, onClick: this.onRemoveClick })
	            : undefined;
	        return (React.createElement("span", tslib_1.__assign({}, props_1.removeNonHTMLProps(this.props), { className: tagClasses }),
	            this.props.children,
	            button));
	        var _b;
	    };
	    return Tag;
	}(React.Component));
	Tag.displayName = "Blueprint.Tag";
	Tag = tslib_1.__decorate([
	    PureRender
	], Tag);
	exports.Tag = Tag;
	exports.TagFactory = React.createFactory(Tag);
	
	//# sourceMappingURL=tag.js.map


/***/ }),
/* 81 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var PureRender = __webpack_require__(26);
	var React = __webpack_require__(7);
	var abstractComponent_1 = __webpack_require__(5);
	var Classes = __webpack_require__(16);
	var utils_1 = __webpack_require__(8);
	var buttons_1 = __webpack_require__(37);
	var icon_1 = __webpack_require__(39);
	var Toast = (function (_super) {
	    tslib_1.__extends(Toast, _super);
	    function Toast() {
	        var _this = _super !== null && _super.apply(this, arguments) || this;
	        _this.handleActionClick = function (e) {
	            utils_1.safeInvoke(_this.props.action.onClick, e);
	            _this.triggerDismiss(false);
	        };
	        _this.handleCloseClick = function () { return _this.triggerDismiss(false); };
	        _this.startTimeout = function () {
	            if (_this.props.timeout > 0) {
	                _this.setTimeout(function () { return _this.triggerDismiss(true); }, _this.props.timeout);
	            }
	        };
	        return _this;
	    }
	    Toast.prototype.render = function () {
	        var _a = this.props, className = _a.className, iconName = _a.iconName, intent = _a.intent, message = _a.message;
	        return (React.createElement("div", { className: classNames(Classes.TOAST, Classes.intentClass(intent), className), onBlur: this.startTimeout, onFocus: this.clearTimeouts, onMouseEnter: this.clearTimeouts, onMouseLeave: this.startTimeout, tabIndex: 0 },
	            React.createElement(icon_1.Icon, { iconName: iconName }),
	            React.createElement("span", { className: Classes.TOAST_MESSAGE }, message),
	            React.createElement("div", { className: classNames(Classes.BUTTON_GROUP, Classes.MINIMAL) },
	                this.maybeRenderActionButton(),
	                React.createElement(buttons_1.Button, { iconName: "cross", onClick: this.handleCloseClick }))));
	    };
	    Toast.prototype.componentDidMount = function () {
	        this.startTimeout();
	    };
	    Toast.prototype.componentDidUpdate = function (prevProps) {
	        if (prevProps.timeout <= 0 && this.props.timeout > 0) {
	            this.startTimeout();
	        }
	        else if (prevProps.timeout > 0 && this.props.timeout <= 0) {
	            this.clearTimeouts();
	        }
	    };
	    Toast.prototype.componentWillUnmount = function () {
	        this.clearTimeouts();
	    };
	    Toast.prototype.maybeRenderActionButton = function () {
	        var action = this.props.action;
	        if (action == null) {
	            return undefined;
	        }
	        else {
	            return React.createElement(buttons_1.AnchorButton, tslib_1.__assign({}, action, { intent: undefined, onClick: this.handleActionClick }));
	        }
	    };
	    Toast.prototype.triggerDismiss = function (didTimeoutExpire) {
	        utils_1.safeInvoke(this.props.onDismiss, didTimeoutExpire);
	        this.clearTimeouts();
	    };
	    return Toast;
	}(abstractComponent_1.AbstractComponent));
	Toast.defaultProps = {
	    className: "",
	    message: "",
	    timeout: 5000,
	};
	Toast.displayName = "Blueprint.Toast";
	Toast = tslib_1.__decorate([
	    PureRender
	], Toast);
	exports.Toast = Toast;
	exports.ToastFactory = React.createFactory(Toast);
	
	//# sourceMappingURL=toast.js.map


/***/ }),
/* 82 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var PureRender = __webpack_require__(26);
	var React = __webpack_require__(7);
	var ReactDOM = __webpack_require__(23);
	var abstractComponent_1 = __webpack_require__(5);
	var Classes = __webpack_require__(16);
	var errors_1 = __webpack_require__(10);
	var keys_1 = __webpack_require__(17);
	var position_1 = __webpack_require__(13);
	var utils_1 = __webpack_require__(8);
	var overlay_1 = __webpack_require__(31);
	var toast_1 = __webpack_require__(81);
	var Toaster = Toaster_1 = (function (_super) {
	    tslib_1.__extends(Toaster, _super);
	    function Toaster() {
	        var _this = _super !== null && _super.apply(this, arguments) || this;
	        _this.state = {
	            toasts: [],
	        };
	        // auto-incrementing identifier for un-keyed toasts
	        _this.toastId = 0;
	        _this.getDismissHandler = function (toast) { return function (timeoutExpired) {
	            _this.dismiss(toast.key, timeoutExpired);
	        }; };
	        _this.handleClose = function (e) {
	            // NOTE that `e` isn't always a KeyboardEvent but that's the only type we care about
	            if (e.which === keys_1.ESCAPE) {
	                _this.clear();
	            }
	        };
	        return _this;
	    }
	    /**
	     * Create a new `Toaster` instance that can be shared around your application.
	     * The `Toaster` will be rendered into a new element appended to the given container.
	     */
	    Toaster.create = function (props, container) {
	        if (container === void 0) { container = document.body; }
	        if (props != null && props.inline != null && !utils_1.isNodeEnv("production")) {
	            console.warn(errors_1.TOASTER_WARN_INLINE);
	        }
	        var containerElement = document.createElement("div");
	        container.appendChild(containerElement);
	        return ReactDOM.render(React.createElement(Toaster_1, tslib_1.__assign({}, props, { inline: true })), containerElement);
	    };
	    Toaster.prototype.show = function (props) {
	        var options = this.createToastOptions(props);
	        this.setState(function (prevState) { return ({
	            toasts: [options].concat(prevState.toasts),
	        }); });
	        return options.key;
	    };
	    Toaster.prototype.update = function (key, props) {
	        var options = this.createToastOptions(props, key);
	        this.setState(function (prevState) { return ({
	            toasts: prevState.toasts.map(function (t) { return t.key === key ? options : t; }),
	        }); });
	    };
	    Toaster.prototype.dismiss = function (key, timeoutExpired) {
	        if (timeoutExpired === void 0) { timeoutExpired = false; }
	        this.setState(function (_a) {
	            var toasts = _a.toasts;
	            return ({
	                toasts: toasts.filter(function (t) {
	                    var matchesKey = t.key === key;
	                    if (matchesKey) {
	                        utils_1.safeInvoke(t.onDismiss, timeoutExpired);
	                    }
	                    return !matchesKey;
	                }),
	            });
	        });
	    };
	    Toaster.prototype.clear = function () {
	        this.state.toasts.map(function (t) { return utils_1.safeInvoke(t.onDismiss, false); });
	        this.setState({ toasts: [] });
	    };
	    Toaster.prototype.getToasts = function () {
	        return this.state.toasts;
	    };
	    Toaster.prototype.render = function () {
	        // $pt-transition-duration * 3 + $pt-transition-duration / 2
	        var classes = classNames(Classes.TOAST_CONTAINER, this.getPositionClasses(), this.props.className);
	        return (React.createElement(overlay_1.Overlay, { autoFocus: this.props.autoFocus, canEscapeKeyClose: this.props.canEscapeKeyClear, canOutsideClickClose: false, className: classes, enforceFocus: false, hasBackdrop: false, inline: this.props.inline, isOpen: this.state.toasts.length > 0, onClose: this.handleClose, transitionDuration: 350, transitionName: "pt-toast" }, this.state.toasts.map(this.renderToast, this)));
	    };
	    Toaster.prototype.validateProps = function (props) {
	        if (props.position === position_1.Position.LEFT || props.position === position_1.Position.RIGHT) {
	            console.warn(errors_1.TOASTER_WARN_LEFT_RIGHT);
	        }
	    };
	    Toaster.prototype.renderToast = function (toast) {
	        return React.createElement(toast_1.Toast, tslib_1.__assign({}, toast, { onDismiss: this.getDismissHandler(toast) }));
	    };
	    Toaster.prototype.createToastOptions = function (props, key) {
	        if (key === void 0) { key = "toast-" + this.toastId++; }
	        // clone the object before adding the key prop to avoid leaking the mutation
	        return tslib_1.__assign({}, props, { key: key });
	    };
	    Toaster.prototype.getPositionClasses = function () {
	        var positions = position_1.Position[this.props.position].split("_");
	        // NOTE that there is no -center class because that's the default style
	        return positions.map(function (p) { return Classes.TOAST_CONTAINER + "-" + p.toLowerCase(); });
	    };
	    return Toaster;
	}(abstractComponent_1.AbstractComponent));
	Toaster.defaultProps = {
	    autoFocus: false,
	    canEscapeKeyClear: true,
	    inline: false,
	    position: position_1.Position.TOP,
	};
	Toaster = Toaster_1 = tslib_1.__decorate([
	    PureRender
	], Toaster);
	exports.Toaster = Toaster;
	var Toaster_1;
	
	//# sourceMappingURL=toaster.js.map


/***/ }),
/* 83 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var React = __webpack_require__(7);
	var Classes = __webpack_require__(16);
	var utils_1 = __webpack_require__(8);
	var treeNode_1 = __webpack_require__(84);
	var Tree = (function (_super) {
	    tslib_1.__extends(Tree, _super);
	    function Tree() {
	        var _this = _super !== null && _super.apply(this, arguments) || this;
	        _this.nodeRefs = {};
	        _this.handleNodeCollapse = function (node, e) {
	            _this.handlerHelper(_this.props.onNodeCollapse, node, e);
	        };
	        _this.handleNodeClick = function (node, e) {
	            _this.handlerHelper(_this.props.onNodeClick, node, e);
	        };
	        _this.handleContentRef = function (node, element) {
	            if (element != null) {
	                _this.nodeRefs[node.props.id] = element;
	            }
	            else {
	                // don't want our object to get bloated with old keys
	                delete _this.nodeRefs[node.props.id];
	            }
	        };
	        _this.handleNodeContextMenu = function (node, e) {
	            _this.handlerHelper(_this.props.onNodeContextMenu, node, e);
	        };
	        _this.handleNodeDoubleClick = function (node, e) {
	            _this.handlerHelper(_this.props.onNodeDoubleClick, node, e);
	        };
	        _this.handleNodeExpand = function (node, e) {
	            _this.handlerHelper(_this.props.onNodeExpand, node, e);
	        };
	        return _this;
	    }
	    Tree.nodeFromPath = function (path, treeNodes) {
	        if (path.length === 1) {
	            return treeNodes[path[0]];
	        }
	        else {
	            return Tree.nodeFromPath(path.slice(1), treeNodes[path[0]].childNodes);
	        }
	    };
	    Tree.prototype.render = function () {
	        return (React.createElement("div", { className: classNames(Classes.TREE, this.props.className) }, this.renderNodes(this.props.contents, [], Classes.TREE_ROOT)));
	    };
	    /**
	     * Returns the underlying HTML element of the `Tree` node with an id of `nodeId`.
	     * This element does not contain the children of the node, only its label and controls.
	     * If the node is not currently mounted, `undefined` is returned.
	     */
	    Tree.prototype.getNodeContentElement = function (nodeId) {
	        return this.nodeRefs[nodeId];
	    };
	    Tree.prototype.renderNodes = function (treeNodes, currentPath, className) {
	        var _this = this;
	        if (treeNodes == null) {
	            return null;
	        }
	        var nodeItems = treeNodes.map(function (node, i) {
	            var elementPath = currentPath.concat(i);
	            return (React.createElement(treeNode_1.TreeNode, tslib_1.__assign({}, node, { key: node.id, contentRef: _this.handleContentRef, depth: elementPath.length - 1, onClick: _this.handleNodeClick, onContextMenu: _this.handleNodeContextMenu, onCollapse: _this.handleNodeCollapse, onDoubleClick: _this.handleNodeDoubleClick, onExpand: _this.handleNodeExpand, path: elementPath }), _this.renderNodes(node.childNodes, elementPath)));
	        });
	        return (React.createElement("ul", { className: classNames(Classes.TREE_NODE_LIST, className) }, nodeItems));
	    };
	    Tree.prototype.handlerHelper = function (handlerFromProps, node, e) {
	        if (utils_1.isFunction(handlerFromProps)) {
	            var nodeData = Tree.nodeFromPath(node.props.path, this.props.contents);
	            handlerFromProps(nodeData, node.props.path, e);
	        }
	    };
	    return Tree;
	}(React.Component));
	exports.Tree = Tree;
	exports.TreeFactory = React.createFactory(Tree);
	
	//# sourceMappingURL=tree.js.map


/***/ }),
/* 84 */
/***/ (function(module, exports, __webpack_require__) {

	/*
	 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
	 * Licensed under the BSD-3 License as modified (the “License”); you may obtain a copy
	 * of the license at https://github.com/palantir/blueprint/blob/master/LICENSE
	 * and https://github.com/palantir/blueprint/blob/master/PATENTS
	 */
	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var tslib_1 = __webpack_require__(6);
	var classNames = __webpack_require__(25);
	var React = __webpack_require__(7);
	var Classes = __webpack_require__(16);
	var utils_1 = __webpack_require__(8);
	var collapse_1 = __webpack_require__(43);
	var icon_1 = __webpack_require__(39);
	var TreeNode = (function (_super) {
	    tslib_1.__extends(TreeNode, _super);
	    function TreeNode() {
	        var _this = _super !== null && _super.apply(this, arguments) || this;
	        _this.handleCaretClick = function (e) {
	            e.stopPropagation();
	            var _a = _this.props, isExpanded = _a.isExpanded, onCollapse = _a.onCollapse, onExpand = _a.onExpand;
	            utils_1.safeInvoke(isExpanded ? onCollapse : onExpand, _this, e);
	        };
	        _this.handleClick = function (e) {
	            utils_1.safeInvoke(_this.props.onClick, _this, e);
	        };
	        _this.handleContentRef = function (element) {
	            utils_1.safeInvoke(_this.props.contentRef, _this, element);
	        };
	        _this.handleContextMenu = function (e) {
	            utils_1.safeInvoke(_this.props.onContextMenu, _this, e);
	        };
	        _this.handleDoubleClick = function (e) {
	            utils_1.safeInvoke(_this.props.onDoubleClick, _this, e);
	        };
	        return _this;
	    }
	    TreeNode.prototype.render = function () {
	        var _a = this.props, children = _a.children, className = _a.className, hasCaret = _a.hasCaret, iconName = _a.iconName, isExpanded = _a.isExpanded, isSelected = _a.isSelected, label = _a.label;
	        var showCaret = hasCaret == null ? React.Children.count(children) > 0 : hasCaret;
	        var caretClass = showCaret ? Classes.TREE_NODE_CARET : Classes.TREE_NODE_CARET_NONE;
	        var caretStateClass = isExpanded ? Classes.TREE_NODE_CARET_OPEN : Classes.TREE_NODE_CARET_CLOSED;
	        var caretClasses = classNames(caretClass, Classes.ICON_STANDARD, (_b = {},
	            _b[caretStateClass] = showCaret,
	            _b));
	        var classes = classNames(Classes.TREE_NODE, (_c = {},
	            _c[Classes.TREE_NODE_SELECTED] = isSelected,
	            _c[Classes.TREE_NODE_EXPANDED] = isExpanded,
	            _c), className);
	        var contentClasses = classNames(Classes.TREE_NODE_CONTENT, "pt-tree-node-content-" + this.props.depth);
	        return (React.createElement("li", { className: classes },
	            React.createElement("div", { className: contentClasses, onClick: this.handleClick, onContextMenu: this.handleContextMenu, onDoubleClick: this.handleDoubleClick, ref: this.handleContentRef },
	                React.createElement("span", { className: caretClasses, onClick: showCaret ? this.handleCaretClick : null }),
	                React.createElement(icon_1.Icon, { className: Classes.TREE_NODE_ICON, iconName: iconName }),
	                React.createElement("span", { className: Classes.TREE_NODE_LABEL }, label),
	                this.maybeRenderSecondaryLabel()),
	            React.createElement(collapse_1.Collapse, { isOpen: isExpanded }, children)));
	        var _b, _c;
	    };
	    TreeNode.prototype.maybeRenderSecondaryLabel = function () {
	        if (this.props.secondaryLabel != null) {
	            return React.createElement("span", { className: Classes.TREE_NODE_SECONDARY_LABEL }, this.props.secondaryLabel);
	        }
	        else {
	            return undefined;
	        }
	    };
	    return TreeNode;
	}(React.Component));
	exports.TreeNode = TreeNode;
	
	//# sourceMappingURL=treeNode.js.map


/***/ })
/******/ ])
});
;
//# sourceMappingURL=core.bundle.js.map