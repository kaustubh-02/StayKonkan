-- =====================================================================
-- StayKonkan — Phase 2: Complete PostgreSQL Database Schema
-- Version: 1.0
-- Target: PostgreSQL 15+
-- Design: Normalized to 3NF, modular-monolith aligned (one section per
--         Phase 1 module), built to scale past 100,000 users.
-- =====================================================================

-- ============================================================
-- 0. EXTENSIONS
-- ============================================================
CREATE EXTENSION IF NOT EXISTS pgcrypto;   -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pg_trgm;    -- fuzzy/partial text search (hotel/restaurant name search)

-- ============================================================
-- 0.1 SHARED TRIGGER FUNCTIONS
-- ============================================================
CREATE OR REPLACE FUNCTION trigger_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =====================================================================
-- 1. IDENTITY & ACCESS MODULE
-- =====================================================================

CREATE TABLE roles (
  id            BIGSERIAL PRIMARY KEY,
  name          VARCHAR(50)  NOT NULL,
  description   VARCHAR(255),
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT uq_roles_name UNIQUE (name)
);

CREATE TABLE permissions (
  id            BIGSERIAL PRIMARY KEY,
  code          VARCHAR(100) NOT NULL,          -- e.g. BOOKING_CONFIRM, HOTEL_MANAGE
  module        VARCHAR(50)  NOT NULL,           -- e.g. BOOKING, HOTEL, COMMISSION
  description   VARCHAR(255),
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT uq_permissions_code UNIQUE (code)
);

CREATE TABLE role_permissions (
  role_id       BIGINT NOT NULL REFERENCES roles(id)       ON DELETE CASCADE,
  permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
  PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
  id              BIGSERIAL PRIMARY KEY,
  uuid            UUID         NOT NULL DEFAULT gen_random_uuid(),
  full_name       VARCHAR(150) NOT NULL,
  email           VARCHAR(150) NOT NULL,
  phone           VARCHAR(20),
  password_hash   VARCHAR(255) NOT NULL,
  status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                  CHECK (status IN ('ACTIVE','INACTIVE','SUSPENDED','DELETED')),
  email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
  phone_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
  last_login_at   TIMESTAMPTZ,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT uq_users_uuid  UNIQUE (uuid),
  CONSTRAINT uq_users_email UNIQUE (email),
  CONSTRAINT uq_users_phone UNIQUE (phone)
);
CREATE INDEX idx_users_status       ON users(status);
CREATE INDEX idx_users_phone        ON users(phone);
CREATE INDEX idx_users_name_trgm    ON users USING gin (full_name gin_trgm_ops);
CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON users
  FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

