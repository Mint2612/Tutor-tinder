ALTER TABLE matching_pairs
 ADD parent_confirm VARCHAR(7) NULL CHECK (parent_confirm IN ('CONFIRM','DECLINE')),
 ADD tutor_confirm VARCHAR(7) NULL CHECK (tutor_confirm IN ('CONFIRM','DECLINE')),
 ADD confirm_expires_at TIMESTAMP(6) NULL;
-- Match cũ nhận đủ 3 ngày kể từ lúc nâng cấp.
UPDATE matching_pairs SET confirm_expires_at = TIMESTAMPADD(DAY,3,CURRENT_TIMESTAMP)
 WHERE status='MATCHED';