package lk.smartroute.service.impl;

import lk.smartroute.dto.ApiDtos.*;
import lk.smartroute.dto.DtoMapper;
import lk.smartroute.exception.ConflictException;
import lk.smartroute.exception.ForbiddenActionException;
import lk.smartroute.exception.ResourceNotFoundException;
import lk.smartroute.model.Passenger;
import lk.smartroute.model.Role;
import lk.smartroute.model.Staff;
import lk.smartroute.model.StaffType;
import lk.smartroute.model.User;
import lk.smartroute.model.UserType;
import lk.smartroute.model.NotificationType;
import lk.smartroute.repository.PassengerRepository;
import lk.smartroute.repository.StaffRepository;
import lk.smartroute.repository.BookingRepository;
import lk.smartroute.repository.SupportTicketRepository;
import lk.smartroute.repository.UserRepository;
import lk.smartroute.service.UserService;
import lk.smartroute.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository users;
    private final PassengerRepository passengers;
    private final StaffRepository staffRepository;
    private final BookingRepository bookings;
    private final SupportTicketRepository supportTickets;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notifications;

    public UserServiceImpl(
        UserRepository users,
        PassengerRepository passengers,
        StaffRepository staffRepository,
        BookingRepository bookings,
        SupportTicketRepository supportTickets,
        PasswordEncoder passwordEncoder,
        NotificationService notifications
    ) {
        this.users = users;
        this.passengers = passengers;
        this.staffRepository = staffRepository;
        this.bookings = bookings;
        this.supportTickets = supportTickets;
        this.passwordEncoder = passwordEncoder;
        this.notifications = notifications;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse currentProfile(String email) {
        return DtoMapper.user(userByEmail(email));
    }

    @Override
    public UserResponse updateProfile(String email, ProfileUpdateRequest request) {
        User user = userByEmail(email);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(request.phone());
        return DtoMapper.user(user);
    }

    @Override
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userByEmail(email);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        notifications.sendSecurityNotification(user, NotificationType.PASSWORD_CHANGED, "Password changed", "Your SmartRoute Lanka password was changed.", "PASSWORD-CHANGED:" + user.getId() + ":" + java.time.Instant.now().toEpochMilli());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(Role role, Boolean active, Pageable pageable) {
        Page<User> page;
        if (role != null && role == Role.PASSENGER) {
            page = active == null
                ? users.findByUserType(UserType.PASSENGER, pageable)
                : users.findByUserTypeAndActive(UserType.PASSENGER, active, pageable);
        } else if (role != null) {
            Page<Staff> staffPage = active == null
                ? staffRepository.findByStaffType(role.toStaffType(), pageable)
                : staffRepository.findByStaffTypeAndUserActive(role.toStaffType(), active, pageable);
            page = staffPage.map(Staff::getUser);
        } else if (active != null) {
            page = users.findByActive(active, pageable);
        } else {
            page = users.findAll(pageable);
        }
        return PageResponse.from(page.map(DtoMapper::user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        return DtoMapper.user(userById(id));
    }

    @Override
    public UserResponse updateRole(Long id, RoleUpdateRequest request, String actorEmail) {
        User user = userById(id);
        if (user.isActive() && user.getRole() == Role.SYSTEM_ADMIN && request.role() != Role.SYSTEM_ADMIN) {
            assertAnotherActiveSystemAdminExists();
        }
        if (request.role() == Role.PASSENGER) {
            user.setUserType(UserType.PASSENGER);
            if (user.getPassenger() == null) {
                Passenger passenger = new Passenger();
                passenger.setUser(user);
                user.setPassenger(passenger);
                passengers.save(passenger);
            }
            if (user.getStaff() != null) {
                if (supportTickets.existsByHandledByStaffId(user.getStaff().getId())) {
                    throw new ConflictException("Cannot convert staff with handled support tickets to passenger");
                }
                staffRepository.delete(user.getStaff());
                user.setStaff(null);
            }
        } else {
            user.setUserType(UserType.STAFF);
            if (user.getPassenger() != null) {
                if (bookings.existsByPassengerId(user.getPassenger().getId())) {
                    throw new ConflictException("Cannot convert passenger with booking history to staff");
                }
                passengers.delete(user.getPassenger());
                user.setPassenger(null);
            }
            Staff staff = user.getStaff();
            if (staff == null) {
                staff = new Staff();
                staff.setUser(user);
                staff.setHireDate(java.time.LocalDate.now());
                user.setStaff(staff);
            }
            staff.setStaffType(request.role().toStaffType());
            staffRepository.save(staff);
        }
        return DtoMapper.user(user);
    }

    @Override
    public UserResponse updateActive(Long id, ActiveUpdateRequest request, String actorEmail) {
        User user = userById(id);
        if (user.getEmail().equalsIgnoreCase(actorEmail) && !request.active()) {
            throw new ForbiddenActionException("You cannot deactivate your own account");
        }
        if (!request.active() && user.isActive() && user.getRole() == Role.SYSTEM_ADMIN) {
            assertAnotherActiveSystemAdminExists();
        }
        boolean wasActive = user.isActive();
        user.setActive(request.active());
        if (wasActive && !request.active()) {
            notifications.sendSecurityNotification(user, NotificationType.ACCOUNT_DISABLED, "Account disabled", "Your SmartRoute Lanka account has been disabled.", "ACCOUNT-DISABLED:" + user.getId() + ":" + java.time.Instant.now().toEpochMilli());
        } else if (!wasActive && request.active()) {
            notifications.sendSecurityNotification(user, NotificationType.ACCOUNT_REACTIVATED, "Account reactivated", "Your SmartRoute Lanka account has been reactivated.", "ACCOUNT-REACTIVATED:" + user.getId() + ":" + java.time.Instant.now().toEpochMilli());
        }
        return DtoMapper.user(user);
    }

    private void assertAnotherActiveSystemAdminExists() {
        long activeSystemAdmins = staffRepository.countByStaffTypeAndUserActive(StaffType.SYSTEM_ADMINISTRATOR, true);
        if (activeSystemAdmins <= 1) {
            throw new ForbiddenActionException("At least one active system administrator is required");
        }
    }

    private User userByEmail(String email) {
        return users.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private User userById(Long id) {
        return users.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
