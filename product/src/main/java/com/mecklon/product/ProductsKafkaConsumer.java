package com.mecklon.product;

import com.mecklon.core.commands.ReleaseProductCommand;
import com.mecklon.core.commands.ReserveProductCommand;
import com.mecklon.core.commands.ReserveProductCommandDetails;
import com.mecklon.core.dtos.ProductDetailsDTO;
import com.mecklon.core.dtos.ProductReservationInfoDTO;
import com.mecklon.product.model.*;
import com.mecklon.product.model.types.FailedReservedProductOutboxEventStatus;
import com.mecklon.product.model.types.ReleasedProductOutboxEventStatus;
import com.mecklon.product.model.types.ReservedProductOutboxEventStatus;
import com.mecklon.product.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductsKafkaConsumer {

    private final MongoTemplate mongoTemplate;
    private final ReservedProductOutBoxEventRepository reservedProductOutBoxEventRepository;
    private final ReserveProductCommandIdempotencyKeyRepository reserveProductCommandIdempotencyKeyRepository;
    private final FailedReservedProductOutboxEventRepository failedReservedProductOutboxEventRepository;

    @Transactional
    @KafkaListener(topics="${reserve-product-command}")
    public void reserveProducts(@Payload ReserveProductCommand command){
        System.out.println("=========");
        System.out.println("received");
        System.out.println("=========");

        reserveProductCommandIdempotencyKeyRepository.insert(new ReserveProductCommandIdempotencyKey(command.getOrderId()));


        int i = 0;
        double totalPrice=0;
        List<ReserveProductCommandDetails> products = command.getProductList();
        for(;i< products.size();i++){
            ReserveProductCommandDetails product = products.get(i);

            Query query = Query.query(
                    Criteria.where("id").is(product.getProductId())
                            .and("stock").gte(product.getQuantity())
            );

            Update update = new Update()
                    .inc("stock", -product.getQuantity());

            Product updatedProduct =
                    mongoTemplate.findAndModify(
                            query,
                            update,
                            FindAndModifyOptions.options().returnNew(true),
                            Product.class
                    );

            if(updatedProduct==null){
                // could not reserve, meaning out of stock
                break;
            }
            totalPrice+= product.getQuantity() * updatedProduct.getPrice();
        }

        if(i == products.size()){
            ReservedProductOutboxEvent event = ReservedProductOutboxEvent.builder()
                    .orderId(command.getOrderId())
                    .totalPrice(totalPrice)
                    .createdAt(Instant.now())
                    .status(ReservedProductOutboxEventStatus.CREATED)
                    .build();
            reservedProductOutBoxEventRepository.save(event);
            return;
        }

        i--;
        for(;i>=0;i--){
            ReserveProductCommandDetails product = products.get(i);

            Query query = new Query();
            query.addCriteria(new Criteria().andOperator(
                    Criteria.where("id").is(product.getProductId())
            ));
            Update update = new Update();
            update.inc("stock",product.getQuantity());

            mongoTemplate.updateFirst(query, update, Product.class);
        }

        FailedReservedProductOutboxEvent event = FailedReservedProductOutboxEvent.builder()
                .orderId(command.getOrderId())
                .createdAt(Instant.now())
                .status(FailedReservedProductOutboxEventStatus.CREATED)
                .build();
        failedReservedProductOutboxEventRepository.save(event);
    }

    private final ReleaseProductCommandIdempotencykeyRepository releaseProductCommandIdempotencykeyRepository;
    private final ReleasedProductOutboxEventRepository releasedProductOutboxEventRepository;

    @KafkaListener(topics="${release-product-command}")
    @Transactional
    public void releaseProductForFailedOrderIdCreation(@Payload ReleaseProductCommand releaseProductCommand){

        for(ProductReservationInfoDTO product: releaseProductCommand.getProductList()){
            Query query = new Query();
            query.addCriteria(new Criteria().andOperator(
                    Criteria.where("id").is(product.getProductId())
            ));
            Update update = new Update();
            update.inc("stock",product.getQuantity());
            Product updatedProduct =
                    mongoTemplate.findAndModify(
                            query,
                            update,
                            FindAndModifyOptions.options().returnNew(true),
                            Product.class
                    );
        }
        releaseProductCommandIdempotencykeyRepository.insert(new ReleaseProductCommandIdempotencyKey(releaseProductCommand.getOrderId()));

        ReleasedProductOutboxEvent releasedProductOutboxEvent = ReleasedProductOutboxEvent.builder()
                .orderId(releaseProductCommand.getOrderId())
                .createdAt(Instant.now())
                .status(ReleasedProductOutboxEventStatus.CREATED)
                .build();
        releasedProductOutboxEventRepository.save(releasedProductOutboxEvent);
    }
}
