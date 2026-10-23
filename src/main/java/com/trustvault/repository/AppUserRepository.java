
package com.trustvault.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trustvault.entity.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

}
