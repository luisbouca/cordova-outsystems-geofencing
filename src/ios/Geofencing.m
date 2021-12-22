//
//  Geofencing.m
//  test
//
//  Created by Luis Bouça on 16/12/2021.
//

#import "Geofencing.h"

@implementation Geofencing

- (void)pluginInitialize{
    [self updateLocationManager];
}

-(void) updateLocationManager{
    _appdelegateInstance = (AppDelegate *)[[UIApplication sharedApplication] delegate];
    _locationManager = self.appdelegateInstance.locationManager;
}

-(void) setup:(CDVInvokedUrlCommand*) command{
    NSUserDefaults *defaults = NSUserDefaults.standardUserDefaults;
    [defaults setValue:[command argumentAtIndex:0] forKey:@"Url"];
    [defaults setValue:[command argumentAtIndex:1] forKey:@"AppId"];
    [defaults setValue:[command argumentAtIndex:2] forKey:@"Key"];
    [defaults setBool:[[command argumentAtIndex:3] boolValue] forKey:@"isDebug"];
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    
}
-(void) checkPermissions:(CDVInvokedUrlCommand*) command{
    [self updateLocationManager];
    if (@available(iOS 14.0, *)) {
        switch ([self.locationManager authorizationStatus]) {
            case kCLAuthorizationStatusAuthorizedAlways:{
                CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:TRUE];
                [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
                break;
            }
            default:{
                CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:FALSE];
                [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
                break;
            }
        }
    } else {
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:[CLLocationManager locationServicesEnabled]];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }
}
-(void) registerFence:(CDVInvokedUrlCommand*) command{
    [self updateLocationManager];
    if ([CLLocationManager isMonitoringAvailableForClass:CLCircularRegion.class]) {
        CLLocationDistance maxDistance = self.locationManager.maximumRegionMonitoringDistance;
        
        CLLocationDegrees latitude = [[command argumentAtIndex:0] doubleValue];
        CLLocationDegrees longitude = [[command argumentAtIndex:1] doubleValue];
        CLLocationDistance radius = [[command argumentAtIndex:2] doubleValue];
        if (radius>maxDistance) {
            radius = maxDistance;
        }
        
        NSString * tag = [command argumentAtIndex:4];
        NSString * masterPolicyNumber = [command argumentAtIndex:5];
        
        CLLocationCoordinate2D center = CLLocationCoordinate2DMake(latitude, longitude);
        
        CLCircularRegion *region = [[CLCircularRegion alloc] initWithCenter:center radius:radius identifier:tag];
        
        region.notifyOnEntry = true;
        region.notifyOnExit = true;
        
        NSUserDefaults *defaults = NSUserDefaults.standardUserDefaults;
        [defaults setValue:masterPolicyNumber forKey:tag];
        
        [self.locationManager startMonitoringForRegion:region];
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }else{
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Monitoring is not available!"];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }
}
-(void) removeFences:(CDVInvokedUrlCommand*) command{
    NSSet<CLRegion *> *regions = self.locationManager.monitoredRegions;
    for(CLRegion* region in regions) {
        [self.locationManager stopMonitoringForRegion:region];
    }
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}
-(void) requestPermission:(CDVInvokedUrlCommand*) command{
    [self updateLocationManager];
    if (![CLLocationManager locationServicesEnabled]) {
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Location Services Disabled!"];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        return;
    }
    
    [self.locationManager requestAlwaysAuthorization];
    
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:TRUE];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}


@end
