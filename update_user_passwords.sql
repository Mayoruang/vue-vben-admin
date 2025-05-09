-- Create a V3 migration to update the user passwords
CREATE OR REPLACE FUNCTION update_password() RETURNS void AS $$
BEGIN
    UPDATE t_user SET password = '$2a$10$4FJ697z9CvmYn0hlxR9zeOT46Oyc0/32mrOutav09P9YjUHrABD0i' 
    WHERE username IN ('admin', 'operator', 'guest', 'vben');
END;
$$ LANGUAGE plpgsql;

SELECT update_password();
DROP FUNCTION update_password(); 