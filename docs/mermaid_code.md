# Mermaid Diagram Code

Use [Mermaid Live Editor](https://mermaid.live) to render each diagram, then export as PNG and place in `docs/`.

---

## 1. Architecture Overview (`architecture.png`)

```mermaid
graph TB
    subgraph Android App
        subgraph View Layer
            MA[MainActivity]
            HF[HomeFragment]
            PF[ProductsFragment]
            OF[OrdersFragment]
            SF[StocksFragment]
            CF[ClientsFragment]
            PDF[ProductDetailsFragment]
            DOF[DailyOrderFragment]
            ODF[OrderDetailsFragment]
            ODialogF[OrderDialogFragment]
            IF[IngredientsFragment]
            PVF[ProvidersFragment]
            SenF[SensorFragment]
            SetF[SettingsFragment]
        end

        subgraph ViewModel Layer
            SVM[SharedViewModel]
            SVMF[SharedViewModelFactory]
        end

        subgraph Data Layer
            RI[RetrofitInstance]
            CM[ConfigManager]
            NCR[NetworkChangeReceiver]
        end

        subgraph API Interfaces
            BakeryAPI
            ClientAPI
            OrderAPI
            RecipeAPI
            IngredientsAPI
            StockAPI
            ProviderAPI
            SensorAPI
        end

        subgraph Adapters
            OA[OrderAdapter]
            PA[ProductAdapter]
            CA[ClientAdapter]
            SA[StockAdapter]
            DIA[DateItemAdapter]
            ODA[OrderDetailsAdapter]
            ODPA[OrderDialogProductAdapter]
            NSA[NegativeStocksAdapter]
            SPA[ShiftProductsAdapter]
            SRA[StaffRecommendationsAdapter]
            SenA[SensorAdapter]
        end
    end

    subgraph Backend Services
        PS["Products & Clients Server :8080"]
        OS["Orders Server :8000"]
        SS["Sensor Server :8001"]
        WS["WebSocket ws://:8000/ws"]
    end

    MA --> HF & PF & OF & SF & CF
    HF & PF & OF & SF & CF --> SVM
    SVM --> RI
    RI --> CM
    RI --> BakeryAPI & ClientAPI & OrderAPI & RecipeAPI & IngredientsAPI & StockAPI & ProviderAPI & SensorAPI
    BakeryAPI & ClientAPI & RecipeAPI & IngredientsAPI & StockAPI & ProviderAPI --> PS
    OrderAPI --> OS
    SensorAPI --> SS
    MA --> WS
```

---

## 2. Navigation Flow (`navigation_flow.png`)

```mermaid
graph TD
    MA[MainActivity<br/>Bottom Navigation]

    MA -->|Tab 1| HF[HomeFragment<br/>Dashboard & Predictions]
    MA -->|Tab 2| PF[ProductsFragment<br/>Product Catalog]
    MA -->|Tab 3| OF[OrdersFragment<br/>Date Calendar]
    MA -->|Tab 4| SF[StocksFragment<br/>Inventory Management]
    MA -->|Tab 5| CF[ClientsFragment<br/>Client Directory]

    PF -->|Click product| PDF[ProductDetailsFragment<br/>Recipe & Details]
    OF -->|Click date| DOF[DailyOrderFragment<br/>Orders for Date]
    DOF -->|Click order| ODF[OrderDetailsFragment<br/>Full Order View]
    DOF -->|Add order FAB| ODialog[OrderDialogFragment<br/>Create New Order]
    ODialog -->|Select client| CF
    ODialog -->|Select products| PF

    SF -->|Menu: Ingredients| IF[IngredientsFragment<br/>Ingredient Table]
    SF -->|Menu: Providers| PVF[ProvidersFragment<br/>Supplier Table]
    SF -->|Menu: Settings| SetF[SettingsFragment<br/>URL Configuration]

    MA -->|Menu: Sensors| SenF[SensorFragment<br/>Temperature & Humidity]

    ODialog -->|Order created| DOF
```

---

## 3. Order Creation Flow (`order_creation_flow.png`)

```mermaid
sequenceDiagram
    actor User
    participant ODialog as OrderDialogFragment
    participant SVM as SharedViewModel
    participant ClientAPI
    participant OrderAPI
    participant WebSocket

    User->>ODialog: Opens Order Dialog
    ODialog->>ODialog: Load hour spinner (10 AM - 6 PM)
    ODialog->>SVM: Observe clients & products

    User->>ODialog: Select client
    ODialog->>OrderAPI: Fetch last order for client
    OrderAPI-->>ODialog: Return previous order details
    ODialog->>ODialog: Auto-populate products from last order

    User->>ODialog: Add/adjust products & quantities
    User->>ODialog: Select date & hour
    User->>ODialog: Click "Create Order"

    alt New Client
        ODialog->>ClientAPI: addClientAndReturnId(ClientDTO)
        ClientAPI-->>ODialog: Return clientId
    end

    ODialog->>OrderAPI: addOrder(OrderDTO)
    OrderAPI-->>ODialog: Return orderId

    loop For each selected product
        ODialog->>OrderAPI: addOrderDetails(orderId, productId, quantity)
        OrderAPI-->>ODialog: Confirm detail added
    end

    ODialog->>WebSocket: send(orderJSON)
    WebSocket-->>ODialog: Acknowledge

    ODialog->>ODialog: Navigate to DailyOrderFragment
```

---

## 4. Stock Prediction Flow (`stock_prediction_flow.png`)

```mermaid
flowchart TD
    A[User toggles Prediction Mode ON] --> B[Select target date]
    B --> C[SharedViewModel fetches all orders until target date]
    C --> D[Extract all products from orders with quantities]

    D --> E{For each product}
    E --> F[Call RecipeAPI.getRecipeOfProduct]
    F --> G[Get ingredient list with quantities per unit]

    G --> H[Calculate: ingredientQty × productQty × orderCount]
    H --> I[Accumulate total per ingredientId]

    I --> J{More products?}
    J -->|Yes| E
    J -->|No| K[Store in allIngredientQuantitiesTillDate LiveData]

    K --> L[StockAdapter observes LiveData]
    L --> M{For each stock item}
    M --> N[currentStock - predictedUsage / quantityPerPackage]
    N --> O{Stock remaining >= 0?}

    O -->|Yes| P[Show green progress bar<br/>with remaining quantity]
    O -->|No| Q[Show red indicator<br/>Negative Stock Alert]

    Q --> R[NegativeStocksAdapter<br/>displays alerts on Home Dashboard]
```

---

## 5. WebSocket Real-Time Communication (`websocket_flow.png`)

```mermaid
sequenceDiagram
    participant App as MainActivity
    participant WS as WebSocket Server<br/>ws://:8000/ws
    participant DOF as DailyOrderFragment

    App->>WS: Connect to ws://host:8000/ws
    WS-->>App: onOpen()

    loop Every 25 seconds
        App->>WS: send("ping")
        WS-->>App: onMessage("pong")
    end

    Note over WS: Another client creates an order

    WS-->>App: onMessage("Refetch orders")
    App->>DOF: fetchDailyOrders()
    DOF->>DOF: Refresh order list

    Note over App: User creates order in OrderDialogFragment

    App->>WS: send(orderJSON)
    WS-->>WS: Broadcast to other clients
```

---

## 6. Staff Recommendation Engine (`staff_recommendation.png`)

```mermaid
flowchart TD
    A[HomeFragment loads shift data] --> B[Fetch orders for selected date]
    B --> C[Calculate totalDayProducts]
    B --> D[Calculate totalShiftProducts<br/>by shift: Noon or Night]

    C --> E{Morning Packaging Staff}
    D --> F{Current Shift Cooks}

    E --> G{totalDayProducts < 200?}
    G -->|Yes| H[1 × Experienced Packager]
    G -->|No| I["(total / 200) × Experienced Packagers"]
    I --> J{"total remainder 200 > 100?"}
    J -->|Yes| K[2 × Junior Packagers]
    J -->|No| L[1 × Junior Packager]

    F --> M{totalShiftProducts < 100?}
    M -->|Yes| N[1 × Experienced Cook]
    M -->|No| O["(total / 100) × Experienced Cooks"]
    O --> P{"total remainder 100 > 50?"}
    P -->|Yes| Q[2 × Junior Cooks]
    P -->|No| R[1 × Junior Cook]

    H --> S[StaffRecommendationsAdapter<br/>displays employee cards]
    I --> S
    K --> S
    L --> S
    N --> S
    O --> S
    Q --> S
    R --> S
```

---

## 7. Data Model Relationships (`data_model.png`)

```mermaid
erDiagram
    CLIENT ||--o{ ORDER : places
    ORDER ||--|{ ORDER_DETAIL : contains
    PRODUCT ||--o{ ORDER_DETAIL : "ordered in"
    PRODUCT ||--|{ RECIPE : "made from"
    INGREDIENT ||--o{ RECIPE : "used in"
    INGREDIENT ||--o{ STOCK : "tracked in"
    PROVIDER ||--o{ STOCK : "supplies"

    CLIENT {
        string clientId PK
        string firmName
        string contactPerson
        string phoneNumber
        string location
        double latitude
        double longitude
        string address
        string type "SPECIAL | REGULAR | KINDERGARTEN"
    }

    ORDER {
        string orderId PK
        string clientId FK
        boolean deliveryNeeded
        string completionDate "yyyy-MM-dd"
        string completionTime "HH:00:00"
        double price
        boolean completed
    }

    ORDER_DETAIL {
        string orderId FK
        string productId FK
        int quantity
    }

    PRODUCT {
        string productId PK
        string name
        double price
        string imageUrl
    }

    RECIPE {
        string productId FK
        string ingredientId FK
        double quantity
    }

    INGREDIENT {
        string ingredientId PK
        string name
        string measurementUnit
        string packaging
    }

    STOCK {
        string ingredientId FK
        string providerId FK
        int quantity
        double price
        int maxQuantity
        int quantityPerPackage
    }

    PROVIDER {
        string providerId PK
        string name
        string phoneNumber
    }

    SENSOR {
        string sensorId PK
        string sensorName "Warehouse1 | Kitchen1"
        double temperature
        double humidity
        string timestamp
    }
```

---

## 8. API Architecture (`api_architecture.png`)

```mermaid
graph LR
    subgraph Android Client
        RI[RetrofitInstance]
        CM[ConfigManager]
        CM -->|baseUrl| RI
    end

    subgraph "Products & Inventory Server :8080"
        PE["/product"]
        CE["/client"]
        RE["/recipe"]
        IE["/ingredients"]
        SE["/stock"]
        PVE["/providers"]
    end

    subgraph "Orders Server :8000"
        OE["/orders"]
        ODE["/orders/{id}/details"]
        WSE["WebSocket /ws"]
    end

    subgraph "Sensor Server :8001"
        SDE["/sensor-data"]
        SDLH["/sensor-data/last-hour"]
        SDLD["/sensor-data/last-day"]
    end

    RI -->|BakeryAPI| PE
    RI -->|ClientAPI| CE
    RI -->|RecipeAPI| RE
    RI -->|IngredientsAPI| IE
    RI -->|StockAPI| SE
    RI -->|ProviderAPI| PVE
    RI -->|OrderAPI| OE
    RI -->|OrderAPI| ODE
    RI -->|OkHttp| WSE
    RI -->|SensorAPI| SDE
    RI -->|SensorAPI| SDLH
    RI -->|SensorAPI| SDLD
```
