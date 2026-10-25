-- Cosmetic Store Database (MySQL 8+) - large sample dataset
DROP DATABASE IF EXISTS cosmetic_store;
CREATE DATABASE cosmetic_store;
USE cosmetic_store;

-- ---------- TABLES ----------
CREATE TABLE product_types (
    type_id         INT AUTO_INCREMENT PRIMARY KEY,
    type_name       VARCHAR(50)  NOT NULL UNIQUE,
    attribute_label VARCHAR(50)  NOT NULL,
    gst_rate        DECIMAL(5,2) NOT NULL CHECK (gst_rate >= 0)
);

CREATE TABLE categories (
    category_id   INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE products (
    product_id      INT PRIMARY KEY,
    name            VARCHAR(100)  NOT NULL,
    brand           VARCHAR(60)   NOT NULL,
    base_price      DECIMAL(10,2) NOT NULL CHECK (base_price >= 0),
    stock_quantity  INT           NOT NULL CHECK (stock_quantity >= 0),
    attribute_value VARCHAR(80),
    category_id     INT NOT NULL,
    type_id         INT NOT NULL,
    FOREIGN KEY (category_id) REFERENCES categories(category_id),
    FOREIGN KEY (type_id)     REFERENCES product_types(type_id) ON DELETE RESTRICT
);

CREATE TABLE stock_transactions (
    txn_id          INT AUTO_INCREMENT PRIMARY KEY,
    product_id      INT NOT NULL,
    quantity_change INT NOT NULL,
    txn_time        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
);

-- ---------- PRODUCT TYPES (8) ----------
INSERT INTO product_types (type_name, attribute_label, gst_rate) VALUES
('Makeup',            'Shade',     18.00),   -- 1
('Skincare',          'Skin Type', 12.00),   -- 2
('Haircare',          'Hair Type', 18.00),   -- 3
('Fragrance',         'Scent',     18.00),   -- 4
('Bath & Body',       'Scent',     12.00),   -- 5
('Nail Care',         'Finish',    18.00),   -- 6
('Mens Grooming',     'Variant',   18.00),   -- 7
('Tools & Accessories','Material', 12.00);   -- 8

-- ---------- CATEGORIES (18) ----------
INSERT INTO categories (category_name) VALUES
('Lip Cosmetics'),            -- 1
('Face Cleansers & Serums'),  -- 2
('Eye Makeup'),               -- 3
('Face Makeup'),              -- 4
('Nail Polish & Care'),       -- 5
('Moisturizers'),             -- 6
('Sunscreen'),                -- 7
('Shampoo'),                  -- 8
('Conditioner & Masks'),      -- 9
('Hair Oil'),                 -- 10
('Hair Styling'),             -- 11
('Perfumes & Mists'),         -- 12
('Deodorants'),               -- 13
('Body Wash'),                -- 14
('Body Lotion & Butter'),     -- 15
('Shaving & Beard'),          -- 16
('Makeup Tools'),             -- 17
('Face Masks');               -- 18

-- ---------- PRODUCTS (50) ----------
INSERT INTO products (product_id, name, brand, base_price, stock_quantity, attribute_value, category_id, type_id) VALUES
(101, 'Matte Lipstick',          'Lakme',            450.00,  50, 'Ruby Red',         1, 1),
(102, 'Neem Face Wash',          'Himalaya',         150.00,  15, 'Acne-prone',       2, 2),
(103, 'Eyeconic Kajal',          'Lakme',            200.00, 100, 'Deep Black',       3, 1),
(104, 'Vitamin C Serum',         'Garnier',          499.00,   5, 'All Skin Types',   2, 2),
(105, 'Liquid Lipstick',         'Maybelline',       599.00,  40, 'Brick Nude',       1, 1),
(106, 'Lip Balm',                'Nivea',            199.00,  80, 'Dry Lips',         1, 2),
(107, 'Lip Gloss',               'Sugar',            399.00,  25, 'Clear Shine',      1, 1),
(108, 'Gel Eyeliner',            'Maybelline',       349.00,  60, 'Jet Black',        3, 1),
(109, 'Volume Mascara',          'Lakme',            475.00,  35, 'Black',            3, 1),
(110, 'Eyeshadow Palette',       'Colorbar',         899.00,  12, 'Sunset Nudes',     3, 1),
(111, 'Compact Powder',          'Lakme',            325.00,  45, 'Ivory',            4, 1),
(112, 'Liquid Foundation',       'Maybelline',       599.00,  30, 'Natural Beige',    4, 1),
(113, 'Concealer',               'Loreal',           749.00,  18, 'Fair Neutral',     4, 1),
(114, 'Cream Blush',             'Sugar',            499.00,  22, 'Peach Pop',        4, 1),
(115, 'Nail Polish',             'Lakme',            150.00,  90, 'Matte',            5, 6),
(116, 'Gel Nail Polish',         'Colorbar',         299.00,  40, 'Glossy',           5, 6),
(117, 'Nail Polish Remover',     'Revlon',           175.00,  55, 'Acetone-free',     5, 6),
(118, 'Soft Moisturizer',        'Cetaphil',         549.00,  28, 'Dry Skin',         6, 2),
(119, 'Aloe Vera Gel',           'Patanjali',        120.00,  70, 'All Skin Types',   6, 2),
(120, 'Night Cream',             'Olay',             899.00,  14, 'Normal to Dry',    6, 2),
(121, 'Sunscreen SPF 50',        'Neutrogena',       699.00,   3, 'Oily Skin',        7, 2),
(122, 'Sunscreen Gel SPF 40',    'Lotus',            450.00,  26, 'Combination',      7, 2),
(123, 'Anti-Hair Fall Shampoo',  'Dove',             320.00,  60, 'Thin Hair',        8, 3),
(124, 'Anti-Dandruff Shampoo',   'Head & Shoulders', 280.00,   8, 'Oily Scalp',       8, 3),
(125, 'Herbal Shampoo',          'Biotique',         350.00,  33, 'All Hair Types',   8, 3),
(126, 'Smooth Conditioner',      'Tresemme',         380.00,  20, 'Frizzy Hair',      9, 3),
(127, 'Repair Hair Mask',        'Loreal',           520.00,  16, 'Damaged Hair',     9, 3),
(128, 'Coconut Hair Oil',        'Parachute',        180.00, 120, 'Dry Scalp',       10, 3),
(129, 'Onion Hair Oil',          'Mamaearth',        399.00,   4, 'Hair Fall',       10, 3),
(130, 'Hair Serum',              'Livon',            250.00,  48, 'Frizz Control',   11, 3),
(131, 'Styling Hair Gel',        'Park Avenue',      150.00,  37, 'Strong Hold',     11, 3),
(132, 'Eau de Parfum',           'Engage',           349.00,  42, 'Floral',          12, 4),
(133, 'Perfume Spray',           'Wild Stone',       799.00,  15, 'Woody',           12, 4),
(134, 'Body Mist',               'The Body Shop',    995.00,  10, 'Vanilla',         12, 4),
(135, 'Deodorant Spray',         'Nivea Men',        220.00,  65, 'Fresh',           13, 4),
(136, 'Roll-on Deodorant',       'Dove',             175.00,  50, 'Original',        13, 4),
(137, 'Body Wash',               'Dove',             299.00,  38, 'Cucumber',        14, 5),
(138, 'Shower Gel',              'Nivea',            249.00,   2, 'Cherry Blossom',  14, 5),
(139, 'Body Lotion',             'Vaseline',         280.00,  55, 'Cocoa Glow',      15, 5),
(140, 'Body Butter',             'The Body Shop',   1295.00,   7, 'Shea',            15, 5),
(141, 'Beard Oil',               'Beardo',           399.00,  24, 'Cedarwood',       16, 7),
(142, 'Shaving Foam',            'Gillette',         220.00,  44, 'Classic',         16, 7),
(143, 'Aftershave Gel',          'Park Avenue',      199.00,  30, 'Cool Blue',       16, 7),
(144, 'Face Wash for Men',       'Garnier Men',      210.00,  52, 'Oil Clear',        2, 7),
(145, 'Makeup Brush Set',        'Vega',             899.00,  19, 'Synthetic',       17, 8),
(146, 'Makeup Sponge',           'Colorbar',         299.00,  27, 'Latex-free',      17, 8),
(147, 'Eyelash Curler',          'Vega',             249.00,  21, 'Steel',           17, 8),
(148, 'Face Roller',             'Mamaearth',        599.00,   9, 'Rose Quartz',     17, 8),
(149, 'Sheet Mask',              'Innisfree',        150.00,  85, 'Green Tea',       18, 2),
(150, 'Clay Mask',               'Mamaearth',        449.00,  13, 'Oily Skin',       18, 2);

-- ---------- STOCK TRANSACTIONS (sample history) ----------
INSERT INTO stock_transactions (product_id, quantity_change, txn_time) VALUES
(101,  50, '2026-09-01 10:00:00'),
(101,  -5, '2026-09-03 14:20:00'),
(102,  20, '2026-09-01 10:05:00'),
(102,  -5, '2026-09-05 16:45:00'),
(104,  30, '2026-09-02 11:00:00'),
(104, -25, '2026-09-10 18:30:00'),
(121,  20, '2026-09-04 09:30:00'),
(121, -17, '2026-09-12 13:10:00'),
(124,  25, '2026-09-04 09:40:00'),
(124, -17, '2026-09-15 17:00:00'),
(129,  15, '2026-09-06 12:00:00'),
(129, -11, '2026-09-18 15:25:00'),
(138,  12, '2026-09-07 10:20:00'),
(138, -10, '2026-09-20 19:05:00'),
(128,  60, '2026-09-08 11:15:00'),
(139, -10, '2026-09-22 12:40:00'),
(112, -10, '2026-09-25 16:00:00'),
(149,  40, '2026-10-01 10:00:00'),
(135, -15, '2026-10-05 18:15:00'),
(103,  20, '2026-10-08 09:50:00');

-- ---------- VIEWS ----------
CREATE VIEW product_details AS
SELECT p.product_id, p.brand, p.name, c.category_name,
       t.type_name, t.attribute_label, p.attribute_value,
       p.stock_quantity, p.base_price,
       ROUND(p.base_price * (1 + t.gst_rate / 100), 2) AS final_price
FROM products p
JOIN categories    c ON c.category_id = p.category_id
JOIN product_types t ON t.type_id     = p.type_id;

CREATE VIEW low_stock_report AS
SELECT product_id, brand, name, stock_quantity
FROM products
WHERE stock_quantity < 20
ORDER BY stock_quantity;

-- ---------- PROCEDURE: buy/sell (negative qty = sell) ----------
DELIMITER //
CREATE PROCEDURE process_transaction(IN p_id INT, IN qty INT)
BEGIN
    DECLARE current_stock INT;
    SELECT stock_quantity INTO current_stock FROM products WHERE product_id = p_id FOR UPDATE;

    IF current_stock IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Product ID not found.';
    ELSEIF qty < 0 AND ABS(qty) > current_stock THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient stock.';
    ELSE
        UPDATE products SET stock_quantity = stock_quantity + qty WHERE product_id = p_id;
        INSERT INTO stock_transactions (product_id, quantity_change) VALUES (p_id, qty);
    END IF;
END //
DELIMITER ;

-- ---------- EXAMPLE QUERIES ----------
-- All products:         SELECT * FROM product_details;
-- Low stock:            SELECT * FROM low_stock_report;
-- Sell 3 of ID 101:     CALL process_transaction(101, -3);
-- Restock 10 of 104:    CALL process_transaction(104, 10);
-- Products per type:    SELECT type_name, COUNT(*) FROM product_details GROUP BY type_name;
-- Stock value by type:  SELECT type_name, SUM(stock_quantity * base_price) AS value
--                       FROM product_details GROUP BY type_name;
  --       SELECT p.name, -SUM(t.quantity_change) AS sold
  --               FROM stock_transactions t JOIN products p USING (product_id)
   --              WHERE t.quantity_change < 0 GROUP BY p.name ORDER BY sold DESC;
