package de.yyuh.iridium.boot;

import de.yyuh.iridium.shared.result.Result;

public interface IridiumComponentRegistry {

  public void scan();

  public default Result<Object, String> instantiate(final Class<?> clazz) {
    return Result.of(() -> {
      return (Object) clazz.getDeclaredConstructor().newInstance();
    }).mapErr(Exception::getMessage);
  }

}
