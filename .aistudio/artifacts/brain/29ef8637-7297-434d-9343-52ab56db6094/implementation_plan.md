# My Shop - Cloud Firestore & Offline Sync Architecture

Configure Firebase Firestore as the permanent, authoritative cloud database for **My Shop**, pairing it with an offline-first native Room database (serving as the Android counterpart to web IndexedDB) with automatic two-way synchronization, Google Sign-In authentication, zero-duplicate transaction integrity, JSON backup & restore, CSV export, and secure Firestore security rules.

## User Review & Critical Decisions

> [!IMPORTANT]
> The architectural directions below were confirmed in Phase 1 clarification:

- **Confirmed Decision 1 (Offline Engine)**: Native Android Room database with an active mutation queue (`sync_queue`) providing instant offline reads/writes and reliable two-way background sync with Firestore when connectivity is restored.
- **Confirmed Decision 2 (Authentication)**: Single-owner Google Account sign-in using Android Jetpack `CredentialManager` (`GetSignInWithGoogleOption`). All shop business records are isolated and guarded by Firestore security rules locked to the shop owner's authenticated UID.
- **Confirmed Decision 3 (Conflict & Transaction Handling)**: Latest timestamp wins (Last-Write-Wins with audit metadata). Transactions use immutable, client-generated unique UUID idempotency tokens (`txn_${timestamp}_${uuid}`) so repeated offline sync attempts can never produce duplicate sales or inaccurate stock deductions.

---

## 1. Overview & Core Concept

**My Shop** is a point-of-sale (POS) and inventory management platform designed to run reliably on Android devices and share data seamlessly with desktop web browsers via Firebase Cloud Firestore.

### Key Capabilities
- **Universal Shop Entities**: Complete lifecycle tracking for **Products & Barcodes**, **Stock Movements**, **Purchases**, **Sales Transactions**, **Customers**, **Suppliers**, **Payments**, **Expenses**, and **Business Reports**.
- **Permanent Cloud Source of Truth**: All operational records are synchronized to Cloud Firestore so the shop's data is never trapped on a single device and survives app reinstalls or device upgrades.
- **Offline-First Resilience**: Full offline operation. Cashiers can scan items, ring up sales, update inventory, and log payments without an internet connection. Mutations are recorded locally in Room and automatically synchronized to Firestore once network connectivity returns.
- **Data Integrity & Idempotency**: Unique transaction IDs with deduplication guarantees prevent double-charging or duplicate inventory decrements during intermittent network transitions.
- **Data Portability & Peace of Mind**: Manual full-database JSON backup and restore, plus CSV exports for products, sales, inventory, and expense ledgers. A live top-bar sync badge indicates whether the app is `Synced`, `Syncing`, or `Offline (N pending)`.

---

## 2. User Experience & Visual Design

### Visual Identity & Theme
- **Aesthetic Direction**: High-trust, professional retail workstation. High contrast, clean cards, prominent numerical displays for prices and stock counts, and tactile feedback.
- **Color Palette**:
  - Dominant Dark Slate / Midnight Navy surface (`#0F172A`, `#1E293B`)
  - Emerald Green accent for positive cash flow, sales, and healthy stock (`#10B981`)
  - Indigo / Cobalt for primary navigation and action buttons (`#3B82F6` / `#6366F1`)
  - Amber / Coral for low-stock alerts, pending sync items, and payment dues (`#F59E0B`, `#EF4444`)
- **Typography & Hierarchy**: Clear, monospace tabular figures for monetary amounts and quantities; bold Sans-Serif headings for product titles and screen sections.

### Key User Flows & Screen Navigation
1. **Authentication Gate**:
   - Clean, professional login screen featuring "Sign in with Google" via Jetpack Credential Manager.
   - Upon sign-in, the user's shop profile is initialized or loaded from Firestore.
2. **POS Checkout (Sales Counter)**:
   - Barcode search / rapid product catalog grid.
   - Real-time cart calculation with tax, discounts, and payment method selector (Cash, Card, Digital Wallet, Due/Credit).
   - Instant receipt generation with unique transaction ID and local stock decrement.
3. **Inventory & Products**:
   - Product list with search by barcode or name, category filtering, and low-stock warnings.
   - Add/Edit product dialog (barcode, cost price, retail price, current stock, min stock threshold).
   - Stock movement ledger showing timestamped entries for adjustments, sales, and supplier purchases.
4. **Purchases & Suppliers**:
   - Record stock replenishment from suppliers with invoice reference and payment status.
   - Supplier ledger tracking outstanding balances and payment history.
5. **Customers & Accounts**:
   - Customer directory with store credit/due tracking and payment collection.
6. **Expenses & Cash Flow**:
   - Log shop utility bills, rent, wages, and petty cash outlays categorized by type.
7. **Reports & Analytics**:
   - Daily, weekly, and monthly sales volume, gross profit, inventory valuation, and top-selling products.
   - One-tap CSV export of sales and inventory reports.
8. **Sync & Backup Center**:
   - Live sync status card with pending sync queue inspector and "Sync Now" button.
   - "Export JSON Backup" and "Restore from JSON" tools for complete disaster recovery.

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Room DB as Local Cache & Offline Sync Engine
- **Approach**: Room SQLite database mirrors the Firestore data model with an additional `sync_queue` table capturing all offline creations, modifications, and deletions.
- **Why**: Room provides sub-millisecond local queries, reactive Flow streams for the Jetpack Compose UI, and 100% offline uptime. Background sync workers reconcile Room with Firestore automatically.
- **Alternative Considered**: Relying purely on Firestore's built-in SDK offline cache. While Firestore has internal caching, a dedicated Room database with explicit `sync_queue` provides fine-grained control over conflict resolution, manual JSON backup/export, and visible pending item inspection.

