package com.pobluesky.voc.feign;

import com.pobluesky.voc.global.util.model.JsonResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.relational.core.sql.In;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "inquiry")
public interface InquiryClient {

    @GetMapping("/api/inquiries/exists/{inquiryId}")
    Boolean checkInquiryExists(@PathVariable("inquiryId") Long inquiryId);

//    @GetMapping("/api/inquiries/{id}")
//    Inquiry getInquiryById(Long inquiryId);

    @GetMapping("/api/inquiries/without-token/{inquiryId}")
    JsonResult<Inquiry> getInquiryByIdWithoutToken(@PathVariable("inquiryId") Long inquiryId);
}
