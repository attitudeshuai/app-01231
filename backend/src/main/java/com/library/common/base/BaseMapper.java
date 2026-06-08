package com.library.common.base;

import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

/**
 * 通用Mapper基类
 * 提供基础的CRUD操作接口，具体SQL在XML中实现
 *
 * @param <T> 实体类型
 * @author Library System
 * @since 1.0.0
 */
public interface BaseMapper<T> {

    /**
     * 根据ID查询
     *
     * @param id 主键ID
     * @return 实体对象
     */
    T selectById(@Param("id") Long id);

    /**
     * 查询所有记录
     *
     * @return 实体列表
     */
    List<T> selectAll();

    /**
     * 根据条件查询列表
     *
     * @param params 查询参数
     * @return 实体列表
     */
    List<T> selectByCondition(@Param("params") Map<String, Object> params);

    /**
     * 分页查询
     *
     * @param params 查询参数
     * @param offset 偏移量
     * @param limit  每页数量
     * @return 实体列表
     */
    List<T> selectPage(@Param("params") Map<String, Object> params,
                       @Param("offset") int offset,
                       @Param("limit") int limit);

    /**
     * 统计总数
     *
     * @param params 查询参数
     * @return 记录数
     */
    long countByCondition(@Param("params") Map<String, Object> params);

    /**
     * 插入记录
     *
     * @param entity 实体对象
     * @return 影响行数
     */
    int insert(T entity);

    /**
     * 批量插入
     *
     * @param entities 实体列表
     * @return 影响行数
     */
    int batchInsert(@Param("list") List<T> entities);

    /**
     * 更新记录
     *
     * @param entity 实体对象
     * @return 影响行数
     */
    int updateById(T entity);

    /**
     * 根据ID删除
     *
     * @param id 主键ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 批量删除
     *
     * @param ids ID列表
     * @return 影响行数
     */
    int deleteBatchIds(@Param("ids") List<Long> ids);

    /**
     * 检查是否存在
     *
     * @param params 查询参数
     * @return 是否存在
     */
    boolean exists(@Param("params") Map<String, Object> params);
}
