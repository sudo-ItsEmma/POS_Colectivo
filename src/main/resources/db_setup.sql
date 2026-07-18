-- =============================================================================
-- SISTEMA DE PUNTO DE VENTA - POS COLECTIVO | v4.0 (Corregido y Actualizado)
-- =============================================================================

CREATE DATABASE IF NOT EXISTS pos_colectivo
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE pos_colectivo;

-- 1. Role
CREATE TABLE IF NOT EXISTS Role (
    idRole          TINYINT UNSIGNED    NOT NULL AUTO_INCREMENT,
    roleName        VARCHAR(50)         NOT NULL COMMENT 'Admin, Sales',
    roleDescription VARCHAR(255)                 COMMENT 'Descripción de permisos',
    CONSTRAINT pk_Role          PRIMARY KEY (idRole),
    CONSTRAINT uq_Role_name     UNIQUE      (roleName)
) ENGINE=InnoDB;

-- 2. Employee
CREATE TABLE IF NOT EXISTS Employee (
    idEmployee              INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    firstNameEmployee       VARCHAR(100)    NOT NULL,
    lastNameEmployee        VARCHAR(100)    NOT NULL,
    secondLastNameEmployee  VARCHAR(100),
    phoneEmployee           VARCHAR(20),
    isEmployeeActive        TINYINT(1)      NOT NULL DEFAULT 1,
    CONSTRAINT pk_Employee PRIMARY KEY (idEmployee)
) ENGINE=InnoDB;

-- 3. UserAccount
CREATE TABLE IF NOT EXISTS UserAccount (
    idUserAccount   INT UNSIGNED        NOT NULL AUTO_INCREMENT,
    idEmployee      INT UNSIGNED        NOT NULL,
    idRole          TINYINT UNSIGNED    NOT NULL,
    usernameAccount VARCHAR(100)        NOT NULL,
    passwordAccount VARCHAR(255)        NOT NULL,
    lastLoginDate   DATETIME,
    isAccountActive TINYINT(1)          NOT NULL DEFAULT 1,
    mustChangePassword TINYINT(1)       NOT NULL DEFAULT 0,
    CONSTRAINT pk_UserAccount           PRIMARY KEY (idUserAccount),
    CONSTRAINT uq_UserAccount_employee  UNIQUE      (idEmployee),
    CONSTRAINT uq_UserAccount_username  UNIQUE      (usernameAccount),
    CONSTRAINT fk_UserAccount_Employee  FOREIGN KEY (idEmployee)  REFERENCES Employee(idEmployee),
    CONSTRAINT fk_UserAccount_Role      FOREIGN KEY (idRole)      REFERENCES Role(idRole)
) ENGINE=InnoDB;

-- 4. PaymentMethod
CREATE TABLE IF NOT EXISTS PaymentMethod (
    idPaymentMethod TINYINT UNSIGNED    NOT NULL AUTO_INCREMENT,
    methodName      VARCHAR(50)         NOT NULL,
    CONSTRAINT pk_PaymentMethod         PRIMARY KEY (idPaymentMethod),
    CONSTRAINT uq_PaymentMethod_name    UNIQUE      (methodName)
) ENGINE=InnoDB;

-- 5. Entrepreneur
CREATE TABLE IF NOT EXISTS Entrepreneur (
    idEntrepreneur      INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    brandName           VARCHAR(150)    NOT NULL,
    contactName         VARCHAR(200)    NOT NULL,
    contactPhone        VARCHAR(20),
    emailEntrepreneur   VARCHAR(150),
    contractSignDate    DATE            NOT NULL,
    monthlyRentAmount   DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    isEntityActive      TINYINT(1)      NOT NULL DEFAULT 1,
    CONSTRAINT pk_Entrepreneur PRIMARY KEY (idEntrepreneur)
) ENGINE=InnoDB;

-- 6. Product
CREATE TABLE IF NOT EXISTS Product (
    idProduct           INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    idEntrepreneur      INT UNSIGNED    NOT NULL,
    fullProductCode     VARCHAR(10)     NOT NULL,
    productDescription  VARCHAR(255),
    department          VARCHAR(100),
    currentPrice        DECIMAL(10,2)   NOT NULL,
    currentStock        INT UNSIGNED    NOT NULL DEFAULT 0,
    minStockAlert       INT UNSIGNED             DEFAULT 1,
    isProductActive     TINYINT(1)      NOT NULL DEFAULT 1,
    CONSTRAINT pk_Product               PRIMARY KEY (idProduct),
    CONSTRAINT uq_Product_code          UNIQUE      (fullProductCode),
    CONSTRAINT fk_Product_Entrepreneur  FOREIGN KEY (idEntrepreneur) REFERENCES Entrepreneur(idEntrepreneur)
) ENGINE=InnoDB;

