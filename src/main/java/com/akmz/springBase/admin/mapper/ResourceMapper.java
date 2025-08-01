package com.akmz.springBase.admin.mapper;

import com.akmz.springBase.admin.model.entity.Resource;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface ResourceMapper {
    List<Resource> findAllResources();
    List<Resource> findResourcesByRoleId(Long roleId);
    Resource findResourceById(Long resourceId);
    Resource findResourceByPattern(String resourcePattern);
    void insertResource(Resource resource);
    void updateResource(Resource resource);
    void deleteResource(Long resourceId);
}
