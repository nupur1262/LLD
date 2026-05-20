import Enums.ProductType;
import Models.Driver;
import Models.Location;
import Models.Rider;
import Models.Trip;
import Models.Vehicle;
import Services.OnBoardingManager;
import Services.RideManager;
import Strategies.DriverMatchingStrategy.ProximityBasedMatching;
import Strategies.FareCalculationStrategy.DistanceBasedFare;

public class Main {
    public static void main(String[] args) {
        try {
            System.out.println("====== Starting Ride Booking System ======\n");

            // 1. Initialize Managers & Default Strategies
            OnBoardingManager onBoardingManager = new OnBoardingManager();
            RideManager rideManager = new RideManager(onBoardingManager, new ProximityBasedMatching(), new DistanceBasedFare());

            // 2. Register Rider
            Location riderLoc = new Location(12.9716, 77.5946); 
            Rider rider = new Rider("R1", "Alice", riderLoc, "9999999999");
            onBoardingManager.registerRider(rider);
            System.out.println("Rider 'Alice' registered at location: " + riderLoc.getLatitude() + ", " + riderLoc.getLongitude());

            // 3. Register Driver
            Location driverLoc = new Location(12.9720, 77.5950);
            Vehicle vehicle = new Vehicle("V1", "KA-01-1234", ProductType.GO, 4);
            Driver driver = new Driver("D1", "Bob", "8888888888", vehicle);
            driver.setLocation(driverLoc); 
            driver.setAvailable(true);     
            onBoardingManager.registerDriver(driver);
            System.out.println("Driver 'Bob' registered with Vehicle: " + vehicle.getNumberplate());

            System.out.println("\n--- Rider Requests a Ride ---");
            Location destination = new Location(12.9279, 77.6271);
            
            // Rider Alice wants to book a 'GO' ride
            System.out.println("Alice is requesting a 'GO' ride...");
            Trip ride = rideManager.requestRide(rider, riderLoc, destination, ProductType.GO);
            System.out.println("Ride Requested! Current Trip Status: " + ride.getStatus());

            System.out.println("\n--- Driver Accepts the Ride ---");
            // Simulating the scenario where the system pings Driver Bob and he accepts
            rideManager.acceptRide(ride, driver);
            System.out.println("Driver Bob accepted the ride.");
            System.out.println("Trip Status: " + ride.getStatus());
            System.out.println("Driver 'Bob' Availability: " + driver.getAvailable());
            System.out.println("Generated OTP for the trip is: " + ride.getOtp());

            System.out.println("\n--- Driver Arrives & Starts Ride ---");
            // Rider Alice provides the OTP to Bob
            System.out.println("Verifying OTP...");
            rideManager.startRide(ride, ride.getOtp());
            System.out.println("Trip Status: " + ride.getStatus());

            System.out.println("\n--- Driver Reaches Destination ---");
            rideManager.endRide(ride);
            System.out.println("Trip Status: " + ride.getStatus());
            
            // Fare details
            System.out.println("Total Fare Calculated: \u20B9" + String.format("%.2f", ride.getFare()));
            
            // Verifying Driver is free again
            System.out.println("Driver 'Bob' Availability: " + driver.getAvailable());

            System.out.println("\n====== Ride Booking Completed Successfully ======");
        } catch (Exception e) {
            System.out.println("\n[ERROR] An exception occurred!");
            e.printStackTrace(System.out);
        }
    }
}
