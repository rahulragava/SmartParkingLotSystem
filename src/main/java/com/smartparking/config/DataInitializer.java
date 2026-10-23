package com.smartparking.config;

import com.smartparking.domain.FeeRule;
import com.smartparking.domain.VehicleType;
import com.smartparking.repository.FeeRuleRepository;
import com.smartparking.repository.ParkingFloorRepository;
import com.smartparking.service.FloorAdminService;
import com.smartparking.web.dto.CreateFloorRequest;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Seeds default pricing (idempotent) and, optionally, a demo lot when the DB has no floors. */
@Component
public class DataInitializer implements ApplicationRunner {

    private final FeeRuleRepository feeRuleRepository;
    private final ParkingFloorRepository floorRepository;
    private final FloorAdminService floorAdminService;
    private final boolean seedSampleLot;

    public DataInitializer(FeeRuleRepository feeRuleRepository,
                           ParkingFloorRepository floorRepository,
                           FloorAdminService floorAdminService,
                           @Value("${parking.seed.sample-lot:true}") boolean seedSampleLot) {
        this.feeRuleRepository = feeRuleRepository;
        this.floorRepository = floorRepository;
        this.floorAdminService = floorAdminService;
        this.seedSampleLot = seedSampleLot;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedRule(VehicleType.MOTORCYCLE, "10.00", "100.00", 15);
        seedRule(VehicleType.CAR, "20.00", "200.00", 15);
        seedRule(VehicleType.BUS, "50.00", "500.00", 15);

        if (seedSampleLot && floorRepository.count() == 0) {
            floorAdminService.createFloor(new CreateFloorRequest(0, 10, 20, 2));
            floorAdminService.createFloor(new CreateFloorRequest(1, 5, 25, 2));
            floorAdminService.createFloor(new CreateFloorRequest(2, 0, 30, 0));
        }
    }

    private void seedRule(VehicleType type, String hourly, String cap, int graceMinutes) {
        if (feeRuleRepository.findByVehicleType(type).isEmpty()) {
            feeRuleRepository.save(new FeeRule(type, new BigDecimal(hourly), new BigDecimal(cap), graceMinutes));
        }
    }
}
