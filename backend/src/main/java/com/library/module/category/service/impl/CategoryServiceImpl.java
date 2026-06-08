package com.library.module.category.service.impl;

import com.library.common.base.BaseServiceImpl;
import com.library.common.exception.BusinessException;
import com.library.common.response.ResultCode;
import com.library.module.category.dto.CategoryDTO;
import com.library.module.category.entity.Category;
import com.library.module.category.mapper.CategoryMapper;
import com.library.module.category.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分类Service实现
 *
 * @author Library System
 * @since 1.0.0
 */
@Slf4j
@Service
public class CategoryServiceImpl extends BaseServiceImpl<CategoryMapper, Category> 
        implements CategoryService {

    @Override
    public List<Category> getCategoryTree() {
        List<Category> allCategories = baseMapper.selectAllOrdered();
        return buildTree(allCategories, 0L);
    }

    /**
     * 构建分类树
     */
    private List<Category> buildTree(List<Category> categories, Long parentId) {
        Map<Long, List<Category>> groupByParent = categories.stream()
                .collect(Collectors.groupingBy(c -> c.getParentId() == null ? 0L : c.getParentId()));

        List<Category> roots = groupByParent.getOrDefault(parentId, new ArrayList<>());
        for (Category category : roots) {
            category.setChildren(buildTree(categories, category.getId()));
        }
        return roots;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCategory(CategoryDTO categoryDTO) {
        // 检查名称是否存在
        if (existsByName(categoryDTO.getName())) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXIST, "分类名称已存在");
        }

        Category category = new Category();
        category.setName(categoryDTO.getName());
        category.setDescription(categoryDTO.getDescription());
        category.setParentId(categoryDTO.getParentId() != null ? categoryDTO.getParentId() : 0L);
        category.setSortOrder(categoryDTO.getSortOrder() != null ? categoryDTO.getSortOrder() : 0);

        save(category);
        log.info("创建分类成功: {}", category.getName());

        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(Long id, CategoryDTO categoryDTO) {
        Category existCategory = getById(id);
        if (existCategory == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "分类不存在");
        }

        // 检查名称是否被其他分类使用
        if (categoryDTO.getName() != null && !categoryDTO.getName().equals(existCategory.getName())) {
            Category byName = baseMapper.selectByName(categoryDTO.getName());
            if (byName != null) {
                throw new BusinessException(ResultCode.DATA_ALREADY_EXIST, "分类名称已存在");
            }
            existCategory.setName(categoryDTO.getName());
        }

        if (categoryDTO.getDescription() != null) {
            existCategory.setDescription(categoryDTO.getDescription());
        }
        if (categoryDTO.getParentId() != null) {
            // 不能将自己设为父分类
            if (categoryDTO.getParentId().equals(id)) {
                throw new BusinessException(ResultCode.DATA_ERROR, "不能将自己设为父分类");
            }
            existCategory.setParentId(categoryDTO.getParentId());
        }
        if (categoryDTO.getSortOrder() != null) {
            existCategory.setSortOrder(categoryDTO.getSortOrder());
        }

        updateById(existCategory);
        log.info("更新分类成功: {}", existCategory.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        Category category = getById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "分类不存在");
        }

        // 检查是否有子分类
        int childCount = baseMapper.countByParentId(id);
        if (childCount > 0) {
            throw new BusinessException(ResultCode.DATA_ERROR, "该分类下有子分类，无法删除");
        }

        // 检查是否有图书使用该分类
        int bookCount = baseMapper.countBooksByCategoryId(id);
        if (bookCount > 0) {
            throw new BusinessException(ResultCode.DATA_ERROR, "该分类下有图书，无法删除");
        }

        removeById(id);
        log.info("删除分类成功: {}", category.getName());
    }

    @Override
    public boolean existsByName(String name) {
        return baseMapper.selectByName(name) != null;
    }
}
