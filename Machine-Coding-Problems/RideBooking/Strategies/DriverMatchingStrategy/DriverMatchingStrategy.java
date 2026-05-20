package Strategies.DriverMatchingStrategy;

import java.util.List;

import Enums.ProductType;
import Models.Driver;
import Models.Rider;

public interface DriverMatchingStrategy {

    public List<Driver> getDrivers(Rider rider, ProductType type, List<Driver>availableDrivers);
}
