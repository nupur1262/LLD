package ECommerce.Models;

import java.util.List;

public class User 
{
    private String UserId;
    private String name;
    private String email;
    private String contact;
    private List<Address> addresses;
    
    public User(String userId, String name, String email, String contact, List<Address> addresses) {
        UserId = userId;
        this.name = name;
        this.email = email;
        this.contact = contact;
        this.addresses = addresses;
    }

    public String getUserId() {
        return UserId;
    }

    public void setUserId(String userId) {
        UserId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }
}
