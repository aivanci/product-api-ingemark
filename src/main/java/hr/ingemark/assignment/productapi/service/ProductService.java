package hr.ingemark.assignment.productapi.service;

import static hr.ingemark.assignment.productapi.service.ExchangeRateService.PRICE_SCALE;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hr.ingemark.assignment.productapi.dto.ProductRequest;
import hr.ingemark.assignment.productapi.dto.ProductResponse;
import hr.ingemark.assignment.productapi.exception.DuplicateProductCodeException;
import hr.ingemark.assignment.productapi.exception.ProductNotFoundException;
import hr.ingemark.assignment.productapi.model.ProductEntity;
import hr.ingemark.assignment.productapi.repo.ProductRepository;
import hr.ingemark.assignment.productapi.util.ProductMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ExchangeRateService exchangeRateService;
    private final ProductMapper productMapper;

    /**
     * Deliberately NOT annotated with {@code @Transactional}: {@code buildProductFromRequest} may
     * trigger an outbound HTTP call to the HNB API (on exchange rate cache miss), and a remote call
     * must not hold a DB connection open.
     */
    public ProductResponse createProduct(ProductRequest request) {
        assertCodeIsUnique(request.code());
        ProductEntity product = buildProductFromRequest(request);
        ProductEntity saved = saveProduct(product);
        return productMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        ProductEntity product = fetchProduct(id);
        return productMapper.toResponse(product);
    }

    private void assertCodeIsUnique(String code) {
        if (productRepository.existsByCode(code)) {
            throw new DuplicateProductCodeException("Product with code '%s' already exists".formatted(code));
        }
    }

    private ProductEntity saveProduct(ProductEntity product) {
        try {
            return productRepository.save(product);
        } catch (DataIntegrityViolationException ex) {
            throw duplicateCodeException(product.getCode());
        }
    }

    private ProductEntity buildProductFromRequest(ProductRequest request) {
        BigDecimal priceEur = normalizePrice(request.priceEur());
        BigDecimal priceUsd = exchangeRateService.convertEurToUsd(priceEur);
        return new ProductEntity(request.code(), request.name(), priceEur, priceUsd, request.available());
    }

    private BigDecimal normalizePrice(BigDecimal price) {
        // @Digits on the request guarantees at most 2 decimal places, so this only pads the scale
        // (e.g. 100 -> 100.00), keeping the POST response consistent with what NUMERIC(12,2) returns.
        return price.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    private DuplicateProductCodeException duplicateCodeException(String code) {
        return new DuplicateProductCodeException("Product with code '%s' already exists".formatted(code));
    }

    private ProductEntity fetchProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product with id %d not found".formatted(id)));
    }

}
