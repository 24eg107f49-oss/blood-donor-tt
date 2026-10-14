package com.blooddonor.donor;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

@Entity
@Table(name = "donors")
public class Donor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Size(max = 100)
    private String fullName;

    @NotNull @Min(18) @Max(100)
    private Integer age;

    @NotBlank
    @Pattern(regexp = "A\\+|A-|B\\+|B-|AB\\+|AB-|O\\+|O-", message = "Invalid blood group")
    private String bloodGroup;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank @Pattern(regexp = "^[+0-9() .-]{7,20}$", message = "Invalid phone number")
    private String phone;

    @NotBlank @Size(max = 100)
    private String city;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @PastOrPresent
    private LocalDate lastDonationDate;
    private boolean available = true;
    private boolean publiclyListed;

    @SuppressWarnings("java:S1186")
    public Donor() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public LocalDate getLastDonationDate() { return lastDonationDate; }
    public void setLastDonationDate(LocalDate lastDonationDate) { this.lastDonationDate = lastDonationDate; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public boolean isPubliclyListed() { return publiclyListed; }
    public void setPubliclyListed(boolean publiclyListed) { this.publiclyListed = publiclyListed; }
}
