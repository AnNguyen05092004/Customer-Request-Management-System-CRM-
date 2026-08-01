-- Demo-only accounts. Password for every account is: 1234
INSERT INTO members (email, name, role, password)
VALUES
    ('admin@bzcom.com', 'Admin', 'ADMIN', '$2y$10$9FEnUY37Ok54UtidIbFlyOyNct/GMaF9dG1.GtIemnW6vWTPyfnGW'),
    ('dev1@bzcom.com', 'Dev One', 'DEVELOPER', '$2y$10$9FEnUY37Ok54UtidIbFlyOyNct/GMaF9dG1.GtIemnW6vWTPyfnGW'),
    ('dev2@bzcom.com', 'Dev Two', 'DEVELOPER', '$2y$10$9FEnUY37Ok54UtidIbFlyOyNct/GMaF9dG1.GtIemnW6vWTPyfnGW'),
    ('client1@bzcom.com', 'Client One', 'CLIENT', '$2y$10$9FEnUY37Ok54UtidIbFlyOyNct/GMaF9dG1.GtIemnW6vWTPyfnGW');

INSERT INTO requests (title, description, category, priority, status, client_id)
SELECT seed.title, seed.description, seed.category, seed.priority, 'PENDING', member.id
FROM (
    VALUES
        ('Login fails', 'Click login shows 500', 'BUG', 'HIGH'),
        ('Add Google login', 'SSO with Google', 'FEATURE', 'MEDIUM'),
        ('How to reset pass?', 'Cannot find the button', 'INQUIRY', 'LOW')
) AS seed(title, description, category, priority)
JOIN members member ON member.email = 'client1@bzcom.com';
