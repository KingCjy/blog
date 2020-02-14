package com.kingcjy.replication.controller;

import com.kingcjy.replication.entity.Product;
import com.kingcjy.replication.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("")
    public ResponseEntity<?> getProducts() {
        List<Product> productList = productService.getProducts();
        return new ResponseEntity<>(productList, HttpStatus.OK);
    }
    @GetMapping("/master")
    public ResponseEntity<?> getProductsFromMaster() {
        List<Product> productList = productService.getProductsMaster();
        return new ResponseEntity<>(productList, HttpStatus.OK);
    }

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        productService.test();

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
