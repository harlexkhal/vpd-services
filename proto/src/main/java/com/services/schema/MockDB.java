package com.services.schema;

import com.services.schema.UserCredentials;
import java.util.ArrayList;
import java.util.List;

public class MockDB {

    public static List<UserCredentials> getUsersFromMockDb() {
        return new ArrayList<UserCredentials>() {
            {
                add(UserCredentials.newBuilder().setUid("5m4R7C0d3r").setAccountNumber("0012345").setHashedPassword("$2a$10$gB6nIILr/5AHH8eCgrImiu4GmbBN8D.76KADDH.RiNxl.V7JFYQQq").setEmailOrPhone("harlexibeh01@gmail.com").setFirstName("Alexander").setLastName("Ibeh").setCountry("Nigeria").setJwt("").build());

                add(UserCredentials.newBuilder().setUid("R1chC0d3").setAccountNumber("004299").setHashedPassword("$2a$10$gB6nIILr/5AHH8eCgrImiu4GmbBN8D.76KADDH.RiNxl.V7JFYQQq").setEmailOrPhone("0821835586").setFirstName("Jenny").setLastName("Edi").setCountry("Ghana").setJwt("").build());
            }
        };
    }
}
