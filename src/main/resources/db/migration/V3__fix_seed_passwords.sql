-- V3 — Fix seed user passwords (BCrypt hash of "Password123")
UPDATE users
SET password_hash = '$2a$10$IOFQDV0tgCz2i68Xr4RE1uJiuG0w4hRvqDSEBbyTBDk4i.3DkWcLK'
WHERE email IN (
    'admin@reclaim.app',
    'ama.mensah@ug.edu.gh',
    'kwame.asante@ug.edu.gh',
    'efua.darkwa@ug.edu.gh',
    'kofi.boateng@ug.edu.gh'
);
