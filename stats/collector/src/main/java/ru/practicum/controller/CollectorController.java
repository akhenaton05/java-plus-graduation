package ru.practicum.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Value;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.kafka.KafkaCollectorProducer;
import ru.practicum.mapper.UserActionMapper;
import io.grpc.stub.StreamObserver;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class CollectorController extends UserActionControllerGrpc.UserActionControllerImplBase {
    @Value("${kafka.producer.topics.user-actions}")
    private String topic;
    private final KafkaCollectorProducer kafkaCollectorProducer;
    private final UserActionMapper userActionMapper;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        try {
            log.info("Обработка контроллером collectUserAction сообщения UserActionProto {}", request);
            UserActionAvro avro = userActionMapper.mapToAvro(request);
            kafkaCollectorProducer.send(topic, avro);
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
            log.debug("Успешная обработка события {}", request);
        } catch (Exception e) {
            log.error("Error processing request: request={}, type={}, error={}", request, e.getMessage(), e);
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }
}
