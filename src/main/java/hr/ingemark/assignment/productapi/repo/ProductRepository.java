package hr.ingemark.assignment.productapi.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import hr.ingemark.assignment.productapi.model.ProductEntity;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    boolean existsByCode(String code);
}
