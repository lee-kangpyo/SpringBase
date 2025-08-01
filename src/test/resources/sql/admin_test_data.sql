-- admin_test_data.sql
-- This script is used to set up test data for admin-related tests.
-- It includes an admin user with ROLE_ADMIN authority.

-- Clean up existing data (optional, depending on test setup)
-- DELETE FROM authorities;

-- Insert test users
INSERT INTO users (USER_NAME, PASSWORD, EMAIL, USE_YN, LOGIN_FAILURE_COUNT, LAST_FAILURE_TIMESTAMP) VALUES
    ('user', '$2a$12$Ju75hxXXA2D81vXxj3GWSOUf7YoliRdbX.RcNRGfEmFzz2UfDTzpa', 'user@example.com', 'Y', 0, NULL),
    ('admin', '$2a$12$yFszpJFFoBOOn9HgwjHGxeW4exvztGBa75x6X6wWFXnctbu0r.eiu', 'admin@example.com', 'Y', 0, NULL),
    ('disabledUser', '$2a$12$Ju75hxXXA2D81vXxj3GWSOUf7YoliRdbX.RcNRGfEmFzz2UfDTzpa', 'disabled@example.com', 'N', 0, NULL);

-- Insert authorities

