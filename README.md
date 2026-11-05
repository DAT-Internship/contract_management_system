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
| `FINANCE` | `25-00002` | `finance123` |

Existing users are not modified. Use the Finance account to add invoices and record payments.

### Invoice form

- `description` is required.
- `invoiceNo` is optional and must be unique.
- `invoiceDate` defaults to the current date.
- `dueDate` is optional. When omitted, it defaults to the invoice date plus the backend payment term (30 days).
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

### Notifications and email

- Contract approval notifies active Finance users, the contract creator, contract manager, project in-charge, project manager, and Admin. The Finance notification links directly to the add-invoice form.
- Routine invoice creation/edits, payment updates, and ordinary contract edits do not create noisy Admin notifications or emails; audit logs retain the changes. When an invoice payment changes and leaves a balance outstanding, Finance, Admin, the contract creator, contract manager, and project manager receive an in-app notification and email showing paid and remaining amounts. Finance is notified when each invoice becomes fully paid. Finance, the contract manager, creator, project manager, and Admin are notified when every invoice is paid and total payments cover the contract's tax-inclusive gross amount.
- Payment reminders run 7 days before the due date, on the due date, and weekly while overdue (starting one day after the due date).
- Active contracts receive expiry reminders 90, 60, 30, and 7 days before the end date. The creator, contract manager, project in-charge, project manager, and Admin receive these reminders.
- Every contract edit notifies Admin. A transition to terminated/cancelled additionally notifies the contract stakeholders and Finance; terminated contracts are excluded from future expiry reminders.
- Notifications are stored in the user's notification inbox and email attempts are recorded in Email Monitoring. Admin receives the same update emails as notifications addressed to Admin.
- Configure `CMS_SMTP_HOST`, `CMS_SMTP_PORT`, `CMS_SMTP_USERNAME`, `CMS_SMTP_PASSWORD`, and `CMS_SMTP_FROM` for email delivery. Without valid SMTP settings, in-app notifications are still stored but email attempts are marked failed.

Database migrations contain schema changes only (`V1`–`V8`). Master data is not seeded by Flyway.

A ready-to-run seed is provided at `src/main/resources/db/seed/master-data.sql`. It contains all master data plus the `FINANCE`, `APPROVER`, and `OPERATOR` roles, the PMO department, and the correct currency codes. It is idempotent and can be re-run:

```bash
mariadb --skip-ssl -ucmsuser -proot cms_system < src/main/resources/db/seed/master-data.sql
```

The application initializer creates the default `ADMIN` and `FINANCE` roles and users listed above. Payment statuses must be named `Pending`, `Paid`, `Overdue`, and `Partially Paid` (any code is accepted; the service looks up by code first, then by name).
