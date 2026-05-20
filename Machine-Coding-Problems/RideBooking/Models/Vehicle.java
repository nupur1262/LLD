package Models;

import Enums.ProductType;

public class Vehicle {
    private String id;
    private String numberplate;
    private ProductType productType;
    private Integer capacity;

    public Vehicle(String id, String numberplate, ProductType productType, Integer capacity) {
        this.id = id;
        this.numberplate = numberplate;
        this.productType = productType;
        this.capacity = capacity;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNumberplate() {
        return numberplate;
    }

    public void setNumberplate(String numberplate) {
        this.numberplate = numberplate;
    }

    public ProductType getProductType() {
        return productType;
    }

    public void setProductType(ProductType productType) {
        this.productType = productType;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }
}
