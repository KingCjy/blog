package com.kingcjy.replication.service;

import com.kingcjy.replication.entity.Product;
import com.kingcjy.replication.entity.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService2 {

    private final ProductRepository productRepository;


    public Product getProduct() {
        log.info("currentTransactionName2 : {}" , TransactionSynchronizationManager.getCurrentTransactionName());
        return productRepository.findById(1L).get();
    }
}
