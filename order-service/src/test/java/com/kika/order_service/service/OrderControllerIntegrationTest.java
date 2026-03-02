package com.kika.order_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kika.inventory.grpc.Inventory;
import com.kika.inventory.grpc.InventoryServiceGrpc;
import com.kika.order_service.config.JwtUserContextFilter;
import com.kika.order_service.dto.OrderItemDto;
import com.kika.order_service.entity.Order;
import com.kika.order_service.entity.Status;
import com.kika.order_service.repository.OrderItemRepository;
import com.kika.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class OrderControllerIntegrationTest {

 @Autowired
 private MockMvc mockMvc;

 @Autowired
 private ObjectMapper objectMapper;

 @Autowired
 private OrderRepository orderRepository;

 @Autowired
 private OrderItemRepository orderItemRepository;

 @MockBean
 private InventoryServiceGrpc.InventoryServiceBlockingStub inventoryStub;

 @MockBean
 private KafkaTemplate<String, Object> kafkaTemplate;

 @MockBean
 private JwtUserContextFilter jwtUserContextFilter;

 @BeforeEach
 void setUp() {
  orderItemRepository.deleteAll();
  orderRepository.deleteAll();
 }

 @Test
 void createOrderPublic_shouldCreateNewOrder() throws Exception {
  when(jwtUserContextFilter.getCurrentUserId()).thenReturn(100L);

  mockMvc.perform(post("/api/v1/orders/public"))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.status").value("NEW"))
          .andExpect(jsonPath("$.userId").value(100L));
 }

 @Test
 void addProductPublic_shouldAddItem() throws Exception {
  Order order = orderRepository.save(Order.builder().userId(1L).status(Status.NEW).totalPrice(0.0).build());

  OrderItemDto dto = new OrderItemDto("prod1", 2, 50.0);

  mockMvc.perform(post("/api/v1/orders/public/{id}/add-product", order.getId())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(dto)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalPrice").value(100.0));
 }

 @Test
 void reserveManager_shouldReserveSuccessfully() throws Exception {
  Order order = orderRepository.save(Order.builder().userId(1L).status(Status.NEW).totalPrice(100.0).build());

  when(inventoryStub.reserveStock(any())).thenReturn(
          Inventory.ReserveStockResponse.newBuilder().setSuccess(true).build()
  );

  mockMvc.perform(post("/api/v1/orders/manager/reserve/{id}", order.getId())
                  .param("userId", "1"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("RESERVED"));
 }

 @Test
 void payPublic_shouldPayOrder() throws Exception {
  Order order = orderRepository.save(Order.builder().userId(1L).status(Status.RESERVED).totalPrice(100.0).build());

  when(inventoryStub.commitReservation(any())).thenReturn(
          Inventory.CommitReservationResponse.getDefaultInstance()
  );


  mockMvc.perform(post("/api/v1/orders/public/pay/{id}", order.getId()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("PAID"));
 }

 @Test
 void cancelPublic_shouldCancelOrder() throws Exception {
  Order order = orderRepository.save(Order.builder().userId(1L).status(Status.NEW).totalPrice(100.0).build());


  mockMvc.perform(post("/api/v1/orders/public/cancel/{id}", order.getId()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("CANCELLED"));
 }

 @Test
 void getAllManager_shouldReturnPagedOrders() throws Exception {
  orderRepository.save(Order.builder().userId(1L).status(Status.NEW).build());
  orderRepository.save(Order.builder().userId(2L).status(Status.PAID).build());

  mockMvc.perform(get("/api/v1/orders/manager")
                  .param("page", "0")
                  .param("size", "10"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content", hasSize(2)));
 }
}