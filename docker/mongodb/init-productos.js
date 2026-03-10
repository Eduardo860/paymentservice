// init-productos.js
db.createCollection("products");
db.products.insertMany([
  {
    name: "Laptop",
    description: "Laptop HP 15.6 pulgadas",
    price: 899.99,
    stock: 10
  },
  {
    name: "Mouse",
    description: "Mouse inalámbrico Logitech",
    price: 29.99,
    stock: 50
  },
  {
    name: "Teclado",
    description: "Teclado mecánico RGB",
    price: 79.99,
    stock: 25
  },
  {
    name: "Monitor",
    description: "Monitor 27 pulgadas 4K",
    price: 399.99,
    stock: 15
  }
]);
