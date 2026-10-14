package com.blooddonor.donor;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@RestController
@RequestMapping("/api/donors")
public class DonorController {
    private final DonorRepository repository;
    private final String adminApiKey;

    public DonorController(DonorRepository repository,
                           @Value("${app.admin-api-key:}") String adminApiKey) {
        this.repository = repository;
        this.adminApiKey = adminApiKey;
    }

    @GetMapping
    public List<Donor> getDonors(
            @RequestParam(required = false) String bloodGroup,
            @RequestParam(required = false) String city) {
        boolean hasGroup = bloodGroup != null && !bloodGroup.isBlank();
        boolean hasCity = city != null && !city.isBlank();
        if (hasGroup && hasCity) {
            return repository.findByPubliclyListedTrueAndBloodGroupIgnoreCaseAndCityContainingIgnoreCaseOrderByIdDesc(
                bloodGroup, city);
        }
        if (hasGroup) return repository.findByPubliclyListedTrueAndBloodGroupIgnoreCaseOrderByIdDesc(bloodGroup);
        if (hasCity) return repository.findByPubliclyListedTrueAndCityContainingIgnoreCaseOrderByIdDesc(city);
        return repository.findByPubliclyListedTrueOrderByIdDesc();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Donor createDonor(@Valid @RequestBody DonorRequest request) {
        return repository.save(toDonor(request, new Donor()));
    }

    @GetMapping("/{id}")
    public Donor getDonor(@PathVariable Long id) {
        return repository.findById(id)
            .filter(Donor::isPubliclyListed)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Donor not found"));
    }

    @PutMapping("/{id}")
    public Donor updateDonor(@PathVariable Long id,
                             @RequestHeader(value = "X-Admin-Key", required = false) String suppliedKey,
                     @Valid @RequestBody DonorRequest request) {
        requireAdmin(suppliedKey);
        Donor donor = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Donor not found"));
        return repository.save(toDonor(request, donor));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDonor(@PathVariable Long id,
                            @RequestHeader(value = "X-Admin-Key", required = false) String suppliedKey) {
        requireAdmin(suppliedKey);
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Donor not found");
        }
        repository.deleteById(id);
    }

    private void requireAdmin(String suppliedKey) {
        if (adminApiKey.isBlank() || suppliedKey == null
                || !MessageDigest.isEqual(adminApiKey.getBytes(StandardCharsets.UTF_8),
                                          suppliedKey.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin API key is invalid");
        }
    }

    private Donor toDonor(DonorRequest request, Donor donor) {
        donor.setFullName(request.fullName().trim());
        donor.setAge(request.age());
        donor.setBloodGroup(request.bloodGroup());
        donor.setPhone(request.phone().trim());
        donor.setCity(request.city().trim());
        donor.setLastDonationDate(request.lastDonationDate());
        donor.setAvailable(request.available() == null || request.available());
        donor.setPubliclyListed(Boolean.TRUE.equals(request.publiclyListed()));
        return donor;
    }
}
