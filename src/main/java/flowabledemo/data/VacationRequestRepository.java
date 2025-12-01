package flowabledemo.data;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VacationRequestRepository extends JpaRepository<VacationRequestEntity, Long> {
}
