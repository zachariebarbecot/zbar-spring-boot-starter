package fr.zbar.starter.transaction;

import java.util.function.Supplier;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class Transactions {

  private final TransactionTemplate writeTemplate;
  private final TransactionTemplate readTemplate;

  public Transactions(PlatformTransactionManager transactionManager) {
    this.writeTemplate = new TransactionTemplate(transactionManager);
    this.readTemplate = new TransactionTemplate(transactionManager);
    this.readTemplate.setReadOnly(true);
  }

  public <T> T execute(Supplier<T> action) {
    return writeTemplate.execute(status -> action.get());
  }

  public void run(Runnable action) {
    writeTemplate.executeWithoutResult(status -> action.run());
  }

  public <T> T read(Supplier<T> action) {
    return readTemplate.execute(status -> action.get());
  }
}
