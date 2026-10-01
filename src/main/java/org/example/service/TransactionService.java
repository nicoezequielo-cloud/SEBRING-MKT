package org.example.service;

import org.example.dto.TransactionCreateRequest;
import org.example.dto.TransactionUpdateRequest;
import org.example.exception.TransactionNotFoundException;
import org.example.exception.UserNotFoundException;
import org.example.exception.VehicleNotFoundException;
import org.example.model.Transaction;
import org.example.repository.TransactionRepository;
import org.example.repository.UserRepository;
import org.example.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
public class TransactionService {

    private static final String DEFAULT_STATUS = "pending";
    private static final String CANCELLED_STATUS = "cancelled";

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    public TransactionService(TransactionRepository transactionRepository, UserRepository userRepository,
                               VehicleRepository vehicleRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
    }

    public Transaction createTransaction(TransactionCreateRequest request) {
        if (!userRepository.existsById(request.getUserId())) {
            throw new UserNotFoundException(request.getUserId());
        }
        if (!vehicleRepository.existsById(request.getVehicleId())) {
            throw new VehicleNotFoundException(request.getVehicleId());
        }

        Transaction transaction = new Transaction();
        transaction.setUserId(request.getUserId());
        transaction.setVehicleId(request.getVehicleId());
        transaction.setTransactionType(request.getTransactionType());
        transaction.setTotalAmount(request.getTotalAmount());
        transaction.setPaymentMethod(request.getPaymentMethod());
        transaction.setReceiptUrl(request.getReceiptUrl());
        transaction.setStatus(DEFAULT_STATUS);
        transaction.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        return transactionRepository.save(transaction);
    }

    public Transaction updateTransaction(Integer id, TransactionUpdateRequest request) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));

        transaction.setStatus(request.getStatus());
        transaction.setPaymentMethod(request.getPaymentMethod());
        transaction.setReceiptUrl(request.getReceiptUrl());

        return transactionRepository.save(transaction);
    }

    public void deleteTransaction(Integer id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
        transaction.setStatus(CANCELLED_STATUS);
        transactionRepository.save(transaction);
    }
}
