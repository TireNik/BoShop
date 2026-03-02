package com.kika.inventory_service.grpc;

import com.kika.inventory.grpc.InventoryServiceGrpc;
import com.kika.inventory_service.entity.Inventory;
import com.kika.inventory_service.repository.InventoryRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;


@GrpcService
@RequiredArgsConstructor
public class InventoryGrpcServiceImpl extends InventoryServiceGrpc.InventoryServiceImplBase {

    private final InventoryRepository inventoryRepo;

    @Override
    @Transactional
    public void reserveStock(com.kika.inventory.grpc.Inventory.ReserveStockRequest request,
                             StreamObserver<com.kika.inventory.grpc.Inventory.ReserveStockResponse> observer) {
        boolean success = true;
        String message = "OK";

        for (var item : request.getItemsList()) {
            Inventory inv = inventoryRepo.findByProductId(item.getProductId())
                    .orElseGet(() -> Inventory.builder().productId(item.getProductId()).available(0).build());

            if (inv.getAvailable() < item.getQuantity()) {
                success = false;
                message = "Not enough stock for " + item.getProductId();
                break;
            } else {
                inv.setAvailable(inv.getAvailable() - item.getQuantity());
                inv.setReserved(inv.getReserved() + item.getQuantity());
                inventoryRepo.save(inv);
            }
        }

        observer.onNext(com.kika.inventory.grpc.Inventory.ReserveStockResponse.newBuilder()
                .setSuccess(success)
                .setMessage(message)
                .build());
        observer.onCompleted();
    }

    @Override
    @Transactional
    public void cancelReservation(com.kika.inventory.grpc.Inventory.CancelReservationRequest request,
                                  StreamObserver<com.kika.inventory.grpc.Inventory.CancelReservationResponse> responseObserver) {
        boolean success = true;
        String message = "Reservation cancelled";

        for (var item : request.getItemsList()) {
            Inventory inv = inventoryRepo.findByProductId(item.getProductId())
                    .orElse(null);

            if (inv != null && inv.getReserved() >= item.getQuantity()) {
                inv.setAvailable(inv.getAvailable() + item.getQuantity());
                inv.setReserved(inv.getReserved() - item.getQuantity());
                inventoryRepo.save(inv);
            } else {
                success = false;
                message = "Not enough reserved stock for " + item.getProductId();
            }
        }

        responseObserver.onNext(com.kika.inventory.grpc.Inventory.CancelReservationResponse.newBuilder()
                    .setSuccess(success)
                    .setMessage(message)
                    .build());
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void commitReservation(com.kika.inventory.grpc.Inventory.CommitReservationRequest request,
                                  StreamObserver<com.kika.inventory.grpc.Inventory.CommitReservationResponse> responseObserver) {
        boolean success = true;
        String message = "Reservation committed";

        for (var item : request.getItemsList()) {
            Inventory inv = inventoryRepo.findByProductId(item.getProductId())
                    .orElse(null);
            if (inv != null && inv.getReserved() >= item.getQuantity()) {
                inv.setReserved(inv.getReserved() - item.getQuantity());
                inventoryRepo.save(inv);
            } else {
                success = false;
                message = "Not enough reserved stock for " + item.getProductId();
            }
        }

        responseObserver.onNext(com.kika.inventory.grpc.Inventory.CommitReservationResponse.newBuilder()
                    .setSuccess(success)
                    .setMessage(message)
                    .build());
        responseObserver.onCompleted();
    }
}