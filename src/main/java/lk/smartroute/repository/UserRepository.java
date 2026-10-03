package lk.smartroute.repository;

import lk.smartroute.model.User;
import lk.smartroute.model.UserType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Page<User> findByUserType(UserType userType, Pageable pageable);

    List<User> findByUserTypeAndActiveTrue(UserType userType);

    Page<User> findByUserTypeAndActive(UserType userType, boolean active, Pageable pageable);

    Page<User> findByActive(boolean active, Pageable pageable);

    @Query("select u from User u where u.active = true")
    Page<User> findAllActiveUsers(Pageable pageable);

    @Query("""
        select u
        from User u
        where lower(u.firstName) like lower(concat('%', :keyword, '%'))
           or lower(u.lastName) like lower(concat('%', :keyword, '%'))
           or lower(u.email) like lower(concat('%', :keyword, '%'))
        """)
    Page<User> searchByFirstNameLastNameOrEmail(@Param("keyword") String keyword, Pageable pageable);

    long countByUserType(UserType userType);

    long countByActiveTrue();
}
