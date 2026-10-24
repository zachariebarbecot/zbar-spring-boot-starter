package fr.zbar.starter.transaction;

import java.util.function.Supplier;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class Transactions {

  private final TransactionTemplate writeTemplate;

  public Transactions(PlatformTransactionManager transactionManager) {
    this.writeTemplate = new TransactionTemplate(transactionManager);
  }

  public <T> T execute(Supplier<T> action) {
    return writeTemplate.execute(status -> action.get());
  }
}
