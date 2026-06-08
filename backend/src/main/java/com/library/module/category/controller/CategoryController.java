package com.library.module.category.controller;

import com.library.aspect.OperationLog;
import com.library.common.response.Result;
import com.library.module.category.dto.CategoryDTO;
import com.library.module.category.entity.Category;
import com.library.module.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类Controller
 *
 * <p>提供图书分类管理相关的RESTful API，包括分类CRUD和树形结构查询。
 * 分类支持多级嵌套（父子关系），写操作需要管理员角色。</p>
 *
 * @author Library System
 * @since 1.0.0
 * @see CategoryService
 */
@Tag(name = "分类管理")
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "获取分类列表")
    @GetMapping
    public Result<List<Category>> list() {
        List<Category> categories = categoryService.listAll();
        return Result.success(categories);
    }

    @Operation(summary = "获取分类树")
    @GetMapping("/tree")
    public Result<List<Category>> tree() {
        List<Category> tree = categoryService.getCategoryTree();
        return Result.success(tree);
    }

    @Operation(summary = "获取分类详情")
    @GetMapping("/{id}")
    public Result<Category> getById(@PathVariable Long id) {
        Category category = categoryService.getById(id);
        return Result.success(category);
    }

    @Operation(summary = "新增分类")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(value = "新增分类", type = OperationLog.OperationType.CREATE)
    public Result<Long> create(@Valid @RequestBody CategoryDTO categoryDTO) {
        Long categoryId = categoryService.createCategory(categoryDTO);
        return Result.success(categoryId);
    }

    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(value = "更新分类", type = OperationLog.OperationType.UPDATE)
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CategoryDTO categoryDTO) {
        categoryService.updateCategory(id, categoryDTO);
        return Result.success();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(value = "删除分类", type = OperationLog.OperationType.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success();
    }
}
