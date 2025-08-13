package com.akmz.springBase.admin.controller;

import com.akmz.springBase.admin.model.dto.*;
import com.akmz.springBase.admin.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "관리자 API", description = "관리자 기능 관련 API (역할, 메뉴 리소스, 사용자 관리)")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // --- Role Management ---
    @Operation(summary = "[GET] 모든 역할 조회", description = "시스템에 등록된 모든 역할을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "성공적으로 모든 역할을 조회했습니다.")
    @GetMapping("/roles")
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        return ResponseEntity.ok(adminService.getAllRoles());
    }

    @Operation(summary = "[GET] 역할 ID로 조회", description = "특정 역할 ID에 해당하는 역할을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "성공적으로 역할을 조회했습니다.")
    @ApiResponse(responseCode = "404", description = "역할을 찾을 수 없습니다.")
    @GetMapping("/roles/{roleId}")
    public ResponseEntity<RoleResponse> getRoleById(
            @Parameter(description = "조회할 역할 ID", required = true) @PathVariable Long roleId) {
        RoleResponse role = adminService.getRoleById(roleId);
        return role != null ? ResponseEntity.ok(role) : ResponseEntity.notFound().build();
    }

    @Operation(summary = "[POST] 새 역할 생성", description = "새로운 역할을 생성합니다.")
    @ApiResponse(responseCode = "201", description = "역할이 성공적으로 생성되었습니다.")
    @PostMapping("/roles")
    public ResponseEntity<RoleResponse> createRole(
            @Parameter(description = "생성할 역할 정보", required = true) @RequestBody RoleCreateRequest request) {
        RoleResponse newRole = adminService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(newRole);
    }

    @Operation(summary = "[PUT] 역할 업데이트", description = "특정 역할 ID에 해당하는 역할을 업데이트합니다.")
    @ApiResponse(responseCode = "200", description = "역할이 성공적으로 업데이트되었습니다.")
    @ApiResponse(responseCode = "404", description = "업데이트할 역할을 찾을 수 없습니다.")
    @PostMapping("/roles/{roleId}/update")
    public ResponseEntity<RoleResponse> updateRole(
            @Parameter(description = "업데이트할 역할 ID", required = true) @PathVariable Long roleId,
            @Parameter(description = "업데이트할 역할 정보", required = true) @RequestBody RoleUpdateRequest request) {
        RoleResponse updatedRole = adminService.updateRole(roleId, request);
        return updatedRole != null ? ResponseEntity.ok(updatedRole) : ResponseEntity.notFound().build();
    }

    @Operation(summary = "[DELETE] 역할 삭제", description = "특정 역할 ID에 해당하는 역할을 삭제합니다.")
    @ApiResponse(responseCode = "204", description = "역할이 성공적으로 삭제되었습니다.")
    @PostMapping("/roles/{roleId}/delete")
    public ResponseEntity<Void> deleteRole(
            @Parameter(description = "삭제할 역할 ID", required = true) @PathVariable Long roleId) {
        adminService.deleteRole(roleId);
        return ResponseEntity.noContent().build();
    }

    // --- Menu Resource Management ---
    @Operation(summary = "[GET] 모든 메뉴 리소스 조회", description = "시스템에 등록된 모든 메뉴 리소스를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "성공적으로 모든 메뉴 리소스를 조회했습니다.")
    @GetMapping("/resources/menu")
    public ResponseEntity<List<ResourceResponse>> getAllMenuResources() {
        return ResponseEntity.ok(adminService.getAllMenuResources());
    }

    @Operation(summary = "[GET] 메뉴 리소스 ID로 조회", description = "특정 메뉴 리소스 ID에 해당하는 메뉴 리소스를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "성공적으로 메뉴 리소스를 조회했습니다.")
    @ApiResponse(responseCode = "404", description = "메뉴 리소스를 찾을 수 없습니다.")
    @GetMapping("/resources/menu/{resourceId}")
    public ResponseEntity<ResourceResponse> getMenuResourceById(
            @Parameter(description = "조회할 메뉴 리소스 ID", required = true) @PathVariable Long resourceId) {
        ResourceResponse resource = adminService.getMenuResourceById(resourceId);
        return resource != null ? ResponseEntity.ok(resource) : ResponseEntity.notFound().build();
    }

    @Operation(summary = "[POST] 새 메뉴 리소스 생성", description = "새로운 메뉴 리소스를 생성합니다.")
    @ApiResponse(responseCode = "201", description = "메뉴 리소스가 성공적으로 생성되었습니다.")
    @PostMapping("/resources/menu")
    public ResponseEntity<ResourceResponse> createMenuResource(
            @Parameter(description = "생성할 메뉴 리소스 정보", required = true) @RequestBody MenuResourceRequest request) {
        ResourceResponse newResource = adminService.createMenuResource(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(newResource);
    }

    @Operation(summary = "[PUT] 메뉴 리소스 업데이트", description = "특정 메뉴 리소스 ID에 해당하는 메뉴 리소스를 업데이트합니다.")
    @ApiResponse(responseCode = "200", description = "메뉴 리소스가 성공적으로 업데이트되었습니다.")
    @ApiResponse(responseCode = "404", description = "업데이트할 메뉴 리소스를 찾을 수 없습니다.")
    @PostMapping("/resources/menu/{resourceId}/update")
    public ResponseEntity<ResourceResponse> updateMenuResource(
            @Parameter(description = "업데이트할 메뉴 리소스 ID", required = true) @PathVariable Long resourceId,
            @Parameter(description = "업데이트할 메뉴 리소스 정보", required = true) @RequestBody MenuResourceRequest request) {
        ResourceResponse updatedResource = adminService.updateMenuResource(resourceId, request);
        return updatedResource != null ? ResponseEntity.ok(updatedResource) : ResponseEntity.notFound().build();
    }

    @Operation(summary = "[DELETE] 메뉴 리소스 삭제", description = "특정 메뉴 리소스 ID에 해당하는 메뉴 리소스를 삭제합니다.")
    @ApiResponse(responseCode = "204", description = "메뉴 리소스가 성공적으로 삭제되었습니다.")
    @PostMapping("/resources/menu/{resourceId}/delete")
    public ResponseEntity<Void> deleteMenuResource(
            @Parameter(description = "삭제할 메뉴 리소스 ID", required = true) @PathVariable Long resourceId) {
        adminService.deleteMenuResource(resourceId);
        return ResponseEntity.noContent().build();
    }

    // --- User Management ---
    @Operation(summary = "[GET] 모든 사용자 및 역할 조회", description = "시스템에 등록된 모든 사용자와 해당 사용자의 역할을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "성공적으로 사용자 목록을 조회했습니다.")
    @GetMapping("/userList")
    public ResponseEntity<List<UserAdminViewResponse>> userList() {
        List<UserAdminViewResponse> result = adminService.getAllUsersWithRoles();
        return ResponseEntity.ok().body(result);
    }

    @Operation(summary = "[PUT] 사용자 활성화", description = "특정 사용자를 활성화합니다.")
    @ApiResponse(responseCode = "200", description = "사용자가 성공적으로 활성화되었습니다.")
    @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다.")
    @PostMapping("/users/{userName}/activate")
    public ResponseEntity<Void> activateUser(
            @Parameter(description = "활성화할 사용자명", required = true) @PathVariable String userName) {
        adminService.activateUser(userName);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "[PUT] 사용자 비활성화", description = "특정 사용자를 비활성화합니다.")
    @ApiResponse(responseCode = "200", description = "사용자가 성공적으로 비활성화되었습니다.")
    @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다.")
    @PostMapping("/users/{userName}/deactivate")
    public ResponseEntity<Void> deactivateUser(
            @Parameter(description = "비활성화할 사용자명", required = true) @PathVariable String userName) {
        adminService.deactivateUser(userName);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "[PUT] 사용자 로그인 실패 횟수 초기화", description = "특정 사용자의 로그인 실패 횟수를 초기화하여 계정 잠금을 해제합니다.")
    @ApiResponse(responseCode = "200", description = "로그인 실패 횟수가 성공적으로 초기화되었습니다.")
    @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다.")
    @PostMapping("/users/{userName}/reset-login-failure")
    public ResponseEntity<Void> resetLoginFailure(
            @Parameter(description = "로그인 실패 횟수를 초기화할 사용자명", required = true) @PathVariable String userName) {
        adminService.resetLoginFailureCount(userName);
        return ResponseEntity.ok().build();
    }

    // --- User Role Management ---
    @Operation(summary = "[POST] 사용자에게 역할 할당", description = "특정 사용자에게 역할을 할당합니다.")
    @ApiResponse(responseCode = "201", description = "역할이 성공적으로 할당되었습니다.")
    @ApiResponse(responseCode = "404", description = "사용자 또는 역할을 찾을 수 없습니다.")
    @PostMapping("/users/{userName}/roles/{roleId}")
    public ResponseEntity<Void> assignRoleToUser(
            @Parameter(description = "역할을 할당할 사용자명", required = true) @PathVariable String userName,
            @Parameter(description = "할당할 역할 ID", required = true) @PathVariable Long roleId) {
        adminService.assignRoleToUser(userName, roleId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "[DELETE] 사용자로부터 역할 제거", description = "특정 사용자로부터 역할을 제거합니다.")
    @ApiResponse(responseCode = "204", description = "역할이 성공적으로 제거되었습니다.")
    @ApiResponse(responseCode = "404", description = "사용자 또는 역할을 찾을 수 없습니다.")
    @PostMapping("/users/{userName}/roles/{roleId}/delete")
    public ResponseEntity<Void> removeRoleFromUser(
            @Parameter(description = "역할을 제거할 사용자명", required = true) @PathVariable String userName,
            @Parameter(description = "제거할 역할 ID", required = true) @PathVariable Long roleId) {
        adminService.removeRoleFromUser(userName, roleId);
        return ResponseEntity.noContent().build();
    }

    // --- Role Resource Mapping Management ---
    @Operation(summary = "[POST] 역할에 리소스 추가", description = "특정 역할에 리소스를 추가하여 매핑합니다.")
    @ApiResponse(responseCode = "201", description = "리소스가 역할에 성공적으로 추가되었습니다.")
    @ApiResponse(responseCode = "404", description = "역할 또는 리소스를 찾을 수 없습니다.")
    @PostMapping("/roles/{roleId}/resources/{resourceId}")
    public ResponseEntity<Void> addResourceToRole(
            @Parameter(description = "리소스를 추가할 역할 ID", required = true) @PathVariable Long roleId,
            @Parameter(description = "역할에 추가할 리소스 ID", required = true) @PathVariable Long resourceId) {
        adminService.addResourceToRole(roleId, resourceId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "[DELETE] 역할에서 리소스 제거", description = "특정 역할에서 리소스를 제거합니다.")
    @ApiResponse(responseCode = "204", description = "리소스가 역할에서 성공적으로 제거되었습니다.")
    @ApiResponse(responseCode = "404", description = "역할 또는 리소스를 찾을 수 없습니다.")
    @PostMapping("/roles/{roleId}/resources/{resourceId}/delete")
    public ResponseEntity<Void> removeResourceFromRole(
            @Parameter(description = "리소스를 제거할 역할 ID", required = true) @PathVariable Long roleId,
            @Parameter(description = "역할에서 제거할 리소스 ID", required = true) @PathVariable Long resourceId) {
        adminService.removeResourceFromRole(roleId, resourceId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "[GET] 역할에 할당된 리소스 조회", description = "특정 역할에 할당된 모든 리소스를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "성공적으로 리소스를 조회했습니다.")
    @ApiResponse(responseCode = "404", description = "역할을 찾을 수 없습니다.")
    @GetMapping("/roles/{roleId}/resources")
    public ResponseEntity<List<ResourceResponse>> getResourcesByRoleId(
            @Parameter(description = "리소스를 조회할 역할 ID", required = true) @PathVariable Long roleId) {
        return ResponseEntity.ok(adminService.getResourcesByRoleId(roleId));
    }
}
