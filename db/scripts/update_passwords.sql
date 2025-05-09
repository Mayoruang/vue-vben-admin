-- Update the password hash for all users
UPDATE t_user 
SET password = '$2a$10$4FJ697z9CvmYn0hlxR9zeOT46Oyc0/32mrOutav09P9YjUHrABD0i' 
WHERE username IN ('admin', 'operator', 'guest', 'vben'); 