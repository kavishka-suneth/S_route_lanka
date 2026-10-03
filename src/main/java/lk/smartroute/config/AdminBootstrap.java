package lk.smartroute.config;

import lk.smartroute.model.Staff;
import lk.smartroute.model.StaffType;
import lk.smartroute.model.User;
import lk.smartroute.model.UserType;
import lk.smartroute.repository.StaffRepository;
import lk.smartroute.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class AdminBootstrap implements CommandLineRunner {
    private final UserRepository users;
    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminBootstrap(
        UserRepository users,
        StaffRepository staffRepository,
        PasswordEncoder passwordEncoder,
        @Value("${ADMIN_EMAIL:}") String email,
        @Value("${ADMIN_PASSWORD:}") String password
    ) {
        this.users = users;
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (!email.isBlank() && !password.isBlank() && !users.existsByEmailIgnoreCase(email)) {
            User admin = new User();
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            admin.setEmail(email.trim().toLowerCase());
            admin.setPasswordHash(passwordEncoder.encode(password));
            admin.setPhone(null);
            admin.setUserType(UserType.STAFF);
            admin.setEmailVerified(true);
            User saved = users.save(admin);

            Staff staff = new Staff();
            staff.setUser(saved);
            staff.setHireDate(LocalDate.now());
            staff.setStaffType(StaffType.SYSTEM_ADMINISTRATOR);
            staffRepository.save(staff);
        }
    }
}
