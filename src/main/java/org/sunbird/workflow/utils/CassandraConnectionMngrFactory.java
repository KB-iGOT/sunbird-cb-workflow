package org.sunbird.workflow.utils;

public class CassandraConnectionMngrFactory {

    private static volatile CassandraConnectionManager instance;

    public static CassandraConnectionManager getInstance() {
        if (instance == null) {
            synchronized (CassandraConnectionMngrFactory.class) {
                if (instance == null) {
                    instance = new CassandraConnectionManagerImpl();
                }
            }
        }
        return instance;
    }
}