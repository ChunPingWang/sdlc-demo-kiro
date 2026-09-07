package com.example.lifepremium.service;

import com.example.lifepremium.dto.response.CalculationRecordResponse;
import com.example.lifepremium.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface CalculationRecordService {

    PageResponse<CalculationRecordResponse> query(
            UUID agentId, boolean isAdmin,
            LocalDate fromDate, LocalDate toDate,
            Pageable pageable);
}
