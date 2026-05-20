package Strategies.FareCalculationStrategy;

import Models.Product;
import Models.Trip;

public class DistanceAndTimeFare implements FareCalculationStrategy {

    @Override
    public Double calculateFare(Product product, Trip trip) {
        double distanceInKm = trip.getSrc().distanceTo(trip.getDest());
        
        // As we don't have a live routing engine, estimate time (e.g., 2 minutes per kilometer average speed bias)
        double estimatedTimeMinutes = distanceInKm * 2.0;
        
        return product.getBaseFare() 
             + (distanceInKm * product.getPerKmRate()) 
             + (estimatedTimeMinutes * product.getPerMinRate());
    }

}
