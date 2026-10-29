package org.example.postureservice.dataaccess.repository;

import org.example.postureservice.dataaccess.entity.SecurityPostureEntity;
import org.example.postureservice.dataaccess.entity.SecurityPostureId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SecurityPostureRepository extends JpaRepository<SecurityPostureEntity, SecurityPostureId> {

    Optional<SecurityPostureEntity> findByServiceIdAndEnvironment(String serviceId, String environment);
}
