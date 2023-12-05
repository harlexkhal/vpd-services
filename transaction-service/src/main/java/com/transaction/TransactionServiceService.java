package com.transaction;

import com.services.schema.JWT;
import com.services.schema.MockDB;
import com.services.schema.UserCredentials;
import com.services.schema.TransactionRequest;
import com.services.schema.TransactionResponse;
import com.services.schema.AuthenticationServiceGrpc;
import com.services.schema.AuthorizationServiceServiceGrpc;
import com.services.schema.TransactionServiceServiceGrpc;
import com.services.schema.AuthValidateTokenResponse;
import com.services.schema.AuthorizeRequest;
import com.services.schema.AuthorizeResponse;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import net.devh.boot.grpc.client.inject.GrpcClient;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class TransactionServiceService extends TransactionServiceServiceGrpc.TransactionServiceServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(TransactionServiceService.class);

    @GrpcClient("authentication-service")
    private AuthenticationServiceGrpc.AuthenticationServiceBlockingStub authenticationClient;

    @GrpcClient("authorization-service")
    private AuthorizationServiceServiceGrpc.AuthorizationServiceServiceBlockingStub authorizationClient;

    @Override
    public void transfer(TransactionRequest request, StreamObserver<TransactionResponse> responseObserver) {
        try {
            // Validate JWT with Authentication Service
            AuthValidateTokenResponse authResponse = authenticationClient.validateToken(JWT.newBuilder().setJwt(request.getJwt()).build());

            // Validate access with Authorization Service
            AuthorizeResponse authorizeResponse = authorizationClient.validateAccess(AuthorizeRequest.newBuilder()
                    .setJwt(request.getJwt())
                    .setOperation("Transfer")
                    .build());

            if (!authorizeResponse.getSuccessful()) {
                responseObserver.onError(Status.PERMISSION_DENIED.withDescription(authorizeResponse.getMessage()).asRuntimeException());
                return;
            }

            UserCredentials user = MockDB.getUsersFromMockDb().stream()
                    .filter(u -> u.getUid().equals(authResponse.getUid()))
                    .findFirst()
                    .orElse(null);

            if (user == null) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("User not found.").asRuntimeException());
                return;
            }

            // Response Sender with Mock Receipient Data
            TransactionResponse response = TransactionResponse.newBuilder()
                    .setRefId("REF123456")
                    .setAmount(request.getAmount())
                    .setSenderName(user.getFirstName() + " " + user.getLastName())
                    .setReceiverName("Elon Musketeer")
                    .setSenderAccountNumber(user.getAccountNumber())
                    .setReceiverAccountNumber("9876543210")
                    .setNarration("Transfer")
                    .setJwt(request.getJwt())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (io.grpc.StatusRuntimeException e) {
            log.error("gRPC call failed: ", e);
            responseObserver.onError(Status.fromThrowable(e).asRuntimeException());
        } catch (Exception e) {
            log.error("Error in transfer: ", e);
            responseObserver.onError(Status.INTERNAL.withDescription("Internal server error.").asRuntimeException());
        }
    }
}
