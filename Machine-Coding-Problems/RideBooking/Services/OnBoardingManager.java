package Services;

import java.util.List;
import java.util.stream.Collectors;

import Enums.ProductType;
import Models.Rider;
import Models.Driver;

public class OnBoardingManager {
    private List<Rider> riders = new java.util.ArrayList<>();
    private List<Driver> drivers = new java.util.ArrayList<>();
    
    public void registerRider(Rider r)
    {
        riders.add(r);
    }

    public void registerDriver(Driver d)
    {
        drivers.add(d);
    }

    public List<Driver> getAvailableDrivers()
    {
       return drivers.stream().filter(d-> d.getAvailable()).collect(Collectors.toList());
    }
    
    public List<Driver> getAvailableDrivers(ProductType type)
    {
       return drivers.stream().filter(d-> d.getAvailable() && d.getVehicle().getProductType() == type).collect(Collectors.toList());
    }
}