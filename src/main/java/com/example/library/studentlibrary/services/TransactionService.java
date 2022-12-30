package com.example.library.studentlibrary.services;

import com.example.library.studentlibrary.models.*;
import com.example.library.studentlibrary.repositories.BookRepository;
import com.example.library.studentlibrary.repositories.CardRepository;
import com.example.library.studentlibrary.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.example.library.studentlibrary.models.CardStatus.DEACTIVATED;

@Service
public class TransactionService {

    @Autowired
    BookRepository bookRepository5;

    @Autowired
    CardRepository cardRepository5;

    @Autowired
    TransactionRepository transactionRepository5;

    @Value("${books.max_allowed}")
    int max_allowed_books;

    @Value("${books.max_allowed_days}")
    int getMax_allowed_days;

    @Value("${books.fine.per_day}")
    int fine_per_day;

    public String issueBook(int cardId, int bookId) throws Exception {

        Transaction transaction = null;
        //check whether bookId and cardId already exist
        if(cardRepository5.existsById(cardId) && bookRepository5.existsById(bookId)) {
            Book book = bookRepository5.findById(bookId).get();
            Card card = cardRepository5.findById(cardId).get();

            //conditions required for successful transaction of issue book:
            //1. book is present and available
            // If it fails: throw new Exception("Book is either unavailable or not present");
            if(!book.isAvailable())
                throw new Exception("Book is either unavailable or not present");

            //2. card is present and activated
            // If it fails: throw new Exception("Card is invalid");
            else if(card.getCardStatus() == DEACTIVATED)
                throw new Exception("Book is either unavailable or not present");

            //3. number of books issued against the card is strictly less than max_allowed_books
            // If it fails: throw new Exception("Book limit has reached for this card");
            else if(card.getBooks().size() >= max_allowed_books)
                throw new Exception("Book limit has reached for this card");

            //If the transaction is successful, save the transaction to the list of transactions and return the id
            else {
                transaction = Transaction.builder()
                                            .book(book)
                                            .card(card)
                                            .isIssueOperation(true)
                                            .transactionDate(new Date())
                                            .transactionStatus(TransactionStatus.SUCCESSFUL)
                                            .build();
                transactionRepository5.save(transaction);
                book.setAvailable(false);
                bookRepository5.updateBook(book);
                return transaction.getTransactionId();
            }
            //Note that the error message should match exactly in all cases
        }
        transaction = Transaction.builder()
                .isIssueOperation(true)
                .transactionDate(new Date())
                .transactionStatus(TransactionStatus.FAILED)
                .build();
        return transaction.getTransactionId(); //return transactionId instead
    }

    public Transaction returnBook(int cardId, int bookId) throws Exception{

        List<Transaction> transactions = transactionRepository5.find(cardId, bookId,TransactionStatus.SUCCESSFUL, true);
        Transaction transaction = transactions.get(transactions.size() - 1);

        //for the given transaction calculate the fine amount considering the book has been returned exactly when this function is called
        Date issueDate = transaction.getTransactionDate();
        Date returnDate = new Date();
        int days = (int) returnDate.getTime() - (int) issueDate.getTime();
        int fine = 0;
        if(days > getMax_allowed_days){
            fine = fine_per_day * (days-15);
        }

        //make the book available for other users
        Book book = transaction.getBook();
        book.setAvailable(true);
        bookRepository5.updateBook(book);

        //make a new transaction for return book which contains the fine amount as well

        return Transaction.builder()
                            .book(book)
                            .card(transaction.getCard())
                            .isIssueOperation(false)
                            .transactionDate(new Date())
                            .transactionStatus(TransactionStatus.SUCCESSFUL)
                            .build(); //return the transaction after updating all details
    }
}