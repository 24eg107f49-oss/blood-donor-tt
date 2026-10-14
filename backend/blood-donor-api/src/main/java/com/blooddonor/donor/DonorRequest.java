package com.blooddonor.donor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record DonorRequest(
        @NotBlank @Size(max = 100) String fullName,
        @NotNull @Min(18) @Max(100) Integer age,
        @NotBlank @Pattern(regexp = "A\\+|A-|B\\+|B-|AB\\+|AB-|O\\+|O-", message = "Invalid blood group")
        String bloodGroup,
        @NotBlank @Pattern(regexp = "^[+0-9() .-]{7,20}$", message = "Invalid phone number")
        String phone,
        @NotBlank @Size(max = 100) String city,
        @PastOrPresent LocalDate lastDonationDate,
        Boolean available,
        Boolean publiclyListed) {
}