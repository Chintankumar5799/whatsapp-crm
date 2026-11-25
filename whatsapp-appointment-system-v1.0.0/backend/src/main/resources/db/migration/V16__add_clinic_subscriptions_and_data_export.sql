-- ==============================================================================
-- V16__add_clinic_subscriptions_and_data_export.sql
-- Clinic Subscription Tiers, Plan Limits, SaaS Admin & Discharge Tracking
-- ==============================================================================

-- 1. Add subscription and plan limit columns to clinics table
ALTER TABLE clinics
    ADD COLUMN IF NOT EXISTS subscription_plan VARCHAR(30) NOT NULL DEFAULT 'TRIAL',
    ADD COLUMN IF NOT EXISTS subscription_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS trial_ends_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS subscription_ends_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS max_doctors INT NOT NULL DEFAULT 3,
    ADD COLUMN IF NOT EXISTS max_staff INT NOT NULL DEFAULT 10;

-- 2. Update default clinic to PROFESSIONAL tier with extended active validity
UPDATE clinics
SET subscription_plan = 'PROFESSIONAL',
    subscription_status = 'ACTIVE',
    max_doctors = 5,
    max_staff = 20,
    subscription_ends_at = CURRENT_TIMESTAMP + INTERVAL '365 days'
WHERE id = 1;

-- 3. Add discharge summary notes column to consultations table for OPD discharge/closure
ALTER TABLE consultations
    ADD COLUMN IF NOT EXISTS discharge_summary TEXT,
    ADD COLUMN IF NOT EXISTS discharge_condition VARCHAR(100),
    ADD COLUMN IF NOT EXISTS discharge_date TIMESTAMP;

