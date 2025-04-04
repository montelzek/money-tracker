package com.montelzek.moneytrack.controller;

import com.montelzek.moneytrack.config.SecurityConfig;
import com.montelzek.moneytrack.dto.UserRegisterDTO;
import com.montelzek.moneytrack.model.Role;
import com.montelzek.moneytrack.model.User;
import com.montelzek.moneytrack.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@ExtendWith(MockitoExtension.class)
@Import(SecurityConfig.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoleService roleService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private ExchangeRateService exchangeRateService;

    @MockitoBean
    private BudgetService budgetService;

    @MockitoBean
    private FinancialGoalService financialGoalService;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private UserRegisterDTO userRegisterDTO;
    private Role userRole;
    private User testUser;

    @BeforeEach
    public void setup() {
        userRegisterDTO = new UserRegisterDTO();
        userRegisterDTO.setEmail("test@gmail.com");
        userRegisterDTO.setPassword("password");
        userRegisterDTO.setFirstName("Jane");
        userRegisterDTO.setLastName("Doe");

        userRole = new Role();
        userRole.setName(Role.ERole.ROLE_USER);

        testUser = new User(
                userRegisterDTO.getEmail(),
                "encodedPassword",
                userRegisterDTO.getFirstName(),
                userRegisterDTO.getLastName()
        );
        testUser.setId(1L);
        testUser.setRoles(Collections.singleton(userRole));
    }

    @Test
    public void showLoginPage_shouldShowLoginPage() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    public void showRegistrationForm_shouldShowRegistrationForm() throws Exception{
        // Act & Assert
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    public void saveUser_uniqueEmailAndValidData_shouldRegisterUser() throws Exception {
        // Arrange
        when(userService.existsByEmail(userRegisterDTO.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(userRegisterDTO.getPassword())).thenReturn("encodedPassword");
        when(roleService.findByName(Role.ERole.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(userService.save(any(User.class))).thenReturn(testUser);

        // Act & Assert
        mockMvc.perform(post("/register")
                .flashAttr("user", userRegisterDTO)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true"));

        verify(userService).existsByEmail(userRegisterDTO.getEmail());
        verify(passwordEncoder).encode(userRegisterDTO.getPassword());
        verify(roleService).findByName(Role.ERole.ROLE_USER);
        verify(userService).save(any(User.class));
    }

    @Test
    public void saveUser_existingEmail_shouldNotRegisterUser() throws Exception{
        // Arrange
        when(userService.existsByEmail(userRegisterDTO.getEmail())).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/register")
                .flashAttr("user", userRegisterDTO)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("user", "email"));

        verify(userService).existsByEmail(userRegisterDTO.getEmail());
        verify(userService, never()).save(any(User.class));
    }

    @Test
    public void saveUser_invalidData_shouldNotRegisterUser() throws Exception {
        // Arrange
        userRegisterDTO.setEmail(null);

        // Act & Assert
        mockMvc.perform(post("/register")
                .flashAttr("user", userRegisterDTO)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("user", "email"));

        verify(userService, never()).existsByEmail(anyString());
        verify(userService, never()).save(any(User.class));
    }

    @Test
    @WithMockUser
    public void dashboard_shouldShowDashboard() throws Exception{
        // Arrange
        Long userId = testUser.getId();
        when(userService.getCurrentUserId()).thenReturn(userId);
        when(userService.findById(userId)).thenReturn(Optional.of(testUser));
        when(categoryService.findByType(anyString())).thenReturn(Collections.emptyList());
        when(accountService.findUsersAccounts(userId)).thenReturn(Collections.emptyList());
        when(accountService.getTotalBalance(userId)).thenReturn(BigDecimal.TEN);
        when(transactionService.getIncomeFromLastMonth(userId)).thenReturn(BigDecimal.valueOf(100));
        when(transactionService.getExpensesFromLastMonth(userId)).thenReturn(BigDecimal.valueOf(50));
        when(transactionService.getExpensesByCategoryFromLastMonth(userId)).thenReturn(Collections.singletonMap("Food", BigDecimal.valueOf(25)));
        when(transactionService.getTransactionsFromLastSixMonths(userId)).thenReturn(Collections.emptyMap());
        when(transactionService.getRecentTransactions(userId)).thenReturn(Collections.emptyList());
        when(exchangeRateService.getRates()).thenReturn(Collections.singletonMap("USD", BigDecimal.ONE));
        when(budgetService.findActiveBudgets(eq(userId), any(LocalDate.class))).thenReturn(Collections.emptyList());
        when(financialGoalService.findTop3Goals(userId)).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists(
                        "transaction", "incomeCategories", "expenseCategories",
                        "financialGoalCategories", "accounts", "user", "totalBalance",
                        "incomeFromLastMonth", "expensesFromLastMonth", "expensesByCategory",
                        "transactionsLastSixMonths", "recentTransactions", "rates",
                        "activeBudgets", "financialGoals"
                ));

        verify(userService).getCurrentUserId();
        verify(userService).findById(userId);
        verify(categoryService, times(3)).findByType(anyString());
        verify(accountService).findUsersAccounts(userId);
        verify(accountService).getTotalBalance(userId);
        verify(transactionService).getIncomeFromLastMonth(userId);
        verify(transactionService).getExpensesFromLastMonth(userId);
        verify(transactionService).getExpensesByCategoryFromLastMonth(userId);
        verify(transactionService).getTransactionsFromLastSixMonths(userId);
        verify(transactionService).getRecentTransactions(userId);
        verify(exchangeRateService).getRates();
        verify(budgetService).findActiveBudgets(eq(userId), any(LocalDate.class));
        verify(financialGoalService).findTop3Goals(userId);
    }

    @Test
    @WithMockUser
    void showAccessDenied_shouldShowAccessDeniedPage() throws Exception {
        mockMvc.perform(get("/access-denied"))
                .andExpect(status().isOk())
                .andExpect(view().name("access-denied"));
    }
}
