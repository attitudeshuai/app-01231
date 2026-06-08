package com.library.module.borrow.controller;

import com.library.aspect.OperationLog;
import com.library.common.response.PageResult;
import com.library.common.response.Result;
import com.library.common.util.SecurityUtil;
import com.library.module.borrow.dto.BorrowQueryDTO;
import com.library.module.borrow.entity.BorrowRecord;
import com.library.module.borrow.service.BorrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 借阅Controller
 *
 * <p>提供图书借阅管理相关的RESTful API，包括借阅、归还、续借、
 * 逾期管理、借阅统计等功能。管理员可代借阅和查看所有记录，
 * 普通用户只能查看自己的借阅记录。</p>
 *
 * <h3>借阅规则：</h3>
 * <ul>
 *   <li>每人最多同时借阅{@value com.library.module.borrow.service.impl.BorrowServiceImpl#MAX_BORROW_COUNT}本</li>
 *   <li>默认借阅期限为系统配置的天数</li>
 *   <li>每本书最多续借1次</li>
 *   <li>逾期自动计算罚款</li>
 * </ul>
 *
 * @author Library System
 * @since 1.0.0
 * @see BorrowService
 */
@Tag(name = "借阅管理")
@RestController
@RequestMapping("/borrows")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;

    @Operation(summary = "分页查询借阅记录")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<BorrowRecord>> page(BorrowQueryDTO queryDTO) {
        PageResult<BorrowRecord> page = borrowService.pageBorrows(queryDTO);
        return Result.success(page);
    }

    @Operation(summary = "获取借阅详情")
    @GetMapping("/{id}")
    public Result<BorrowRecord> getById(@PathVariable Long id) {
        BorrowRecord record = borrowService.getById(id);
        return Result.success(record);
    }

    /**
     * 借阅图书
     *
     * <p>当前登录用户借阅指定图书，自动检查库存、借阅数量限制、是否已借阅等。</p>
     *
     * @param request 包含bookId的请求体
     * @return 借阅记录ID
     */
    @Operation(summary = "借阅图书")
    @PostMapping
    @OperationLog(value = "借阅图书", type = OperationLog.OperationType.CREATE)
    public Result<Long> borrow(@RequestBody Map<String, Long> request) {
        Long userId = SecurityUtil.getCurrentUserId();
        Long bookId = request.get("bookId");
        Long recordId = borrowService.borrowBook(userId, bookId);
        return Result.success(recordId);
    }

    /**
     * 管理员代借阅
     *
     * <p>管理员指定用户ID和图书ID进行代借阅操作。</p>
     *
     * @param request 包含userId和bookId的请求体
     * @return 借阅记录ID
     */
    @Operation(summary = "管理员代借阅")
    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(value = "管理员代借阅", type = OperationLog.OperationType.CREATE)
    public Result<Long> adminBorrow(@RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        Long bookId = request.get("bookId");
        Long recordId = borrowService.borrowBook(userId, bookId);
        return Result.success(recordId);
    }

    /**
     * 归还图书
     *
     * <p>逾期时自动计算罚款并恢复库存。</p>
     *
     * @param id 借阅记录ID
     */
    @Operation(summary = "归还图书")
    @PutMapping("/{id}/return")
    @OperationLog(value = "归还图书", type = OperationLog.OperationType.UPDATE)
    public Result<Void> returnBook(@PathVariable Long id) {
        borrowService.returnBook(id);
        return Result.success();
    }

    /**
     * 续借图书
     *
     * <p>每本书最多续借1次，续借后到期日期顺延。</p>
     *
     * @param id 借阅记录ID
     */
    @Operation(summary = "续借图书")
    @PutMapping("/{id}/renew")
    @OperationLog(value = "续借图书", type = OperationLog.OperationType.UPDATE)
    public Result<Void> renewBook(@PathVariable Long id) {
        borrowService.renewBook(id);
        return Result.success();
    }

    @Operation(summary = "我的借阅记录")
    @GetMapping("/my")
    public Result<PageResult<BorrowRecord>> myBorrows(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = SecurityUtil.getCurrentUserId();
        PageResult<BorrowRecord> page = borrowService.getMyBorrows(userId, status, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "当前借阅列表")
    @GetMapping("/current")
    public Result<List<BorrowRecord>> currentBorrows() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<BorrowRecord> records = borrowService.getCurrentBorrows(userId);
        return Result.success(records);
    }

    @Operation(summary = "逾期列表")
    @GetMapping("/overdue")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<BorrowRecord>> overdueList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<BorrowRecord> page = borrowService.getOverdueList(pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "借阅统计")
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> statistics() {
        Map<String, Object> stats = borrowService.getStatistics();
        return Result.success(stats);
    }

    /**
     * 检查当前用户是否可借阅指定图书
     *
     * <p>检查库存、借阅数量限制、是否已借阅等条件。</p>
     *
     * @param bookId 图书ID
     * @return 是否可借阅
     */
    @Operation(summary = "检查是否可借阅")
    @GetMapping("/can-borrow")
    public Result<Boolean> canBorrow(@RequestParam Long bookId) {
        Long userId = SecurityUtil.getCurrentUserId();
        boolean canBorrow = borrowService.canBorrow(userId, bookId);
        return Result.success(canBorrow);
    }
}
