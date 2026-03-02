package com.kika.product_service.grpc;

import com.kika.product.grpc.*;
import com.kika.product_service.entity.Product;
import com.kika.product_service.repository.ProductRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class ProductGrpcServiceImpl extends ProductGrpcServiceGrpc.ProductGrpcServiceImplBase {

    private final ProductRepository productRepository;


    @Override
    public void getProduct(ProductIdRequest request, StreamObserver<ProductResponse> responseObserver) {
        try {
            Product product = productRepository.getProductById(request.getId());

            com.kika.product.grpc.Product grpcProduct = com.kika.product.grpc.Product.newBuilder()
                    .setId(product.getId())
                    .setName(product.getName())
                    .setDescription(product.getDescription() != null ? product.getDescription() : "")
                    .setPrice(product.getPrice().doubleValue())
                    .setPublished(product.isPublished())
                    .build();

            ProductResponse response = ProductResponse.newBuilder()
                    .setProduct(grpcProduct)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            responseObserver.onError(
                Status.NOT_FOUND
                    .withDescription("Product not found with id: " + request.getId())
                    .asRuntimeException()
            );
        } catch (Exception e) {
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Internal server error")
                    .asRuntimeException()
            );
        }
    }
}