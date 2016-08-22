'use strict';

loadAsset("assert.js");

assertThrows(function() {
  return arguments.caller;
}, TypeError);

assertThrows(function() {
  return arguments.callee;
}, TypeError);

"success";
