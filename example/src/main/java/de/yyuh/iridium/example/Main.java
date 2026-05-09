package de.yyuh.iridium.example;

import de.yyuh.iridium.boot.Iridium;
import de.yyuh.iridium.boot.IridiumBootstrap;

@IridiumBootstrap
public final class Main {

  public static void main(final String[] args) {
    Iridium.run(Main.class, args);
  }
}
