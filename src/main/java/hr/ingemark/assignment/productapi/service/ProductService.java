package hr.ingemark.assignment.productapi.service;

import static hr.ingemark.assignment.productapi.service.ExchangeRateService.PRICE_SCALE;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hr.ingemark.assignment.productapi.dto.PageResponse;
import hr.ingemark.assignment.productapi.dto.ProductRequest;
import hr.ingemark.assignment.productapi.dto.ProductResponse;
import hr.ingemark.assignment.productapi.exception.DuplicateProductCodeException;
import hr.ingemark.assignment.productapi.exception.InvalidSortPropertyException;
import hr.ingemark.assignment.productapi.exception.ProductNotFoundException;
import hr.ingemark.assignment.productapi.model.ProductEntity;
import hr.ingemark.assignment.productapi.repo.ProductRepository;
import hr.ingemark.assignment.productapi.util.ProductMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "id");

    // Wire field name (as documented in the API contract) -> entity property name.
    // Clients sort by the names they see in responses; unknown names get a 400, never a 500.
    private static final Map<String, String> SORTABLE_PROPERTIES = Map.of(
            "id", "id",
            "code", "code",
            "name", "name",
            "price_eur", "priceEur",
            "price_usd", "priceUsd",
            "is_available", "available"
    );

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

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> listProducts(Pageable pageable) {
        Page<ProductResponse> page = productRepository.findAll(translateSort(pageable))
                .map(productMapper::toResponse);
        return PageResponse.of(page);
    }

    private Pageable translateSort(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            // Deterministic default ordering
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), DEFAULT_SORT);
        }

        List<Order> translatedOrders = new ArrayList<>();
        for (Order order : pageable.getSort()) {
            translatedOrders.add(new Order(order.getDirection(), entityPropertyFor(order.getProperty())));
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(translatedOrders));
    }

    private String entityPropertyFor(String wireName) {
        String entityProperty = SORTABLE_PROPERTIES.get(wireName);
        if (entityProperty == null) {
            throw new InvalidSortPropertyException(
                    "Unknown sort property '%s'. Sortable properties: %s"
                            .formatted(wireName, sortablePropertyNames()));
        }
        return entityProperty;
    }

    private String sortablePropertyNames() {
        return SORTABLE_PROPERTIES.keySet().stream().sorted().collect(Collectors.joining(", "));
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