-- 7. Sale
CREATE TABLE IF NOT EXISTS Sale (
    idSale              INT UNSIGNED        NOT NULL AUTO_INCREMENT,
    idUserAccount       INT UNSIGNED        NOT NULL,
    idPaymentMethod     TINYINT UNSIGNED    NOT NULL,
    saleDateTime        DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    totalSaleAmount     DECIMAL(10,2)       NOT NULL,
    saleStatus          VARCHAR(20)         NOT NULL DEFAULT 'Activa',
    paymentDetails      VARCHAR(100),
    -- Nullable: solo se llena cuando la venta viene de liquidar un apartado
    -- (ver Booking, tabla #10 más abajo — el FK se agrega después de esa tabla
    -- porque Booking se define más adelante en este script). Sirve para que
    -- Arqueo/Corte de Caja puedan excluir estas ventas de "efectivo del día":
    -- su dinero ya se cuenta a través de BookingPayment (anticipo + abonos +
    -- pago final), y sumarlas también aquí duplicaría el anticipo.
    idBooking           INT UNSIGNED,
    CONSTRAINT pk_Sale                  PRIMARY KEY (idSale),
    CONSTRAINT fk_Sale_UserAccount      FOREIGN KEY (idUserAccount)   REFERENCES UserAccount(idUserAccount),
    CONSTRAINT fk_Sale_PaymentMethod    FOREIGN KEY (idPaymentMethod) REFERENCES PaymentMethod(idPaymentMethod)
) ENGINE=InnoDB;

-- 8. SaleDetail
CREATE TABLE IF NOT EXISTS SaleDetail (
    idSaleDetail        INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    idSale              INT UNSIGNED    NOT NULL,
    idProduct           INT UNSIGNED    NOT NULL,
    quantitySold        INT UNSIGNED    NOT NULL DEFAULT 1,
    unitPriceAtSale     DECIMAL(10,2)   NOT NULL,
    discountApplied     DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    subtotalDetail      DECIMAL(10,2)   NOT NULL,
    isSettled           TINYINT(1)      NOT NULL DEFAULT 0,
    -- Nullable: solo se llena cuando isSettled pasa a 1, señalando exactamente qué Settlement
    -- pagó esta línea (sin esto, solo se sabría por isSettled=1 + fecha aproximada, ambiguo
    -- si un emprendedor recibe varios pagos). El FK se agrega después de la tabla Settlement
    -- (definida más abajo en este script) por el mismo motivo que Sale.idBooking.
    idSettlement        INT UNSIGNED,
    CONSTRAINT pk_SaleDetail            PRIMARY KEY (idSaleDetail),
    CONSTRAINT fk_SaleDetail_Sale       FOREIGN KEY (idSale)      REFERENCES Sale(idSale),
    CONSTRAINT fk_SaleDetail_Product    FOREIGN KEY (idProduct)   REFERENCES Product(idProduct)
) ENGINE=InnoDB;

