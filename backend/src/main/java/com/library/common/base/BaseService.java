package com.library.common.base;

import com.library.common.response.PageResult;
import java.util.List;
import java.util.Map;

/**
 * 通用Service接口
 *
 * @param <T> 实体类型
 * @author Library System
 * @since 1.0.0
 */
public interface BaseService<T> {

    /**
     * 根据ID查询
     *
     * @param id 主键ID
     * @return 实体对象
     */
    T getById(Long id);

    /**
     * 查询所有记录
     *
     * @return 实体列表
     */
    List<T> listAll();

    /**
     * 根据条件查询列表
     *
     * @param params 查询参数
     * @return 实体列表
     */
    List<T> listByCondition(Map<String, Object> params);

    /**
     * 分页查询
     *
     * @param params   查询参数
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    PageResult<T> page(Map<String, Object> params, int pageNum, int pageSize);

    /**
     * 新增记录
     *
     * @param entity 实体对象
     * @return 是否成功
     */
    boolean save(T entity);

    /**
     * 批量新增
     *
     * @param entities 实体列表
     * @return 是否成功
     */
    boolean saveBatch(List<T> entities);

    /**
     * 更新记录
     *
     * @param entity 实体对象
     * @return 是否成功
     */
    boolean updateById(T entity);

    /**
     * 根据ID删除
     *
     * @param id 主键ID
     * @return 是否成功
     */
    boolean removeById(Long id);

    /**
     * 批量删除
     *
     * @param ids ID列表
     * @return 是否成功
     */
    boolean removeByIds(List<Long> ids);

    /**
     * 统计总数
     *
     * @param params 查询参数
     * @return 记录数
     */
    long count(Map<String, Object> params);
}
