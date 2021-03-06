package com.palmap.exhibition.dao;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.internal.DaoConfig;

import com.palmap.exhibition.dao.CoordinateModel;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table COORDINATE_MODEL.
*/
public class CoordinateModelDao extends AbstractDao<CoordinateModel, Long> {

    public static final String TABLENAME = "COORDINATE_MODEL";

    /**
     * Properties of entity CoordinateModel.<br/>
     * Can be used for QueryBuilder and for referencing column names.
    */
    public static class Properties {
        public final static Property Id = new Property(0, Long.class, "id", true, "_id");
        public final static Property X = new Property(1, double.class, "x", false, "X");
        public final static Property Y = new Property(2, double.class, "y", false, "Y");
        public final static Property Z = new Property(3, double.class, "z", false, "Z");
        public final static Property PointId = new Property(4, String.class, "pointId", false, "POINT_ID");
        public final static Property AtyId = new Property(5, String.class, "atyId", false, "ATY_ID");
    };


    public CoordinateModelDao(DaoConfig config) {
        super(config);
    }
    
    public CoordinateModelDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(SQLiteDatabase db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "'COORDINATE_MODEL' (" + //
                "'_id' INTEGER PRIMARY KEY ," + // 0: id
                "'X' REAL NOT NULL ," + // 1: x
                "'Y' REAL NOT NULL ," + // 2: y
                "'Z' REAL NOT NULL ," + // 3: z
                "'POINT_ID' TEXT NOT NULL UNIQUE ," + // 4: pointId
                "'ATY_ID' TEXT NOT NULL );"); // 5: atyId
    }

    /** Drops the underlying database table. */
    public static void dropTable(SQLiteDatabase db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "'COORDINATE_MODEL'";
        db.execSQL(sql);
    }

    /** @inheritdoc */
    @Override
    protected void bindValues(SQLiteStatement stmt, CoordinateModel entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
        stmt.bindDouble(2, entity.getX());
        stmt.bindDouble(3, entity.getY());
        stmt.bindDouble(4, entity.getZ());
        stmt.bindString(5, entity.getPointId());
        stmt.bindString(6, entity.getAtyId());
    }

    /** @inheritdoc */
    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }    

    /** @inheritdoc */
    @Override
    public CoordinateModel readEntity(Cursor cursor, int offset) {
        CoordinateModel entity = new CoordinateModel( //
            cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // id
            cursor.getDouble(offset + 1), // x
            cursor.getDouble(offset + 2), // y
            cursor.getDouble(offset + 3), // z
            cursor.getString(offset + 4), // pointId
            cursor.getString(offset + 5) // atyId
        );
        return entity;
    }
     
    /** @inheritdoc */
    @Override
    public void readEntity(Cursor cursor, CoordinateModel entity, int offset) {
        entity.setId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setX(cursor.getDouble(offset + 1));
        entity.setY(cursor.getDouble(offset + 2));
        entity.setZ(cursor.getDouble(offset + 3));
        entity.setPointId(cursor.getString(offset + 4));
        entity.setAtyId(cursor.getString(offset + 5));
     }
    
    /** @inheritdoc */
    @Override
    protected Long updateKeyAfterInsert(CoordinateModel entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }
    
    /** @inheritdoc */
    @Override
    public Long getKey(CoordinateModel entity) {
        if(entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    /** @inheritdoc */
    @Override    
    protected boolean isEntityUpdateable() {
        return true;
    }
    
}
