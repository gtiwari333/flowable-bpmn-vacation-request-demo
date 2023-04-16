package flowabledemo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VacationUpdateRequest {
    int numberOfDays;
    String vacationPurpose;
    boolean shouldAppeal;
}
