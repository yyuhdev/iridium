package de.yyuh.iridium.shared.result;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents a result that can be either successful (Ok) or failed (Err).
 *
 * <p>
 * This sealed interface provides functional error handling similar to
 * Rust's Result type. The Ok variant contains a success value, while
 * the Err variant contains an error value.
 *
 * @param <T> the type of the success value
 * @param <E> the type of the error value
 */
public sealed interface Result<T, E> permits Result.Ok, Result.Err {

  /**
   * Creates a successful result containing a value.
   *
   * @param value the success value
   * @param <T>   the value type
   * @param <E>   the error type
   * @return an Ok result
   */
  static <T, E> Result<T, E> ok(final T value) {
    return new Ok<>(value);
  }

  /**
   * Creates an error result containing an error.
   *
   * @param error the error value
   * @param <T>   the value type
   * @param <E>   the error type
   * @return an Err result
   */
  static <T, E> Result<T, E> err(final E error) {
    return new Err<>(error);
  }

  boolean isOk();

  default boolean isErr() {
    return !isOk();
  }

  /**
   * Unwraps the value, throwing if this is an Err.
   *
   * @return the success value
   * @throws NoSuchElementException if this is an Err
   */

  T unwrap();

  /**
   * Unwraps the value or returns a default if this is an Err.
   *
   * @param defaultValue the value to return if this is an Err
   * @return the value or the default
   */

  T unwrapOr(final T defaultValue);

  /**
   * Unwraps the value or computes it from the error if this is an Err.
   *
   * @param fn the function to apply to the error value
   * @return the value or the computed result
   */

  T unwrapOrElse(final Function<E, T> fn);

  /**
   * Unwraps the error value, throwing if this is an Ok.
   *
   * @return the error value
   * @throws NoSuchElementException if this is an Ok
   */

  E unwrapErr();

  /**
   * Maps the success value using the provided function.
   *
   * @param fn  the function to apply to the value
   * @param <U> the new value type
   * @return a new Result with the mapped value
   */
  <U> Result<U, E> map(final Function<T, U> fn);

  /**
   * Maps the error value using the provided function.
   *
   * @param fn  the function to apply to the error
   * @param <F> the new error type
   * @return a new Result with the mapped error
   */
  <F> Result<T, F> mapErr(final Function<E, F> fn);

  /**
   * Chains Result computations, flattening nested results.
   *
   * @param fn  the function to apply to the value
   * @param <U> the new value type
   * @return the result of the chained computation
   */
  <U> Result<U, E> flatMap(final Function<T, Result<U, E>> fn);

  /**
   * Executes an action if this is an Ok, returning this for chaining.
   *
   * @param action the action to execute
   * @return this Result
   */

  Result<T, E> ifOk(final Consumer<T> action);

  /**
   * Executes an action if this is an Err, returning this for chaining.
   *
   * @param action the action to execute
   * @return this Result
   */

  Result<T, E> ifErr(final Consumer<E> action);

  /**
   * Returns an Optional containing the value if Ok, or empty if Err.
   *
   * @return an Optional with the value
   */

  Optional<T> ok();

  /**
   * Returns an Optional containing the error if Err, or empty if Ok.
   *
   * @return an Optional with the error
   */

  Optional<E> err();

  /**
   * Represents a successful result containing a value.
   *
   * @param <T> the value type
   * @param <E> the error type
   */
  record Ok<T, E>(T value) implements Result<T, E> {

    /** {@inheritDoc} */
    @Override
    public boolean isOk() {
      return true;
    }

    /** {@inheritDoc} */
    @Override
    public T unwrap() {
      return value;
    }

    /** {@inheritDoc} */
    @Override
    public T unwrapOr(final T defaultValue) {
      return value;
    }

    /** {@inheritDoc} */
    @Override
    public T unwrapOrElse(final Function<E, T> fn) {
      return value;
    }

    /** {@inheritDoc} */
    @Override
    public E unwrapErr() {
      throw new NoSuchElementException("Called unwrapErr() on Ok: " + value);
    }

    /** {@inheritDoc} */
    @Override
    public <U> Result<U, E> map(final Function<T, U> fn) {
      return Result.ok(fn.apply(value));
    }

    /** {@inheritDoc} */
    @Override
    public <F> Result<T, F> mapErr(final Function<E, F> fn) {
      return Result.ok(value);
    }

    /** {@inheritDoc} */
    @Override
    public <U> Result<U, E> flatMap(final Function<T, Result<U, E>> fn) {
      return fn.apply(value);
    }

    /** {@inheritDoc} */
    @Override
    public Result<T, E> ifOk(final Consumer<T> action) {
      action.accept(value);
      return this;
    }

    /** {@inheritDoc} */
    @Override
    public Result<T, E> ifErr(final Consumer<E> action) {
      return this;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<T> ok() {
      return Optional.of(value);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<E> err() {
      return Optional.empty();
    }
  }

  /**
   * Represents an error result containing an error value.
   *
   * @param <T> the value type
   * @param <E> the error type
   */
  record Err<T, E>(E error) implements Result<T, E> {

    public Err {
      if (error == null)
        throw new IllegalArgumentException("Err value must not be null");
    }

    /** {@inheritDoc} */
    @Override
    public boolean isOk() {
      return false;
    }

    /** {@inheritDoc} */
    @Override
    public T unwrap() {
      throw new NoSuchElementException("Called unwrap() on Err: " + error);
    }

    /** {@inheritDoc} */
    @Override
    public T unwrapOr(final T defaultValue) {
      return defaultValue;
    }

    /** {@inheritDoc} */
    @Override
    public T unwrapOrElse(final Function<E, T> fn) {
      return fn.apply(error);
    }

    /** {@inheritDoc} */
    @Override
    public E unwrapErr() {
      return error;
    }

    /** {@inheritDoc} */
    @Override
    public <U> Result<U, E> map(final Function<T, U> fn) {
      return Result.err(error);
    }

    /** {@inheritDoc} */
    @Override
    public <F> Result<T, F> mapErr(final Function<E, F> fn) {
      return Result.err(fn.apply(error));
    }

    /** {@inheritDoc} */
    @Override
    public <U> Result<U, E> flatMap(final Function<T, Result<U, E>> fn) {
      return Result.err(error);
    }

    /** {@inheritDoc} */
    @Override
    public Result<T, E> ifOk(final Consumer<T> action) {
      return this;
    }

    /** {@inheritDoc} */
    @Override
    public Result<T, E> ifErr(final Consumer<E> action) {
      action.accept(error);
      return this;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<T> ok() {
      return Optional.empty();
    }

    /** {@inheritDoc} */
    @Override
    public Optional<E> err() {
      return Optional.of(error);
    }
  }

  /**
   * A supplier that may throw an exception.
   *
   * @param <T> the result type
   * @param <X> the exception type
   */
  @FunctionalInterface
  interface ThrowingSupplier<T, X extends Throwable> {
    T get() throws X;
  }

  /**
   * Converts a throwing supplier to a Result.
   *
   * @param supplier the throwing supplier
   * @param <T>      the result type
   * @param <X>      the exception type
   * @return an Ok with the result or an Err with the exception
   */
  static <T, X extends Throwable> Result<T, X> of(
      final ThrowingSupplier<T, X> supplier) {
    try {
      return ok(supplier.get());
    } catch (Throwable e) {
      @SuppressWarnings("unchecked")
      X error = (X) e;
      return err(error);
    }
  }
}
