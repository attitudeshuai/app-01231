package com.library.module.book.mapper;

import com.library.common.base.BaseMapper;
import com.library.module.book.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 图书Mapper
 *
 * @author Library System
 * @since 1.0.0
 */
@Mapper
public interface BookMapper extends BaseMapper<Book> {

    /**
     * 根据ISBN查询
     */
    Book selectByIsbn(@Param("isbn") String isbn);

    /**
     * 分页查询（带分类名称）
     */
    List<Book> selectPageWithCategory(@Param("params") Map<String, Object> params,
                                       @Param("offset") int offset,
                                       @Param("limit") int limit);

    /**
     * 更新库存
     */
    int updateStock(@Param("id") Long id,
                    @Param("totalStock") Integer totalStock,
                    @Param("availableStock") Integer availableStock);

    /**
     * 减少可用库存
     */
    int decreaseAvailableStock(@Param("id") Long id, @Param("count") int count);

    /**
     * 增加可用库存
     */
    int increaseAvailableStock(@Param("id") Long id, @Param("count") int count);

    /**
     * 查询库存预警图书
     */
    List<Book> selectStockWarning(@Param("threshold") int threshold,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);

    /**
     * 统计库存预警数量
     */
    long countStockWarning(@Param("threshold") int threshold);

    /**
     * 更新图书状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
