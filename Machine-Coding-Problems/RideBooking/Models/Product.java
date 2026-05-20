package Models;

import Enums.ProductType;

public class Product {
    private String id;
    private ProductType type;
    private Double baseFare;
    private Double perKmRate;
    private Double perMinRate;

    public Product(String id, ProductType type, Double baseFare, Double perKmRate, Double perMinRate) {
        this.id = id;
        this.type = type;
        this.baseFare = baseFare;
        this.perKmRate = perKmRate;
        this.perMinRate = perMinRate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ProductType getType() {
        return type;
    }

    public void setType(ProductType type) {
        this.type = type;
    }

    public Double getBaseFare() {
        return baseFare;
    }

    public void setBaseFare(Double baseFare) {
        this.baseFare = baseFare;
    }

    public Double getPerKmRate() {
        return perKmRate;
    }

    public void setPerKmRate(Double perKmRate) {
        this.perKmRate = perKmRate;
    }

    public Double getPerMinRate() {
        return perMinRate;
    }

    public void setPerMinRate(Double perMinRate) {
        this.perMinRate = perMinRate;
    }
}
