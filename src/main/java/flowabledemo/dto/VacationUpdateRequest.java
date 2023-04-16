package flowabledemo.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class VacationUpdateRequest {
    int numberOfDays;
    String vacationPurpose;
    boolean shouldAppeal;
}
