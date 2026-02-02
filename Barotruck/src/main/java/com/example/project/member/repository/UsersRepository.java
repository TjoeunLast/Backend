package com.example.project.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.project.member.domain.Users;
import com.example.project.member.domain.Users.UsersBuilder;

@Repository
public interface UsersRepository extends JpaRepository<Users, Long>{

	public Optional<Users> findByEmail(String username);

	public Optional<Users> findByUserId(Long userId);

	public boolean existsByNickname(String nickname);


}
