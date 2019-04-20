package com.kingcjy.replication.service;

import com.kingcjy.replication.entity.Product;
import com.kingcjy.replication.entity.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<Product> getProducts() {
        return productRepository.findAll();
    }
    @Transactional
    public List<Product> getProductsMaster() {
        return productRepository.findAll();
    }
}
