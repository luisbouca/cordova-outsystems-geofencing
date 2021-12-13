var exec = require('cordova/exec');

exports.registerFence = function (success, error,latitude,longitude,radius,duration,id,masterPolicyNumber) {
    exec(success, error, 'Geofencing', 'registerFence', [latitude,longitude,radius,duration,id,masterPolicyNumber]);
};

exports.removeFences = function (success, error) {
    exec(success, error, 'Geofencing', 'removeFences', []);
};

exports.requestPermission = function (success, error) {
    exec(success, error, 'Geofencing', 'requestPermission', []);
};

exports.checkPermission = function (success, error) {
    exec(success, error, 'Geofencing', 'checkPermission', []);
};
exports.setup = function (success, error,url,appid,key) {
    exec(success, error, 'Geofencing', 'setup', [url,appid,key]);
};
