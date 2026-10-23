package com.smartparking.repository;

import com.smartparking.domain.FeeRule;
import com.smartparking.domain.VehicleType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeeRuleRepository extends JpaRepository<FeeRule, Long> {
    Optional<FeeRule> findByVehicleType(VehicleType vehicleType);
}
