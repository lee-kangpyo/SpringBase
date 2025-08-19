package com.akmz.springBase.menu.service;

import com.akmz.springBase.admin.mapper.ResourceMapper;
import com.akmz.springBase.admin.mapper.UserRoleMapper;
import com.akmz.springBase.admin.model.entity.Resource;
import com.akmz.springBase.admin.model.entity.UserRole;
import com.akmz.springBase.menu.model.dto.MenuResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final UserRoleMapper userRoleMapper;
    private final ResourceMapper resourceMapper;

    public List<MenuResponse> getMenusForCurrentUser(String currentUserName) {
        // 1. 기존 메소드를 사용해 사용자의 Role 목록 조회
        List<UserRole> userRoles = userRoleMapper.findUserRolesByUserName(currentUserName);

        if (userRoles.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 조회된 Role 목록에서 ID만 추출
        List<Long> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());

        // 3. 역할(Role)에 따른 메뉴 리소스 조회
        List<Resource> accessibleMenus = resourceMapper.findMenuResourcesByRoleIds(roleIds);

        // 4. 계층 구조로 변환
        return buildMenuTree(accessibleMenus);
    }

    public List<MenuResponse> getMenusForRoles (List<Long> roleIds) {
        List<Resource> accessibleMenus = resourceMapper.findMenuResourcesByRoleIds(roleIds);
        return buildMenuTree(accessibleMenus);
    }

    private List<MenuResponse> buildMenuTree(List<Resource> resources) {
        // 순서 보장을 위해 LinkedHashMap 사용
        Map<Long, MenuResponse> menuMap = resources.stream()
                .map(MenuResponse::fromEntity)
                .collect(Collectors.toMap(MenuResponse::getMenuId, menu -> menu, (v1, v2) -> v1, java.util.LinkedHashMap::new));

        List<MenuResponse> rootMenus = new ArrayList<>();
        menuMap.values().forEach(menu -> {
            if (menu.getParentId() == null) {
                rootMenus.add(menu);
            } else {
                MenuResponse parent = menuMap.get(menu.getParentId());
                if (parent != null) {
                    parent.getChildren().add(menu);
                }
            }
        });

        // 최종적으로 모든 레벨에서 순서를 보장하기 위해 정렬 로직 추가
        sortMenusByDisplayOrder(rootMenus);

        return rootMenus;
    }

    private void sortMenusByDisplayOrder(List<MenuResponse> menus) {
        menus.sort(java.util.Comparator.comparing(MenuResponse::getDisplayOrder));
        for (MenuResponse menu : menus) {
            if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
                sortMenusByDisplayOrder(menu.getChildren());
            }
        }
    }
}

