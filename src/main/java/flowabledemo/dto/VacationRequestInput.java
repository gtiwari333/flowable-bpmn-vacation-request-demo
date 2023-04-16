package flowabledemo.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class VacationRequestInput {
    String employeeName;
    int numberOfDays;
    String vacationPurpose;
}
