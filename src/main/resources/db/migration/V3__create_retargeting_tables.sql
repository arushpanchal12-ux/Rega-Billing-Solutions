-- Retargeting System Database Migration
-- V3__create_retargeting_tables.sql

-- Update pre_customers table for retargeting
ALTER TABLE pre_customers ADD COLUMN IF NOT EXISTS unsubscribed_at TIMESTAMP NULL;
ALTER TABLE pre_customers ADD COLUMN IF NOT EXISTS last_retargeting_sent TIMESTAMP NULL;
ALTER TABLE pre_customers ADD COLUMN IF NOT EXISTS retargeting_week INTEGER DEFAULT 0;
ALTER TABLE pre_customers ADD COLUMN IF NOT EXISTS conversion_source VARCHAR(50) NULL;

-- Retargeting Templates Table
CREATE TABLE IF NOT EXISTS retargeting_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_name VARCHAR(255) NOT NULL,
    template_type VARCHAR(50) NOT NULL,
    subject_line VARCHAR(500),
    message_content TEXT NOT NULL,
    campaign_week INTEGER NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    expected_conversion_rate DOUBLE,
    cost_per_message DOUBLE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_template_type_week (template_type, campaign_week)
);

-- Retargeting Campaigns Table
CREATE TABLE IF NOT EXISTS retargeting_campaigns (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pre_customer_id BIGINT NOT NULL,
    campaign_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
    message_content TEXT,
    email_subject VARCHAR(500),
    scheduled_at TIMESTAMP,
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    opened_at TIMESTAMP,
    clicked_at TIMESTAMP,
    delivery_status VARCHAR(100),
    external_message_id VARCHAR(255),
    error_message TEXT,
    campaign_week INTEGER,
    retry_count INTEGER DEFAULT 0,
    cost_incurred DOUBLE DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (pre_customer_id) REFERENCES pre_customers(id),
    INDEX idx_retargeting_status_scheduled (status, scheduled_at),
    INDEX idx_retargeting_pre_customer (pre_customer_id),
    INDEX idx_retargeting_external_id (external_message_id)
);

-- Retargeting Metrics Table
CREATE TABLE IF NOT EXISTS retargeting_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT,
    pre_customer_id BIGINT,
    event_type VARCHAR(50) NOT NULL,
    event_timestamp TIMESTAMP NOT NULL,
    metadata TEXT,
    cost_incurred DOUBLE DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_metrics_event_date (event_type, event_timestamp),
    INDEX idx_metrics_campaign (campaign_id),
    INDEX idx_metrics_pre_customer (pre_customer_id)
);

-- Insert Default Email Templates
INSERT IGNORE INTO retargeting_templates (template_name, template_type, subject_line, message_content, campaign_week, expected_conversion_rate, cost_per_message) VALUES
('week1_email', 'EMAIL', 
'Complete Your Rega Billing Setup - Only ₹499!', 
'<html><body><h2>Hi {{firstName}},</h2><p>You signed up {{daysSinceSignup}} days ago and we want to help you get started with Rega Billing Solutions!</p><h3>Why Complete Your Setup Today:</h3><ul><li>✅ Automated invoice generation - Save 5+ hours weekly</li><li>✅ Payment tracking & reminders - Never miss a payment</li><li>✅ Professional customer experience - Branded invoices & receipts</li><li>✅ Real-time analytics - Track your business growth</li><li>✅ 24/7 customer support - We''re here to help</li></ul><p><strong>Special Launch Offer: Get started for just ₹499!</strong></p><p>That''s less than what you''d spend on manual billing tools in a week.</p><center><a href="https://regabilling.com/complete-payment?email={{email}}&source=retargeting_week1" style="background-color: #007bff; color: white; padding: 12px 24px; text-decoration: none; border-radius: 4px;">Complete Payment - ₹499 Only</a></center><p>Questions? Simply reply to this email - our team is ready to help you succeed!</p><p>Best regards,<br><strong>The Rega Billing Team</strong><br>Making billing simple for businesses like yours</p></body></html>', 1, 0.12, 0.50),

