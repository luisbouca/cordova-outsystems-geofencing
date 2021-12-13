//
//  AppDelegate+Geofencing.h
//  test
//
//  Created by Luis Bouça on 13/12/2021.
//

#ifndef AppDelegate_Geofencing_h
#define AppDelegate_Geofencing_h

#import "AppDelegate.h"
#import <CoreLocation/CoreLocation.h>

@interface AppDelegate (CDVGeofencing) <CLLocationManagerDelegate>

@property() CLLocationManager* locationManager;

- (BOOL) xxx_application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions;

@end


#endif /* AppDelegate_Geofencing_h */
