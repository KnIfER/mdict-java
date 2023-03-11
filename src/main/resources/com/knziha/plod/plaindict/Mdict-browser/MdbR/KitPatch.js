if (typeof Element.prototype.remove != 'function') {  
    Element.prototype.remove = function() {
        this.parentElement.removeChild(this);
    }
    NodeList.prototype.remove = HTMLCollection.prototype.remove = function() {
        for(var i = this.length - 1; i >= 0; i--) {
            if(this[i] && this[i].parentElement) {
                this[i].parentElement.removeChild(this[i]);
            }
        }
    }    
} 

if (typeof Element.prototype.prepend != 'function') {  
    Element.prototype.prepend = function(e) {
        this.insertBefore(e, this.firstChild);
    } 
} 

if (typeof String.prototype.startsWith != 'function') {  
    String.prototype.startsWith = function (prefix){  
        return this.slice(0, prefix.length) === prefix;  
    };  
    window.kit=1;
} 
if (typeof Event.prototype.composedPath	!= 'function') {  
    Event.prototype.composedPath = function (){  
		var e=this;
		if (e.path) return e.path
		var target = e.target;
		e.path = [];
		while (target.parentNode !== null) {
			e.path.push(target);
			target = target.parentNode;
		}
		e.path.push(document, window);
		return e.path;  
    };  
    window.kit=1;
} 
if (typeof String.prototype.endsWith != 'function') {  
    String.prototype.endsWith = function (suffix){  
        return this.slice(-suffix.length) === suffix;  
    };  
} 
if (typeof Object.assign != 'function') {
    Object.assign = function (target, varArgs) {
        'use strict';
        if (target == null) {
            throw new TypeError('Cannot convert undefined or null to object');
        }
        var to = Object(target);
        for (var index = 1; index < arguments.length; index++) {
            var nextSource = arguments[index];

            if (nextSource != null) {
                for (var nextKey in nextSource) {
                    if (Object.prototype.hasOwnProperty.call(nextSource, nextKey)) {
                        to[nextKey] = nextSource[nextKey];
                    }
                }
            }
        }
        return to;
    };
}

if(![].at && window.Reflect) {
	function at(n) {
		try{n = Math.trunc(n) || 0;
		if (n < 0) n += this.length;
		if (n < 0 || n >= this.length) return undefined;
		return this[n];
		}catch(e){console.log(e)}
	}

	var TypedArray = Reflect.getPrototypeOf(Int8Array);
	var arr = [Array, String, TypedArray];
	for (var i=0;i<3;i++) {
		var C = arr[i];
		Object.defineProperty(C.prototype, "at",
							{ value: at,
								writable: true,
								enumerable: false,
								configurable: true });
	}
}

if(!window.structuredClone){
    window.structuredClone = function structuredClone(e) {
          return JSON.parse(JSON.stringify(e));
	}
}