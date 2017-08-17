
/**
 * Minimalistic Promise class. FIXME: add timeout feature
 *
 * Warning: this one works different (same as kontraktor-promises)
 *
 * a kpromise is created unresolved like "new KPromise()" or ressolved with "new KPromise(result,error)";
 *
 * onyone might call complete(result,error) on an unresolved KPromise. so resolve ~= complete( result, null ) and reject ~= complete(null,error);
 *
 * @param optional initialResult
 * @constructor
 */
class KPromise {

  constructor(initialResult,error) {
    this.res = (initialResult || error) ? [initialResult ? initialResult : null, error ? error : null] : null;
    this.cb = null;
    this.nextPromise = null;
  };

  isCompleted() { return this.res; };

  _notify() {
    const res = this.cb.apply(null,this.res);
    this.cb = null;
    if ( res instanceof KPromise ) {
      res.then(this.nextPromise);
    } else {
      this.nextPromise.complete(this.res[0],this.res[1]);
    }
  };

  then(cb) {
    if ( this.cb )
      throw "double callback registration on promise";
    this.cb = cb;
    this.nextPromise = new KPromise();
    if ( this.res ) {
      this._notify();
    }
    return this.nextPromise;
  };

  resolve(result) {
    this.complete(result,null);
  }

  reject(err) {
    this.complete(null,err);
  }

  complete(r,e) {
    if ( this.res )
      throw "double completion on promise";
    this.res = [r,e];
    if ( this.cb ) {
      this._notify();
    }
  };

}

class DecodingHelper {

  constructor() {
    this.postObjectDecodingMap = {}; // maps java type to a transorming function
  }

  registerDecoder(javatype, mapfunction ) {
    this.postObjectDecodingMap[javatype] = mapfunction;
  }

  /**
   * a function obj => obj applied on raw incoming messages
   * @param transformerfunction
   */
  setTransformer(transformerfunction) {
    this.optionalTransformer = transformerfunction;
  }

  /**
   * makes a fst json serialized object more js-friendly
   * @param obj
   * @param preserveTypeAsAttribute - create a _typ property on each object denoting original java type
   * @param optionalTransformer - called as a map function for each object. if returns != null => replace given object in tree with result
   * @returns {*}
   */
  transformJavaJson(obj, preserveTypeAsAttribute, optionalTransformer) {
    if ( !optionalTransformer )
      optionalTransformer = this.optionalTransformer;

    if (optionalTransformer) {
      var trans = optionalTransformer.apply(null, [obj]);
      if (trans)
        return trans;
    }
    if (!obj)
      return obj;
    if (obj["styp"] && obj["seq"]) {
      var arr = this.transformJavaJson(obj["seq"], preserveTypeAsAttribute, optionalTransformer);
      if (arr) {
        arr.shift();
        if (preserveTypeAsAttribute)
          arr["_typ"] = obj["styp"];
      }
      return arr;
    }
    if (obj["typ"] && obj["obj"]) {
      if ('list' === obj['typ'] || 'map' === obj['typ'] ) {
        // remove leading element length from arraylist, map
        obj["obj"].shift();
      }
      var res = this.transformJavaJson(obj["obj"], preserveTypeAsAttribute, optionalTransformer);
      if (preserveTypeAsAttribute)
        res["_typ"] = obj["typ"];
      if ( typeof this.postObjectDecodingMap[res["_typ"]] !== 'undefined' ) { // aplly mapping if present
        res = this.postObjectDecodingMap[res["_typ"]].apply(null,[res]);
      }
      return res;
    }
    for (var property in obj) {
      if (obj.hasOwnProperty(property) && obj[property] != null) {
        if (obj[property].constructor == Object) {
          obj[property] = this.transformJavaJson(obj[property], preserveTypeAsAttribute, optionalTransformer);
        } else if (obj[property].constructor == Array) {
          for (var i = 0; i < obj[property].length; i++) {
            obj[property][i] = this.transformJavaJson(obj[property][i], preserveTypeAsAttribute, optionalTransformer);
          }
        }
      }
    }
    return obj;
  }

  /**
   * create a java readable object from a javascript object
   *
   * @param type - java type (full qualified or simple class name if registered so at java side)
   * @param obj
   * @returns {{typ: *, obj: *}}
   */
  jobj( type, obj ) {
    delete obj._key; // only quick fix - see: #859
    return { typ: type, obj: obj }
  }

  /**
   * create wrapper object to make given list a valid fst-json Java array (non-primitive)
   *
   * @param type - 'array' for object array, else type of java array
   * @param list - list of properly structured (for java) objects
   */
  jarray( type, list ) {
    list.splice( 0, 0, list.length ); // insert number of elements at 0
    return { styp: type, seq: list };
  }

  // shorthand for java object array
  joa( jsArr ) {
    return this.jarray("array",jsArr);
  }

  /**
   * transforms a java hashmap to a JS object, assumes keys are strings
   * @param jmap
   */
  jsmap(jmap) {
    var res = {};
    for ( var i = 0; i < jmap.length; i+=2 ) {
      res[jmap[i]] = jmap[i+1];
    }
    return res;
  }

  /**
   *
   * build java list style collection
   * Does not work for java Map's
   *
   * @param type - 'list' for ArrayList, 'set' for HashSet, class name for subclassed
   * @param list - list of properly structured (for java) objects
   */
  jcoll( type, list ) {
    list.splice( 0, 0, list.length ); // insert number of elements at 0
    return { typ: type, obj: list };
  }

  /**
   * builds a java hashmap from array like '[ key, val, key, val ]' or a js object
   *
   * if list is an object, build a hashmap from the properties of that object
   *
   * @param type - "map" or class name if subclassed map is used OPTIONAL
   * @param list - array of [ key, val, key1, val1 } OR a javascript object
   */
  jmap( type, list ) {
    if ( ! list ) { // if type is ommited default to map
      list = type;
      type = 'map';
    }
    if( Object.prototype.toString.call( list ) === '[object Array]' ) {
        list.splice( 0, 0, list.length/2 ); // insert number of elements at 0
        return { typ: type, obj: list };
    } else {
      var res = { typ: type, obj: [] };
      var count = 0;
      for (var property in list) {
        if (list.hasOwnProperty(property)) {
            count++;
            res.obj.push(property);
            res.obj.push(list[property]);
        }
      }
      res.obj.splice( 0, 0, res.obj.length/2 ); // insert number of elements at 0
      return res;
    }
  }
}

if ( typeof module !== 'undefined' ) {
  module.exports = {
    KPromise : KPromise,
    DecodingHelper : DecodingHelper
  };
}
