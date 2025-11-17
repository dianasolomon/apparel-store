## **ApparelEngine – Apparel Store Backend**

A production-grade e-commerce backend built using Spring Boot, MySQL, and PayPal Payments, supporting automated feed ingestion, product browsing, cart, order, and checkout workflows.

---

## 🚀 **Overview**

ApparelEngine is a backend system for an apparel e-commerce application.  
It handles:

- Catalog management  
- Pricing & inventory  
- Cart operations  
- Order processing  
- PayPal payment integration  

The system is designed for high performance, automation, and clean architecture.

---

## ⭐ **Features**

- Automated CSV feed import (product, price, inventory)
- Product Listing Page (PLP) APIs
- Product Detail Page (PDP) APIs
- Cart operations (add, update, remove)
- Order creation & order history
- PayPal payment integration

---

## 🧰 **Tech Stack**

- **Java 17+**
- **Spring Boot**
- **Spring Data JPA**
- **MySQL**
- **OpenCSV** (for feed processing)
- **PayPal REST API**
- **Lombok**
- **Spring Scheduling (Cron Jobs)**

---

## 🚀 **Run Locally**

1. Add database config in `application.properties`  
2. Place CSV files in the `/feeds/` directory  

**Start the server:**
```bash
mvn spring-boot:run
