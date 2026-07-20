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
                title       = "Cold Connect API",
                version     = "1.0",
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

            ## Language Support
            Set language at signup. Supported: `en`, `ha`, `yo`, `ig`, `pcm`
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

        // ── Customer API ──────────────────────────────────────────────────────────
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
                                "/v1/support/**",
                                "/v1/notifications/**",
                                "/v1/impact/**",
                                "/v1/events/**",
                                "/v1/sync/**"
                        )
                        .build();
        }

        // ── Driver API ────────────────────────────────────────────────────────────
        @Bean
        public GroupedOpenApi driverApi() {
                return GroupedOpenApi.builder()
                        .group("2-driver")
                        .displayName("Driver API")
                        .pathsToMatch("/v1/driver/**")
                        .build();
        }

        // ── Operator API ──────────────────────────────────────────────────────────
        @Bean
        public GroupedOpenApi operatorApi() {
                return GroupedOpenApi.builder()
                        .group("3-operator")
                        .displayName("Operator API")
                        .pathsToMatch("/v1/operator/**")
                        .build();
        }

        // ── Admin API ─────────────────────────────────────────────────────────────
        @Bean
        public GroupedOpenApi adminApi() {
                return GroupedOpenApi.builder()
                        .group("4-admin")
                        .displayName("Admin API")
                        .pathsToMatch("/api/**")
                        .build();
        }

//        // ── Public / Website API ──────────────────────────────────────────────────
//        @Bean
//        public GroupedOpenApi publicApi() {
//                return GroupedOpenApi.builder()
//                        .group("5-public")
//                        .displayName("Public & Website API")
//                        .pathsToMatch(
//                                "/v1/leads/**",
//                                "/v1/public/**",
//                                "/v1/newsletter/**",
//                                "/web/**"
//                        )
//                        .build();
//        }

        // ── All endpoints ─────────────────────────────────────────────────────────
        @Bean
        public GroupedOpenApi allApi() {
                return GroupedOpenApi.builder()
                        .group("0-all")
                        .displayName("All Endpoints")
                        .pathsToMatch("/**")
                        .build();
        }

        // ── Tag ordering ──────────────────────────────────────────────────────────
        @Bean
        public OpenAPI customOpenApi() {
                return new OpenAPI()
                        .tags(List.of(
                                // Customer
                                new Tag().name("Customer Auth")
                                        .description("Phone OTP signup and login"),
                                new Tag().name("Profile")
                                        .description("Profile, language and preferences"),
                                new Tag().name("Regions")
                                        .description("Region config and feature flags"),
                                new Tag().name("Hubs")
                                        .description("Hub search and capacity"),
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
                                        .description("Crate, trip and cold-chain tracking"),
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
                                // Driver
                                new Tag().name("Driver")
                                        .description("Safety checks and cold chain"),
                                // Operator
                                new Tag().name("Operator")
                                        .description("Cold room live monitoring"),
                                // Admin
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
                                // Public
                                new Tag().name("Public")
                                        .description("Website lead capture and impact stats")
                        ));
        }
}