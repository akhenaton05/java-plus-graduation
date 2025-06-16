package ru.practicum.controller;

import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.time.Instant;

@Slf4j
@Service
public class UserActionClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub client;

    public void sendUserAction(long userId, long eventId, ActionTypeProto actionType, Instant timestamp) {
        log.info("IN HERE");
        try {
            UserActionProto request = UserActionProto.newBuilder()
                    .setUserId(userId)
                    .setEventId(eventId)
                    .setActionType(actionType)
                    .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                            .setSeconds(timestamp.getEpochSecond())
                            .setNanos(timestamp.getNano())
                            .build())
                    .build();

            client.collectUserAction(request);
            log.debug("Sent user action: userId={}, eventId={}, actionType={}", userId, eventId, actionType);
        } catch (StatusRuntimeException e) {
            log.error("Failed to send user action: userId={}, eventId={}, actionType={}", userId, eventId, actionType, e);
            throw new RuntimeException("gRPC call failed", e);
        }
    }
}
