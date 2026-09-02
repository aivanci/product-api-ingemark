package hr.ingemark.assignment.productapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.test.util.ReflectionTestUtils;

import hr.ingemark.assignment.productapi.dto.PageResponse;
import hr.ingemark.assignment.productapi.dto.ProductRequest;
import hr.ingemark.assignment.productapi.dto.ProductResponse;
import hr.ingemark.assignment.productapi.exception.DuplicateProductCodeException;
import hr.ingemark.assignment.productapi.exception.InvalidSortPropertyException;
import hr.ingemark.assignment.productapi.exception.ProductNotFoundException;
import hr.ingemark.assignment.productapi.model.ProductEntity;
import hr.ingemark.assignment.productapi.repo.ProductRepository;
import hr.ingemark.assignment.productapi.util.ProductMapper;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final String VALID_CODE = "ABCDEFGHIJ";

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ExchangeRateService exchangeRateService;

    private final ProductMapper productMapper = Mappers.getMapper(ProductMapper.class);

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, exchangeRateService, productMapper);
    }

    @Test
    void createProduct_savesProductWithServerCalculatedUsdPrice() {
        ProductRequest request = new ProductRequest(VALID_CODE, "Widget", new BigDecimal("100.00"), true);
        given(productRepository.existsByCode(VALID_CODE)).willReturn(false);
        given(exchangeRateService.convertEurToUsd(new BigDecimal("100.00"))).willReturn(new BigDecimal("108.50"));
        given(productRepository.save(any(ProductEntity.class))).willAnswer(this::withGeneratedId);

        ProductResponse response = productService.createProduct(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.code()).isEqualTo(VALID_CODE);
        assertThat(response.priceEur()).isEqualByComparingTo("100.00");
        assertThat(response.priceUsd()).isEqualByComparingTo("108.50");
        assertThat(response.available()).isTrue();
        verify(productRepository).save(any(ProductEntity.class));
    }

    @Test
    void createProduct_throwsWhenCodeAlreadyExists() {
        ProductRequest request = new ProductRequest(VALID_CODE, "Widget", new BigDecimal("100.00"), true);
        given(productRepository.existsByCode(VALID_CODE)).willReturn(true);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(DuplicateProductCodeException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    void getProduct_returnsMappedResponseWhenFound() {
        ProductEntity product = new ProductEntity(VALID_CODE, "Widget", new BigDecimal("100.00"), new BigDecimal("108.50"), true);
        ReflectionTestUtils.setField(product, "id", 1L);
        given(productRepository.findById(1L)).willReturn(Optional.of(product));

        ProductResponse response = productService.getProduct(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.code()).isEqualTo(VALID_CODE);
    }

    @Test
    void getProduct_throwsWhenNotFound() {
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(99L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void listProducts_mapsPageOfProductsToResponses() {
        ProductEntity product = new ProductEntity(VALID_CODE, "Widget", new BigDecimal("100.00"), new BigDecimal("108.50"), true);
        ReflectionTestUtils.setField(product, "id", 1L);
        PageRequest pageable = PageRequest.of(0, 20);
        given(productRepository.findAll(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(product), pageable, 1));

        PageResponse<ProductResponse> response = productService.listProducts(pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).code()).isEqualTo(VALID_CODE);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
    }

    @Test
    void listProducts_translatesWireSortNamesToEntityProperties() {
        given(productRepository.findAll(any(Pageable.class))).willReturn(Page.empty());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        productService.listProducts(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "price_eur")));

        verify(productRepository).findAll(pageableCaptor.capture());
        Order order = pageableCaptor.getValue().getSort().getOrderFor("priceEur");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void listProducts_appliesDeterministicDefaultSortWhenUnsorted() {
        given(productRepository.findAll(any(Pageable.class))).willReturn(Page.empty());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        productService.listProducts(PageRequest.of(0, 20));

        verify(productRepository).findAll(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("id")).isNotNull();
    }

    @Test
    void listProducts_rejectsUnknownSortProperty() {
        assertThatThrownBy(() -> productService.listProducts(PageRequest.of(0, 20, Sort.by("bogus"))))
                .isInstanceOf(InvalidSortPropertyException.class);
    }


    private ProductEntity withGeneratedId(InvocationOnMock invocation) {
        ProductEntity product = invocation.getArgument(0);
        ReflectionTestUtils.setField(product, "id", 1L);
        return product;
    }
}
