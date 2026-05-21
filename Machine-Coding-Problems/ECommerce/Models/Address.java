package ECommerce.Models;

public class Address{
    private String AddressId;
    private String addressLine1;
    private String addressLine2;
    private String addressLine3;
    private String pincode;

    public Address(String id, String addressLine1, String addressLine2, String addressLine3, String pincode) {
        this.AddressId = id;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.addressLine3 = addressLine3;
        this.pincode = pincode;
    }

    public String getId() {
        return AddressId;
    }

    public void setId(String id) {
        this.AddressId = id;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getAddressLine3() {
        return addressLine3;
    }

    public void setAddressLine3(String addressLine3) {
        this.addressLine3 = addressLine3;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }
}