package com.akmz.springBase.menu.controller;

import com.akmz.springBase.menu.model.dto.MenuResponse;
import com.akmz.springBase.menu.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@Tag(name = "메뉴 API", description = "사용자별 동적 메뉴 조회 API")
@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @Operation(summary = "현재 사용자 메뉴 조회", description = "로그인한 사용자의 역할에 따라 접근 가능한 메뉴 목록을 계층 구조로 조회합니다.")
    @GetMapping
    public ResponseEntity<List<MenuResponse>> getMenusForCurrentUser(Principal principal) {
        List<MenuResponse> menus = menuService.getMenusForCurrentUser(principal.getName());
        return ResponseEntity.ok(menus);
    }
}

