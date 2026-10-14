package com.blooddonor.donor;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DonorRepository extends JpaRepository<Donor, Long> {
    List<Donor> findByPubliclyListedTrueOrderByIdDesc();
    List<Donor> findByPubliclyListedTrueAndBloodGroupIgnoreCaseOrderByIdDesc(String bloodGroup);
    List<Donor> findByPubliclyListedTrueAndCityContainingIgnoreCaseOrderByIdDesc(String city);
    List<Donor> findByPubliclyListedTrueAndBloodGroupIgnoreCaseAndCityContainingIgnoreCaseOrderByIdDesc(
            String bloodGroup, String city);
    List<Donor> findByOwnerEmailIgnoreCaseOrderByIdDesc(String ownerEmail);
    List<Donor> findAllByOrderByIdDesc();
}
