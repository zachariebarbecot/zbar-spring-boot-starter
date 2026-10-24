package fr.zbar.starter.transaction;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

@AutoConfiguration(
    afterName = "org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration")
@ConditionalOnProperty(prefix = "zbar.transaction", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(TransactionProperties.class)
public class TransactionAutoConfiguration {

  @Bean
  @ConditionalOnSingleCandidate(PlatformTransactionManager.class)
  @ConditionalOnMissingBean
  Transactions transactions(PlatformTransactionManager transactionManager) {
    return new Transactions(transactionManager);
  }
}
