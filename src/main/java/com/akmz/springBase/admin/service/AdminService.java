package com.akmz.springBase.admin.service;

import com.akmz.springBase.admin.mapper.ResourceMapper;
import com.akmz.springBase.admin.mapper.RoleMapper;
import com.akmz.springBase.admin.mapper.RoleResourceMappingMapper;
import com.akmz.springBase.admin.mapper.UserRoleMapper;
import com.akmz.springBase.admin.mapper.UserMapper; // Added
import com.akmz.springBase.admin.model.dto.*;
import com.akmz.springBase.admin.model.entity.Resource;
import com.akmz.springBase.admin.model.entity.Role;
import com.akmz.springBase.admin.model.entity.RoleResourceMapping;
import com.akmz.springBase.admin.model.entity.UserRole;
import com.akmz.springBase.auth.mapper.AuthMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final RoleMapper roleMapper;
    private final ResourceMapper resourceMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleResourceMappingMapper roleResourceMappingMapper;
    private final UserMapper userMapper;
    private final AuthMapper authMapper;

    // --- Role Management ---
    public List<RoleResponse> getAllRoles() {
        return roleMapper.findAllRoles().stream()
                .map(RoleResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public RoleResponse getRoleById(Long roleId) {
        Role role = roleMapper.findRoleById(roleId);
        return role != null ? RoleResponse.fromEntity(role) : null;
    }

    @Transactional
    public RoleResponse createRole(RoleCreateRequest request) {
        Role role = new Role();
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        roleMapper.insertRole(role);
        return RoleResponse.fromEntity(role);
    }

    @Transactional
    public RoleResponse updateRole(Long roleId, RoleUpdateRequest request) {
        Role role = roleMapper.findRoleById(roleId);
        if (role == null) {
            return null; // Or throw an exception
        }
        role.setDescription(request.getDescription());
        roleMapper.updateRole(role);
        return RoleResponse.fromEntity(role);
    }

    @Transactional
    public void deleteRole(Long roleId) {
        roleMapper.deleteRole(roleId);
    }

    // --- Menu Resource Management ---
    public List<ResourceResponse> getAllMenuResources() {
        return resourceMapper.findAllResources().stream()
                .filter(r -> "MENU_ITEM".equals(r.getResourceType()))
                .map(ResourceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public ResourceResponse getMenuResourceById(Long resourceId) {
        Resource resource = resourceMapper.findResourceById(resourceId);
        return resource != null && "MENU_ITEM".equals(resource.getResourceType()) ? ResourceResponse.fromEntity(resource) : null;
    }

    @Transactional
    public ResourceResponse createMenuResource(MenuResourceRequest request) {
        Resource resource = new Resource();
        resource.setResourceType("MENU_ITEM");
        resource.setResourcePattern(request.getResourcePattern());
        resource.setDescription(request.getDescription());
        resource.setMenuName(request.getMenuName());
        resource.setMenuUrl(request.getMenuUrl());
        resource.setIconName(request.getIconName());
        resource.setParentResourceId(request.getParentResourceId());
        resource.setDisplayOrder(request.getDisplayOrder());
        resource.setIsGroup(request.getIsGroup());
        resource.setHttpMethod(request.getHttpMethod()); // Added
        resource.setUseYn(request.getUseYn() != null ? request.getUseYn() : "Y"); // Added with default
        resourceMapper.insertResource(resource);
        return ResourceResponse.fromEntity(resource);
    }

    @Transactional
    public ResourceResponse updateMenuResource(Long resourceId, MenuResourceRequest request) {
        Resource resource = resourceMapper.findResourceById(resourceId);
        if (resource == null || !"MENU_ITEM".equals(resource.getResourceType())) {
            return null; // Or throw an exception
        }
        resource.setResourcePattern(request.getResourcePattern());
        resource.setDescription(request.getDescription());
        resource.setMenuName(request.getMenuName());
        resource.setMenuUrl(request.getMenuUrl());
        resource.setIconName(request.getIconName());
        resource.setParentResourceId(request.getParentResourceId());
        resource.setDisplayOrder(request.getDisplayOrder());
        resource.setIsGroup(request.getIsGroup());
        resource.setHttpMethod(request.getHttpMethod()); // Added
        resource.setUseYn(request.getUseYn()); // Added
        resourceMapper.updateResource(resource);
        return ResourceResponse.fromEntity(resource);
    }

    @Transactional
    public void deleteMenuResource(Long resourceId) {
        resourceMapper.deleteResource(resourceId);
    }

    // --- User Management ---
    public List<UserAdminViewResponse> getAllUsersWithRoles() {
        return userMapper.findAllWithRoles();
    }

    @Transactional
    public void activateUser(String userName) {
        authMapper.updateUserUseYn(userName, "Y");
    }

    @Transactional
    public void deactivateUser(String userName) {
        authMapper.updateUserUseYn(userName, "N");
    }

    @Transactional
    public void resetLoginFailureCount(String userName) {
        authMapper.resetLoginFailureCount(userName);
    }

    // --- User Role Management ---
    @Transactional
    public void assignRoleToUser(String userName, Long roleId) {
        UserRole userRole = new UserRole();
        userRole.setUserName(userName);
        userRole.setRoleId(roleId);
        userRoleMapper.insertUserRole(userRole);
    }

    @Transactional
    public void removeRoleFromUser(String userName, Long roleId) {
        userRoleMapper.deleteUserRole(userName, roleId);
    }

    // --- Role Resource Mapping Management ---
    @Transactional
    public void addResourceToRole(Long roleId, Long resourceId) {
        RoleResourceMapping mapping = new RoleResourceMapping();
        mapping.setRoleId(roleId);
        mapping.setResourceId(resourceId);
        roleResourceMappingMapper.insertMapping(mapping);
    }

    @Transactional
    public void removeResourceFromRole(Long roleId, Long resourceId) {
        roleResourceMappingMapper.deleteMapping(roleId, resourceId);
    }

    public List<ResourceResponse> getResourcesByRoleId(Long roleId) {
        return resourceMapper.findResourcesByRoleId(roleId).stream()
                .map(ResourceResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
