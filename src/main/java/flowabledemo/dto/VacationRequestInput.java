package flowabledemo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VacationRequestInput {
    String employeeName;
    int numberOfDays;
    String vacationPurpose;
}
