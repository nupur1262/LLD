package Services;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import Enums.ProductType;
import Enums.RideStatus;
import Models.Driver;
import Models.Location;
import Models.Product;
import Models.Rider;
import Models.Trip;
import Strategies.DriverMatchingStrategy.DriverMatchingStrategy;
import Strategies.FareCalculationStrategy.FareCalculationStrategy;

public class RideManager {
    private List<Trip> activeTrips = new ArrayList<>();
    
    private OnBoardingManager onBoardingManager;
    private DriverMatchingStrategy driverMatchingStrategy;
    private FareCalculationStrategy fareCalculationStrategy;

    public RideManager(OnBoardingManager onBoardingManager, 
                       DriverMatchingStrategy driverMatchingStrategy, 
                       FareCalculationStrategy fareCalculationStrategy) {
        this.onBoardingManager = onBoardingManager;
        this.driverMatchingStrategy = driverMatchingStrategy;
        this.fareCalculationStrategy = fareCalculationStrategy;
    }

    public Trip requestRide(Rider rider, Location src, Location dest, ProductType type) {
        Trip trip = new Trip(rider, src, dest, type);
        trip.setStatus(RideStatus.REQUESTED);
        
        List<Driver> availableDrivers = onBoardingManager.getAvailableDrivers(type);
        List<Driver> matchedDrivers = driverMatchingStrategy.getDrivers(rider, type, availableDrivers);
        
        if (matchedDrivers.isEmpty()) {
            throw new RuntimeException("No drivers available for this ride at the moment.");
        }
        
        // Storing the trip. We could auto-assign here, or let the Drivers selectively accept.
        activeTrips.add(trip);
        return trip;
    }

    public void acceptRide(Trip trip, Driver driver) {
        if (trip.getStatus() != RideStatus.REQUESTED) {
            throw new IllegalStateException("Trip is not in REQUESTED state, cannot accept!");
        }
        
        trip.setDriver(driver);
        trip.setStatus(RideStatus.ACCEPTED); 
        trip.setOtp(generateOtp());
        
        // Critical Constraints: Lock the driver and set active trips
        driver.setAvailable(false);
        driver.setActiveTrip(trip);
        trip.getRider().setActiveTrip(trip);
    }

    public void startRide(Trip trip, Integer otp) {
        if (trip.getStatus() != RideStatus.ACCEPTED) {
            throw new IllegalStateException("Trip is not accepted by a driver yet!");
        }
        if (!trip.getOtp().equals(otp)) {
            throw new IllegalArgumentException("Invalid OTP! Cannot start ride.");
        }
        
        trip.setStatus(RideStatus.IN_PROGRESS);
    }

    public void endRide(Trip trip) {
        if (trip.getStatus() != RideStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot end a trip that is not currently in progress.");
        }
        
        trip.setStatus(RideStatus.COMPLETED);
        
        // Ensure product pricing metadata exists (mocking it if null for the interview demo)
        Product product = trip.getProduct();
        if (product == null) {
             // Mock BaseFare: 50, PerKmRate: 12, PerMinRate: 2
             product = new Product("GO", trip.getProductType(), 50.0, 12.0, 2.0);
             trip.setProduct(product);
        }
        
        // Calculate the Final Fare utilizing the pluggable strategy!
        Double finalFare = fareCalculationStrategy.calculateFare(product, trip);
        trip.setFare(finalFare);
        
        // Critical Constraints: Free up the driver
        Driver driver = trip.getDriver();
        if (driver != null) {
            driver.setAvailable(true);
            driver.setActiveTrip(null);
        }
        trip.getRider().setActiveTrip(null);
    }

    private Integer generateOtp() {
        Random random = new Random();
        return random.nextInt(9000) + 1000;
    }
}