package com.library.module.category.service;

import com.library.common.base.BaseService;
import com.library.module.category.dto.CategoryDTO;
import com.library.module.category.entity.Category;

import java.util.List;

/**
 * 分类Service接口
 *
 * @author Library System
 * @since 1.0.0
 */
public interface CategoryService extends BaseService<Category> {

    /**
     * 获取分类树
     */
    List<Category> getCategoryTree();

    /**
     * 创建分类
     */
    Long createCategory(CategoryDTO categoryDTO);

    /**
     * 更新分类
     */
    void updateCategory(Long id, CategoryDTO categoryDTO);

    /**
     * 删除分类
     */
    void deleteCategory(Long id);

    /**
     * 检查分类名称是否存在
     */
    boolean existsByName(String name);
}
