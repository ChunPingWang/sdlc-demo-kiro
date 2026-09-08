package com.example.lifepremium.service;

import com.example.lifepremium.dto.response.PageResponse;
import com.example.lifepremium.dto.response.RateTableVersionResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

public interface RateTableService {

    RateTableVersionResponse upload(
            MultipartFile file, String productCode,
            LocalDate effectiveDate, UUID adminId);

    PageResponse<RateTableVersionResponse> listVersions(String productCode, Pageable pageable);
}
