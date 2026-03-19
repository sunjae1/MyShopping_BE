package myex.shopping.repository.jpa;

import myex.shopping.domain.Category;
import myex.shopping.domain.Item;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;



import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaItemRepository.class)
class JpaItemRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private JpaItemRepository itemRepository;

    @Test
    @DisplayName("새로운 아이템 저장 테스트")
    void saveNewItem() {
        // given
        Item item = new Item();
        item.setItemName("Test Item");
        item.setPrice(10000);
        item.setQuantity(10);

        // when
        Item savedItem = itemRepository.save(item);

        // then
        assertThat(savedItem.getId()).isNotNull();
        assertThat(savedItem.getItemName()).isEqualTo("Test Item");
        assertThat(savedItem.isDeleted()).isFalse();

        Item foundItem = em.find(Item.class, savedItem.getId());
        assertThat(foundItem).isNotNull();
        assertThat(foundItem.getItemName()).isEqualTo("Test Item");
    }

    @Test
    @DisplayName("기존 아이템 업데이트 테스트 (save or merge)")
    void updateExistingItem() {
        // given
        Item item = new Item();
        item.setItemName("Original Item");
        item.setPrice(10000);
        item.setQuantity(10);
        Item savedItem = em.persistAndFlush(item);
        em.detach(savedItem); // 준영속 상태로 변경

        // when
        savedItem.setItemName("Updated Item");
        Item updatedItem = itemRepository.save(savedItem);
        em.flush();

        // then
        Item foundItem = em.find(Item.class, savedItem.getId());
        assertThat(foundItem.getItemName()).isEqualTo("Updated Item");
    }

    @Test
    @DisplayName("ID로 아이템 조회 (삭제되지 않은 아이템)")
    void findById_NotDeleted() {
        // given
        Item item = new Item();
        item.setItemName("Find Me");
        item.setPrice(1000);
        item.setQuantity(1);
        item.setDeleted(false);
        em.persistAndFlush(item);

        // when
        Optional<Item> foundItemOpt = itemRepository.findById(item.getId());

        // then
        assertThat(foundItemOpt).isPresent();
        assertThat(foundItemOpt.get().getId()).isEqualTo(item.getId());
    }

    @Test
    @DisplayName("ID로 아이템 조회 (삭제된 아이템)")
    void findById_Deleted() {
        // given
        Item item = new Item();
        item.setItemName("Deleted Item");
        item.setPrice(1000);
        item.setQuantity(1);
        item.setDeleted(true); // soft-deleted
        em.persistAndFlush(item);

        // when
        Optional<Item> foundItemOpt = itemRepository.findById(item.getId());

        // then
        assertThat(foundItemOpt).isNotPresent();
    }

    @Test
    @DisplayName("모든 아이템 조회 (삭제된 아이템 제외)")
    void findAll_ExcludesDeleted() {
        // given
        Item item1 = new Item();
        item1.setItemName("Item 1");
        item1.setPrice(1000);
        item1.setQuantity(1);
        item1.setDeleted(false);
        em.persist(item1);

        Item item2 = new Item();
        item2.setItemName("Item 2");
        item2.setPrice(2000);
        item2.setQuantity(2);
        item2.setDeleted(true); // soft-deleted
        em.persist(item2);

        Item item3 = new Item();
        item3.setItemName("Item 3");
        item3.setPrice(3000);
        item3.setQuantity(3);
        item3.setDeleted(false);
        em.persist(item3);
        em.flush();

        // when
        List<Item> items = itemRepository.findAll();

        // then
        assertThat(items).hasSize(2);
        assertThat(items).extracting(Item::getItemName).containsExactlyInAnyOrder("Item 1", "Item 3");
    }
    
    @Test
    @DisplayName("아이템 정보 업데이트")
    void updateItem() {
        // given
        Item originalItem = new Item();
        originalItem.setItemName("Original Name");
        originalItem.setPrice(100);
        originalItem.setQuantity(10);
        originalItem.setImageUrl("original.jpg");
        Category originalCategory = new Category();
        originalCategory.setName("원본 카테고리");
        em.persist(originalCategory);
        originalItem.changeCategory(originalCategory);
        em.persistAndFlush(originalItem);
        Long itemId = originalItem.getId();
        em.detach(originalItem);

        Category updatedCategory = new Category();
        updatedCategory.setName("수정 카테고리");
        em.persistAndFlush(updatedCategory);

        Item updateParam = new Item();
        updateParam.setItemName("Updated Name");
        updateParam.setPrice(200);
        updateParam.setQuantity(20);
        updateParam.setImageUrl("updated.jpg");
        updateParam.changeCategory(updatedCategory);

        // when
        itemRepository.update(itemId, updateParam);
        em.flush();
        em.clear();
        
        // then
        Item foundItem = em.find(Item.class, itemId);
        assertThat(foundItem.getItemName()).isEqualTo("Updated Name");
        assertThat(foundItem.getPrice()).isEqualTo(200);
        assertThat(foundItem.getQuantity()).isEqualTo(20);
        assertThat(foundItem.getImageUrl()).isEqualTo("updated.jpg");
        assertThat(foundItem.getCategory()).isNotNull();
        assertThat(foundItem.getCategory().getName()).isEqualTo("수정 카테고리");
    }

    @Test
    @DisplayName("아이템 소프트 삭제 테스트")
    void deleteItem() {
        // given
        Item item = new Item();
        item.setItemName("To Be Deleted");
        item.setPrice(1000);
        item.setQuantity(5);
        em.persistAndFlush(item);
        Long itemId = item.getId();

        // when
        itemRepository.deleteItem(itemId);
        em.flush();
        em.clear();

        // then
        Item deletedItem = em.find(Item.class, itemId);
        assertThat(deletedItem).isNotNull();
        assertThat(deletedItem.isDeleted()).isTrue();

        // findById should not return soft-deleted items
        Optional<Item> foundItemOpt = itemRepository.findById(itemId);
        assertThat(foundItemOpt).isNotPresent();
    }

    @Test
    @DisplayName("이름으로 아이템 검색")
    void searchByName() {
        // given
        Item item1 = new Item();
        item1.setItemName("Apple iPhone");
        item1.setPrice(1500000);
        item1.setQuantity(10);
        em.persist(item1);

        Item item2 = new Item();
        item2.setItemName("Samsung Galaxy");
        item2.setPrice(1300000);
        item2.setQuantity(10);
        em.persist(item2);
        
        Item item3 = new Item();
        item3.setItemName("Apple MacBook");
        item3.setPrice(2500000);
        item3.setQuantity(5);
        em.persist(item3);
        em.flush();

        // when
        List<Item> searchResults = itemRepository.searchByName("Apple");

        // then
        assertThat(searchResults).hasSize(2);
        assertThat(searchResults).extracting(Item::getItemName).contains("Apple iPhone", "Apple MacBook");
    }
}
