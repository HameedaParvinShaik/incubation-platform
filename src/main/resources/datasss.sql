-- =============================================
-- DATA INITIALIZATION (WITHOUT DELETING)
-- =============================================

USE incubation_db;

-- =============================================
-- INSERT ROLES (Skip if exists)
-- =============================================
INSERT IGNORE INTO roles (id, name, description) VALUES 
(1, 'ROLE_ADMIN', 'System Administrator'),
(2, 'ROLE_MANAGER', 'Incubation Manager'),
(3, 'ROLE_MENTOR', 'Mentor'),
(4, 'ROLE_FOUNDER', 'Startup Founder'),
(5, 'ROLE_EVALUATOR', 'Evaluator'),
(6, 'ROLE_INVESTOR', 'Investor'),
(7, 'ROLE_TEAM_MEMBER', 'Team Member');

-- =============================================
-- INSERT USERS (Skip if exists) 
-- Password: 123456
-- =============================================
INSERT IGNORE INTO users (id, email, first_name, last_name, password, is_active, created_at, updated_at) VALUES 
(1, 'admin@startup.com', 'Admin', 'User', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 1, NOW(), NOW()),
(2, 'manager@startup.com', 'Manager', 'User', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 1, NOW(), NOW()),
(3, 'mentor@startup.com', 'Mentor', 'User', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 1, NOW(), NOW()),
(4, 'founder@startup.com', 'Founder', 'User', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 1, NOW(), NOW()),
(5, 'evaluator@startup.com', 'Evaluator', 'User', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 1, NOW(), NOW()),
(6, 'investor@startup.com', 'Investor', 'User', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 1, NOW(), NOW()),
(7, 'team@startup.com', 'Team', 'Member', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 1, NOW(), NOW());

-- =============================================
-- ASSIGN ROLES (Skip if exists)
-- =============================================
INSERT IGNORE INTO user_roles (user_id, role_id) VALUES 
(1, 1), (2, 2), (3, 3), (4, 4), (5, 5), (6, 6), (7, 7);