('week2_email', 'EMAIL', 
'Don''t Let Manual Billing Slow Your Growth - Join 2,500+ Businesses!', 
'<html><body><h2>Hi {{firstName}},</h2><p>We noticed you haven''t completed your Rega Billing Solutions setup yet.</p><p>Thousands of businesses are already streamlining their billing with our platform. Don''t let manual billing slow you down!</p><h3>What You''re Missing:</h3><ul><li>⏰ Hours of manual work eliminated</li><li>💰 Faster payment collection</li><li>📊 Real-time business insights</li><li>🚀 Professional customer experience</li></ul><p><strong>Limited Time: Get started for just ₹499 - that''s less than the cost of hiring help for one day!</strong></p><center><a href="https://regabilling.com/complete-payment?email={{email}}&source=retargeting_week2" style="background-color: #28a745; color: white; padding: 12px 24px; text-decoration: none; border-radius: 4px;">Start Your Billing Journey</a></center><p>Questions? We''re here to help: support@regabilling.com</p><p>Best,<br>The Rega Billing Team</p></body></html>', 2, 0.08, 0.50),

('week3_email', 'EMAIL', 
'Final Reminder: Your Billing Solution is Still Waiting - ₹499', 
'<html><body><h2>Hi {{firstName}},</h2><p>This is our final reminder about your Rega Billing Solutions account.</p><p>It''s been {{daysSinceSignup}} days since you started the signup process, and we''ve been holding your spot.</p><h3>What Happens When You Complete Setup Today:</h3><ul><li>✅ Instant access to your billing dashboard</li><li>✅ Create your first professional invoice in 2 minutes</li><li>✅ Set up automated payment reminders</li><li>✅ Start tracking your business growth</li><li>✅ Free onboarding call with our team</li></ul><p><strong>Still Just ₹499 - No Hidden Costs</strong></p><center><a href="https://regabilling.com/complete-payment?email={{email}}&source=retargeting_week3" style="background-color: #dc3545; color: white; padding: 12px 24px; text-decoration: none; border-radius: 4px;">Complete Setup Now - ₹499</a></center><p>Thank you for considering Rega Billing Solutions for your business.</p><p>Best regards,<br>The Rega Team</p></body></html>', 3, 0.05, 0.50),

('week4_email', 'EMAIL', 
'Account Closing Soon - Last Chance to Join Rega Billing - ₹499', 
'<html><body><h2>Hi {{firstName}},</h2><p>After {{daysSinceSignup}} days of holding your spot, we need to make room for other businesses.</p><p><strong>This is your absolute last chance.</strong></p><h3>What You''re Missing:</h3><ul><li>🏆 Join 2,500+ successful businesses</li><li>⏰ Save 25+ hours monthly on billing tasks</li><li>💰 Improve payment collection by 40%</li><li>📊 Get real-time business insights</li><li>🎯 Focus on growth, not admin work</li></ul><center><a href="https://regabilling.com/complete-payment?email={{email}}&source=retargeting_week4_final" style="background-color: #dc3545; color: white; padding: 15px 30px; text-decoration: none; border-radius: 5px; font-weight: bold;">SECURE MY SPOT NOW - ₹499</a></center><p>Don''t let manual billing hold your business back any longer.</p><p>Final regards,<br>The Rega Team</p></body></html>', 4, 0.03, 0.50);

-- Insert Default SMS Templates  
INSERT IGNORE INTO retargeting_templates (template_name, template_type, message_content, campaign_week, expected_conversion_rate, cost_per_message) VALUES
('week2_sms', 'SMS', 
'Hi {{firstName}}! Don''t let manual billing slow your growth. Join 2,500+ businesses using Rega Billing for just ₹499. Complete setup: https://regabilling.com/pay?email={{email}}&src=sms2', 
2, 0.06, 3.00),

('week4_sms', 'SMS', 
'{{firstName}}, FINAL NOTICE: Your Rega Billing account closes today! Secure your spot now for ₹499. Don''t miss out: https://regabilling.com/pay?email={{email}}&src=sms4 - Rega Team', 
4, 0.04, 3.00);
