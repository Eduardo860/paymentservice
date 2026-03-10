// init-ordenes.js
db.createCollection("orders");
db.orders.insertOne({
  userId: 1,
  totalAmount: 500.00,
  status: "PENDING",
  createdAt: new Date()
});
