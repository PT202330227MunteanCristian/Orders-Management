package dao;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import connection.ConnectionFactory;
/**
 * Abstract DAO (Data Access Object) class that provides common database operations for a specific entity type.
 * @param <T> The entity type that the DAO operates on.
 */
public abstract class AbstarctDao<T> {
    protected static final Logger LOGGER = Logger.getLogger(AbstarctDao.class.getName());
    private final Class<T> type;
    /**
     * Constructs an instance of the AbstractDao class.
     */
    public AbstarctDao() {
        this.type = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }
    /**
     * Creates a SELECT query for finding a record by its ID.
     * @param field The field name to search by (typically "id").
     * @return The SELECT query string.
     */
    private String createselectquery(String field) {
        StringBuilder str = new StringBuilder();
        str.append("SELECT ");
        str.append(" * ");
        str.append(" FROM ");
        str.append(type.getSimpleName());
        str.append(" WHERE " + field + " =?");
        return str.toString();
    }
    /**
     * Finds a record by its ID.
     * @param id The ID of the record to find.
     * @return The found entity, or null if not found.
     */
    public T findById(int id) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        String query = createselectquery("id");
        try {
            connection = ConnectionFactory.getConnection();
            statement = connection.prepareStatement(query);
            statement.setInt(1, id);
            resultSet = statement.executeQuery();

            return createobject(resultSet).get(0);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, type.getName() + "DAO:findbyId " + e.getMessage());
        } finally {
            ConnectionFactory.close(resultSet);
            ConnectionFactory.close(statement);
            ConnectionFactory.close(connection);
        }
        return null;
    }
    /**
     * Creates objects from a ResultSet.
     * @param resultset The ResultSet containing the data.
     * @return A list of created objects.
     */
    private List<T> createobject(ResultSet resultset) {
        List<T> list = new ArrayList<T>();
        Constructor[] constrs = type.getDeclaredConstructors();
        Constructor constr = null;
        for (int i = 0; i < constrs.length; i++) {
            constr = constrs[i];
            if (constr.getGenericParameterTypes().length == 0)
                break;
        }
        try {
            while (resultset.next()) {
                constr.setAccessible(true);
                T instance = (T) constr.newInstance();
                for (Field field : type.getDeclaredFields()) {
                    String fieldName = field.getName();
                    Object value = resultset.getObject(fieldName);
                    PropertyDescriptor propertyDescriptor = new PropertyDescriptor(fieldName, type);
                    Method method = propertyDescriptor.getWriteMethod();
                    method.invoke(instance, value);
                }
                list.add(instance);
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IntrospectionException e) {
            e.printStackTrace();
        }
        return list;
    }
    /**
     * Creates a SELECT query for finding all records in the table.
     * @param field A field name to include in the query (optional).
     * @return The SELECT query string.
     */
    private String createfindallquery(String field) {
        StringBuilder str = new StringBuilder();
        str.append("SELECT ");
        str.append(" * ");
        str.append(" FROM `");
        str.append(type.getSimpleName());
        str.append("`");
        return str.toString();
    }
    /**
     * Finds all records in the table.
     * @return A list of all entities in the table, or null if an error occurs.
     */
    public List<T> findAll() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        String query = createfindallquery("");

        try {
            connection = ConnectionFactory.getConnection();
            statement = connection.prepareStatement(query);
            resultSet = statement.executeQuery();
            return createobject(resultSet);

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, type.getName() + "DAO:findall" + e.getMessage());
        }finally {
            ConnectionFactory.close(resultSet);
            ConnectionFactory.close(statement);
            ConnectionFactory.close(connection);
        }
        return null;
    }
    /**
     * Creates an INSERT query for inserting a new record.
     * @param field A field name to include in the query (optional).
     * @return The INSERT query string.
     */
    private String createinsertquery(String field) {
        StringBuilder str = new StringBuilder();
        str.append("insert into ");
        str.append(type.getSimpleName());
        str.append(" values (");
        return str.toString();
    }
    /**
     * Inserts a new record into the table.
     * @param t The entity to insert.
     */
    public void insert(T t) {
        String query = createinsertquery("");
        Connection conn = null;
        PreparedStatement stm = null;

        try {
            for (Field fld : type.getDeclaredFields()) {
                fld.setAccessible(true);
                Object val = fld.get(t);
                if (val instanceof String) {
                    query = query + "'" + val.toString() + "',";
                } else {
                    query = query + val.toString() + ",";
                }
            }
            query = query.substring(0, query.length() - 1) + ")";
            conn = ConnectionFactory.getConnection();
            stm = conn.prepareStatement(query);
            stm.execute();
        } catch (SQLException | IllegalAccessException e) {
            LOGGER.log(Level.WARNING, type.getName() + "DAO:insert " + e.getMessage());
        } finally {
            ConnectionFactory.close(stm);
            ConnectionFactory.close(conn);
        }
    }
    /**
     * Creates an UPDATE query for updating an existing record.
     * @param field A field name to include in the query (optional).
     * @return The UPDATE query string.
     */
    private String createupdatequery(String field) {
        StringBuilder str = new StringBuilder();
        str.append("update ");
        str.append(type.getSimpleName());
        str.append(" set ");
        return str.toString();
    }
    /**
     * Updates an existing record in the table.
     * @param t The entity to update.
     */
    public void update(T t) {
        String query = createupdatequery("");
        Connection conn = null;
        PreparedStatement stm = null;

        int id = 0;
        try {
            for (Field fld : type.getDeclaredFields()) {
                fld.setAccessible(true);
                Object val = fld.get(t);
                if (fld.getName().equals("id")) {
                    id = (int) val;
                } else {
                    if (val instanceof String)
                        query = query + fld.getName() + "='" + val.toString() + "',";
                    else
                        query = query + fld.getName() + "=" + val.toString() + ",";
                }
            }
            query = query.substring(0, query.length() - 1) + " where id=" + id;
            conn = ConnectionFactory.getConnection();
            stm = conn.prepareStatement(query);
            stm.execute();
        } catch (SQLException | IllegalAccessException e) {
            LOGGER.log(Level.WARNING, type.getName() + "DAO:insert " + e.getMessage());
        } finally {
            ConnectionFactory.close(stm);
            ConnectionFactory.close(conn);
        }
    }
    /**
     * Creates a DELETE query for deleting a record.
     * @param field A field name to include in the query (optional).
     * @return The DELETE query string.
     */
    private String createdeletequery(String field) {
        StringBuilder str = new StringBuilder();
        str.append("DELETE FROM `");
        str.append(type.getSimpleName());
        str.append("` WHERE ID=");
        return str.toString();
    }
    /**
     * Deletes a record from the table.
     * @param t The entity to delete.
     */
    public void delete(T t) {
        String query = createdeletequery("");
        Connection conn = null;
        PreparedStatement stm = null;
        int id = 0;
        try {
            for (Field fld : type.getDeclaredFields()) {
                fld.setAccessible(true);
                Object val = fld.get(t);
                if (fld.getName().equals("id")) {
                    id = (int) val;
                }
            }
            query = query + id;
            conn = ConnectionFactory.getConnection();
            stm = conn.prepareStatement(query);
            stm.execute();
        } catch (SQLException | IllegalAccessException e) {
            LOGGER.log(Level.WARNING, type.getName() + "DAO:insert " + e.getMessage());
        } finally {
            ConnectionFactory.close(stm);
            ConnectionFactory.close(conn);
        }
    }
    /**
     * Retrieves the header of the table.
     * @return An array of column names as the header of the table.
     */
    public Object[] getheaderoftable() {
        Field[] fields = type.getDeclaredFields();
        Object[] hdr = new Object[fields.length];
        int i = 0;
        while (i < fields.length) {
            String nm = fields[i].getName();
            hdr[i] = nm;
            i++;
        }
        return hdr;
    }
    /**
     * Retrieves the table data as a 2D array.
     * @param lists The list of entities to be included in the table.
     * @return A 2D array representing the table data.
     */
    public Object[][] gettable(List<T> lists) {
        Field[] fld = type.getDeclaredFields();
        Object[][] tbl = new Object[lists.size()][fld.length];
        try {
            for (int i = 0; i < lists.size(); i++) {
                for (int j = 0; j < fld.length; j++) {
                    fld[j].setAccessible(true);
                    tbl[i][j] = fld[j].get(lists.get(i));
                }
            }
            return tbl;
        } catch (IllegalAccessException e) {
            LOGGER.log(Level.WARNING, type.getName() + "DAO gettable" + e.getMessage());
        }
        return null;
    }
}
