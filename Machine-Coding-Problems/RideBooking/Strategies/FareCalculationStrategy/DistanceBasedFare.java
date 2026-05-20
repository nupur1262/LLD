package Strategies.FareCalculationStrategy;

import Models.Product;
import Models.Trip;

public class DistanceBasedFare implements FareCalculationStrategy {

    @Override
    public Double calculateFare(Product product, Trip trip) {
        double distanceInKm = trip.getSrc().distanceTo(trip.getDest());
        
        // Base fare + (actual distance * per km rate)
        return product.getBaseFare() + (distanceInKm * product.getPerKmRate());
    }

}
