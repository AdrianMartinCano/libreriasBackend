package dev.pimon.ecommerce.web;

import lombok.RequiredArgsConstructor;
import dev.pimon.common.dto.ApiResponse;
import dev.pimon.common.dto.PageResponse;
import dev.pimon.ecommerce.dto.OrderDto;
import dev.pimon.ecommerce.service.OrderService;
import dev.pimon.security.annotation.CurrentUser;
import dev.pimon.security.model.AuthUser;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Historial de pedidos del usuario autenticado */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PageResponse<OrderDto>>> myOrders(
            @CurrentUser AuthUser user,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.myOrders(user.id(), pageable)));
    }

    @GetMapping("/my/{id}")
    public ResponseEntity<ApiResponse<OrderDto>> myOrder(
            @PathVariable String id,
            @CurrentUser AuthUser user) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getMyOrder(id, user.id())));
    }
}
