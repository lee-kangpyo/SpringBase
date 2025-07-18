package com.akmz.springBase.admin.controller;

import com.akmz.springBase.admin.mapper.UserMapper;
import com.akmz.springBase.admin.model.dto.UserAdminViewResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserMapper userMapper;

    @GetMapping("/userList")
    public ResponseEntity<?> userList() {
        List<UserAdminViewResponse> result =  userMapper.findAll();
        log.info("List ::: {}", result);
        return ResponseEntity.ok().body(result);
    }
}