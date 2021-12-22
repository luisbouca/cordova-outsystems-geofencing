//
//  AppDelegate+Geofencing.m
//  test
//
//  Created by Luis Bouça on 13/12/2021.
//

#import "AppDelegate+Geofencing.h"
#import <objc/runtime.h>

@implementation AppDelegate (CDVGeofencing)

- (NSString *)locationManager{
    return objc_getAssociatedObject(self, @selector(locationManager));
}

- (void)setLocationManager:(CLLocationManager *)locationManager{
    objc_setAssociatedObject(self, @selector(locationManager), locationManager, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (BOOL)xxx_application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions{
    self.locationManager = [[CLLocationManager alloc] init];
    self.locationManager.delegate = self;
    self.locationManager.desiredAccuracy = kCLLocationAccuracyHundredMeters;
    self.locationManager.allowsBackgroundLocationUpdates = true;
    [self.locationManager startUpdatingLocation];
    return [self xxx_application:application didFinishLaunchingWithOptions:launchOptions];
        
}

+ (void)load {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        
        Class class = [self class];

        SEL originalSelector = @selector(application:didFinishLaunchingWithOptions:);
        SEL swizzledSelector = @selector(xxx_application:didFinishLaunchingWithOptions:);
        
        Method originalMethod = class_getInstanceMethod(class, originalSelector);
        Method swizzledMethod = class_getInstanceMethod(class, swizzledSelector);
        
        BOOL didAddMethod = class_addMethod(class, originalSelector, method_getImplementation(swizzledMethod), method_getTypeEncoding(swizzledMethod));
        
        if (didAddMethod) {
            class_replaceMethod(class, swizzledSelector, method_getImplementation(originalMethod), method_getTypeEncoding(originalMethod));
        } else {
            method_exchangeImplementations(originalMethod, swizzledMethod);
        }
        
    });
}

// called when user Exits a monitored region
- (void)locationManager:(CLLocationManager *)manager didExitRegion:(CLRegion *)region {
    if ([region isKindOfClass:CLCircularRegion.class]) {
        [self handleEvent:region withType:1 withLocation:manager.location];
    }
}
    
    // called when user Enters a monitored region
- (void)locationManager:(CLLocationManager *)manager didEnterRegion:(CLRegion *)region{
    if ([region isKindOfClass:CLCircularRegion.class]) {
        [self handleEvent:region withType:0 withLocation:manager.location];
    }
}

-(void) handleEvent:(CLRegion*)region withType:(NSInteger)type withLocation:(CLLocation*)location{
    
    NSLog(@"Fence Detected!");
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    NSString* identifier = region.identifier;
    NSString* policyNumber = [defaults stringForKey:identifier];;
    NSDateFormatter *dateFormatter=[[NSDateFormatter alloc] init];
    [dateFormatter setDateFormat:@"yyyy-MM-dd HH:mm:ss"];
    NSString* currDate =[dateFormatter stringFromDate:[NSDate date]];
    currDate = [currDate stringByReplacingOccurrencesOfString:@" " withString:@"T"];
    NSString* latitude = [NSString stringWithFormat:@"%.12lf",location.coordinate.latitude];
    NSString* longitude = [NSString stringWithFormat:@"%.12lf",location.coordinate.longitude];
    if (type == 1){
        identifier = @"Fora de fence";
    }
    NSDictionary *newGeofence = @{@"MasterPolicyNumber":policyNumber,
                                  @"Datetime":currDate,
                                  @"FenceAction":@(type),
                                  @"Tag":identifier,
                                  @"Latitude":latitude,
                                  @"Longitude":longitude};
    NSArray *unmutablequeue = [defaults arrayForKey:@"GeofencingQueue"];
    NSMutableArray *queue;
    if (unmutablequeue != nil) {
        queue = [[NSMutableArray alloc]initWithArray:unmutablequeue];
    }else{
        queue = [[NSMutableArray alloc] init];
    }
    
    [queue addObject:newGeofence];
    
    NSError *error;
    NSData *postData = [NSJSONSerialization dataWithJSONObject:queue options:0 error:&error];
    if (error != nil) {
        [defaults setValue:queue forKey:@"GeofencingQueue"];
        return;
    }
    
    NSMutableURLRequest *request = [[NSMutableURLRequest alloc] init];
        [request setHTTPMethod:@"POST"];
        [request setHTTPBody:postData];
        [request setValue:[defaults stringForKey:@"AppId"] forHTTPHeaderField:@"X-Contacts-AppId"];
        [request setValue:[defaults stringForKey:@"Key"] forHTTPHeaderField:@"X-Contacts-Key"];
        [request setValue:@"application/json" forHTTPHeaderField:@"Content-Type"];
        [request setURL:[NSURL URLWithString:[defaults stringForKey:@"Url"]]];
    
    NSLog(@"Fence Sent!");
    [[[NSURLSession sharedSession] dataTaskWithRequest:request completionHandler:
      ^(NSData * _Nullable data,
        NSURLResponse * _Nullable response,
        NSError * _Nullable error) {
        
        if (error != nil) {
            
            NSLog(@"Fence Error!");
            [defaults setValue:queue forKey:@"GeofencingQueue"];
            return;
        }
        
        NSInteger statusCode = [(NSHTTPURLResponse *)response statusCode];
        NSString *fencemsg =@"Fence Sent status code:";
        NSLog(@"%@", [fencemsg stringByAppendingString:[@(statusCode) stringValue] ]);
        if (statusCode != 200) {
            [defaults setValue:queue forKey:@"GeofencingQueue"];
        }else{
            [defaults setValue:[[NSArray alloc]init] forKey:@"GeofencingQueue"];
        }
        
    }] resume];
}

- (void)locationManager:(CLLocationManager *)manager didUpdateLocations:(NSArray<CLLocation *> *)locations{
    NSLog(@"Location Updated!");
}

- (void)locationManager:(CLLocationManager *)manager didFailWithError:(NSError *)error{
    NSString* msg =@"There was an error getting Location!\n";
    msg = [msg stringByAppendingString:error.localizedDescription];
    NSLog(@"%@", msg);
    
}

- (void)locationManager:(CLLocationManager *)manager monitoringDidFailForRegion:(CLRegion *)region withError:(NSError *)error{
    NSString* msg =@"There was an error monitoring Location!\n";
    msg = [msg stringByAppendingString:error.localizedDescription];
    NSLog(@"%@", msg);
}

- (void)locationManagerDidChangeAuthorization:(CLLocationManager *)manager{
    if (@available(iOS 14.0, *)) {
        switch (manager.authorizationStatus) {
            case kCLAuthorizationStatusAuthorizedAlways:{
                NSLog(@"Always Permission Granted!");
                break;
            }
            case kCLAuthorizationStatusAuthorizedWhenInUse:{
                NSLog(@"When in use Permission Granted!");
                break;
            }
            default:{
                NSLog(@"Permission Not Granted!");
                break;
            }
        }
    } else {
        if (CLLocationManager.locationServicesEnabled) {
            NSLog(@"Permission Granted!");
        }
    }
}

@end
