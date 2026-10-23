package com.smartparking.web;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smartparking.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:apitest;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class ParkingApiTest {

    @Autowired MockMvc mvc;
    @Autowired ParkingTicketRepository tickets;
    @Autowired VehicleRepository vehicles;
    @Autowired ParkingSpotRepository spots;
    @Autowired ParkingFloorRepository floors;

    @BeforeEach
    void clean() {
        tickets.deleteAllInBatch();
        vehicles.deleteAllInBatch();
        spots.deleteAllInBatch();
        floors.deleteAllInBatch();
    }

    private void createFloor() throws Exception {
        mvc.perform(post("/api/v1/admin/floors").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"floorNumber\":0,\"smallSpots\":1,\"mediumSpots\":1,\"largeSpots\":1}"))
                .andExpect(status().isCreated());
    }

    @Test
    void fullFlowOverHttp() throws Exception {
        createFloor();

        MvcResult in = mvc.perform(post("/api/v1/parking/check-in").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"licensePlate\":\"ka01ab1234\",\"vehicleType\":\"CAR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.licensePlate").value("KA01AB1234"))
                .andExpect(jsonPath("$.spotType").value("MEDIUM"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();
        String ticket = com.jayway.jsonpath.JsonPath.read(in.getResponse().getContentAsString(), "$.ticketNumber");

        mvc.perform(get("/api/v1/parking/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSpots").value(3))
                .andExpect(jsonPath("$.availableSpots").value(2));

        mvc.perform(get("/api/v1/parking/tickets/" + ticket))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mvc.perform(post("/api/v1/parking/check-out").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticketNumber\":\"" + ticket + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.fee").value(0.0)); // within grace period

        mvc.perform(post("/api/v1/parking/check-out").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticketNumber\":\"" + ticket + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void validationAndErrorStatuses() throws Exception {
        mvc.perform(post("/api/v1/parking/check-in").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"licensePlate\":\"\",\"vehicleType\":\"CAR\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.licensePlate", notNullValue()));

        mvc.perform(post("/api/v1/parking/check-in").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"licensePlate\":\"AB12\",\"vehicleType\":\"SPACESHIP\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/v1/parking/check-in").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"licensePlate\":\"AB12\",\"vehicleType\":\"CAR\"}"))
                .andExpect(status().isConflict()); // no floors configured -> lot full

        mvc.perform(get("/api/v1/parking/tickets/UNKNOWN")).andExpect(status().isNotFound());
    }
}
