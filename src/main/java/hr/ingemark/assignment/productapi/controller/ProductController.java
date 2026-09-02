package hr.ingemark.assignment.productapi.controller;

import java.net.URI;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hr.ingemark.assignment.productapi.dto.PageResponse;
import hr.ingemark.assignment.productapi.dto.ProductRequest;
import hr.ingemark.assignment.productapi.dto.ProductResponse;
import hr.ingemark.assignment.productapi.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Product catalog management")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Create a product",
            description = "price_usd is computed server-side from price_eur and the current HNB "
                    + "USD middle rate; it cannot be supplied by the client.")
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.created(locationOf(response)).body(response);
    }

    @Operation(summary = "Get a product by id")
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @Operation(summary = "List products",
            description = "Paginated. Sort by wire field names, e.g. ?sort=price_eur,desc&sort=name,asc. "
                    + "Unknown sort properties are rejected with 400.")
    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> listProducts(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(productService.listProducts(pageable));
    }


    private URI locationOf(ProductResponse response) {
        return URI.create("/api/v1/products/" + response.id());
    }
}