### Decision 2: Google Sign-In & Single-Owner Shop Scoping
- **Approach**: Jetpack Credential Manager with Google Sign-In. All Firestore documents are scoped under the shop owner's UID: `/users/{ownerUid}/...`.
- **Why**: Google Sign-In is secure, standard, and provisioned without manual password management. It enables the same shop owner to log into both this Android app and any desktop web dashboard with identical credentials.

### Decision 3: Idempotent Transactions & UUID Generation
- **Approach**: Every sale and stock movement is stamped at creation with a deterministic client-generated UUID (`txn_${System.currentTimeMillis()}_${UUID.randomUUID()}`).
- **Why**: When an offline device reconnects, repeated sync retry attempts or transient network timeouts will never insert duplicate sales or double-decrement stock on Firestore.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI Layer                        │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌───────────────┐ │
│  │ POS Checkout │ │  Inventory   │ │ Customers &  │ │   Reports &   │ │
│  │   & Cart     │ │  & Barcodes  │ │  Suppliers   │ │  Sync Status  │ │
│  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘ └───────┬───────┘ │
└─────────┼────────────────┼────────────────┼─────────────────┼──────────┘
          ▼                ▼                ▼                 ▼
┌────────────────────────────────────────────────────────────────────────┐
│                   ViewModels & State Management                        │
│    (ShopViewModel, PosViewModel, InventoryViewModel, SyncViewModel)    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                       ShopRepository & SyncManager                     │
│  - Executes immediate writes to Room DB                                │
│  - Appends mutations to SyncQueue                                      │
│  - Triggers bidirectional sync when network is active                  │
└───────────────────┬────────────────────────────────┬───────────────────┘
                    │                                │
                    ▼                                ▼
┌─────────────────────────────────────┐  ┌───────────────────────────────┐
│       Local Persistence (Room)      │  │    Remote Persistence (Cloud) │
│  ┌───────────────────────────────┐  │  │  ┌──────────────────────────┐ │
│  │ Products, Barcodes, Inventory │  │  │  │ Google Cloud Firestore   │ │
│  ├───────────────────────────────┤  │  │  │ (Custom Database ID)     │ │
│  │ Sales, Purchases, StockMoves  │  │  │  ├──────────────────────────┤ │
│  ├───────────────────────────────┤  │  │  │ Firebase Authentication  │ │
│  │ Customers, Suppliers, Expense │  │  │  │ (Google Sign-In)         │ │
│  ├───────────────────────────────┤  │  │  ├──────────────────────────┤ │
│  │ SyncQueue (Pending mutations) │  │  │  │ Hardened Security Rules  │ │
│  └───────────────────────────────┘  │  │  └──────────────────────────┘ │
└─────────────────────────────────────┘  └───────────────────────────────┘
```

### Data Schema Overview (Room Entities & Firestore Documents)

1. **`products`**:
   `id`, `barcode`, `name`, `category`, `costPrice`, `sellingPrice`, `stockQuantity`, `minStockAlert`, `updatedAt`, `syncStatus`
2. **`stock_movements`**:
   `id`, `productId`, `productName`, `deltaQuantity`, `movementType` (`SALE`, `PURCHASE`, `ADJUSTMENT`, `RETURN`), `referenceId`, `notes`, `timestamp`
3. **`sales`**:
   `id` (unique transaction ID), `customerName`, `customerPhone`, `itemsJson`, `subtotal`, `tax`, `discount`, `totalAmount`, `paymentMethod` (`CASH`, `CARD`, `ONLINE`, `DUE`), `timestamp`, `syncStatus`
4. **`purchases`**:
   `id`, `supplierName`, `invoiceNumber`, `itemsJson`, `totalAmount`, `paymentStatus`, `timestamp`, `syncStatus`
5. **`customers` & `suppliers`**:
   `id`, `name`, `phone`, `email`, `outstandingBalance`, `updatedAt`
6. **`expenses` & `payments`**:
   `id`, `category`, `amount`, `paymentMethod`, `notes`, `timestamp`
7. **`sync_queue`**:
   `id`, `entityType`, `entityId`, `action` (`CREATE`, `UPDATE`, `DELETE`), `payloadJson`, `timestamp`, `retryCount`

### Step-by-Step Execution Plan
1. **Firebase Provisioning & Dependencies**:
   - Extract debug keystore fingerprints and execute `ProvisionFirebase` via `rpc_action`.
   - Activate Firestore, Auth, Credential Manager, and Room dependencies in `build.gradle.kts`.
2. **Schema & Security Rules Testing**:
   - Author `firebase-blueprint.json` and strict `firestore.rules` enforcing user isolation (`request.auth.uid == userId`).
   - Run JavaScript rules unit tests (`node --test firestore.test.js`) and Robolectric integration tests.
   - Deploy rules via `DeployRules` RPC.
3. **Room Database Implementation**:
   - Define entities, DAOs, and Room database with offline sync queue.
4. **Sync Engine & Repository**:
   - Implement `ShopRepository` and `SyncEngine` handling automatic two-way synchronization, network state observation, and conflict resolution.
5. **JSON Backup/Restore & CSV Export**:
   - Implement complete database serialization/deserialization with Android Storage Access Framework (SAF) or share intents.
6. **Jetpack Compose UI**:
   - Google Sign-In auth gate.
   - POS checkout, inventory catalog, customer/supplier ledgers, expense tracker, reports, and sync manager screens.
   - Top-bar sync status indicator and responsive M3 layout.
7. **Verification**:
   - End-to-end compilation with `compile_applet`.
