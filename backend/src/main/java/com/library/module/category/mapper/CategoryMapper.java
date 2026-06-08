package com.library.module.category.mapper;

import com.library.common.base.BaseMapper;
import com.library.module.category.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 分类Mapper
 *
 * @author Library System
 * @since 1.0.0
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    /**
     * 查询所有分类（按排序号排序）
     */
    List<Category> selectAllOrdered();

    /**
     * 根据父级ID查询子分类
     */
    List<Category> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 根据分类名称查询
     */
    Category selectByName(@Param("name") String name);

    /**
     * 检查分类是否被使用
     */
    int countBooksByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 检查是否有子分类
     */
    int countByParentId(@Param("parentId") Long parentId);
}
