package Strategies.DriverMatchingStrategy;

import java.util.List;

import Enums.ProductType;
import Models.Driver;
import Models.Rider;

public class DemandSurgeMatching implements DriverMatchingStrategy {

    @Override
    public List<Driver> getDrivers(Rider rider, ProductType type, List<Driver> availableDrivers) {
        // In a demand surge, we strictly match drivers that are very close (e.g., within 2km)
        // and severely limit the supply to the top 2 closest ones to create artificial scarcity effect
        double maxSurgeRadiusKm = 2.0;

        return availableDrivers.stream()
                .filter(d -> d.getLocation().distanceTo(rider.getLocation()) <= maxSurgeRadiusKm)
                .sorted((d1, d2) -> {
                    double dist1 = d1.getLocation().distanceTo(rider.getLocation());
                    double dist2 = d2.getLocation().distanceTo(rider.getLocation());
                    return Double.compare(dist1, dist2);
                })
                .limit(2)
                .collect(java.util.stream.Collectors.toList());
    }

}
