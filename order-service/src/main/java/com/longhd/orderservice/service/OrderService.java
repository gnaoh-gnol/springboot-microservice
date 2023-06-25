package com.longhd.orderservice.service;

import com.longhd.orderservice.dto.InventoryResponse;
import com.longhd.orderservice.dto.OrderItemDto;
import com.longhd.orderservice.dto.OrderRequest;
import com.longhd.orderservice.model.Order;
import com.longhd.orderservice.model.OrderItems;
import com.longhd.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;

    public void placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        List<OrderItems> orderItems = orderRequest.getOrderItems()
                .stream()
                .map(this::mapOrderDtoToOrder)
                .toList();
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setOrderItems(orderItems);

        List<String> skuCodes = order.getOrderItems().stream().map(OrderItems::getSkuCode).toList();

        // Call inventory service and place order if product is in stock
        InventoryResponse[] inventoryResponses = webClientBuilder.build().get()
                .uri("http://inventory-service/api/v1/inventory",
                        uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();

        boolean allProductInStock = Arrays.stream(inventoryResponses).allMatch(InventoryResponse::isInStock);
        if (allProductInStock) {
            orderRepository.save(order);
        } else  {
            throw new IllegalArgumentException("Product is not in stock, please try again later.");
        }

    }

    private OrderItems mapOrderDtoToOrder(OrderItemDto orderItemDto) {
        OrderItems orderItems = new OrderItems();
        orderItems.setPrice(orderItemDto.getPrice());
        orderItems.setQuantity(orderItemDto.getQuantity());
        orderItems.setSkuCode(orderItemDto.getSkuCode());
        return orderItems;
    }
}
