package com.montelzek.moneytrack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 120)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type")
    private AccountType accountType;

    @NotNull
    private BigDecimal balance;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    private List<Transaction> transactions;

    public enum AccountType {
        SAVINGS,
        CHECKING,
        CREDIT_CARD,
        CASH
    }

    public enum Currency {
        USD,
        EUR,
        PLN,
        GBP,
        CHF,
        JPY
    }

    public Account(String name, AccountType accountType, BigDecimal balance, Currency currency) {
        this.name = name;
        this.accountType = accountType;
        this.balance = balance;
        this.currency = currency;
    }
}
