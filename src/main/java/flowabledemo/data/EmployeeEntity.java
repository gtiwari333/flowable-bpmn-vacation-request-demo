package flowabledemo.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.Set;

@Entity
@Getter
@Setter
public class EmployeeEntity {
    @Id
    private String id;

    private String firstName;
    private String lastName;
    private String email;
    private int maxVacationAllowed;

    @Enumerated(EnumType.STRING)
    private Set<Role> roles;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<VacationRequestEntity> vacationRequests;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        EmployeeEntity that = (EmployeeEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(firstName, that.firstName) && Objects.equals(lastName, that.lastName) && Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(firstName);
        result = 31 * result + Objects.hashCode(lastName);
        result = 31 * result + Objects.hashCode(email);
        return result;
    }
}

enum Role {
    EMPLOYEE, MANAGER
}