package com.coldconnect.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title   = "Cold Connect API",
                version = "1.0",
                description = """
            Truewatt Cold Connect — Food & Package Delivery Platform

            **Base URL:** https://cold-connect.onrender.com

            ## Authentication
            - **Customers & Drivers** → Phone OTP:
              `POST /v1/auth/signup` → `POST /v1/auth/otp/verify`
            - **Admins & Operators** → Email/Password:
              `POST /api/auth/register` → `POST /api/auth/login`

            ## Test OTP
            During development use `1234` as OTP for any phone number.

            ## Language
            `en` · `ha` · `yo` · `ig` · `pcm`

            ---

            ## Enum Reference

            ### Role
            `CUSTOMER` · `DRIVER` · `OPERATOR` · `ADMIN`

            ### Booking — serviceType
            `STORAGE` · `TRANSPORT` · `PICKUP` · `BUNDLE`

            ### Booking — status
            `PENDING` · `CONFIRMED` · `IN_PROGRESS` · `COMPLETED` · `CANCELLED`

            ### Booking — paymentStatus
            `UNPAID` · `PARTIALLY_PAID` · `PAID` · `REFUNDED`

            ### Payment — method
            `CASH` · `BANK_TRANSFER` · `WALLET` · `CARD`

            ### Payment — status
            `PENDING` · `CAPTURED` · `FAILED` · `REFUNDED`

            ### Wallet — method
            `BANK_TRANSFER` · `CARD` · `CASH`

            ### Marketplace — lot status
            `DRAFT` · `LIVE` · `RESERVED` · `SOLD` · `WITHDRAWN`

            ### Marketplace — order status
            `PENDING` · `CONFIRMED` · `PACKED` · `IN_TRANSIT` · `DELIVERED` · `COLLECTED` · `CANCELLED`

            ### Marketplace — fulfilmentType
            `DELIVERY` · `COLLECTION`

            ### Marketplace — paymentPreference
            `PREPAID` · `PAY_ON_DELIVERY` · `CREDIT`

            ### Support — type
            `DISPUTE` · `MISSING_CRATE` · `FEE_ISSUE` · `TEMP_CONCERN` · `DRIVER_INCIDENT`

            ### Support — severity
            `CRITICAL` · `HIGH` · `MEDIUM` · `LOW`

            ### Crate — status
            `INTAKE` · `IN_STORAGE` · `IN_TRANSIT` · `DELIVERED` · `SOLD` · `LOST`

            ### Hub — status
            `ACTIVE` · `INACTIVE` · `FULL` · `MAINTENANCE`

            ### Hub — powerType
            `SOLAR` · `GRID` · `HYBRID` · `GENERATOR`

            ### Hub — powerStatus
            `ON_GRID` · `ON_SOLAR` · `ON_BATTERY` · `OUTAGE`

            ### Trip — status
            `PLANNED` · `IN_PROGRESS` · `COMPLETED` · `CANCELLED`

            ### Safety check — result
            `PASS` · `FAIL` · `PENDING`

            ### Safety check item — mark
            `ok` · `defect`

            ### Safety check item — severity
            `CRITICAL` · `MAJOR` · `MINOR`

            ### Driver — vettingStatus
            `PENDING` · `APPROVED` · `REJECTED` · `SUSPENDED`

            ### Driver — trainingStatus
            `NOT_STARTED` · `IN_PROGRESS` · `COMPLETED`

            ### Vehicle — status
            `ACTIVE` · `INACTIVE` · `MAINTENANCE`

            ### Inventory event — eventType
            `CHECK_IN` · `ZONE_MOVE` · `QUALITY_CHECK` · `CHECKOUT` · `DISPUTE` · `LOSS` · `SALE`

            ### Crate status after inventory event
            `INTAKE` · `IN_STORAGE` · `IN_TRANSIT` · `DELIVERED` · `SOLD` · `LOST`

            ### Consent — consentStatus
            `accepted` · `declined`

            ### Customer type — persona
            `FARMER` · `MARKET_TRADER` · `COOPERATIVE` · `BUYER` · `PROCESSOR`

            ### Partner — partnerType
            `FUNDER` · `LENDER` · `BUYER` · `HUB_HOST` · `DISTRIBUTOR`

            ### Waitlist — status
            `WAITING` · `NOTIFIED` · `EXPIRED` · `CANCELLED`

            ### Notification — channel
            `APP` · `SMS` · `WHATSAPP`

            ### KYB — status
            `UNVERIFIED` · `PREPAID_ONLY` · `PAY_ON_DELIVERY` · `CREDIT_APPROVED`
            """,
                contact = @Contact(
                        name  = "Cold Connect Dev",
                        email = "dev@coldconnect.app"
                )
        ),
        servers = {
                @Server(url = "https://cold-connect.onrender.com", description = "Production"),
                @Server(url = "http://localhost:8080",             description = "Local")
        }
)
@SecurityScheme(
        name         = "bearerAuth",
        type         = SecuritySchemeType.HTTP,
        scheme       = "bearer",
        bearerFormat = "JWT",
        description  = "Paste accessToken from POST /v1/auth/otp/verify or POST /api/auth/login"
)
public class OpenApiConfig {

