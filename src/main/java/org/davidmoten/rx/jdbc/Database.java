package org.davidmoten.rx.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;

import org.davidmoten.rx.pool.Pool;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.functions.Action;

public class Database implements AutoCloseable {

    private final Flowable<Connection> connections;

    private final Action onClose;

    private Database(Flowable<Connection> connections, Action onClose) {
        this.connections = connections;
        this.onClose = onClose;
    }

    public static Database from(Flowable<Connection> connections, Action onClose) {
        return new Database(connections, onClose);
    }

    public static Database from(Pool<Connection> pool) {
        return new Database(pool.members().cast(Connection.class), () -> pool.close());
    }

    public Flowable<Connection> connections() {
        return connections;
    }

    @Override
    public void close() throws Exception {
        onClose.run();
    }

    public SelectBuilder select(String sql) {
        return new SelectBuilder(sql, connections());
    }

    public TransactedBuilder<Object> transacted() {
        // TODO
        return new TransactedBuilder<Object>(() -> null);
    }

    public static <T> Database tx(Tx<T> tx) {
        TxImpl<T> t = (TxImpl<T>) tx;
        TransactedConnection c = t.connection().fork();
        return new Database(Flowable.<Connection>just(c), () ->{});
    }

    public static final Object NULL_CLOB = new Object();

    public static final Object NULL_NUMBER = new Object();

    public static Object toSentinelIfNull(String s) {
        if (s == null)
            return NULL_CLOB;
        else
            return s;
    }

    /**
     * Sentinel object used to indicate in parameters of a query that rather
     * than calling {@link PreparedStatement#setObject(int, Object)} with a null
     * we call {@link PreparedStatement#setNull(int, int)} with
     * {@link Types#CLOB}. This is required by many databases for setting CLOB
     * and BLOB fields to null.
     */
    public static final Object NULL_BLOB = new Object();

    public static Object toSentinelIfNull(byte[] bytes) {
        if (bytes == null)
            return NULL_BLOB;
        else
            return bytes;
    }

}
