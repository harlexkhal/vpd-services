const express = require('express');
const bodyParser = require('body-parser');
const router = express.Router();
const grpc = require('@grpc/grpc-js');
const protoLoader = require('@grpc/proto-loader');

const PROTO_PATH = __dirname + '/../../../proto/src/main/proto/schema.proto';

const packageDefinition = protoLoader.loadSync(PROTO_PATH, {
  keepCase: true,
  longs: String,
  enums: String,
  defaults: true,
  oneofs: true
});


router.use(bodyParser.json());

/**
 * @route POST /login
 * @group Authentication - Operations about user authentication
 * @param {string} email_or_phone.body.required - email or phone credential for user authentication
 * @param {string} password.body.required - user's password
 * @returns {object} 200 - An object with user information and JWT token
 * @returns {Error}  INVALID_ARGUMENT - Invalid email/phone or password
 * @returns {Error}  INTERNAL - Internal server error
 */
router.post('/login', (req, res) => {

const hello_proto = grpc.loadPackageDefinition(packageDefinition).com.services.schema;
const target = 'localhost:8080';
const client = new hello_proto.AuthenticationService(target, grpc.credentials.createInsecure());

  const { email_or_phone, password } = req.body;

  if (!email_or_phone || !password) {
    return res.status(400).json({ error: "Email or phone and password must be provided." });
  }

  // gRPC call to Authentication Service to generateToken
  client.generateToken({ email_or_phone, password }, (err, response) => {
    if (err) {
      return res.status(500).json({ error: err.details });
    }

    res.json(response);
  });
});

/**
 * @route POST /transfer
 * @group Transaction - Operations related to financial transactions
 * @description This route handles money transfer requests. It requires a JWT token for authentication 
 * and authorization, which should be provided in the Authorization header. The route calls the gRPC 
 * Transaction Service to perform the transfer.
 * 
 * @param {string} authorization.header.required - Bearer JWT token for user authentication and authorization
 * @param {number} amount.body.required - The amount of money to transfer
 * @param {string} receiver_account_number.body.required - The account number of the transfer recipient
 * 
 * @returns {object} 200 - An object with transaction details on successful transfer
 * @returns {Error} 400 - Bad request error if JWT token, amount, or receiver account number is not provided
 * @returns {Error} 500 - Internal server error or error from the gRPC service
 */
router.post('/transfer', (req, res) => {

    const hello_proto = grpc.loadPackageDefinition(packageDefinition).com.services.schema;
    const target = 'localhost:8082';
    const client = new hello_proto.TransactionServiceService(target, grpc.credentials.createInsecure());

    const authHeader = req.headers.authorization || '';
    const match = authHeader.match(/^Bearer\s+(.*)$/i);

    if (!match) {
        return res.status(400).json({ error: "JWT token must be provided in the Authorization header." });
    }

    const jwt = match[1];
    const { amount, receiver_account_number } = req.body;

    if (!amount || !receiver_account_number) {
        return res.status(400).json({ error: "Amount and receiver account number must be provided." });
    }

    // gRPC call to Transaction Service to transfer
    client.transfer({ jwt, amount, receiver_account_number }, (err, response) => {
        if (err) {
            return res.status(500).json({ error: err.details });
        }
        res.json(response);
    });
});


// routes below Are not specific to clients use as they are just here as some sort of integration test;
// to test some of the internal services used by transfer and login.

router.get('/validate', (req, res) => {
  
const hello_proto = grpc.loadPackageDefinition(packageDefinition).com.services.schema;
const target = 'localhost:8080';
const client = new hello_proto.AuthenticationService(target, grpc.credentials.createInsecure());

  const authHeader = req.headers.authorization || '';
  const match = authHeader.match(/^Bearer\s+(.*)$/i);
  
  if (!match) {
    return res.status(400).json({ error: "JWT token must be provided in the Authorization header." });
  }

  const jwt = match[1];

  // gRPC call to Authentication Service to validateToken
  client.validateToken({ jwt }, (err, response) => {
    if (err) {
      if (err.code === grpc.status.UNAUTHENTICATED) {
        return res.status(401).json({ error: "Auth session expired or invalid JWT token." });
      }
      return res.status(500).json({ error: err.details });
    }

    res.json(response);
  });
});

router.get('/authorize', (req, res) => {

 const hello_proto = grpc.loadPackageDefinition(packageDefinition).com.services.schema;
const target = 'localhost:8081';
const client = new hello_proto.AuthorizationServiceService(target, grpc.credentials.createInsecure());

  // Extract JWT from Authorization header
  const authHeader = req.headers.authorization || '';
  const match = authHeader.match(/^Bearer\s+(.*)$/i);

  if (!match) {
    return res.status(400).json({ error: "JWT token must be provided in the Authorization header." });
  }

  const jwt = match[1];
  const { operation } = req.body;

  if (!operation) {
    return res.status(400).json({ error: "Operation must be provided." });
  }

  // gRPC call to Authorization Service to validateAccess
  client.validateAccess({ operation, jwt }, (err, response) => {
    if (err) {
      if (err.code === grpc.status.UNAUTHENTICATED) {
        return res.status(401).json({ error: "Invalid or expired JWT token." });
      }
      return res.status(500).json({ error: err.details });
    }
    res.json(response);
  });
});

module.exports = router;
