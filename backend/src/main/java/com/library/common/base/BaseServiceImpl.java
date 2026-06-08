package com.library.common.base;

import com.library.common.response.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用Service实现基类
 *
 * @param <M> Mapper类型
 * @param <T> 实体类型
 * @author Library System
 * @since 1.0.0
 */
public abstract class BaseServiceImpl<M extends BaseMapper<T>, T> implements BaseService<T> {

    @Autowired
    protected M baseMapper;

    @Override
    public T getById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public List<T> listAll() {
        return baseMapper.selectAll();
    }

    @Override
    public List<T> listByCondition(Map<String, Object> params) {
        if (params == null) {
            params = new HashMap<>();
        }
        return baseMapper.selectByCondition(params);
    }

    @Override
    public PageResult<T> page(Map<String, Object> params, int pageNum, int pageSize) {
        if (params == null) {
            params = new HashMap<>();
        }
        // 计算偏移量
        int offset = (pageNum - 1) * pageSize;
        // 查询数据
        List<T> list = baseMapper.selectPage(params, offset, pageSize);
        // 查询总数
        long total = baseMapper.countByCondition(params);
        // 返回分页结果
        return PageResult.of(list, total, pageNum, pageSize);
    }

    @Override
    public boolean save(T entity) {
        return baseMapper.insert(entity) > 0;
    }

    @Override
    public boolean saveBatch(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return true;
        }
        return baseMapper.batchInsert(entities) > 0;
    }

    @Override
    public boolean updateById(T entity) {
        return baseMapper.updateById(entity) > 0;
    }

    @Override
    public boolean removeById(Long id) {
        return baseMapper.deleteById(id) > 0;
    }

    @Override
    public boolean removeByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return true;
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    public long count(Map<String, Object> params) {
        if (params == null) {
            params = new HashMap<>();
        }
        return baseMapper.countByCondition(params);
    }
}
