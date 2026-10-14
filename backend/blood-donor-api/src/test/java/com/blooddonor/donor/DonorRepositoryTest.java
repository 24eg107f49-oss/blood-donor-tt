package com.blooddonor.donor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:donor-repository;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DonorRepositoryTest {
    @Autowired
    private DonorRepository repository;

    @Test
    void publicSearchFiltersOutPrivateProfilesAndMatchesGroupAndCity() {
        Donor listed = donor("Listed donor", "O+", "Hyderabad", true);
        Donor privateDonor = donor("Private donor", "O+", "Hyderabad", false);
        Donor otherCity = donor("Other city", "O+", "Chennai", true);
        repository.saveAll(java.util.List.of(listed, privateDonor, otherCity));

        assertThat(repository.findByPubliclyListedTrueOrderByIdDesc())
                .containsExactly(otherCity, listed);
        assertThat(repository.findByPubliclyListedTrueAndBloodGroupIgnoreCaseAndCityContainingIgnoreCaseOrderByIdDesc(
                "o+", "HYD"))
                .containsExactly(listed);
    }

    private Donor donor(String name, String group, String city, boolean publiclyListed) {
        Donor donor = new Donor();
        donor.setFullName(name);
        donor.setAge(30);
        donor.setBloodGroup(group);
        donor.setPhone("+1 555 010 1000");
        donor.setCity(city);
        donor.setAvailable(true);
        donor.setPubliclyListed(publiclyListed);
        return donor;
    }
}