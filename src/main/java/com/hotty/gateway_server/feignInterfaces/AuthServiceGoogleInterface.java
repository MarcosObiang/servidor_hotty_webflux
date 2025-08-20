
package com.hotty.gateway_server.feignInterfaces;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

import feign.Response;

@FeignClient("auth")
public interface AuthServiceGoogleInterface {

    
    @PostMapping("internal/google/oauth2callback")
   ResponseEntity <String> googleCallback(String code);



}