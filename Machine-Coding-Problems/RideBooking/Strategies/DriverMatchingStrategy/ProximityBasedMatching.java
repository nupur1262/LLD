package Strategies.DriverMatchingStrategy;

import java.util.List;

import Enums.ProductType;
import Models.Driver;
import Models.Rider;

public class ProximityBasedMatching implements DriverMatchingStrategy {

    @Override
    public List<Driver> getDrivers(Rider rider, ProductType type, List<Driver> availableDrivers) {
        // Sort available drivers by their distance to the rider's location, then return closest top 5
        return availableDrivers.stream()
                .sorted((d1, d2) -> {
                    double dist1 = d1.getLocation().distanceTo(rider.getLocation());
                    double dist2 = d2.getLocation().distanceTo(rider.getLocation());
                    return Double.compare(dist1, dist2);
                })
                .limit(5)
                .collect(java.util.stream.Collectors.toList());
    }

}
