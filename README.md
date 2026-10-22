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
