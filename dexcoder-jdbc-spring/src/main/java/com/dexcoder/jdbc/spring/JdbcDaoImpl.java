package com.dexcoder.jdbc.spring;

import com.dexcoder.jdbc.*;
import com.dexcoder.jdbc.build.Criteria;
import com.dexcoder.jdbc.utils.ClassUtils;
import com.dexcoder.jdbc.utils.NameUtils;
import com.dexcoder.jdbc.utils.StrUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.util.CollectionUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * jdbc操作dao
 * <p/>
 * Created by liyd on 3/3/15.
 */
@SuppressWarnings("unchecked")
public class JdbcDaoImpl implements JdbcDao {

    /**
     * spring jdbcTemplate 对象
     */
    protected JdbcOperations jdbcTemplate;

    /**
     * 名称处理器，为空按默认执行
     */
    protected NameHandler nameHandler;

    /**
     * rowMapper，为空按默认执行
     */
    protected String rowMapperClass;

    /**
     * 自定义sql处理
     */
    protected SqlFactory sqlFactory;

    /**
     * 数据库方言
     */
    protected String dialect;

    /**
     * 插入数据
     *
     * @param entity   the entity
     * @param criteria the criteria
     * @return long long
     */
    private Long insert(Object entity, Criteria criteria) {
        Class<?> entityClass = SqlUtils.getEntityClass(entity, criteria);
        NameHandler handler = this.getNameHandler();
        String pkValue = handler.getPKValue(entityClass, this.dialect);
        if (StrUtils.isNotBlank(pkValue)) {
            String primaryName = handler.getPKName(entityClass);
            if (criteria == null) {
                criteria = Criteria.create(entityClass);
            }
            criteria.setPKValueName(NameUtils.getCamelName(primaryName), pkValue);
        }
        final BoundSql boundSql = SqlUtils.buildInsertSql(entity, criteria, this.getNameHandler());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(new PreparedStatementCreator() {
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                PreparedStatement ps = con.prepareStatement(boundSql.getSql(),
                        new String[]{boundSql.getPkName()});
                int index = 0;
                for (Object param : boundSql.getParameters()) {
                    index++;
                    ps.setObject(index, param);
                }
                return ps;
            }
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Long insert(Object entity) {
        NameHandler handler = this.getNameHandler();
        Criteria criteria = Criteria.insert(entity.getClass());
        String pkValue = handler.getPKValue(entity.getClass(), this.dialect);
        if (StrUtils.isNotBlank(pkValue)) {
            String primaryName = handler.getPKName(entity.getClass());
            criteria.setPKValueName(NameUtils.getCamelName(primaryName), pkValue);
        }
        return this.insert(entity, null);
    }

    public Long insert(Criteria criteria) {
        return this.insert(null, criteria);
    }

    public void save(Object entity) {
        final BoundSql boundSql = SqlUtils.buildInsertSql(entity, null, this.getNameHandler());
        jdbcTemplate.update(boundSql.getSql(), boundSql.getParameters().toArray());
    }

    public void save(Criteria criteria) {
        final BoundSql boundSql = SqlUtils.buildInsertSql(null, criteria, this.getNameHandler());
        jdbcTemplate.update(boundSql.getSql(), boundSql.getParameters().toArray());
    }

    public void update(Criteria criteria) {
        BoundSql boundSql = SqlUtils.buildUpdateSql(null, criteria, this.getNameHandler());
        jdbcTemplate.update(boundSql.getSql(), boundSql.getParameters().toArray());
    }

    public void update(Object entity) {
        BoundSql boundSql = SqlUtils.buildUpdateSql(entity, null, this.getNameHandler());
        jdbcTemplate.update(boundSql.getSql(), boundSql.getParameters().toArray());
    }

    public void delete(Criteria criteria) {
        BoundSql boundSql = SqlUtils.buildDeleteSql(null, criteria, this.getNameHandler());
        jdbcTemplate.update(boundSql.getSql(), boundSql.getParameters().toArray());
    }

    public void delete(Object entity) {
        BoundSql boundSql = SqlUtils.buildDeleteSql(entity, null, this.getNameHandler());
        jdbcTemplate.update(boundSql.getSql(), boundSql.getParameters().toArray());
    }

    public void delete(Class<?> clazz, Long id) {
        BoundSql boundSql = SqlUtils.buildDeleteSql(clazz, id, this.getNameHandler());
        jdbcTemplate.update(boundSql.getSql(), boundSql.getParameters().toArray());
    }

    public void deleteAll(Class<?> clazz) {
        String tableName = this.getNameHandler().getTableName(clazz, null);
        String sql = "TRUNCATE TABLE " + tableName;
        jdbcTemplate.execute(sql);
    }

    public <T> List<T> queryList(Criteria criteria) {
        BoundSql boundSql = SqlUtils.buildListSql(null, criteria, this.getNameHandler());
        List<?> list = jdbcTemplate.query(boundSql.getSql(), boundSql.getParameters().toArray(),
                this.getRowMapper(criteria.getEntityClass()));
        return (List<T>) list;
    }

    public <T> List<T> queryList(Class<?> clazz) {
        BoundSql boundSql = SqlUtils.buildListSql(null, Criteria.create(clazz),
                this.getNameHandler());
        List<?> list = jdbcTemplate.query(boundSql.getSql(), boundSql.getParameters().toArray(),
                this.getRowMapper(clazz));
        return (List<T>) list;
    }

    public <T> List<T> queryList(T entity) {
        BoundSql boundSql = SqlUtils.buildListSql(entity, null, this.getNameHandler());
        List<?> list = jdbcTemplate.query(boundSql.getSql(), boundSql.getParameters().toArray(),
                this.getRowMapper(entity.getClass()));
        return (List<T>) list;
    }

    public <T> List<T> queryList(T entity, Criteria criteria) {
        BoundSql boundSql = SqlUtils.buildListSql(entity, criteria, this.getNameHandler());
        List<?> list = jdbcTemplate.query(boundSql.getSql(), boundSql.getParameters().toArray(),
                this.getRowMapper(entity.getClass()));
        return (List<T>) list;
    }

    public int queryCount(Object entity, Criteria criteria) {
        BoundSql boundSql = SqlUtils.buildCountSql(entity, criteria, this.getNameHandler());
        return jdbcTemplate.queryForInt(boundSql.getSql(), boundSql.getParameters().toArray());
    }

    public int queryCount(Object entity) {
        BoundSql boundSql = SqlUtils.buildCountSql(entity, null, this.getNameHandler());
        return jdbcTemplate.queryForInt(boundSql.getSql(), boundSql.getParameters().toArray());
    }

    public int queryCount(Criteria criteria) {
        BoundSql boundSql = SqlUtils.buildCountSql(null, criteria, this.getNameHandler());
        return jdbcTemplate.queryForInt(boundSql.getSql(), boundSql.getParameters().toArray());
    }

    public <T> T get(Class<T> clazz, Long id) {
        BoundSql boundSql = SqlUtils.buildByIdSql(clazz, id, null, this.getNameHandler());

        //采用list方式查询，当记录不存在时返回null而不会抛出异常
        List<T> list = jdbcTemplate.query(boundSql.getSql(), this.getRowMapper(clazz), id);
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return list.iterator().next();
    }

    public <T> T get(Criteria criteria, Long id) {
        BoundSql boundSql = SqlUtils.buildByIdSql(null, id, criteria, this.getNameHandler());

        //采用list方式查询，当记录不存在时返回null而不会抛出异常
        List<T> list = (List<T>) jdbcTemplate.query(boundSql.getSql(),
                this.getRowMapper(criteria.getEntityClass()), id);
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return list.iterator().next();
    }

    public <T> T querySingleResult(T entity) {
        BoundSql boundSql = SqlUtils.buildQuerySql(entity, null, this.getNameHandler());

        //采用list方式查询，当记录不存在时返回null而不会抛出异常
        List<?> list = jdbcTemplate.query(boundSql.getSql(), boundSql.getParameters().toArray(),
                this.getRowMapper(entity.getClass()));
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return (T) list.iterator().next();
    }

    public <T> T querySingleResult(Criteria criteria) {
        BoundSql boundSql = SqlUtils.buildQuerySql(null, criteria, this.getNameHandler());
        //采用list方式查询，当记录不存在时返回null而不会抛出异常
        List<?> list = jdbcTemplate.query(boundSql.getSql(), boundSql.getParameters().toArray(),
                this.getRowMapper(criteria.getEntityClass()));
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return (T) list.iterator().next();
    }

    public List<Map<String, Object>> queryForSql(String sqlId, Map<String, Object> params) {
        this.sqlFactory.getBoundSql("User", sqlId, params);
        return null;
    }

    public List<Map<String, Object>> queryForSql(String sqlId, String name, Object object) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(name, object);
        return this.queryForSql(sqlId, map);
    }
//
//    public void updateForSql(String sqlId, Map<String, Object> params) {
//        BoundSql boundSql = this.getSqlHandler().getSql(sqlId, params);
//        jdbcTemplate.update(boundSql.getSql(), boundSql.getParams().toArray());
//    }
//
//    public void updateForSql(String sqlId, String name, Object object) {
//        Map<String, Object> map = new HashMap<String, Object>();
//        map.put(name, object);
//        this.updateForSql(sqlId, map);
//    }

    public byte[] getBlobValue(Class<?> clazz, String fieldName, Long id) {
        String primaryName = nameHandler.getPKName(clazz);
        String columnName = nameHandler.getColumnName(fieldName);
        String tableName = nameHandler.getTableName(clazz, null);
        String tmp_sql = "select t.%s from %s t where t.%s = ?";
        String sql = String.format(tmp_sql, columnName, tableName, primaryName);
        return jdbcTemplate.query(sql, new Object[]{id}, new ResultSetExtractor<byte[]>() {
            public byte[] extractData(ResultSet rs) throws SQLException, DataAccessException {
                if (rs.next()) {
                    return rs.getBytes(1);
                }
                return null;
            }
        });
    }

    /**
     * 获取rowMapper对象
     *
     * @param clazz
     * @return
     */
    protected <T> RowMapper<T> getRowMapper(Class<T> clazz) {

        if (StrUtils.isBlank(rowMapperClass)) {
            return BeanPropertyRowMapper.newInstance(clazz);
        } else {
            return (RowMapper<T>) ClassUtils.newInstance(rowMapperClass);
        }
    }

    /**
     * 获取名称处理器
     *
     * @return
     */
    protected NameHandler getNameHandler() {

        if (this.nameHandler == null) {
            this.nameHandler = new DefaultNameHandler();
        }
        return this.nameHandler;
    }

    public void setJdbcTemplate(JdbcOperations jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setNameHandler(NameHandler nameHandler) {
        this.nameHandler = nameHandler;
    }

    public void setRowMapperClass(String rowMapperClass) {
        this.rowMapperClass = rowMapperClass;
    }

    public void setDialect(String dialect) {
        this.dialect = dialect;
    }

    public void setSqlFactory(SqlFactory sqlFactory) {
        this.sqlFactory = sqlFactory;
    }
}