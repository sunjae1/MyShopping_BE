ALTER TABLE item
DROP FOREIGN KEY fk_item_category;

ALTER TABLE item
ADD CONSTRAINT fk_item_category
FOREIGN KEY (category_id)
REFERENCES category(id)
ON DELETE SET NULL;