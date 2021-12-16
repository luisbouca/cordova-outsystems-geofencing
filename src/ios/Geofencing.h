//
//  Geofencing.h
//  test
//
//  Created by Luis Bouça on 16/12/2021.
//

#ifndef Geofencing_h
#define Geofencing_h

#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>
#import <CoreLocation/CoreLocation.h>
#import "AppDelegate+Geofencing.h"

@interface Geofencing : CDVPlugin

@property AppDelegate* appdelegateInstance;
@property CLLocationManager* locationManager;
@property NSString* callback;

-(void) setup:(CDVInvokedUrlCommand*) command;
-(void) checkPermissions:(CDVInvokedUrlCommand*) command;
-(void) registerFence:(CDVInvokedUrlCommand*) command;
-(void) removeFences:(CDVInvokedUrlCommand*) command;
-(void) requestPermission:(CDVInvokedUrlCommand*) command;

@end

#endif /* Geofencing_h */
