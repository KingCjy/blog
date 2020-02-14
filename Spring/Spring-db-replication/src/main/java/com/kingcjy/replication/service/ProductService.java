package com.kingcjy.replication.service;

import com.kingcjy.replication.entity.Product;
import com.kingcjy.replication.entity.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductService2 productService2;

    @Transactional(propagation = Propagation.NEVER)
    public void test() {
        log.info("currentTransactionName1 : {}" , TransactionSynchronizationManager.getCurrentTransactionName());
        Product product = getProduct();
        product.setContents(UUID.randomUUID().toString());
    }
    @Transactional(readOnly = true)
    public Product getProduct() {
        log.info("currentTransactionName2 : {}" , TransactionSynchronizationManager.getCurrentTransactionName());
        return productRepository.findById(1L).get();
    }

    public void save(Product product) {
//        productRepository.save(product);
        throw new RuntimeException("A");
    }

    @Transactional(readOnly = true)
    public List<Product> getProducts() {
        return productRepository.findAll();
    }
    @Transactional
    public List<Product> getProductsMaster() {
        return productRepository.findAll();
    }

}