        @Bean
        public GroupedOpenApi customerApi() {
                return GroupedOpenApi.builder()
                        .group("1-customer")
                        .displayName("Customer API")
                        .pathsToMatch(
                                "/v1/auth/**",
                                "/v1/profile/**",
                                "/v1/regions/**",
                                "/v1/hubs/**",
                                "/v1/commodities/**",
                                "/v1/bookings/**",
                                "/v1/wallet/**",
                                "/v1/payments/**",
                                "/v1/receipts/**",
                                "/v1/cart/**",
                                "/v1/marketplace/**",
                                "/v1/crates/**",
                                "/v1/trips/**",
                                "/v1/lots/**",
                                "/v1/sensor-readings/**",
                                "/v1/iot/**",
                                "/v1/support/**",
                                "/v1/notifications/**",
                                "/v1/impact/**",
                                "/v1/events/**",
                                "/v1/sync/**",
                                "/v1/customer-types/**"
                        )
                        .build();
        }

        @Bean
        public GroupedOpenApi driverApi() {
                return GroupedOpenApi.builder()
                        .group("2-driver")
                        .displayName("Driver API")
                        .pathsToMatch("/v1/driver/**")
                        .build();
        }

        @Bean
        public GroupedOpenApi operatorApi() {
                return GroupedOpenApi.builder()
                        .group("3-operator")
                        .displayName("Operator API")
                        .pathsToMatch("/v1/operator/**")
                        .build();
        }

        @Bean
        public GroupedOpenApi adminApi() {
                return GroupedOpenApi.builder()
                        .group("4-admin")
                        .displayName("Admin API")
                        .pathsToMatch("/api/**")
                        .build();
        }

        @Bean
        public GroupedOpenApi publicApi() {
                return GroupedOpenApi.builder()
                        .group("5-public")
                        .displayName("Public & Website API")
                        .pathsToMatch(
                                "/v1/leads/**",
                                "/v1/public/**",
                                "/v1/newsletter/**",
                                "/web/**"
                        )
                        .build();
        }

        @Bean
        public GroupedOpenApi allApi() {
                return GroupedOpenApi.builder()
                        .group("0-all")
                        .displayName("All Endpoints")
                        .pathsToMatch("/**")
                        .build();
        }

        @Bean
        public OpenAPI customOpenApi() {
                return new OpenAPI()
                        .tags(List.of(
                                new Tag().name("Customer Auth")
                                        .description("Phone OTP signup and login"),
                                new Tag().name("Profile")
                                        .description("Profile, language and preferences"),
                                new Tag().name("Regions")
                                        .description("Region config and feature flags"),
                                new Tag().name("Hubs")
                                        .description("Hub search, capacity, temperature and power"),
                                new Tag().name("Hub Waitlist")
                                        .description("Join waitlist for full hubs"),
                                new Tag().name("Commodities")
                                        .description("Commodity catalogue per region"),
                                new Tag().name("Bookings")
                                        .description("Cold storage and transport bookings"),
                                new Tag().name("Wallet")
                                        .description("Wallet balance, top-up and withdrawals"),
                                new Tag().name("Wallet and Payments")
                                        .description("Payments and receipts"),
                                new Tag().name("Cart")
                                        .description("Marketplace shopping cart"),
                                new Tag().name("Marketplace")
                                        .description("Browse lots, orders and sell crates"),
                                new Tag().name("Tracking")
                                        .description("Crate, trip, IoT and cold-chain tracking"),
                                new Tag().name("Support")
                                        .description("Support cases"),
                                new Tag().name("Notifications")
                                        .description("Alerts and updates"),
                                new Tag().name("Impact")
                                        .description("Environmental impact metrics"),
                                new Tag().name("Events")
                                        .description("Analytics and evidence event logging"),
                                new Tag().name("Offline Sync")
                                        .description("Batch sync for offline-first clients"),
                                new Tag().name("Public")
                                        .description("Customer types, leads and public impact stats"),
                                new Tag().name("Driver")
                                        .description("Safety checks and cold chain"),
                                new Tag().name("Operator")
                                        .description("Cold room live monitoring"),
                                new Tag().name("Admin Auth")
                                        .description("Email/password auth for admins"),
                                new Tag().name("Admin Users")
                                        .description("User management"),
                                new Tag().name("Admin Analytics")
                                        .description("Platform analytics"),
                                new Tag().name("Admin Bookings")
                                        .description("Booking management"),
                                new Tag().name("Admin Dispatch")
                                        .description("Trip dispatch and routing"),
                                new Tag().name("Admin Inventory")
                                        .description("Crate and inventory management"),
                                new Tag().name("Admin Payments")
                                        .description("Payments ledger and reconciliation"),
                                new Tag().name("Admin Support")
                                        .description("Support queue management"),
                                new Tag().name("Admin Operators")
                                        .description("Hub management"),
                                new Tag().name("Admin IoT")
                                        .description("Cold chain IoT monitoring"),
                                new Tag().name("Admin Marketplace")
                                        .description("Marketplace operations"),
                                new Tag().name("Admin Safety")
                                        .description("Fleet safety compliance"),
                                new Tag().name("Admin Impact")
                                        .description("DARES evidence export"),
                                new Tag().name("Admin Settings")
                                        .description("Rate and platform settings"),
                                new Tag().name("Admin Customer Types")
                                        .description("Customer type management")
                        ));
        }
}