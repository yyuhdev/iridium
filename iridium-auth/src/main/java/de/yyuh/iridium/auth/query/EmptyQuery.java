package de.yyuh.iridium.auth.query;

import org.jspecify.annotations.NullMarked;

import de.yyuh.celery.api.query.AbstractQuery;
import de.yyuh.celery.api.query.IQuery;

@NullMarked
public final class EmptyQuery<T> extends AbstractQuery<T> {

  private EmptyQuery(final Builder<T> builder) {
    super(builder);
  }

  public static <T> IQuery<T> of(final Class<T> entityClass) {
    return new Builder<T>(entityClass).build();
  }

  private static final class Builder<T> extends AbstractQuery.Builder<T, Builder<T>> {
    Builder(final Class<T> entityClass) {
      super(entityClass);
    }

    @Override
    public IQuery<T> build() {
      return new EmptyQuery<>(this);
    }
  }
}
