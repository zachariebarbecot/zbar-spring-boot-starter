package fr.zbar.starter.test.fixtures.clean.adapter;

import fr.zbar.starter.test.fixtures.clean.domain.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderAdapter {

  public Order load() {
    return new Order("1");
  }
}
