package com.authentication;

import com.services.schema.JWT;
import com.services.schema.MockDB;
import com.services.schema.UserCredentials;
import com.services.schema.AuthGenerateTokenRequest;
import com.services.schema.AuthenticationServiceGrpc;
import com.services.schema.AuthGenerateTokenResponse;
import com.services.schema.AuthValidateTokenResponse;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.SignatureAlgorithm;

import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Date;
import java.security.Key;
import java.util.concurrent.TimeUnit;

@GrpcService
public class AuthenticationService extends AuthenticationServiceGrpc.AuthenticationServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private Key key = Keys.secretKeyFor(SignatureAlgorithm.HS512);

  @Override
  public void generateToken(AuthGenerateTokenRequest request, StreamObserver<AuthGenerateTokenResponse> responseObserver) {
     List<UserCredentials> users = MockDB.getUsersFromMockDb();

     try {
        UserCredentials user = users.stream()
                .filter(u -> u.getEmailOrPhone().equals(request.getEmailOrPhone()))
                .findFirst()
                .orElse(null);

        if (user != null && new BCryptPasswordEncoder().matches(request.getPassword(), user.getHashedPassword())) {
            String jwt = generateJwtToken(user);
            AuthGenerateTokenResponse response = AuthGenerateTokenResponse.newBuilder()
                    .setUid(user.getUid())
                    .setFirstName(user.getFirstName())
                    .setLastName(user.getLastName())
                    .setJwt(jwt)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(Status.UNAUTHENTICATED.withDescription("Invalid email/phone or password.").asRuntimeException());
        }
    } catch (Exception e) {
        log.error("Exception in generateToken: ", e);
        responseObserver.onError(Status.INTERNAL.withDescription("Internal server error.").asRuntimeException());
    }
  }

@Override
   public void validateToken(JWT request, StreamObserver<AuthValidateTokenResponse> responseObserver) {
        try {
            Jws<Claims> parsedToken = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(request.getJwt());
            Date expiration = parsedToken.getBody().getExpiration();
            Date now = new Date();

            if (expiration.before(now)) {
                responseObserver.onError(Status.UNAUTHENTICATED.withDescription("Auth session expired.").asRuntimeException());
                return;
            }

            long diffInMillies = expiration.getTime() - now.getTime();
            long minutesUntilExpiration = TimeUnit.MILLISECONDS.toMinutes(diffInMillies);

            AuthValidateTokenResponse response;
            if (minutesUntilExpiration <= 3) {

                UserCredentials userCredentials = getUserCredentialsFromClaims(parsedToken.getBody());
                String newJwt = generateJwtToken(userCredentials);

                response = AuthValidateTokenResponse.newBuilder()
                .setUid(userCredentials.getUid())
                .setJwt(newJwt)
                .build();
            } else {
                UserCredentials userCredentials = getUserCredentialsFromClaims(parsedToken.getBody());
                response = AuthValidateTokenResponse.newBuilder().setUid(userCredentials.getUid()).setJwt(request.getJwt()).build();
            }

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (ExpiredJwtException e) {
            responseObserver.onError(Status.UNAUTHENTICATED.withDescription("Auth session expired.").asRuntimeException());
        } catch (JwtException e) {
            responseObserver.onError(Status.UNAUTHENTICATED.withDescription("Invalid JWT token.").asRuntimeException());
        } catch (Exception e) {
            log.error("validateAuth error: ", e);
            responseObserver.onError(Status.INTERNAL.withDescription("Internal server error.").asRuntimeException());
        }
    }

    private UserCredentials getUserCredentialsFromClaims(Claims claims) {
        return UserCredentials.newBuilder()
                .setUid(claims.get("uid", String.class))
                .setFirstName(claims.get("first_name", String.class))
                .setLastName(claims.get("last_name", String.class))
                .build();
    }

private String generateJwtToken(UserCredentials user) {
    long nowMillis = System.currentTimeMillis();
    Date now = new Date(nowMillis);

    // expires 5 minutes from now
    long expMillis = nowMillis + 5 * 60 * 1000;
    Date exp = new Date(expMillis);

    String jwt = Jwts.builder()
            .setSubject(user.getUid())
            .claim("uid", user.getUid())
            .claim("first_name", user.getFirstName())
            .claim("last_name", user.getLastName())
            .setIssuedAt(now)
            .setExpiration(exp)
            .signWith(key)
            .compact();

    return jwt;
}

  private String encryptPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
  }
}
