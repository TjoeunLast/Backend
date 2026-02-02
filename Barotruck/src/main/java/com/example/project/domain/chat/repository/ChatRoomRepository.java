package com.example.project.domain.chat.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.project.domain.chat.domain.ChatRoom;
import com.example.project.domain.chat.dto.ChatRoomType;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    // 기본 제공되는 save, findById, deleteById 사용
    
}