package com.example.demo;

import com.example.demo.controllers.ItemController;
import com.example.demo.controllers.OrderController;
import com.example.demo.controllers.UserController;
import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.UserOrder;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.ItemRepository;
import com.example.demo.model.persistence.repositories.OrderRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.CreateUserRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SareetaApplicationTests {

    @InjectMocks
    private UserController userController;

    @InjectMocks
    private ItemController itemController;

    @InjectMocks
    private OrderController orderController;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    private User sampleUser;
    private Item item1;
    private Item item2;
    private Cart cart;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(passwordEncoder.encode(any(CharSequence.class))).thenReturn("encodedPassword");
        
        sampleUser = createUser("testUser");
        item1 = createItem(1L, "Item 1", BigDecimal.valueOf(5.99));
        item2 = createItem(2L, "Item 2", BigDecimal.valueOf(3.50));
      
        cart = new Cart();
        cart.setItems(new ArrayList<>());
        cart.setTotal(BigDecimal.ZERO);
    }

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setCart(new Cart());
        return user;
    }

    private Item createItem(Long id, String name, BigDecimal price) {
        Item item = new Item();
        item.setId(id);
        item.setName(name);
        item.setPrice(price);
        return item;
    }

    @Test
    public void testCreateUserSuccess() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setPassword("testpassword");
        request.setConfirmPassword("testpassword");

        ResponseEntity<User> response = userController.createUser(request);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("testuser", response.getBody().getUsername());
    }

    @Test
    public void testCreateUserPasswordMismatch() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setPassword("password1");
        request.setConfirmPassword("password2");

        ResponseEntity<User> response = userController.createUser(request);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    public void testFindUserById() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        ResponseEntity<User> response = userController.findById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("testUser", response.getBody().getUsername());
    }

    @Test
    public void testFindUserByIdNotFound() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        ResponseEntity<User> response = userController.findById(2L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testFindUserByUserNameSuccess() {
        when(userRepository.findByUsername("testUser")).thenReturn(sampleUser);

        ResponseEntity<User> response = userController.findByUserName("testUser");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("testUser", response.getBody().getUsername());
    }

    @Test
    public void testGetItems() {
        when(itemRepository.findAll()).thenReturn(List.of(item1));

        ResponseEntity<List<Item>> response = itemController.getItems();

        assertNotNull(response.getBody());
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        assertEquals("Item 1", response.getBody().get(0).getName());
    }

    @Test
    public void testGetItemsByNameSuccess() {
        when(itemRepository.findByName("Item 1")).thenReturn(List.of(item1));

        ResponseEntity<List<Item>> response = itemController.getItemsByName("Item 1");

        assertNotNull(response.getBody());
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        assertEquals("Item 1", response.getBody().get(0).getName());
    }

    @Test
    public void testAddItem() {
        Cart cart = new Cart();
        cart.addItem(item1);

        assertEquals(1, cart.getItems().size());
        assertEquals(BigDecimal.valueOf(5.99), cart.getTotal());

        cart.addItem(item2);
        assertEquals(2, cart.getItems().size());
        assertEquals(BigDecimal.valueOf(9.49), cart.getTotal());
    }

    @Test
    public void testRemoveItem() {
        Cart cart = new Cart();
        cart.addItem(item1);

        assertEquals(BigDecimal.valueOf(5.99), cart.getTotal());

        cart.removeItem(item1);

        assertEquals(BigDecimal.ZERO.setScale(2), cart.getTotal().setScale(2));
    }

    @Test
    public void testSubmitOrder() {
        Cart cart = sampleUser.getCart();
        cart.addItem(item1);

        when(userRepository.findByUsername("testUser")).thenReturn(sampleUser);

        ResponseEntity<UserOrder> response = orderController.submit("testUser");

        assertNotNull(response.getBody());
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().getItems().size());
        assertEquals(BigDecimal.valueOf(5.99), response.getBody().getTotal());
    }

    @Test
    public void testGetOrdersForUser() {
        User user = new User();
        user.setUsername("testUser");

        UserOrder order = new UserOrder();
        order.setUser(user);

        cart.setItems(new ArrayList<>());  // Ensuring items is initialized
        order.setItems(cart.getItems());

        // Mock repository responses
        when(userRepository.findByUsername("testUser")).thenReturn(user);
        when(orderRepository.findByUser(user)).thenReturn(List.of(order));

        // Call controller method
        ResponseEntity<List<UserOrder>> response = orderController.getOrdersForUser("testUser");

        // Validate response
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        assertEquals("testUser", response.getBody().get(0).getUser().getUsername());
    }

}