-- Many-to-many: a user may hold multiple roles (e.g. ADMIN + STAFF)
CREATE TABLE user_roles (
  user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role_id      BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
  assigned_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_tokens (
  id           BIGSERIAL PRIMARY KEY,
  user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash   VARCHAR(255) NOT NULL,
  expires_at   TIMESTAMPTZ  NOT NULL,
  revoked      BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_active ON refresh_tokens(user_id) WHERE revoked = FALSE;

CREATE TABLE hotel_owners (
  id                    BIGSERIAL PRIMARY KEY,
  user_id               BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  business_name         VARCHAR(150),
  gst_number            VARCHAR(20),
  payout_bank_account   VARCHAR(50),
  payout_ifsc           VARCHAR(15),
  verified              BOOLEAN NOT NULL DEFAULT FALSE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_hotel_owners_user UNIQUE (user_id)
);

-- =====================================================================
-- 2. DESTINATION & ATTRACTION MODULE
-- =====================================================================

CREATE TABLE destinations (
  id          BIGSERIAL PRIMARY KEY,
  name        VARCHAR(100) NOT NULL,
  slug        VARCHAR(120) NOT NULL,
  state       VARCHAR(50)  NOT NULL DEFAULT 'Maharashtra',
  description TEXT,
  geo_lat     NUMERIC(9,6),
  geo_lng     NUMERIC(9,6),
  is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT uq_destinations_name UNIQUE (name),
  CONSTRAINT uq_destinations_slug UNIQUE (slug)
);

CREATE TABLE attractions (
  id             BIGSERIAL PRIMARY KEY,
  destination_id BIGINT NOT NULL REFERENCES destinations(id) ON DELETE CASCADE,
  name           VARCHAR(150) NOT NULL,
  description    TEXT,
  category       VARCHAR(50),   -- BEACH, FORT, TEMPLE, WATERFALL, VIEWPOINT
  geo_lat        NUMERIC(9,6),
  geo_lng        NUMERIC(9,6),
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_attractions_destination ON attractions(destination_id);

-- =====================================================================
-- 3. HOTEL MODULE
-- =====================================================================

CREATE TABLE hotels (
  id             BIGSERIAL PRIMARY KEY,
  owner_id       BIGINT REFERENCES hotel_owners(id) ON DELETE SET NULL,
  destination_id BIGINT NOT NULL REFERENCES destinations(id) ON DELETE RESTRICT,
  name           VARCHAR(150) NOT NULL,
  slug           VARCHAR(180) NOT NULL,
  description    TEXT,
  address        VARCHAR(255) NOT NULL,
  geo_lat        NUMERIC(9,6),
  geo_lng        NUMERIC(9,6),
  star_rating    SMALLINT CHECK (star_rating BETWEEN 1 AND 5),
  status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                 CHECK (status IN ('ACTIVE','INACTIVE','PENDING_APPROVAL')),
  check_in_time  TIME NOT NULL DEFAULT '13:00',
  check_out_time TIME NOT NULL DEFAULT '11:00',
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_hotels_slug UNIQUE (slug)
);
CREATE INDEX idx_hotels_destination     ON hotels(destination_id);
CREATE INDEX idx_hotels_status          ON hotels(status);
CREATE INDEX idx_hotels_owner           ON hotels(owner_id);
CREATE INDEX idx_hotels_name_trgm       ON hotels USING gin (name gin_trgm_ops);
-- Composite: the single most common query — "active hotels in a destination"
CREATE INDEX idx_hotels_dest_status     ON hotels(destination_id, status);
CREATE TRIGGER trg_hotels_updated_at BEFORE UPDATE ON hotels
  FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

CREATE TABLE hotel_images (
  id                    BIGSERIAL PRIMARY KEY,
  hotel_id              BIGINT NOT NULL REFERENCES hotels(id) ON DELETE CASCADE,
  cloudinary_url        VARCHAR(500) NOT NULL,
  cloudinary_public_id  VARCHAR(255),
  is_cover              BOOLEAN NOT NULL DEFAULT FALSE,
  display_order         SMALLINT NOT NULL DEFAULT 0,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_hotel_images_hotel ON hotel_images(hotel_id);
-- Enforce max one cover image per hotel
CREATE UNIQUE INDEX uq_hotel_one_cover ON hotel_images(hotel_id) WHERE is_cover = TRUE;

CREATE TABLE room_types (
  id          BIGSERIAL PRIMARY KEY,
  name        VARCHAR(80) NOT NULL,     -- STANDARD, DELUXE, SUITE, COTTAGE, DORMITORY
  description VARCHAR(255),
  CONSTRAINT uq_room_types_name UNIQUE (name)
);

CREATE TABLE rooms (
  id                 BIGSERIAL PRIMARY KEY,
  hotel_id           BIGINT NOT NULL REFERENCES hotels(id) ON DELETE CASCADE,
  room_type_id       BIGINT NOT NULL REFERENCES room_types(id) ON DELETE RESTRICT,
  name               VARCHAR(100) NOT NULL,
  capacity_adults    SMALLINT NOT NULL DEFAULT 2 CHECK (capacity_adults > 0),
  capacity_children  SMALLINT NOT NULL DEFAULT 0 CHECK (capacity_children >= 0),
  base_price         NUMERIC(10,2) NOT NULL CHECK (base_price >= 0),
  total_units        SMALLINT NOT NULL CHECK (total_units > 0),
  is_active          BOOLEAN NOT NULL DEFAULT TRUE,
  created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_room_hotel_name UNIQUE (hotel_id, name)
);
CREATE INDEX idx_rooms_hotel       ON rooms(hotel_id);
CREATE INDEX idx_rooms_hotel_type  ON rooms(hotel_id, room_type_id);
CREATE INDEX idx_rooms_price       ON rooms(base_price);

CREATE TABLE amenities (
  id       BIGSERIAL PRIMARY KEY,
  name     VARCHAR(80) NOT NULL,
  icon     VARCHAR(50),
  category VARCHAR(30),  -- SAFETY, COMFORT, RECREATION, CONNECTIVITY
  CONSTRAINT uq_amenities_name UNIQUE (name)
);

CREATE TABLE hotel_amenities (
  hotel_id    BIGINT NOT NULL REFERENCES hotels(id)    ON DELETE CASCADE,
  amenity_id  BIGINT NOT NULL REFERENCES amenities(id) ON DELETE CASCADE,
  PRIMARY KEY (hotel_id, amenity_id)
);

CREATE TABLE room_availability (
  id               BIGSERIAL PRIMARY KEY,
  room_id          BIGINT NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
  available_date   DATE NOT NULL,
  units_available  SMALLINT NOT NULL CHECK (units_available >= 0),
  price_override   NUMERIC(10,2),
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_room_date UNIQUE (room_id, available_date)
);
-- Composite index: the core search-availability query
CREATE INDEX idx_room_availability_room_date ON room_availability(room_id, available_date);
CREATE INDEX idx_room_availability_date      ON room_availability(available_date);

-- =====================================================================
-- 4. BOOKING MODULE (Core Domain)
-- =====================================================================

CREATE TABLE booking_statuses (
  id         SMALLSERIAL PRIMARY KEY,
  code       VARCHAR(30) NOT NULL,  -- PENDING, CONTACTED, OWNER_CONFIRMED, CUSTOMER_CONFIRMED,
                                     -- ADVANCE_PAID, CONFIRMED, COMPLETED, REJECTED, CANCELLED
  label      VARCHAR(50) NOT NULL,
  sort_order SMALLINT NOT NULL DEFAULT 0,
  CONSTRAINT uq_booking_statuses_code UNIQUE (code)
);

CREATE TABLE booking_requests (
  id                 BIGSERIAL PRIMARY KEY,
  uuid               UUID NOT NULL DEFAULT gen_random_uuid(),
  customer_id        BIGINT NOT NULL REFERENCES users(id)  ON DELETE RESTRICT,
  hotel_id           BIGINT NOT NULL REFERENCES hotels(id) ON DELETE RESTRICT,
  room_id            BIGINT NOT NULL REFERENCES rooms(id)  ON DELETE RESTRICT,
  status_id          SMALLINT NOT NULL REFERENCES booking_statuses(id),
  assigned_admin_id  BIGINT REFERENCES users(id) ON DELETE SET NULL,
  check_in           DATE NOT NULL,
  check_out          DATE NOT NULL,
  guests_adults      SMALLINT NOT NULL DEFAULT 1,
  guests_children    SMALLINT NOT NULL DEFAULT 0,
  source             VARCHAR(20) NOT NULL DEFAULT 'MANUAL' CHECK (source IN ('MANUAL','INSTANT')),
  quoted_price       NUMERIC(10,2),
  advance_amount     NUMERIC(10,2),
  notes              TEXT,
  created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_booking_requests_uuid UNIQUE (uuid),
  CONSTRAINT chk_booking_dates CHECK (check_out > check_in)
);
CREATE INDEX idx_booking_requests_customer     ON booking_requests(customer_id);
CREATE INDEX idx_booking_requests_hotel        ON booking_requests(hotel_id);
CREATE INDEX idx_booking_requests_status       ON booking_requests(status_id);
CREATE INDEX idx_booking_requests_admin        ON booking_requests(assigned_admin_id);
CREATE INDEX idx_booking_requests_checkin      ON booking_requests(check_in);
-- Composite: admin dashboard "queue per hotel per status"
CREATE INDEX idx_booking_requests_hotel_status ON booking_requests(hotel_id, status_id);
-- Composite: "my bookings" screen, most recent first
CREATE INDEX idx_booking_requests_customer_created ON booking_requests(customer_id, created_at DESC);
CREATE TRIGGER trg_booking_requests_updated_at BEFORE UPDATE ON booking_requests
  FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

CREATE TABLE booking_status_history (
  id                  BIGSERIAL PRIMARY KEY,
  booking_request_id  BIGINT NOT NULL REFERENCES booking_requests(id) ON DELETE CASCADE,
  from_status_id      SMALLINT REFERENCES booking_statuses(id),
  to_status_id        SMALLINT NOT NULL REFERENCES booking_statuses(id),
  changed_by          BIGINT REFERENCES users(id) ON DELETE SET NULL,
  note                VARCHAR(500),
  changed_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_booking_status_history_booking ON booking_status_history(booking_request_id);

-- Auto-log every status transition on booking_requests
CREATE OR REPLACE FUNCTION log_booking_status_change()
RETURNS TRIGGER AS $$
BEGIN
  IF OLD.status_id IS DISTINCT FROM NEW.status_id THEN
    INSERT INTO booking_status_history (booking_request_id, from_status_id, to_status_id, changed_at)
    VALUES (NEW.id, OLD.status_id, NEW.status_id, now());
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_booking_status_change AFTER UPDATE ON booking_requests
  FOR EACH ROW EXECUTE FUNCTION log_booking_status_change();

CREATE TABLE confirmed_bookings (
  id                    BIGSERIAL PRIMARY KEY,
  booking_request_id    BIGINT NOT NULL REFERENCES booking_requests(id) ON DELETE RESTRICT,
  final_price           NUMERIC(10,2) NOT NULL CHECK (final_price >= 0),
  commission_amount     NUMERIC(10,2) NOT NULL DEFAULT 0,
  confirmed_by          BIGINT REFERENCES users(id) ON DELETE SET NULL,
  confirmed_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  completed_at          TIMESTAMPTZ,
  cancelled_at          TIMESTAMPTZ,
  cancellation_reason   VARCHAR(255),
  CONSTRAINT uq_confirmed_bookings_request UNIQUE (booking_request_id)
);
CREATE INDEX idx_confirmed_bookings_confirmed_at ON confirmed_bookings(confirmed_at);

-- =====================================================================
-- 5. PAYMENT & COMMISSION MODULE
-- =====================================================================

CREATE TABLE payments (
  id                  BIGSERIAL PRIMARY KEY,
  booking_request_id  BIGINT NOT NULL REFERENCES booking_requests(id) ON DELETE RESTRICT,
  amount              NUMERIC(10,2) NOT NULL CHECK (amount > 0),
  payment_type        VARCHAR(20) NOT NULL CHECK (payment_type IN ('ADVANCE','BALANCE','REFUND')),
  mode                VARCHAR(20) NOT NULL CHECK (mode IN ('UPI','CASH','BANK_TRANSFER','CARD','GATEWAY')),
  status              VARCHAR(20) NOT NULL DEFAULT 'RECORDED'
                       CHECK (status IN ('RECORDED','PENDING','FAILED','REFUNDED')),
  reference_no        VARCHAR(100),
  recorded_by         BIGINT REFERENCES users(id) ON DELETE SET NULL,
  recorded_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_payments_booking ON payments(booking_request_id);
CREATE INDEX idx_payments_status  ON payments(status);

-- Gateway-level transaction detail (manual today, Razorpay/Stripe-ready)
CREATE TABLE payment_transactions (
  id                BIGSERIAL PRIMARY KEY,
  payment_id        BIGINT NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
  gateway           VARCHAR(30) NOT NULL DEFAULT 'MANUAL',  -- MANUAL, RAZORPAY, STRIPE
  gateway_txn_id    VARCHAR(150),
  gateway_order_id  VARCHAR(150),
  status            VARCHAR(20) NOT NULL DEFAULT 'INITIATED'
                     CHECK (status IN ('INITIATED','SUCCESS','FAILED','REFUNDED')),
  raw_response      JSONB,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_gateway_txn UNIQUE (gateway, gateway_txn_id)
);
CREATE INDEX idx_payment_transactions_payment ON payment_transactions(payment_id);

CREATE TABLE commission_rules (
  id              BIGSERIAL PRIMARY KEY,
  hotel_id        BIGINT NOT NULL REFERENCES hotels(id) ON DELETE CASCADE,
  rule_type       VARCHAR(10) NOT NULL CHECK (rule_type IN ('PERCENT','FLAT')),
  value           NUMERIC(8,2) NOT NULL CHECK (value >= 0),
  effective_from  DATE NOT NULL DEFAULT CURRENT_DATE,
  effective_to    DATE,
  is_active       BOOLEAN NOT NULL DEFAULT TRUE,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT chk_commission_rule_dates CHECK (effective_to IS NULL OR effective_to > effective_from)
);
-- Partial index: only active rules are looked up at booking-confirm time
CREATE INDEX idx_commission_rules_hotel_active ON commission_rules(hotel_id) WHERE is_active = TRUE;

CREATE TABLE commissions (
  id                     BIGSERIAL PRIMARY KEY,
  confirmed_booking_id   BIGINT NOT NULL REFERENCES confirmed_bookings(id) ON DELETE CASCADE,
  hotel_id               BIGINT NOT NULL REFERENCES hotels(id) ON DELETE RESTRICT,
  commission_rule_id     BIGINT REFERENCES commission_rules(id) ON DELETE SET NULL,
  amount                 NUMERIC(10,2) NOT NULL CHECK (amount >= 0),
  status                 VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','SETTLED')),
  settled_at             TIMESTAMPTZ,
  created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_commissions_confirmed_booking UNIQUE (confirmed_booking_id)
);
CREATE INDEX idx_commissions_hotel  ON commissions(hotel_id);
CREATE INDEX idx_commissions_status ON commissions(status);
-- Composite: monthly commission report per hotel
CREATE INDEX idx_commissions_hotel_created ON commissions(hotel_id, created_at);

-- =====================================================================
-- 6. RESTAURANT MODULE (future-ready vertical)
-- =====================================================================

CREATE TABLE restaurants (
  id             BIGSERIAL PRIMARY KEY,
  owner_id       BIGINT REFERENCES hotel_owners(id) ON DELETE SET NULL,
  destination_id BIGINT NOT NULL REFERENCES destinations(id) ON DELETE RESTRICT,
  name           VARCHAR(150) NOT NULL,
  slug           VARCHAR(180) NOT NULL,
  cuisine_type   VARCHAR(100),
  description    TEXT,
  address        VARCHAR(255) NOT NULL,
  geo_lat        NUMERIC(9,6),
  geo_lng        NUMERIC(9,6),
  status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE')),
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_restaurants_slug UNIQUE (slug)
);
CREATE INDEX idx_restaurants_destination ON restaurants(destination_id);

CREATE TABLE restaurant_images (
  id             BIGSERIAL PRIMARY KEY,
  restaurant_id  BIGINT NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
  cloudinary_url VARCHAR(500) NOT NULL,
  is_cover       BOOLEAN NOT NULL DEFAULT FALSE,
  display_order  SMALLINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_restaurant_one_cover ON restaurant_images(restaurant_id) WHERE is_cover = TRUE;

CREATE TABLE restaurant_menu_items (
  id             BIGSERIAL PRIMARY KEY,
  restaurant_id  BIGINT NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
  name           VARCHAR(150) NOT NULL,
  description    VARCHAR(255),
  price          NUMERIC(8,2) NOT NULL CHECK (price >= 0),
  category       VARCHAR(50),   -- STARTER, MAIN, DESSERT, BEVERAGE
  is_veg         BOOLEAN NOT NULL DEFAULT TRUE,
  is_available   BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_menu_items_restaurant ON restaurant_menu_items(restaurant_id);

-- =====================================================================
-- 7. CAB BOOKING MODULE (future-ready vertical)
-- =====================================================================

CREATE TABLE cab_drivers (
  id           BIGSERIAL PRIMARY KEY,
  user_id      BIGINT REFERENCES users(id) ON DELETE SET NULL,
  full_name    VARCHAR(150) NOT NULL,
  phone        VARCHAR(20)  NOT NULL,
  license_no   VARCHAR(50)  NOT NULL,
  is_verified  BOOLEAN NOT NULL DEFAULT FALSE,
  is_active    BOOLEAN NOT NULL DEFAULT TRUE,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_cab_drivers_phone UNIQUE (phone),
  CONSTRAINT uq_cab_drivers_license UNIQUE (license_no)
);

CREATE TABLE vehicles (
  id               BIGSERIAL PRIMARY KEY,
  driver_id        BIGINT NOT NULL REFERENCES cab_drivers(id) ON DELETE CASCADE,
  vehicle_type     VARCHAR(30) NOT NULL,  -- SEDAN, SUV, HATCHBACK, TEMPO_TRAVELLER
  registration_no  VARCHAR(20) NOT NULL,
  capacity         SMALLINT NOT NULL CHECK (capacity > 0),
  is_active        BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT uq_vehicles_registration UNIQUE (registration_no)
);
CREATE INDEX idx_vehicles_driver ON vehicles(driver_id);

CREATE TABLE cab_bookings (
  id               BIGSERIAL PRIMARY KEY,
  customer_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  driver_id        BIGINT REFERENCES cab_drivers(id) ON DELETE SET NULL,
  vehicle_id       BIGINT REFERENCES vehicles(id) ON DELETE SET NULL,
  pickup_location  VARCHAR(255) NOT NULL,
  drop_location    VARCHAR(255) NOT NULL,
  pickup_datetime  TIMESTAMPTZ NOT NULL,
  status           VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING','ASSIGNED','CONFIRMED','COMPLETED','CANCELLED')),
  fare_amount      NUMERIC(10,2),
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_cab_bookings_customer ON cab_bookings(customer_id);
CREATE INDEX idx_cab_bookings_status   ON cab_bookings(status);
CREATE INDEX idx_cab_bookings_pickup_time ON cab_bookings(pickup_datetime);

-- =====================================================================
-- 8. REVIEWS & RATINGS MODULE
-- =====================================================================

CREATE TABLE reviews (
  id                    BIGSERIAL PRIMARY KEY,
  confirmed_booking_id  BIGINT NOT NULL REFERENCES confirmed_bookings(id) ON DELETE CASCADE,
  customer_id           BIGINT NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
  hotel_id              BIGINT NOT NULL REFERENCES hotels(id) ON DELETE CASCADE,
  title                 VARCHAR(150),
  comment               TEXT,
  is_published          BOOLEAN NOT NULL DEFAULT TRUE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_reviews_confirmed_booking UNIQUE (confirmed_booking_id)
);
CREATE INDEX idx_reviews_hotel ON reviews(hotel_id) WHERE is_published = TRUE;

CREATE TABLE ratings (
  id               BIGSERIAL PRIMARY KEY,
  review_id        BIGINT NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
  cleanliness      SMALLINT CHECK (cleanliness BETWEEN 1 AND 5),
  service          SMALLINT CHECK (service BETWEEN 1 AND 5),
  location_rating  SMALLINT CHECK (location_rating BETWEEN 1 AND 5),
  value_for_money  SMALLINT CHECK (value_for_money BETWEEN 1 AND 5),
  overall          NUMERIC(2,1) NOT NULL CHECK (overall BETWEEN 1 AND 5),
  CONSTRAINT uq_ratings_review UNIQUE (review_id)
);

-- =====================================================================
-- 9. ENGAGEMENT MODULE (Wishlist, Notifications, Coupons, Offers)
-- =====================================================================

CREATE TABLE wishlists (
  id          BIGSERIAL PRIMARY KEY,
  user_id     BIGINT NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
  hotel_id    BIGINT NOT NULL REFERENCES hotels(id) ON DELETE CASCADE,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_wishlist_user_hotel UNIQUE (user_id, hotel_id)
);
CREATE INDEX idx_wishlists_user ON wishlists(user_id);

CREATE TABLE notifications (
  id                    BIGSERIAL PRIMARY KEY,
  user_id               BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  channel               VARCHAR(20) NOT NULL CHECK (channel IN ('EMAIL','SMS','WHATSAPP','IN_APP')),
  title                 VARCHAR(150),
  message               VARCHAR(500) NOT NULL,
  is_read               BOOLEAN NOT NULL DEFAULT FALSE,
  related_entity_type   VARCHAR(50),
  related_entity_id     BIGINT,
  sent_at               TIMESTAMPTZ,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- Partial index: unread notification badge/list is the hottest query
CREATE INDEX idx_notifications_user_unread ON notifications(user_id) WHERE is_read = FALSE;
CREATE INDEX idx_notifications_entity ON notifications(related_entity_type, related_entity_id);

CREATE TABLE coupons (
  id                    BIGSERIAL PRIMARY KEY,
  code                  VARCHAR(30) NOT NULL,
  description           VARCHAR(255),
  discount_type         VARCHAR(10) NOT NULL CHECK (discount_type IN ('PERCENT','FLAT')),
  discount_value        NUMERIC(8,2) NOT NULL CHECK (discount_value > 0),
  max_discount_amount   NUMERIC(10,2),
  min_booking_amount    NUMERIC(10,2) DEFAULT 0,
  usage_limit           INT,
  usage_count           INT NOT NULL DEFAULT 0,
  valid_from            TIMESTAMPTZ NOT NULL,
  valid_to              TIMESTAMPTZ NOT NULL,
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_coupons_code UNIQUE (code),
  CONSTRAINT chk_coupon_dates CHECK (valid_to > valid_from)
);
CREATE INDEX idx_coupons_active ON coupons(is_active) WHERE is_active = TRUE;

CREATE TABLE coupon_redemptions (
  id                   BIGSERIAL PRIMARY KEY,
  coupon_id            BIGINT NOT NULL REFERENCES coupons(id) ON DELETE CASCADE,
  user_id              BIGINT NOT NULL REFERENCES users(id)   ON DELETE CASCADE,
  booking_request_id   BIGINT REFERENCES booking_requests(id) ON DELETE SET NULL,
  discount_applied     NUMERIC(10,2) NOT NULL,
  redeemed_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_coupon_user_booking UNIQUE (coupon_id, booking_request_id)
);
CREATE INDEX idx_coupon_redemptions_user ON coupon_redemptions(user_id);

-- Keep coupons.usage_count in sync automatically
CREATE OR REPLACE FUNCTION increment_coupon_usage()
RETURNS TRIGGER AS $$
BEGIN
  UPDATE coupons SET usage_count = usage_count + 1 WHERE id = NEW.coupon_id;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_increment_coupon_usage AFTER INSERT ON coupon_redemptions
  FOR EACH ROW EXECUTE FUNCTION increment_coupon_usage();

CREATE TABLE offers (
  id                BIGSERIAL PRIMARY KEY,
  title             VARCHAR(150) NOT NULL,
  description       TEXT,
  hotel_id          BIGINT REFERENCES hotels(id) ON DELETE CASCADE,
  destination_id    BIGINT REFERENCES destinations(id) ON DELETE CASCADE,
  banner_image_url  VARCHAR(500),
  valid_from        DATE NOT NULL,
  valid_to          DATE NOT NULL,
  is_active         BOOLEAN NOT NULL DEFAULT TRUE,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT chk_offer_dates CHECK (valid_to > valid_from)
);
CREATE INDEX idx_offers_active ON offers(is_active) WHERE is_active = TRUE;

-- =====================================================================
-- 10. TRAVEL PACKAGES MODULE
-- =====================================================================

CREATE TABLE travel_packages (
  id             BIGSERIAL PRIMARY KEY,
  name           VARCHAR(150) NOT NULL,
  slug           VARCHAR(180) NOT NULL,
  description    TEXT,
  duration_days  SMALLINT NOT NULL CHECK (duration_days > 0),
  base_price     NUMERIC(10,2) NOT NULL CHECK (base_price >= 0),
  is_active      BOOLEAN NOT NULL DEFAULT TRUE,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_travel_packages_slug UNIQUE (slug)
);

CREATE TABLE travel_package_items (
  id                  BIGSERIAL PRIMARY KEY,
  travel_package_id   BIGINT NOT NULL REFERENCES travel_packages(id) ON DELETE CASCADE,
  destination_id      BIGINT REFERENCES destinations(id) ON DELETE SET NULL,
  attraction_id       BIGINT REFERENCES attractions(id)  ON DELETE SET NULL,
  day_number          SMALLINT NOT NULL CHECK (day_number > 0),
  notes               VARCHAR(255),
  CONSTRAINT chk_package_item_ref CHECK (destination_id IS NOT NULL OR attraction_id IS NOT NULL)
);
CREATE INDEX idx_package_items_package ON travel_package_items(travel_package_id);

-- =====================================================================
-- 11. PLATFORM / SUPPORT / AUDIT MODULE
-- =====================================================================

CREATE TABLE audit_logs (
  id           BIGSERIAL PRIMARY KEY,
  entity_type  VARCHAR(50) NOT NULL,
  entity_id    BIGINT NOT NULL,
  action       VARCHAR(20) NOT NULL CHECK (action IN ('CREATE','UPDATE','DELETE','STATUS_CHANGE')),
  actor_id     BIGINT REFERENCES users(id) ON DELETE SET NULL,
  diff         JSONB,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_logs_entity  ON audit_logs(entity_type, entity_id);
-- BRIN is ideal here: audit_logs is append-only and queried mostly by recent time range,
-- and a BRIN index is far smaller/cheaper to maintain than a B-tree at high insert volume.
CREATE INDEX idx_audit_logs_created_brin ON audit_logs USING brin (created_at);

CREATE TABLE support_tickets (
  id           BIGSERIAL PRIMARY KEY,
  user_id      BIGINT REFERENCES users(id) ON DELETE SET NULL,
  subject      VARCHAR(150) NOT NULL,
  description  TEXT NOT NULL,
  status       VARCHAR(20) NOT NULL DEFAULT 'OPEN'
               CHECK (status IN ('OPEN','IN_PROGRESS','RESOLVED','CLOSED')),
  priority     VARCHAR(10) NOT NULL DEFAULT 'MEDIUM'
               CHECK (priority IN ('LOW','MEDIUM','HIGH','URGENT')),
  assigned_to  BIGINT REFERENCES users(id) ON DELETE SET NULL,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  resolved_at  TIMESTAMPTZ
);
CREATE INDEX idx_support_tickets_status   ON support_tickets(status);
CREATE INDEX idx_support_tickets_assigned ON support_tickets(assigned_to);

CREATE TABLE contact_messages (
  id           BIGSERIAL PRIMARY KEY,
  name         VARCHAR(150) NOT NULL,
  email        VARCHAR(150) NOT NULL,
  phone        VARCHAR(20),
  message      TEXT NOT NULL,
  is_resolved  BOOLEAN NOT NULL DEFAULT FALSE,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_contact_messages_unresolved ON contact_messages(is_resolved) WHERE is_resolved = FALSE;

-- =====================================================================
-- 12. VIEWS
-- =====================================================================

-- 12.1 Hotel search summary — the single query the Search module needs
CREATE OR REPLACE VIEW vw_hotel_search_summary AS
SELECT
  h.id                AS hotel_id,
  h.name              AS hotel_name,
  h.slug,
  d.name              AS destination_name,
  h.star_rating,
  h.status,
  MIN(r.base_price)   AS min_room_price,
  COALESCE(ROUND(AVG(rt.overall), 1), 0) AS avg_rating,
  COUNT(DISTINCT rv.id) AS review_count,
  (SELECT hi.cloudinary_url FROM hotel_images hi
     WHERE hi.hotel_id = h.id AND hi.is_cover = TRUE LIMIT 1) AS cover_image_url
FROM hotels h
JOIN destinations d       ON d.id = h.destination_id
LEFT JOIN rooms r         ON r.hotel_id = h.id AND r.is_active = TRUE
LEFT JOIN reviews rv      ON rv.hotel_id = h.id AND rv.is_published = TRUE
LEFT JOIN ratings rt      ON rt.review_id = rv.id
WHERE h.status = 'ACTIVE'
GROUP BY h.id, h.name, h.slug, d.name, h.star_rating, h.status;

-- 12.2 Admin booking queue — one query for the dashboard's main screen
CREATE OR REPLACE VIEW vw_admin_booking_queue AS
SELECT
  br.id               AS booking_request_id,
  br.uuid,
  u.full_name         AS customer_name,
  u.phone             AS customer_phone,
  h.name              AS hotel_name,
  rm.name             AS room_name,
  bs.code             AS status_code,
  bs.label            AS status_label,
  br.check_in,
  br.check_out,
  br.assigned_admin_id,
  au.full_name        AS assigned_admin_name,
  br.created_at
FROM booking_requests br
JOIN users u             ON u.id = br.customer_id
JOIN hotels h             ON h.id = br.hotel_id
JOIN rooms rm             ON rm.id = br.room_id
JOIN booking_statuses bs  ON bs.id = br.status_id
LEFT JOIN users au        ON au.id = br.assigned_admin_id
ORDER BY br.created_at DESC;

-- 12.3 Commission report — monthly earnings per hotel
CREATE OR REPLACE VIEW vw_commission_report AS
SELECT
  h.id                              AS hotel_id,
  h.name                            AS hotel_name,
  date_trunc('month', c.created_at)::date AS period,
  COUNT(c.id)                       AS bookings_count,
  SUM(c.amount)                     AS total_commission,
  SUM(c.amount) FILTER (WHERE c.status = 'SETTLED') AS settled_commission,
  SUM(c.amount) FILTER (WHERE c.status = 'PENDING') AS pending_commission
FROM commissions c
JOIN hotels h ON h.id = c.hotel_id
GROUP BY h.id, h.name, date_trunc('month', c.created_at);

-- 12.4 Currently active offers (customer-facing home page)
CREATE OR REPLACE VIEW vw_active_offers AS
SELECT o.*
FROM offers o
WHERE o.is_active = TRUE
  AND CURRENT_DATE BETWEEN o.valid_from AND o.valid_to;

-- =====================================================================
-- 13. SEED / REFERENCE DATA
-- =====================================================================

INSERT INTO roles (name, description) VALUES
  ('CUSTOMER',    'Public platform user who books stays'),
  ('ADMIN',       'Full platform access'),
  ('STAFF',       'Sub-admin, manages assigned bookings'),
  ('HOTEL_OWNER', 'Manages own hotel listings (Phase 2 feature)');

INSERT INTO permissions (code, module, description) VALUES
  ('BOOKING_VIEW',        'BOOKING',    'View booking requests'),
  ('BOOKING_ASSIGN',      'BOOKING',    'Assign booking to staff'),
  ('BOOKING_CONFIRM',     'BOOKING',    'Confirm or reject a booking'),
  ('HOTEL_MANAGE',        'HOTEL',      'Create/update/deactivate hotels'),
  ('COMMISSION_VIEW',     'COMMISSION', 'View commission reports'),
  ('USER_MANAGE',         'IDENTITY',   'Manage platform users and roles');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'ADMIN';

INSERT INTO booking_statuses (code, label, sort_order) VALUES
  ('PENDING',            'Pending Review',        1),
  ('CONTACTED',          'Hotel Contacted',       2),
  ('OWNER_CONFIRMED',    'Owner Confirmed',       3),
  ('CUSTOMER_CONFIRMED', 'Customer Confirmed',    4),
  ('ADVANCE_PAID',       'Advance Paid',          5),
  ('CONFIRMED',          'Confirmed',             6),
  ('COMPLETED',          'Stay Completed',        7),
  ('REJECTED',           'Rejected',              8),
  ('CANCELLED',          'Cancelled',             9);

INSERT INTO destinations (name, slug, description) VALUES
  ('Alibag', 'alibag', 'Popular coastal getaway known for its beaches and historic sea fort'),
  ('Nagaon', 'nagaon', 'Quiet beach town near Alibag, popular for weekend family trips'),
  ('Murud',  'murud',  'Home to Murud-Janjira fort, known for pristine beaches'),
  ('Kashid', 'kashid', 'White-sand beach destination between Alibag and Murud');

INSERT INTO room_types (name, description) VALUES
  ('STANDARD', 'Basic comfortable room with essential amenities'),
  ('DELUXE',   'Upgraded room with sea view or better furnishing'),
  ('SUITE',    'Spacious room with living area'),
  ('COTTAGE',  'Standalone cottage-style unit, popular for beach resorts');

INSERT INTO amenities (name, icon, category) VALUES
  ('Free WiFi',        'wifi',      'CONNECTIVITY'),
  ('Air Conditioning',  'snowflake', 'COMFORT'),
  ('Swimming Pool',     'pool',      'RECREATION'),
  ('Free Parking',      'car',       'COMFORT'),
  ('Beach Access',      'umbrella',  'RECREATION'),
  ('24x7 Power Backup', 'bolt',      'SAFETY');

-- Sample transactional data (illustrative — not for production use)
INSERT INTO users (full_name, email, phone, password_hash, status, email_verified)
VALUES
  ('StayKonkan Admin', 'admin@staykonkan.com', '9800000001', crypt('ChangeMe123!', gen_salt('bf')), 'ACTIVE', TRUE),
  ('Rahul Deshmukh',   'rahul@example.com',    '9800000002', crypt('CustomerPass1', gen_salt('bf')), 'ACTIVE', TRUE);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.email = 'admin@staykonkan.com' AND r.name = 'ADMIN';
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.email = 'rahul@example.com' AND r.name = 'CUSTOMER';

INSERT INTO hotel_owners (user_id, business_name, verified)
SELECT id, 'Konkan Coastal Resorts Pvt Ltd', TRUE FROM users WHERE email = 'admin@staykonkan.com';

INSERT INTO hotels (owner_id, destination_id, name, slug, description, address, star_rating, status)
SELECT ho.id, d.id, 'Nagaon Beach Resort', 'nagaon-beach-resort',
       'A beachfront resort with easy access to Nagaon beach.', 'Beach Road, Nagaon, Raigad', 4, 'ACTIVE'
FROM hotel_owners ho, destinations d WHERE d.slug = 'nagaon' LIMIT 1;

INSERT INTO rooms (hotel_id, room_type_id, name, base_price, total_units)
SELECT h.id, rt.id, 'Deluxe Sea View', 3500.00, 6
FROM hotels h, room_types rt WHERE h.slug = 'nagaon-beach-resort' AND rt.name = 'DELUXE';

INSERT INTO hotel_amenities (hotel_id, amenity_id)
SELECT h.id, a.id FROM hotels h, amenities a
WHERE h.slug = 'nagaon-beach-resort' AND a.name IN ('Free WiFi','Swimming Pool','Beach Access');

INSERT INTO booking_requests (customer_id, hotel_id, room_id, status_id, check_in, check_out, guests_adults, notes)
SELECT u.id, h.id, r.id, bs.id, CURRENT_DATE + 14, CURRENT_DATE + 16, 2, 'Requested via sample seed data'
FROM users u, hotels h, rooms r, booking_statuses bs
WHERE u.email = 'rahul@example.com' AND h.slug = 'nagaon-beach-resort'
  AND r.hotel_id = h.id AND bs.code = 'PENDING';

-- =====================================================================
-- 14. PERFORMANCE & QUERY OPTIMIZATION NOTES
-- =====================================================================
-- (See companion document StayKonkan_Phase2_Database_Design.md, Section 6,
--  for full narrative explanation. Summary of decisions baked into this
--  schema:)
--
-- 1. BIGSERIAL surrogate keys chosen over UUID primary keys for better
--    B-tree index locality and smaller index size at 100k+ row scale;
--    a separate `uuid` column is exposed publicly (in URLs/APIs) so
--    internal sequential IDs are never leaked.
-- 2. Composite indexes are ordered by selectivity/query pattern, e.g.
--    (hotel_id, status_id) for the admin queue, (destination_id, status)
--    for hotel search, (customer_id, created_at DESC) for "My Bookings".
-- 3. Partial indexes (WHERE is_active/ is_read = FALSE / is_cover = TRUE)
--    keep hot-path indexes small — e.g. unread notifications, active
--    coupons/offers, one-cover-image-per-hotel enforcement.
-- 4. BRIN index on audit_logs.created_at — append-only, time-ordered,
--    high-volume table; BRIN is ~100x smaller than B-tree here and still
--    fast for range scans ("audit entries from last 7 days").
-- 5. pg_trgm GIN indexes on hotels.name / users.full_name enable fast
--    fuzzy/partial search ("naga" matches "Nagaon Beach Resort") without
--    a separate search engine at MVP scale.
-- 6. All FKs use explicit ON DELETE behavior: CASCADE for owned child
--    data (images, amenities, availability), RESTRICT for records that
--    must never silently vanish (bookings referencing hotels/rooms/users),
--    SET NULL for optional relationships (assigned_admin_id, owner_id).
-- 7. CHECK constraints enforce state validity at the DB layer as a second
--    line of defense behind the application-layer state machine (Phase 1,
--    Section 7.1) — e.g. check_out > check_in, positive prices/amounts.
-- 8. Money columns use NUMERIC(10,2), never FLOAT/DOUBLE, to avoid
--    rounding errors in financial calculations.
-- 9. Views (Section 12) pre-join the most common read patterns so the
--    application layer issues one query instead of N+1 joins; at higher
--    scale these can become materialized views refreshed on a schedule.
-- 10. Recommended production additions as data grows:
--     - Table partitioning on booking_requests / audit_logs by month
--       once row counts exceed ~10M.
--     - PgBouncer connection pooling in front of PostgreSQL.
--     - Read replica for reporting/analytics queries (vw_commission_report,
--       admin dashboards) to isolate load from the transactional path.
--     - pg_stat_statements enabled to continuously identify slow queries.
--     - Redis cache layer in front of vw_hotel_search_summary results.
