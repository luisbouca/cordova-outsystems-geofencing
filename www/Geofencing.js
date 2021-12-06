var exec = require('cordova/exec');

exports.registerFence = function (success, error,latitude,longitude,radius,duration,id) {
    exec(success, error, 'Geofencing', 'registerFence', []);
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
