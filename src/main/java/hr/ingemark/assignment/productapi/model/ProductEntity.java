package hr.ingemark.assignment.productapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "price_eur", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceEur;

    @Column(name = "price_usd", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceUsd;

    @Column(name = "is_available", nullable = false)
    private boolean available;

    public ProductEntity(String code, String name, BigDecimal priceEur, BigDecimal priceUsd, boolean available) {
        this.code = code;
        this.name = name;
        this.priceEur = priceEur;
        this.priceUsd = priceUsd;
        this.available = available;
    }
}
