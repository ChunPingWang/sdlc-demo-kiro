package com.example.lifepremium.service;

import com.example.lifepremium.dto.request.RateTableUploadRequest;
import com.example.lifepremium.dto.response.PageResponse;
import com.example.lifepremium.dto.response.RateTableUploadResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RateTableService {

    RateTableUploadResponse upload(RateTableUploadRequest request, UUID adminId);

    PageResponse<RateTableUploadResponse> listVersions(String productCode, Pageable pageable);
}
