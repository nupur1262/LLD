package ECommerce.Models;

public class Product 
{
    private String productId;
    private String name;
    private Double price;
    private Boolean isAvailable;
    private Integer inStock;

    public Product(String id, String name, Double price, Boolean isAvailable, Integer inStock) {
        this.productId = id;
        this.name = name;
        this.price = price;
        this.isAvailable = isAvailable;
        this.inStock = inStock;
    }

    public String getId() {
        return productId;
    }

    public void setId(String id) {
        this.productId = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Boolean getAvailable() {
        return isAvailable;
    }

    public void setAvailable(Boolean available) {
        isAvailable = available;
    }

    public Integer getInStock() {
        return inStock;
    }

    public void setInStock(Integer inStock) {
        this.inStock = inStock;
    }
}
