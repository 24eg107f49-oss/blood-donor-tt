package com.blooddonor.donor;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/donors")
public class DonorController {
    private static final String DONOR_NOT_FOUND = "Donor not found";
    private final DonorRepository repository;

    public DonorController(DonorRepository repository) {
        this.repository = repository;
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

    @GetMapping("/mine")
    public List<Donor> getMyDonors(Authentication authentication) {
        return repository.findByOwnerEmailIgnoreCaseOrderByIdDesc(authentication.getName());
    }

    @GetMapping("/admin")
    public List<Donor> getAllDonorsForAdmin() {
        return repository.findAllByOrderByIdDesc();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Donor createDonor(@Valid @RequestBody DonorRequest request, Authentication authentication) {
        Donor donor = toDonor(request, new Donor());
        donor.setOwnerEmail(authentication.getName());
        return repository.save(donor);
    }

    @GetMapping("/{id}")
    public Donor getDonor(@PathVariable Long id, Authentication authentication) {
        Donor donor = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, DONOR_NOT_FOUND));
        if (!donor.isPubliclyListed() && !canManage(donor, authentication)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, DONOR_NOT_FOUND);
        }
        return donor;
    }

    @PutMapping("/{id}")
    public Donor updateDonor(@PathVariable Long id,
                     @Valid @RequestBody DonorRequest request,
                     Authentication authentication) {
        Donor donor = repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, DONOR_NOT_FOUND));
        requireCanManage(donor, authentication);
        return repository.save(toDonor(request, donor));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDonor(@PathVariable Long id, Authentication authentication) {
        Donor donor = repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, DONOR_NOT_FOUND));
        requireCanManage(donor, authentication);
        repository.delete(donor);
    }

    private boolean canManage(Donor donor, Authentication authentication) {
        return hasRole(authentication, "ROLE_ADMIN")
                || (donor.getOwnerEmail() != null
                && donor.getOwnerEmail().equalsIgnoreCase(authentication.getName()));
    }

    private void requireCanManage(Donor donor, Authentication authentication) {
        if (!canManage(donor, authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the donor or an admin can manage this profile");
        }
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
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
