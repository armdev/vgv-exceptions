/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Vedran Grgo Vatavuk
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.vgv.exceptions;

import com.jcabi.immutable.Array;
import java.util.function.Function;
import org.cactoos.Scalar;

/**
 * Exception control that corresponds to java try/catch/finally statements.
 *
 * <p>If you don't want to have any checked exceptions being thrown
 * out of your {@link Try}, you can use
 * {@link com.vgv.exceptions.UncheckedTry} decorator.
 *
 * <p>There is no thread-safety guarantee.
 *
 * <p>This is how you're supposed to use it:
 *
 * <pre> new Try(
 *         new Catch(
 *            ServerException.class,
 *            e -> LOGGER.error("Server exception", e)
 *         ),
 *         new Catch(
 *             ClientException.class,
 *             e -> LOGGER.error("client exception", e)
 *         ),
 *         new Catch(
 *             new Array<>(IllegalStateException.class,
 *                  ValidationException.class),
 *             e -> LOGGER.error("Validation exception", e)
 *         )
 *      ).with(
 *            new Finally(() -> LOGGER.info("function executed")),
 *            new Throws<>(IOException::new)
 *      ).exec(() -> doSomething());
 * </pre>
 * @author Vedran Grgo Vatavuk (123vgv@gmail.com)
 * @version $Id$
 * @since 1.0
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class Try implements Checkable<Exception> {

    /**
     * List of consumers that handle exceptions.
     */
    private final Array<Catchable> catchables;

    /**
     * Ctor.
     * @param chbls List of catchable objects.
     */
    public Try(final Catchable... chbls) {
        this(new Array<>(chbls));
    }

    /**
     * Ctor.
     * @param chbls List of catchable objects..
     */
    public Try(final Array<Catchable> chbls) {
        this.catchables = chbls;
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public <T> T exec(final Scalar<T> scalar) throws Exception {
        try {
            return scalar.value();
            // @checkstyle IllegalCatchCheck (1 line)
        } catch (final Exception exception) {
            this.handle(exception);
            throw exception;
        }
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void exec(final VoidProc proc) throws Exception {
        try {
            proc.exec();
            // @checkstyle IllegalCatchCheck (1 line)
        } catch (final Exception exception) {
            this.handle(exception);
            throw exception;
        }
    }

    /**
     * Creates new Checkable object with additional handling of finally
     * block.
     * @param fnly Finally proc
     * @return Checkable Checkable
     */
    public Checkable<Exception> with(final VoidProc fnly) {
        return new Try.WithFinally<>(this, fnly);
    }

    /**
     * Creates new Checkable object that throws specified exception.
     * @param thrws Throws function.
     * @param <T> Extends Exception
     * @return Checkable Checkable
     */
    public <T extends Exception> Checkable<T> with(
        final Function<Exception, T> thrws) {
        return new Try.WithThrows<>(thrws, this.catchables);
    }

    /**
     * Creates new Checkable object with additional finally/throws
     * functionality.
     * @param fnly Finally
     * @param thrws Throws
     * @param <T> Extends Exception
     * @return Checkable Checkable
     */
    public <T extends Exception> Checkable<T> with(final VoidProc fnly,
        final Function<Exception, T> thrws) {
        return new Try.WithFinally<>(
            new Try.WithThrows<>(thrws, this.catchables),
            fnly
        );
    }

    /**
     * Handles exception.
     * @param exception Exception
     */
    private void handle(final Exception exception) {
        for (final Catchable catchable : this.catchables) {
            catchable.handle(exception);
        }
    }

    /**
     * Checkable decorator that throws specific Exception.
     * @param <E> Extends Exception
     */
    private static class WithThrows<E extends Exception> implements
        Checkable<E> {

        /**
         * Function that wraps generic exception to a specific one.
         */
        private final Function<Exception, E> fun;

        /**
         * Consumer that handles exception.
         */
        private final Array<Catchable> catchables;

        /**
         * Ctor.
         * @param fun Function
         * @param chbls List of catchable objects
         */
        WithThrows(final Function<Exception, E> fun,
            final Iterable<Catchable> chbls) {
            this.fun = fun;
            this.catchables = new Array<>(chbls);
        }

        @Override
        @SuppressWarnings("PMD.AvoidCatchingGenericException")
        public <T> T exec(final Scalar<T> scalar) throws E {
            try {
                return scalar.value();
                // @checkstyle IllegalCatchCheck (1 line)
            } catch (final RuntimeException exception) {
                this.handleUncheckedExp(exception);
                throw exception;
                // @checkstyle IllegalCatchCheck (1 line)
            } catch (final Exception exception) {
                this.handleCheckedExp(exception);
                throw this.fun.apply(exception);
            }
        }

        @Override
        @SuppressWarnings("PMD.AvoidCatchingGenericException")
        public void exec(final VoidProc proc) throws E {
            try {
                proc.exec();
                // @checkstyle IllegalCatchCheck (1 line)
            } catch (final RuntimeException exception) {
                this.handleUncheckedExp(exception);
                throw exception;
                // @checkstyle IllegalCatchCheck (1 line)
            } catch (final Exception exception) {
                this.handleCheckedExp(exception);
                throw this.fun.apply(exception);
            }
        }

        /**
         * Handles checked exception.
         * @param exception Exception
         */
        private void handleCheckedExp(final Exception exception) {
            this.catchables.forEach(catchable -> catchable.handle(exception));
        }

        /**
         * Handles unchecked exception.
         * @param exception Exception
         * @throws E Extends Exception
         */
        private void handleUncheckedExp(final RuntimeException exception)
            throws E {
            if (this.catchables.stream().anyMatch(
                catchable -> catchable.supports(exception)
            )) {
                this.catchables.forEach(
                    catchable -> catchable.handle(exception)
                );
                throw this.fun.apply(exception);
            }
        }
    }

    /**
     * Catchable decorator that controls finally block.
     * @param <E> Extends Exception
     */
    private static class WithFinally<E extends Exception> implements
        Checkable<E> {

        /**
         * Exception control origin.
         */
        private final Checkable<E> origin;

        /**
         * Void procedure.
         */
        private final VoidProc fproc;

        /**
         * Ctor.
         * @param checkable Checkable
         * @param proc Proc
         */
        WithFinally(final Checkable<E> checkable, final VoidProc proc) {
            this.origin = checkable;
            this.fproc = proc;
        }

        @Override
        public <T> T exec(final Scalar<T> scalar) throws E {
            try {
                return this.origin.exec(scalar);
            } finally {
                new UncheckedVoidProc(this.fproc).exec();
            }
        }

        @Override
        public void exec(final VoidProc proc) throws E {
            try {
                this.origin.exec(proc);
            } finally {
                new UncheckedVoidProc(this.fproc).exec();
            }
        }
    }
}
