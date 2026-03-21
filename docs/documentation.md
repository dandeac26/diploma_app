# Pastry Central - Documentation

> Comprehensive technical documentation for the Pastry Central bakery management Android application.

---

## Table of Contents

- [1. Project Overview](#1-project-overview)
- [2. Architecture](#2-architecture)
- [3. Technology Stack](#3-technology-stack)
- [4. Navigation & UI Structure](#4-navigation--ui-structure)
- [5. Feature Breakdown](#5-feature-breakdown)
- [6. API Architecture](#6-api-architecture)
- [7. Data Models](#7-data-models)
- [8. Real-Time Communication](#8-real-time-communication)
- [9. Stock Prediction System](#9-stock-prediction-system)
- [10. Staff Recommendation Engine](#10-staff-recommendation-engine)
- [11. Package Structure](#11-package-structure)

---

## 1. Project Overview

**Pastry Central** is a full-featured Android application designed for bakery/pastry shop operations management. It provides tools for managing the entire bakery workflow — from tracking products and recipes, managing clients and suppliers, handling daily orders with real-time synchronization, to predicting stock consumption and recommending staffing levels based on order volume.

The app communicates with a multi-service backend via REST APIs and WebSocket for real-time updates.

| Property     | Value                       |
| ------------ | --------------------------- |
| Package      | `com.example.myapplication` |
| Min SDK      | 24 (Android 7.0)            |
| Target SDK   | 34 (Android 14)             |
| Language     | Kotlin                      |
| Build System | Gradle 8.5.0                |

---

## 2. Architecture

The application follows the **MVVM (Model-View-ViewModel)** architecture pattern with Fragment-based UI.

![Architecture Overview](img/architecture.png)

### Components

| Layer         | Components                                      | Responsibility                                                       |
| ------------- | ----------------------------------------------- | -------------------------------------------------------------------- |
| **View**      | Fragments, Adapters, Dialogs                    | UI rendering, user interaction                                       |
| **ViewModel** | SharedViewModel                                 | State management, LiveData observables, cross-fragment communication |
| **Data**      | RetrofitInstance, API interfaces, ConfigManager | Network calls, data serialization, configuration                     |
| **Backend**   | REST APIs (ports 8080, 8000, 8001), WebSocket   | Data persistence, real-time messaging                                |

### Data Flow

```
User Action → Fragment → SharedViewModel (LiveData) → Retrofit API → Backend Server
                ↑                    |
                └── Observer ← LiveData Update ← API Response
```

The `SharedViewModel` is shared across all fragments via `SharedViewModelFactory`, enabling cross-fragment communication without tight coupling. All UI updates are reactive through LiveData observers.

---

## 3. Technology Stack

### Core

| Library           | Version | Purpose                   |
| ----------------- | ------- | ------------------------- |
| Kotlin            | 1.9.0   | Primary language          |
| AndroidX Core KTX | 1.10.1  | Android Kotlin extensions |
| AppCompat         | 1.6.1   | Backward-compatible UI    |
| Material Design   | 1.10.0  | UI components & theming   |
| ConstraintLayout  | 2.1.4   | Responsive layouts        |

### Networking

| Library   | Version | Purpose                            |
| --------- | ------- | ---------------------------------- |
| Retrofit2 | 2.9.0   | Type-safe REST client              |
| OkHttp3   | 4.9.1   | HTTP client with WebSocket support |
| Gson      | 2.9.0   | JSON serialization/deserialization |

### Architecture

| Library               | Version | Purpose                          |
| --------------------- | ------- | -------------------------------- |
| ViewModel             | 2.3.1   | Lifecycle-aware state holders    |
| LiveData              | 2.3.1   | Observable data holders          |
| Kotlin Coroutines     | 1.5.2   | Asynchronous programming         |
| Lifecycle Runtime KTX | 2.3.1   | Lifecycle-aware coroutine scopes |

### UI Enhancements

| Library            | Version | Purpose                     |
| ------------------ | ------- | --------------------------- |
| RecyclerView       | 1.2.1   | Efficient scrolling lists   |
| Glide              | 4.12.0  | Image loading & caching     |
| CircleImageView    | 3.1.0   | Circular image views        |
| Facebook Shimmer   | 0.5.0   | Loading skeleton animations |
| GraphView          | 4.2.2   | Chart/graph rendering       |
| SwipeRefreshLayout | 1.1.0   | Pull-to-refresh             |

---

## 4. Navigation & UI Structure

The app uses a single-Activity architecture with `MainActivity` hosting a `BottomNavigationView` for primary navigation between 5 main sections.

![Navigation Flow](img/navigation_flow.png)

### Bottom Navigation Tabs

| Tab      | Fragment           | Description                                                               |
| -------- | ------------------ | ------------------------------------------------------------------------- |
| Home     | `HomeFragment`     | Dashboard with shift management, staff recommendations, stock predictions |
| Products | `ProductsFragment` | Product catalog with images, search, CRUD operations                      |
| Orders   | `OrdersFragment`   | 14-day calendar view for browsing orders by date                          |
| Stocks   | `StocksFragment`   | Inventory management with prediction mode                                 |
| Clients  | `ClientsFragment`  | Client directory with contact info and location                           |

### Secondary Fragments

| Fragment                 | Accessed From            | Purpose                                |
| ------------------------ | ------------------------ | -------------------------------------- |
| `DailyOrderFragment`     | Orders → click date      | All orders for a specific date         |
| `OrderDetailsFragment`   | DailyOrder → click order | Full order view with line items        |
| `OrderDialogFragment`    | DailyOrder → FAB button  | Create new order dialog                |
| `ProductDetailsFragment` | Products → click product | Product details with recipe management |
| `IngredientsFragment`    | Stocks → menu            | Ingredient table management            |
| `ProvidersFragment`      | Stocks → menu            | Supplier table management              |
| `SensorFragment`         | MainActivity → menu      | Temperature/humidity monitoring        |
| `SettingsFragment`       | Stocks → menu            | API URL configuration                  |

---

## 5. Feature Breakdown

### 5.1 Product Management

Full CRUD operations for the bakery product catalog.

- Browse products with circular thumbnail images (Glide + CircleImageView)
- Search/filter products by name or price
- Add products with name, price, and image URL
- Edit and delete products with confirmation dialogs
- Shimmer loading animation while data loads
- Image preview dialog for full-size product images

### 5.2 Recipe Management

Each product has a recipe — a list of ingredients with quantities.

- View recipe (ingredients list) from `ProductDetailsFragment`
- Add ingredients to a recipe with quantity and measurement unit
- Edit ingredient quantities in a recipe
- Delete individual recipe lines
- Ingredient selection via dropdown (populated from IngredientsAPI)

### 5.3 Client Management

Manage bakery clients with categorization and location features.

- Client types: **SPECIAL**, **REGULAR**, **KINDERGARTEN**
- Store firm name, contact person, phone number, address, GPS coordinates
- Waze URL parsing for quick navigation to client location
- Phone number copy to clipboard
- Contact lookup via READ_CONTACTS permission
- Search/filter by name, contact person, or address

### 5.4 Order Management

![Order Creation Flow](img/order_creation_flow.png)

The order system is the core feature, supporting the full order lifecycle:

- **Browse**: 14-day calendar view with date items, week separators
- **Create**: Dialog-based order creation with client selection, product selection, hour picker (10 AM - 6 PM), delivery toggle
- **Auto-populate**: When selecting a client, the system fetches the client's last order and pre-fills products and quantities
- **Track**: View all orders for a date, filter completed/pending, search by client name or price
- **Complete**: Toggle order completion status
- **Delete**: Remove orders with confirmation
- **Real-time**: WebSocket integration for live order updates across devices

**Order Detail Composition**: Each order contains multiple line items (`OrderDetail`), each referencing a product and quantity. Total price is calculated from individual product prices × quantities.

### 5.5 Stock & Inventory Management

![Stock Prediction Flow](img/stock_prediction_flow.png)

Inventory tracking with a unique prediction capability:

- Track stock levels per ingredient-provider combination
- Visual progress bars showing current stock vs. max capacity
- Add stock entries with ingredient, provider, quantity, price, max quantity, and quantity per package
- **Prediction Mode** (toggle): Calculates expected stock consumption until a target date based on pending orders, product recipes, and current stock levels
- **Negative stock alerts**: Red indicators when predicted consumption exceeds available stock
- Search/filter by ingredient name or provider

### 5.6 Home Dashboard

![Staff Recommendation Logic](img/staff_recommendation.png)

The home screen serves as the operational command center:

- **Shift selection**: Noon shift (2 PM - 10 PM) / Night shift (10 PM - 2 PM)
- **Date picker**: View data for any date
- **Staff recommendations**: Algorithm-driven staffing suggestions based on order volume (see [Staff Recommendation Engine](#10-staff-recommendation-engine))
- **Shift products list**: Products needed for the selected shift, filtered by client type
- **Stock predictions till date**: Ingredient consumption forecast with negative stock alerts
- **PDF printing**: Generate printable shift order summaries
- **Shimmer loading** and **SwipeRefreshLayout** for smooth UX

### 5.7 Sensor Integration

IoT sensor monitoring for bakery environment:

- Monitor **Warehouse1** and **Kitchen1** sensors
- Real-time temperature and humidity readings
- Historical data: last 1 hour and last 24 hours
- Connects to a dedicated sensor server on port 8001

### 5.8 Settings

- Configure API base URLs (home network vs. mobile hotspot)
- Toggle between network configurations
- Preference-based UI using AndroidX Preferences
- App restart capability for configuration changes

---

## 6. API Architecture

![API Architecture](img/api_architecture.png)

The app communicates with three backend services:

### Products & Inventory Server (Port 8080)

| API Interface    | Endpoints                                                              | Operations                                 |
| ---------------- | ---------------------------------------------------------------------- | ------------------------------------------ |
| `BakeryAPI`      | `/product`, `/product/{id}`                                            | GET all, POST, PUT, DELETE                 |
| `ClientAPI`      | `/client`, `/client/{id}`                                              | GET all, POST, POST+return ID, PUT, DELETE |
| `RecipeAPI`      | `/recipe`, `/recipe/{productId}`, `/recipe/{productId}/{ingredientId}` | GET all, GET by product, POST, PUT, DELETE |
| `IngredientsAPI` | `/ingredients`, `/ingredients/{id}`                                    | GET all, POST, PUT, DELETE                 |
| `StockAPI`       | `/stock`, `/stock/unique-stocks`, `/stock/{ingredientId}/{providerId}` | GET all, GET unique, POST, PUT, DELETE     |
| `ProviderAPI`    | `/providers`, `/providers/{id}`                                        | GET all, POST, PUT, DELETE                 |

### Orders Server (Port 8000)

| API Interface | Endpoints                                                                  | Operations                                                  |
| ------------- | -------------------------------------------------------------------------- | ----------------------------------------------------------- |
| `OrderAPI`    | `/orders`, `/orders/{id}`, `/orders/byDate/{date}`, `/orders/{id}/details` | GET all, GET by date, POST order, POST details, PUT, DELETE |

### Sensor Server (Port 8001)

| API Interface | Endpoints                                                                                                                                           | Operations     |
| ------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- | -------------- |
| `SensorAPI`   | `/sensor-data`, `/sensor-data/{id}`, `/sensor-data/last-hour`, `/sensor-data/last-day`, `/sensor-data/{id}/last-hour`, `/sensor-data/{id}/last-day` | GET operations |

### Network Configuration

The `RetrofitInstance` object builds Retrofit clients dynamically based on `ConfigManager` settings:

```
Protocol + BaseURL + Port → Retrofit instance
Example: http://192.168.68.56:8080/
```

The `ConfigManager` stores two configurable base URLs (home and mobile network) in SharedPreferences, with a toggle to switch between them.

---

## 7. Data Models

![Data Model Relationships](img/data_model.png)

### DTOs (Data Transfer Objects)

| DTO             | Key Fields                                                                         | Purpose                 |
| --------------- | ---------------------------------------------------------------------------------- | ----------------------- |
| `ProductDTO`    | name, price, imageUrl                                                              | Product creation/update |
| `ClientDTO`     | firmName, contactPerson, phoneNumber, location, latitude, longitude, address, type | Client management       |
| `OrderDTO`      | clientId, deliveryNeeded, completionDate, completionTime, price, completed         | Order lifecycle         |
| `StockDTO`      | ingredientId, providerId, quantity, price, maxQuantity, quantityPerPackage         | Inventory tracking      |
| `RecipeDTO`     | productId, ingredientId, quantity                                                  | Recipe composition      |
| `IngredientDTO` | name, measurementUnit, packaging                                                   | Ingredient catalog      |
| `ProviderDTO`   | name, phoneNumber                                                                  | Supplier info           |

### Client Types

| Type           | Description                            |
| -------------- | -------------------------------------- |
| `SPECIAL`      | Priority clients with special handling |
| `REGULAR`      | Standard bakery clients                |
| `KINDERGARTEN` | Institutional clients (kindergartens)  |

### Date/Time Formats

| Field            | Format       | Example      |
| ---------------- | ------------ | ------------ |
| `completionDate` | `yyyy-MM-dd` | `2026-03-21` |
| `completionTime` | `HH:00:00`   | `14:00:00`   |

---

## 8. Real-Time Communication

![WebSocket Flow](img/websocket_flow.png)

The app maintains a persistent WebSocket connection for real-time order synchronization.

### Connection

- **Endpoint**: `ws://{host}:{port}/ws` (port 8000)
- Established in `MainActivity.connectWebSocket()` on app launch
- Uses OkHttp3 `WebSocketListener`

### Heartbeat

- App sends `"ping"` every **25 seconds**
- Server responds with `"pong"`
- Keeps the connection alive

### Messages

| Message            | Direction                   | Action                                           |
| ------------------ | --------------------------- | ------------------------------------------------ |
| `"ping"`           | App → Server                | Heartbeat                                        |
| `"pong"`           | Server → App / App → Server | Heartbeat response                               |
| `"Refetch orders"` | Server → App                | Triggers `DailyOrderFragment.fetchDailyOrders()` |
| Order JSON         | App → Server                | Notifies server of new order creation            |

### Notifications

When a `"Refetch orders"` message is received, the app:

1. Calls `DailyOrderFragment.fetchDailyOrders()` to reload the order list
2. Can trigger Android push notifications via the `pastry_central_notifs` notification channel

---

## 9. Stock Prediction System

The stock prediction system is a core differentiating feature that forecasts ingredient consumption based on pending orders.

### Algorithm

1. **Input**: All unfulfilled orders from today until a user-selected target date
2. **Product extraction**: For each order, retrieve all products with their quantities
3. **Recipe lookup**: For each product, call `RecipeAPI.getRecipeOfProduct(productId)` to get the ingredient list
4. **Quantity calculation**: For each ingredient in the recipe:
   ```
   totalNeeded[ingredientId] += recipe.quantity × product.quantity
   ```
5. **Package conversion**: Divide total needed by `quantityPerPackage` to get packages needed
6. **Comparison**: Subtract predicted consumption from current stock
7. **Alert**: If result < 0, flag as negative stock (shown with red indicator)

### Implementation

- Managed by `SharedViewModel.calculateAllIngredientQuantitiesTillDate(recipeAPI)`
- Results stored in `allIngredientQuantitiesTillDate` LiveData
- `StockAdapter` observes this LiveData and renders prediction progress bars
- `NegativeStocksAdapter` on the Home Dashboard displays all negative stock alerts

### Visualization

- **Normal mode**: Pink progress bar showing `current / max` stock level
- **Prediction mode**: Blue progress bar showing `(current - predicted) / max` level
- **Negative stock**: Red indicator with negative quantity displayed

---

## 10. Staff Recommendation Engine

The Home Dashboard includes an algorithm-driven staffing recommendation system.

### Employee Roles & Seniority

| Role       | Seniority Levels    |
| ---------- | ------------------- |
| `COOK`     | Experienced, Junior |
| `PACKAGER` | Experienced, Junior |

### Morning Packaging Staff (based on total daily products)

| Condition                                      | Recommendation                      |
| ---------------------------------------------- | ----------------------------------- |
| totalDayProducts < 200                         | 1 Experienced Packager              |
| totalDayProducts >= 200                        | (total / 200) Experienced Packagers |
| totalDayProducts >= 200 AND total % 200 > 100  | + 2 Junior Packagers                |
| totalDayProducts >= 200 AND total % 200 <= 100 | + 1 Junior Packager                 |

### Current Shift Cooks (based on shift products)

| Condition                                       | Recommendation                  |
| ----------------------------------------------- | ------------------------------- |
| totalShiftProducts < 100                        | 1 Experienced Cook              |
| totalShiftProducts >= 100                       | (total / 100) Experienced Cooks |
| totalShiftProducts >= 100 AND total % 100 > 50  | + 2 Junior Cooks                |
| totalShiftProducts >= 100 AND total % 100 <= 50 | + 1 Junior Cook                 |

### Shifts

| Shift | Hours              |
| ----- | ------------------ |
| Noon  | 2:00 PM - 10:00 PM |
| Night | 10:00 PM - 2:00 AM |

---

## 11. Package Structure

```
com.example.myapplication/
├── MainActivity.kt                    # Single activity, bottom nav, WebSocket
│
├── fragments/                         # All UI fragments (17 total)
│   ├── HomeFragment.kt               # Dashboard, predictions, staff
│   ├── ProductsFragment.kt           # Product catalog
│   ├── ProductDetailsFragment.kt     # Product details + recipe
│   ├── OrdersFragment.kt             # Date calendar view
│   ├── DailyOrderFragment.kt         # Orders for specific date
│   ├── OrderDetailsFragment.kt       # Single order details
│   ├── OrderDialogFragment.kt        # Create order dialog
│   ├── CreateOrderFragment.kt        # Alternative order creation
│   ├── StocksFragment.kt             # Inventory + predictions
│   ├── ClientsFragment.kt            # Client directory
│   ├── IngredientsFragment.kt        # Ingredient table
│   ├── ProvidersFragment.kt          # Supplier table
│   ├── SensorFragment.kt             # IoT sensor data
│   └── SettingsFragment.kt           # App configuration
│
├── adapters/                          # RecyclerView adapters (11 total)
│   ├── OrderAdapter.kt
│   ├── ProductAdapter.kt
│   ├── ClientAdapter.kt
│   ├── StockAdapter.kt
│   ├── DateItemAdapter.kt
│   ├── OrderDetailsAdapter.kt
│   ├── OrderDialogProductAdapter.kt
│   ├── NegativeStocksAdapter.kt
│   ├── ShiftProductsAdapter.kt
│   ├── StaffRecommendationsAdapter.kt
│   └── SensorAdapter.kt
│
├── api/                               # Retrofit API interfaces (8 total)
│   ├── BakeryAPI.kt
│   ├── ClientAPI.kt
│   ├── OrderAPI.kt
│   ├── RecipeAPI.kt
│   ├── IngredientsAPI.kt
│   ├── StockAPI.kt
│   ├── ProviderAPI.kt
│   └── SensorAPI.kt
│
├── config/                            # Configuration & networking
│   ├── ConfigManager.kt              # SharedPreferences wrapper
│   ├── RetrofitInstance.kt           # Retrofit singleton builder
│   └── NetworkChangeReceiver.kt      # Network state listener
│
├── shared/                            # Shared state management
│   ├── SharedViewModel.kt            # Central ViewModel
│   └── SharedViewModelFactory.kt     # ViewModel factory
│
├── dtos/                              # Data transfer objects (8 total)
│   ├── ProductDTO.kt
│   ├── ClientDTO.kt
│   ├── OrderDTO.kt
│   ├── StockDTO.kt
│   ├── RecipeDTO.kt
│   ├── IngredientDTO.kt
│   └── ProviderDTO.kt
│
└── dialog/                            # Dialog helpers
    └── ImagePreviewDialog.kt          # Product image preview
```

### Adapter Summary

| Adapter                       | Used In              | Displays                                |
| ----------------------------- | -------------------- | --------------------------------------- |
| `ProductAdapter`              | ProductsFragment     | Product cards with images               |
| `ClientAdapter`               | ClientsFragment      | Client cards with Waze/phone actions    |
| `OrderAdapter`                | DailyOrderFragment   | Order rows with completion toggle       |
| `OrderDetailsAdapter`         | OrderDetailsFragment | Order line items (product + qty)        |
| `OrderDialogProductAdapter`   | OrderDialogFragment  | Selected products during order creation |
| `StockAdapter`                | StocksFragment       | Stock items with progress bars          |
| `DateItemAdapter`             | OrdersFragment       | Date items in calendar picker           |
| `NegativeStocksAdapter`       | HomeFragment         | Low/negative stock alerts               |
| `ShiftProductsAdapter`        | HomeFragment         | Products needed for a shift             |
| `StaffRecommendationsAdapter` | HomeFragment         | Staffing recommendation cards           |
| `SensorAdapter`               | SensorFragment       | Temperature/humidity readings           |
