package Strategies.FareCalculationStrategy;

import Models.Product;
import Models.Trip;

public interface FareCalculationStrategy 
{
    public Double calculateFare(Product product, Trip trip);
}
