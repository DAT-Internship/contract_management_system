# Contract Management System

## Project structure

```text
contract_management_system/
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties
├── src/
│   ├── main/
│   │   ├── java/com/cms/demo/
│   │   │   ├── Application.java
│   │   │   ├── config/.gitkeep
│   │   │   ├── controller/.gitkeep
│   │   │   ├── dto/.gitkeep
│   │   │   ├── entity/
│   │   │   │   ├── ApprovalStep.java
│   │   │   │   ├── AuditLog.java
│   │   │   │   ├── Contract.java
│   │   │   │   ├── ContractAttachment.java
│   │   │   │   ├── ContractRenewal.java
│   │   │   │   ├── CustomerSupplier.java
│   │   │   │   ├── Department.java
│   │   │   │   ├── EmailLog.java
│   │   │   │   ├── ImportBatch.java
│   │   │   │   ├── Invoice.java
│   │   │   │   ├── MasterAgreementFor.java
│   │   │   │   ├── MasterContractCategory.java
│   │   │   │   ├── MasterContractStatus.java
│   │   │   │   ├── MasterContractType.java
│   │   │   │   ├── MasterCurrency.java
│   │   │   │   ├── MasterPaymentStatus.java
│   │   │   │   ├── MasterPaymentType.java
│   │   │   │   ├── Notification.java
│   │   │   │   ├── Project.java
│   │   │   │   ├── Role.java
│   │   │   │   └── User.java
│   │   │   ├── exception/.gitkeep
│   │   │   ├── repository/.gitkeep
│   │   │   ├── security/.gitkeep
│   │   │   └── service/.gitkeep
│   │   └── resources/
│   │       ├── static/
│   │       │   ├── css/app.css
│   │       │   └── images/dat-logo.png
│   │       └── templates/
│   │           └── fragments/layout.html
│   └── test/
│       └── java/com/cms/demo/ApplicationTests.java
├── .gitignore
├── mvnw
├── mvnw.cmd
└── pom.xml
```

## Main directories

- `src/main/java/com/cms/demo/entity/` contains the JPA entity classes.
- `src/main/java/com/cms/demo/controller/` is for MVC controllers.
- `src/main/java/com/cms/demo/service/` is for application services.
- `src/main/java/com/cms/demo/repository/` is for data repositories.
- `src/main/resources/templates/` contains Thymeleaf views and fragments.
- `src/main/resources/static/` contains CSS, images, and other static assets.
- `src/test/` contains automated tests.

The `config/`, `controller/`, `dto/`, `exception/`, `repository/`, `security/`, and `service/` packages contain `.gitkeep` placeholders so Git preserves them until application files are added.

## Payment workspace

The payment module is a Thymeleaf workspace under `/payments`. All pages require an authenticated session.

### Pages

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/payments` | Payment KPI overview and paginated contract payment list. Optional query params: `q`, `status`, `paymentTypeId`, `categoryId`, `page`, `pageSize` |
| `GET` | `/payments/{contractId}` | Contract payment summary, invoices, and audit history |
| `POST` | `/payments/{contractId}/invoices/save` | Create or update an invoice from the invoice modal |
| `POST` | `/payments/{contractId}/payment` | Record a cumulative payment for an invoice |

The invoice and payment forms are Bootstrap modals on the contract payment detail page. The sidebar `Payments` link and the `Payments` action on a contract detail page open the workspace. Finance users can add invoices from the `Add Invoice` button on a payments list row or the `Add Invoice` button on the payment detail page.

The payments list renders each contract across two rows. The first row shows the contract figures; the second row contains only the `View` and `Add Invoice` actions.

Each invoice row has `View` and `Pay` actions. `View` is available to all authenticated users; `Pay` is limited to Finance users.

### Default users

The application initializer creates these users when they do not already exist:

| Role | Employee code | Password |
| --- | --- | --- |
| `ADMIN` | `25-00001` | `admin123` |

Existing users are not modified. Create a user with the `FINANCE` role in User Management to add invoices and record payments.

### Invoice form

- `description` is required.
- `invoiceNo` is optional and must be unique.
- `invoiceDate` defaults to the current date.
- `dueDate` is optional. When omitted, it is calculated from the contract payment type: Monthly adds 1 month, Quarterly 3 months, Semi-Annual 6 months, Annual 12 months, and Lump Sum uses the invoice date. Milestone-based payments require the due date to be entered manually.
- `amount` is the amount before tax.
- If `taxAmount` is omitted, the contract commercial tax rate is applied. The form also recalculates tax from the amount and contract rate as you type.
- If `currencyId` is omitted, the contract currency is used.
- `paymentStatusId` is optional. Leave it blank to derive the status automatically from the paid amount and due date, or select a master payment status explicitly.

### Payment form

`paidAmount` is the cumulative amount received, not the amount of one transaction. For example, after receiving 300 and then another 200 on a 1,000 invoice, submit `500`.

### Status rules

- `PAID`: cumulative paid amount covers the invoice total.
- `OVERDUE`: unpaid or partially paid and the due date has passed.
- `PARTIALLY_PAID`: some amount received, due date not passed.
- `PENDING`: no payment received, due date not passed.
- When no payment status is selected in the invoice form, the invoice status is derived using the rules above.
- Contract-level status is `NO_INVOICES`, `OVERDUE`, `PAID`, `PARTIALLY_PAID`, or `PENDING`, derived from its invoices and contract value.

### Permissions and audit

- Only `FINANCE` users can create invoices, update invoices, and record payments. Other authenticated users have read-only access.
- All authenticated users can read payment data for contracts visible to them.
- Every write records an `audit_logs` row with `entity_type = 'INVOICE'` and `old_value` / `new_value` JSON (`CREATED`, `INVOICE_UPDATED`, `PAYMENT_UPDATED`, `STATUS_CHANGED`).
- A daily scheduler (`app.payment.overdue-cron`, default `0 5 0 * * *`) persists `OVERDUE` status and records a `STATUS_CHANGED` audit entry.

Database migrations contain schema changes only (`V1`–`V8`). Master data is not seeded by Flyway.

A ready-to-run seed is provided at `src/main/resources/db/seed/master-data.sql`. It contains all master data plus the `FINANCE`, `APPROVER`, and `OPERATOR` roles, the PMO department, and the correct currency codes. It is idempotent and can be re-run:

```bash
mariadb --skip-ssl -ucmsuser -proot cms_system < src/main/resources/db/seed/master-data.sql
```

The application initializer creates only the default `ADMIN` role and user listed above. Create Finance users manually. Payment statuses must be named `Pending`, `Paid`, `Overdue`, and `Partially Paid` (any code is accepted; the service looks up by code first, then by name).
