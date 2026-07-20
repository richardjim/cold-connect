package com.coldconnect.service;

import com.coldconnect.entity.Wallet;
import com.coldconnect.entity.WalletTransaction;
import com.coldconnect.exception.AppException;
import com.coldconnect.i18n.AppMessages;
import com.coldconnect.repository.WalletRepository;
import com.coldconnect.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class WalletLedgerService {

    private final WalletRepository            walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final AppMessages                 messages;

    public WalletLedgerService(WalletRepository walletRepository,
                               WalletTransactionRepository transactionRepository,
                               AppMessages messages) {
        this.walletRepository      = walletRepository;
        this.transactionRepository = transactionRepository;
        this.messages              = messages;
    }

    public static class WalletDetail {
        public final BigDecimal              balance;
        public final String                  currency;
        public final List<WalletTransaction> transactions;

        public WalletDetail(BigDecimal balance, String currency,
                            List<WalletTransaction> transactions) {
            this.balance      = balance;
            this.currency     = currency;
            this.transactions = transactions;
        }
    }

    public Wallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserId(userId).orElseGet(() -> {
            Wallet w = new Wallet();
            w.setUserId(userId);
            w.setBalance(BigDecimal.ZERO);
            w.setCurrency("NGN");
            w.setActive(true);
            return walletRepository.save(w);
        });
    }

    public WalletDetail getWalletDetail(Long userId) {
        Wallet wallet = getOrCreateWallet(userId);
        List<WalletTransaction> txs =
                transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return new WalletDetail(wallet.getBalance(), wallet.getCurrency(), txs);
    }

    @Transactional
    public WalletTransaction topUp(Long userId, BigDecimal amount,
                                   String method, String language) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException.BadRequestException(
                    "Top-up amount must be greater than zero");
        }

        Wallet wallet       = getOrCreateWallet(userId);
        BigDecimal before   = wallet.getBalance();
        BigDecimal after    = before.add(amount);

        wallet.setBalance(after);
        walletRepository.save(wallet);

        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(userId);
        tx.setType("TOPUP");
        tx.setAmount(amount);
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        tx.setMethod(method);
        tx.setReference("TOPUP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        tx.setStatus("COMPLETED");
        tx.setDescription(messages.get(AppMessages.Key.WALLET_TOPUP_SUCCESS, language));
        return transactionRepository.save(tx);
    }

    @Transactional
    public WalletTransaction withdraw(Long userId, BigDecimal amount,
                                      String method, String language) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException.BadRequestException(
                    "Withdrawal amount must be greater than zero");
        }

        Wallet wallet = getOrCreateWallet(userId);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new AppException.BadRequestException(
                    messages.get(AppMessages.Key.INSUFFICIENT_BALANCE, language)
                            + " — Available: NGN " + wallet.getBalance());
        }

        BigDecimal before = wallet.getBalance();
        BigDecimal after  = before.subtract(amount);

        wallet.setBalance(after);
        walletRepository.save(wallet);

        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(userId);
        tx.setType("WITHDRAWAL");
        tx.setAmount(amount);
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        tx.setMethod(method);
        tx.setReference("WDR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        tx.setStatus("COMPLETED");
        tx.setDescription(messages.get(AppMessages.Key.WALLET_WITHDRAW_SUCCESS, language));
        return transactionRepository.save(tx);
    }

    @Transactional
    public WalletTransaction debit(Long userId, BigDecimal amount,
                                   String description, String language) {
        Wallet wallet = getOrCreateWallet(userId);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new AppException.BadRequestException(
                    messages.get(AppMessages.Key.INSUFFICIENT_BALANCE, language));
        }

        BigDecimal before = wallet.getBalance();
        BigDecimal after  = before.subtract(amount);

        wallet.setBalance(after);
        walletRepository.save(wallet);

        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(userId);
        tx.setType("PAYMENT");
        tx.setAmount(amount);
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        tx.setMethod("WALLET");
        tx.setReference("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        tx.setStatus("COMPLETED");
        tx.setDescription(description);
        return transactionRepository.save(tx);
    }
}