-- 9. CashSession
CREATE TABLE IF NOT EXISTS CashSession (
    idCashSession       INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    idUserAccount       INT UNSIGNED    NOT NULL,
    openingDateTime     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closingDateTime     DATETIME,
    initialCashAmount   DECIMAL(10,2)   NOT NULL DEFAULT 600.00,
    finalCashAmount     DECIMAL(10,2),
    theoricalAmount     DECIMAL(10,2),
    cashDifference      DECIMAL(10,2),
    -- Desglose del día, guardado al hacer el Corte de Caja (cerrarCaja()), para que
    -- reportes diarios/semanales/mensuales y auditorías puedan sumar estas columnas
    -- directo desde CashSession sin tener que volver a calcular desde Sale/
    -- BookingPayment cada vez (esas tablas solo tienen los movimientos, no un
    -- resumen por día ya armado).
    cashSalesAmount         DECIMAL(10,2),
    cashBookingPaymentsAmount DECIMAL(10,2),
    transferSalesAmount     DECIMAL(10,2),
    transferSalesCount      INT UNSIGNED,
    bookingsNewAmount       DECIMAL(10,2),
    bookingsPaymentsAmount  DECIMAL(10,2),
    -- Desglose de los dos de arriba por método de pago (Efectivo/Transferencia),
    -- para poder cuadrar cuentas y generar reportes sin tener que volver a leer
    -- BookingPayment fila por fila. La porción en efectivo se puede derivar
    -- restando estas columnas de bookingsNewAmount/bookingsPaymentsAmount.
    bookingsNewAmountTransfer      DECIMAL(10,2),
    bookingsPaymentsAmountTransfer DECIMAL(10,2),
    sessionStatus       VARCHAR(20)     NOT NULL DEFAULT 'Abierta',
    -- Columna generada + UNIQUE: solo puede existir una fila con sessionStatus='Abierta'
    -- a la vez (las demás filas generan NULL, y un UNIQUE en MariaDB permite múltiples
    -- NULL). Esto hace atómica la apertura de caja a nivel de BD: dos INSERT
    -- concurrentes con sessionStatus='Abierta' chocan contra este constraint en vez
    -- de depender de un SELECT-luego-INSERT en Java, que sí tiene una carrera posible.
    openSessionGuard    TINYINT GENERATED ALWAYS AS (CASE WHEN sessionStatus = 'Abierta' THEN 1 ELSE NULL END) VIRTUAL,
    CONSTRAINT pk_CashSession               PRIMARY KEY (idCashSession),
    CONSTRAINT fk_CashSession_UserAccount   FOREIGN KEY (idUserAccount) REFERENCES UserAccount(idUserAccount),
    CONSTRAINT uk_CashSession_openSessionGuard UNIQUE (openSessionGuard)
) ENGINE=InnoDB;

-- 9.1 CashCount (Arqueo de caja: puede haber varios por sesión, a diferencia del
-- corte final que es único y vive en las columnas de CashSession)
CREATE TABLE IF NOT EXISTS CashCount (
    idCashCount             INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    idCashSession           INT UNSIGNED    NOT NULL,
    idUserAccount           INT UNSIGNED    NOT NULL,
    countDateTime           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    theoricalAmount         DECIMAL(10,2)   NOT NULL,
    countedAmount           DECIMAL(10,2)   NOT NULL,
    cashDifference          DECIMAL(10,2)   NOT NULL,
    justificationComment    VARCHAR(255),
    CONSTRAINT pk_CashCount                PRIMARY KEY (idCashCount),
    CONSTRAINT fk_CashCount_CashSession     FOREIGN KEY (idCashSession) REFERENCES CashSession(idCashSession),
    CONSTRAINT fk_CashCount_UserAccount     FOREIGN KEY (idUserAccount) REFERENCES UserAccount(idUserAccount)
) ENGINE=InnoDB;

-- 10. Booking (Cabecera)
CREATE TABLE IF NOT EXISTS Booking (
    idBooking       INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    idUserAccount   INT UNSIGNED    NOT NULL,
    customerName    VARCHAR(200)    NOT NULL,
    customerPhone   VARCHAR(20),
    bookingDate     DATE            NOT NULL DEFAULT (CURDATE()),
    expirationDate  DATE            NOT NULL,
    totalAmount     DECIMAL(10,2)   NOT NULL,
    advanceAmount   DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    pendingBalance  DECIMAL(10,2)   NOT NULL,
    bookingStatus   VARCHAR(20)     NOT NULL DEFAULT 'Activo',
    CONSTRAINT pk_Booking PRIMARY KEY (idBooking),
    CONSTRAINT fk_Booking_UserAccount FOREIGN KEY (idUserAccount) REFERENCES UserAccount(idUserAccount)
) ENGINE=InnoDB;

