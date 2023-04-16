package flowabledemo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VacationProcessResult {
    boolean  vacationApproved;
    String denialReason;
}
