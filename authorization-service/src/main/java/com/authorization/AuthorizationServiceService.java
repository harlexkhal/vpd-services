package com.authorization;

import com.services.schema.JWT;
import com.services.schema.MockDB;
import com.services.schema.UserCredentials;
import com.services.schema.AuthorizeRequest;
import com.services.schema.AuthenticationServiceGrpc;
import com.services.schema.AuthorizationServiceServiceGrpc;
import com.services.schema.AuthorizeResponse;
import com.services.schema.AuthValidateTokenResponse;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import net.devh.boot.grpc.client.inject.GrpcClient;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class AuthorizationServiceService extends AuthorizationServiceServiceGrpc.AuthorizationServiceServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationServiceService.class);

    @GrpcClient("authentication-service")
    private AuthenticationServiceGrpc.AuthenticationServiceBlockingStub authenticationClient;

    @Override
    public void validateAccess(AuthorizeRequest request, StreamObserver<AuthorizeResponse> responseObserver) {
        try {
            // Call validateToken from AuthenticationService
            AuthValidateTokenResponse authResponse = authenticationClient.validateToken(JWT.newBuilder().setJwt(request.getJwt()).build());

            UserCredentials user = MockDB.getUsersFromMockDb().stream()
                    .filter(u -> u.getUid().equals(authResponse.getUid()))
                    .findFirst()
                    .orElse(null);

            if (user == null) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("User not found.").asRuntimeException());
                return;
            }

            // Only Authorizes users from Nigeria to make transfers
            if ("Nigeria".equals(user.getCountry()) && "Transfer".equals(request.getOperation())) {
                AuthorizeResponse response = AuthorizeResponse.newBuilder()
                        .setSuccessful(true)
                        .setMessage("Transfer authorized")
                        .setJwt(request.getJwt())
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                AuthorizeResponse response = AuthorizeResponse.newBuilder()
                        .setSuccessful(false)
                        .setMessage("Transfer not authorized")
                        .setJwt(request.getJwt())
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        } catch (io.grpc.StatusRuntimeException e) {
            log.error("gRPC call to validate token failed: ", e);

            responseObserver.onError(Status.fromThrowable(e).asRuntimeException());
        } catch (Exception e) {
            log.error("Error in validateAccess: ", e);

            responseObserver.onError(Status.INTERNAL.withDescription("Internal server error.").asRuntimeException());
        }
    }
}
