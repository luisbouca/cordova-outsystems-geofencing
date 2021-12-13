/********* Geofencing.m Cordova Plugin Implementation *******/

import Foundation
import CoreLocation

@objc(Geofencing) class Geofencing:CDVPlugin, CLLocationManagerDelegate{
    
    
    var appdelegateInstance: AppDelegate? = nil
    var locationManager = CLLocationManager()
    var callback:String = ""
    
    override func pluginInitialize() {
        appdelegateInstance = UIApplication.shared.delegate as? AppDelegate
        locationManager = appdelegateInstance!.locationManager
    }
    
    @objc(setup:) func setup(command:CDVInvokedUrlCommand) {
        let defaults = UserDefaults.standard
        defaults.set(command.argument(at: 0), forKey: "Url")
        defaults.set(command.argument(at: 1), forKey: "AppId")
        defaults.set(command.argument(at: 2), forKey: "Key")
        self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus.ok), callbackId: command.callbackId)
    }
    @objc(checkPermissions:)func checkPermissions(command:CDVInvokedUrlCommand) {
        if #available(iOS 14.0, *) {
            switch locationManager.authorizationStatus{
            case .authorizedAlways:
                self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus.ok, messageAs: true), callbackId: callback)
            case .notDetermined, .restricted, .denied, .authorizedWhenInUse:
                self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus.ok, messageAs: false), callbackId: callback)
            @unknown default:
                self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus.error, messageAs: "Location permission Status unkown"), callbackId: callback)
            }
        } else {
            self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus.ok, messageAs: CLLocationManager.locationServicesEnabled()), callbackId: callback)
            
        }
    }
    @objc(registerFence:)func registerFence(command:CDVInvokedUrlCommand) {
        if CLLocationManager.isMonitoringAvailable(for: CLCircularRegion.self) {
            // Register the region.
            
            let maxDistance = locationManager.maximumRegionMonitoringDistance
            let latitude = CLLocationDegrees(command.argument(at: 0) as! Double)
            let longitude = CLLocationDegrees(command.argument(at: 1) as! Double)
            let center:CLLocationCoordinate2D = CLLocationCoordinate2D.init(latitude: latitude, longitude: longitude);
            var radius = CLLocationDistance(command.argument(at: 2) as! Float);
            let tag = command.argument(at: 4) as! String;
            let masterPolicyNumber = command.argument(at: 5) as! String;
            if radius > maxDistance {
                radius = maxDistance
            }
            let region = CLCircularRegion(center: center,
                                          radius: radius, identifier:tag )
            
            region.notifyOnEntry = true
            region.notifyOnExit = true
            let defaults = UserDefaults.standard
            defaults.set(masterPolicyNumber, forKey: tag)
            locationManager.startMonitoring(for: region)
            self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus.ok), callbackId: command.callbackId)
            return;
        }
        self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus.error, messageAs: "Monitoring is not available!"), callbackId: command.callbackId)
    }
    @objc(removeFences:)func removeFences(command:CDVInvokedUrlCommand) {
        let monitoredRegions = locationManager.monitoredRegions
        monitoredRegions.forEach { (region) in
            locationManager.stopMonitoring(for: region)
        }
    }
    @objc(requestPermission:)func requestPermission(command:CDVInvokedUrlCommand) {
        if !CLLocationManager.locationServicesEnabled() {
            self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus.error, messageAs: "Location Services Disabled!"), callbackId: command.callbackId)
            return
        }
        locationManager.requestAlwaysAuthorization()
        self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus.ok, messageAs:true), callbackId: command.callbackId)
    }
    
}
