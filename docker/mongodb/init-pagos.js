// init-pagos.js
db.createCollection("payments");
db.payments.insertOne({
  orderId: "1",
  amount: 500.00,
  status: "PENDING",
  paymentMethod: "CREDIT_CARD",
  createdAt: new Date()
});
