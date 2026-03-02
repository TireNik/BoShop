package com.kika.product_service.repository;

import com.kika.product_service.dto.ProductFilterDto;
import com.kika.product_service.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductSearchRepository {

    private final MongoTemplate mongoTemplate;

    public Page<Product> search(ProductFilterDto filter) {
        Query query = new Query();

        if (filter.getName() != null && !filter.getName().isBlank()) {
            query.addCriteria(Criteria.where("name").regex(filter.getName(), "i"));
        }

        if (filter.getCategory() != null && !filter.getCategory().isBlank()) {
            query.addCriteria(Criteria.where("category").is(filter.getCategory()));
        }

        if (filter.getPriceFrom() != null && filter.getPriceTo() != null) {
            query.addCriteria(Criteria.where("price")
                    .gte(filter.getPriceFrom())
                    .lte(filter.getPriceTo()));
        } else if (filter.getPriceFrom() != null) {
            query.addCriteria(Criteria.where("price").gte(filter.getPriceFrom()));
        } else if (filter.getPriceTo() != null) {
            query.addCriteria(Criteria.where("price").lte(filter.getPriceTo()));
        }


        Pageable pageable = PageRequest.of(
                filter.getPage(),
                filter.getSize()
        );

        long total = mongoTemplate.count(query, Product.class);
        query.with(pageable);

        List<Product> products = mongoTemplate.find(query, Product.class);

        return new PageImpl<>(products, pageable, total);
    }
}
