package com.kingcjy.replication.config;

import com.kingcjy.replication.util.CircularList;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;
import java.util.stream.Collectors;

public class ReplicationRoutingDataSource extends AbstractRoutingDataSource {

    private CircularList<String> dataSourceNameList;

    @Override
    public void setTargetDataSources(Map<Object, Object> targetDataSources) {
        super.setTargetDataSources(targetDataSources);

        dataSourceNameList = new CircularList<>(
                targetDataSources.keySet()
                        .stream()
                        .filter(key -> key.toString().contains("slave"))
                        .map(key -> key.toString())
                        .collect(Collectors.toList())
        );
    }

    @Override
    protected Object determineCurrentLookupKey() {
        boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        if(isReadOnly) {
            return dataSourceNameList.getOne();
        } else {
            return "master";
        }
    }
}
