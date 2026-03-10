package com.example.project.member.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.project.member.domain.Users;
import com.example.project.security.user.Role;

@Repository
public interface UsersRepository extends JpaRepository<Users, Long>{

	public Optional<Users> findByEmail(String username);

	public Optional<Users> findByUserId(Long userId);

	public boolean existsByNickname(String nickname);

	public List<Users> findAllByRole(Role role, Sort sort);

	Optional<Users> findTopByRoleOrderByUserIdDesc(Role role);

	Optional<Users> findByNameAndPhone(String name, String phone);
	
	// 관리자 회원 조회 차주, 화주 같이 조회
	@Query("SELECT u FROM Users u " +
	           "LEFT JOIN FETCH u.driver " +
	           "LEFT JOIN FETCH u.shipper " +
	           "WHERE u.userId = :userId")
	    Optional<Users> findByIdWithAllDetails(@Param("userId") Long userId);

	List<Users> findAllByDelflagAndSuspendedUntilLessThanEqual(String delflag, LocalDateTime suspendedUntil);


}
