ALTER TABLE item
ADD COLUMN category_id BIGINT;

ALTER TABLE item
ADD CONSTRAINT fk_item_category
FOREIGN KEY (category_id)
REFERENCES category (id);