package flowabledemo.data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class VacationRequestEntity {
    @Id
    private Long id;

    private String reason;
    private int numberOfDays;
    private boolean approved;
    private Long employeeId;
    private Long reviewedByEmployeeId; //manager
}
