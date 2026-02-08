package flowabledemo.vacation.systemtasks;

import flowabledemo.data.EmployeeRepository;
import flowabledemo.data.VacationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Year;

@Component
@Slf4j
@RequiredArgsConstructor
class VacationTools {

    final VacationRequestRepository vacationRequestRepository;
    final EmployeeRepository employeeRepository;

    @Tool(description = "Retrieves number of vacations taken by an employee in current year")
    public int numVacationsTakenByEmployee(Long employeeId) {
        return vacationRequestRepository.countByEmployeeIdAndVacationStartDateBetweenAndApproved(employeeId, LocalDate.of(Year.now().getValue(), 1, 1), LocalDate.of(Year.now().getValue(), 12, 31), true);
    }

    @Tool(description = "Retrieves number of vacations taken by an employee in current year")
    public int numVacationAllowed(Long employeeId) {
        return employeeRepository.findById(employeeId).get().getMaxVacationAllowed();
    }

}