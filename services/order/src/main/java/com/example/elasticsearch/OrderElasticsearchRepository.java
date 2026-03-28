package com.example.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderElasticsearchRepository extends ElasticsearchRepository<OrderDocument, String> {
    List<OrderDocument> findByReference(String reference);
    List<OrderDocument> findByCustomerId(Integer customerId);
    List<OrderDocument> findByStatus(String status);
}

