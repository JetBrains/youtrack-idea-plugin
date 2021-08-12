/*
 Copyright 2017 JetBrains s.r.o.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

/**
 * Module containing a straightforward cache implementation.
 *
 * @module @jetbrains/youtrack-scripting-api/cache
 */

var Cache = function(size, name) {
    if (typeof size !== 'number' || size < 1) {
        throw 'Size should be positive number';
    }
    this.maxSize = size;
    this.name = name;
    this.clear();
};

Cache.prototype.clear = function() {
    this.head = null;
    this.map = {}; // stores {key: key, value: cached_value, prev:prev_stored_item, next: next_stored_item} under key
    this.size = 0;
};

Cache.prototype.get = function(key, creator) {
    if (!key) {
        throw 'Tried to retrieve a value by null key';
    }
    if (typeof key !== 'string') {
        throw 'Key is not a string';
    }
    var insertBefore = function(newElem, oldElem) {
        oldElem.prev.next = newElem;
        newElem.prev = oldElem.prev;
        oldElem.prev = newElem;
        newElem.next = oldElem;
        return newElem;
    };
    var found = this.map[key];
    if (found) {
        console.trace(this.name + ' cache hit, key: ' + key);
        if (this.head === found) {
            return found.value;
        }
        found.prev.next = found.next;
        found.next.prev = found.prev;
        this.head = insertBefore(found, this.head);
        return found.value;
    } else {
        console.trace(this.name + ' cache missed, key: ' + key);
        var created = creator(key); // seem to be ok to allow caching nulls
        switch (this.size) {
            case 0: // mind that there is a fall through
                this.head = {};
                this.head.next = this.head;
                this.head.prev = this.head;
                this.size++;
                break;
            case this.maxSize:
                console.trace('Removing key ' + this.head.key + ' from cache ' + this.name);
                this.head = this.head.prev;
                delete this.map[this.head.key];
                break;
            default:
                this.head = insertBefore({}, this.head);
                this.size++;
        }
        this.head.value = created;
        this.head.key = key;
        this.map[key] = this.head;
        return created;
    }
};

exports.create = function(size, name) {
    return new Cache(size, name);
};