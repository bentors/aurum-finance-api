package com.bentorangel.finance_dashboard.repository;

import com.bentorangel.finance_dashboard.model.Category;
import com.bentorangel.finance_dashboard.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Page<Category> findAllByUser(User user, Pageable pageable);

    Optional<Category> findByIdAndUser(UUID id, User user);

    boolean existsByNameIgnoreCaseAndUser(String name, User user);
}