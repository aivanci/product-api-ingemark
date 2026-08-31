package hr.ingemark.assignment.productapi.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import hr.ingemark.assignment.productapi.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByCode(String code);
}
