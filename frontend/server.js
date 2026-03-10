const express=require("express");const cors=require("cors");const path=require("path");const app=express();app.use(cors());app.use(express.static(__dirname));app.get("/",(req,res)=>res.sendFile(path.join(__dirname,"index.html")));app.listen(3000,()=>console.log("
🚀 Frontend en http://localhost:3000
"));