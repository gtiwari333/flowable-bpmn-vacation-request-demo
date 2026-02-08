package flowabledemo.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface VacationRequestRepository extends JpaRepository<VacationRequestEntity, Long> {
    int countByEmployeeIdAndVacationStartDateBetweenAndApproved(Long employeeId, LocalDate start, LocalDate end, boolean approved);
}