-- El FK de Sale.idBooking se agrega hasta aquí porque Booking se define después
-- de Sale en este script (Sale es la tabla #7, Booking la #10).
ALTER TABLE Sale ADD CONSTRAINT fk_Sale_Booking FOREIGN KEY (idBooking) REFERENCES Booking(idBooking);

-- 10.1 BookingDetail (Los productos del carrito)
CREATE TABLE IF NOT EXISTS BookingDetail (
    idBookingDetail INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    idBooking       INT UNSIGNED    NOT NULL,
    idProduct       INT UNSIGNED    NOT NULL,
    quantity        INT UNSIGNED    NOT NULL DEFAULT 1,
    unitPrice       DECIMAL(10,2)   NOT NULL,
    subtotalDetail  DECIMAL(10,2)   NOT NULL,
    CONSTRAINT pk_BookingDetail PRIMARY KEY (idBookingDetail),
    CONSTRAINT fk_BookingDetail_Booking FOREIGN KEY (idBooking) REFERENCES Booking(idBooking),
    CONSTRAINT fk_BookingDetail_Product FOREIGN KEY (idProduct) REFERENCES Product(idProduct)
) ENGINE=InnoDB;

-- 11. BookingPayment (Historial de abonos con soporte multimetodo)
CREATE TABLE IF NOT EXISTS BookingPayment (
    idBookingPayment    INT UNSIGNED        NOT NULL AUTO_INCREMENT,
    idBooking           INT UNSIGNED        NOT NULL,
    idPaymentMethod     TINYINT UNSIGNED    NOT NULL DEFAULT 1, -- Coincide perfectamente con TINYINT UNSIGNED de PaymentMethod
    paymentAmount       DECIMAL(10,2)       NOT NULL,
    paymentDate         DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_BookingPayment            PRIMARY KEY (idBookingPayment),
    CONSTRAINT fk_BookingPayment_Booking   FOREIGN KEY (idBooking) REFERENCES Booking(idBooking),
    CONSTRAINT fk_BookingPayment_Method    FOREIGN KEY (idPaymentMethod) REFERENCES PaymentMethod(idPaymentMethod)
) ENGINE=InnoDB;

-- 12. ProductReturn
CREATE TABLE IF NOT EXISTS ProductReturn (
    idReturn        INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    idSaleDetail    INT UNSIGNED    NOT NULL,
    idUserAccount   INT UNSIGNED    NOT NULL,
    returnReason    VARCHAR(255),
    returnDateTime  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    refundAmount    DECIMAL(10,2)   NOT NULL,
    CONSTRAINT pk_ProductReturn                 PRIMARY KEY (idReturn),
    -- Una línea de venta (SaleDetail) solo se puede devolver una vez: el motor
    -- lo garantiza, no solo el chequeo en Java (mismo criterio que openSessionGuard en CashSession).
    CONSTRAINT uq_ProductReturn_SaleDetail      UNIQUE      (idSaleDetail),
    CONSTRAINT fk_ProductReturn_SaleDetail      FOREIGN KEY (idSaleDetail)  REFERENCES SaleDetail(idSaleDetail),
    CONSTRAINT fk_ProductReturn_UserAccount     FOREIGN KEY (idUserAccount) REFERENCES UserAccount(idUserAccount)
) ENGINE=InnoDB;

-- 13. Settlement
CREATE TABLE IF NOT EXISTS Settlement (
    idSettlement        INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    idEntrepreneur      INT UNSIGNED    NOT NULL,
    idUserAccount       INT UNSIGNED    NOT NULL,
    settlementDate      DATE            NOT NULL,
    periodStartDate     DATE            NOT NULL,
    periodEndDate       DATE            NOT NULL,
    grossAmount         DECIMAL(10,2)   NOT NULL,
    totalDiscounts      DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    rentDiscount        DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    otherDiscounts      DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    netAmountPaid       DECIMAL(10,2)   NOT NULL,
    CONSTRAINT pk_Settlement                PRIMARY KEY (idSettlement),
    CONSTRAINT fk_Settlement_Entrepreneur   FOREIGN KEY (idEntrepreneur) REFERENCES Entrepreneur(idEntrepreneur),
    CONSTRAINT fk_Settlement_UserAccount    FOREIGN KEY (idUserAccount)  REFERENCES UserAccount(idUserAccount)
) ENGINE=InnoDB;

-- El FK de SaleDetail.idSettlement se agrega hasta aquí porque Settlement se define después
-- de SaleDetail en este script (SaleDetail es la tabla #8, Settlement la #13).
ALTER TABLE SaleDetail ADD CONSTRAINT fk_SaleDetail_Settlement FOREIGN KEY (idSettlement) REFERENCES Settlement(idSettlement);

-- DATA INICIAL DE CATÁLOGOS (SEED DATA)
INSERT IGNORE INTO Role (roleName, roleDescription) VALUES
    ('Admin', 'Acceso total al sistema'),
    ('Sales', 'Acceso operativo y ventas');

INSERT IGNORE INTO PaymentMethod (methodName) VALUES
    ('Efectivo'),
    ('Transferencia'),
    ('Mixto');