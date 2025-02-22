/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.lang3;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.Functions.FailableBiConsumer;
import org.apache.commons.lang3.Functions.FailableBiFunction;
import org.apache.commons.lang3.Functions.FailableCallable;
import org.apache.commons.lang3.Functions.FailableConsumer;
import org.apache.commons.lang3.Functions.FailableFunction;
import org.apache.commons.lang3.Functions.FailableSupplier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FunctionsTest {
    public static class SomeException extends Exception {
        private static final long serialVersionUID = -4965704778119283411L;

        private Throwable t;

        SomeException(String pMsg) {
            super(pMsg);
        }

        public void setThrowable(Throwable pThrowable) {
            t = pThrowable;
        }

        public void test() throws Throwable {
            if (t != null) {
                throw t;
            }
        }
    }
    public static class Testable {
        private Throwable t;

        Testable(Throwable pTh) {
            t = pTh;
        }

        public void setThrowable(Throwable pThrowable) {
            t = pThrowable;
        }

        public void test() throws Throwable {
            test(t);
        }

        public void test(Throwable pThrowable) throws Throwable {
            if (pThrowable != null) {
                throw pThrowable;
            }
        }

        public Integer testInt() throws Throwable {
            return testInt(t);
        }

        public Integer testInt(Throwable pThrowable) throws Throwable {
            if (pThrowable != null) {
                throw pThrowable;
            }
            return 0;
        }
    }

    public static class FailureOnOddInvocations {
        private static int invocation;
        FailureOnOddInvocations() throws SomeException {
            final int i = ++invocation;
            if (i % 2 == 1) {
                throw new SomeException("Odd Invocation: " + i);
            }
        }
    }

    public static class CloseableObject {
        private boolean closed;

        public void run(Throwable pTh) throws Throwable {
            if (pTh != null) {
                throw pTh;
            }
        }

        public void reset() {
            closed = false;
        }

        public void close() {
            closed = true;
        }

        public boolean isClosed() {
            return closed;
        }
    }

    @Test
    void testRunnable() {
        FailureOnOddInvocations.invocation = 0;
        UndeclaredThrowableException e = assertThrows(UndeclaredThrowableException.class, () ->  Functions.run(FailureOnOddInvocations::new));
        final Throwable cause = e.getCause();
        assertNotNull(cause);
        assertTrue(cause instanceof SomeException);
        assertEquals("Odd Invocation: 1", cause.getMessage());

        // Even invocation, should not throw an exception
        Functions.run(FailureOnOddInvocations::new);
    }

    @Test
    void testAsRunnable() {
        FailureOnOddInvocations.invocation = 0;
        Runnable runnable = Functions.asRunnable(() -> new FailureOnOddInvocations());
        UndeclaredThrowableException e = assertThrows(UndeclaredThrowableException.class, () ->  runnable.run());
        final Throwable cause = e.getCause();
        assertNotNull(cause);
        assertTrue(cause instanceof SomeException);
        assertEquals("Odd Invocation: 1", cause.getMessage());

        // Even invocation, should not throw an exception
        runnable.run();
    }

    @Test
    void testCallable() {
        FailureOnOddInvocations.invocation = 0;
        UndeclaredThrowableException e = assertThrows(UndeclaredThrowableException.class, () ->  Functions.run(FailureOnOddInvocations::new));
        final Throwable cause = e.getCause();
        assertNotNull(cause);
        assertTrue(cause instanceof SomeException);
        assertEquals("Odd Invocation: 1", cause.getMessage());
        final FailureOnOddInvocations instance = Functions.call(FailureOnOddInvocations::new);
        assertNotNull(instance);
    }

    @Test
    void testAsCallable() {
        FailureOnOddInvocations.invocation = 0;
        final FailableCallable<FailureOnOddInvocations,SomeException> failableCallable = () -> { return new FailureOnOddInvocations(); };
        final Callable<FailureOnOddInvocations> callable = Functions.asCallable(failableCallable);
        UndeclaredThrowableException e = assertThrows(UndeclaredThrowableException.class, () ->  callable.call());
        final Throwable cause = e.getCause();
        assertNotNull(cause);
        assertTrue(cause instanceof SomeException);
        assertEquals("Odd Invocation: 1", cause.getMessage());
        final FailureOnOddInvocations instance;
        try {
        	instance = callable.call();
        } catch (Exception ex) {
        	throw Functions.rethrow(ex);
        }
        assertNotNull(instance);
    }

    @Test
    void testAcceptConsumer() {
        final IllegalStateException ise = new IllegalStateException();
        final Testable testable = new Testable(ise);
        Throwable e = assertThrows(IllegalStateException.class, () -> Functions.accept(Testable::test, testable));
        assertSame(ise, e);

        final Error error = new OutOfMemoryError();
        testable.setThrowable(error);
        e = assertThrows(OutOfMemoryError.class, () -> Functions.accept(Testable::test, testable));
        assertSame(error, e);

        final IOException ioe = new IOException("Unknown I/O error");
        testable.setThrowable(ioe);
        e = assertThrows(UncheckedIOException.class, () -> Functions.accept(Testable::test, testable));
        final Throwable t = e.getCause();
        assertNotNull(t);
        assertSame(ioe, t);

        testable.setThrowable(null);
        Functions.accept(Testable::test, testable);
    }

    @Test
    void testAsConsumer() {
        final IllegalStateException ise = new IllegalStateException();
        final Testable testable = new Testable(ise);
        final Consumer<Testable> consumer = Functions.asConsumer((t) -> t.test()); 
        Throwable e = assertThrows(IllegalStateException.class, () -> consumer.accept(testable));
        assertSame(ise, e);

        final Error error = new OutOfMemoryError();
        testable.setThrowable(error);
        e = assertThrows(OutOfMemoryError.class, () -> consumer.accept(testable));
        assertSame(error, e);

        final IOException ioe = new IOException("Unknown I/O error");
        testable.setThrowable(ioe);
        e = assertThrows(UncheckedIOException.class, () -> consumer.accept(testable));
        final Throwable t = e.getCause();
        assertNotNull(t);
        assertSame(ioe, t);

        testable.setThrowable(null);
        Functions.accept(Testable::test, testable);
    }

    @Test
    void testAcceptBiConsumer() {
        final IllegalStateException ise = new IllegalStateException();
        final Testable testable = new Testable(null);
        Throwable e = assertThrows(IllegalStateException.class, () -> Functions.accept(Testable::test, testable, ise));
        assertSame(ise, e);

        final Error error = new OutOfMemoryError();
        e = assertThrows(OutOfMemoryError.class, () -> Functions.accept(Testable::test, testable, error));
        assertSame(error, e);

        final IOException ioe = new IOException("Unknown I/O error");
        testable.setThrowable(ioe);
        e = assertThrows(UncheckedIOException.class, () -> Functions.accept(Testable::test, testable, ioe));
        final Throwable t = e.getCause();
        assertNotNull(t);
        assertSame(ioe, t);

        testable.setThrowable(null);
        Functions.accept(Testable::test, testable, (Throwable) null);
    }

    @Test
    void testAsBiConsumer() {
        final IllegalStateException ise = new IllegalStateException();
        final Testable testable = new Testable(null);
        final FailableBiConsumer<Testable, Throwable, Throwable> failableBiConsumer = (t, th) -> { t.setThrowable(th); t.test(); }; 
        final BiConsumer<Testable, Throwable> consumer = Functions.asBiConsumer(failableBiConsumer);
        Throwable e = assertThrows(IllegalStateException.class, () -> consumer.accept(testable, ise));
        assertSame(ise, e);

        final Error error = new OutOfMemoryError();
        e = assertThrows(OutOfMemoryError.class, () -> consumer.accept(testable, error));
        assertSame(error, e);

        final IOException ioe = new IOException("Unknown I/O error");
        testable.setThrowable(ioe);
        e = assertThrows(UncheckedIOException.class, () -> consumer.accept(testable,  ioe));
        final Throwable t = e.getCause();
        assertNotNull(t);
        assertSame(ioe, t);

        consumer.accept(testable, null);
    }

    @Test
    public void testApplyFunction() {
        final IllegalStateException ise = new IllegalStateException();
        final Testable testable = new Testable(ise);
        Throwable e = assertThrows(IllegalStateException.class, () -> Functions.apply(Testable::testInt, testable));
        assertSame(ise, e);

        final Error error = new OutOfMemoryError();
        testable.setThrowable(error);
        e = assertThrows(OutOfMemoryError.class, () -> Functions.apply(Testable::testInt, testable));
        assertSame(error, e);

        final IOException ioe = new IOException("Unknown I/O error");
        testable.setThrowable(ioe);
        e = assertThrows(UncheckedIOException.class, () -> Functions.apply(Testable::testInt, testable));
        final Throwable t = e.getCause();
        assertNotNull(t);
        assertSame(ioe, t);

        testable.setThrowable(null);
        final Integer i = Functions.apply(Testable::testInt, testable);
        assertNotNull(i);
        assertEquals(0, i.intValue());
    }

    @Test
    public void testAsFunction() {
        final IllegalStateException ise = new IllegalStateException();
        final Testable testable = new Testable(ise);
        final FailableFunction<Throwable,Integer,Throwable> failableFunction = (th) -> {
        	testable.setThrowable(th);
        	return Integer.valueOf(testable.testInt());
        };
        final Function<Throwable,Integer> function = Functions.asFunction(failableFunction);
        Throwable e = assertThrows(IllegalStateException.class, () -> function.apply(ise));
        assertSame(ise, e);

        final Error error = new OutOfMemoryError();
        testable.setThrowable(error);
        e = assertThrows(OutOfMemoryError.class, () -> function.apply(error));
        assertSame(error, e);

        final IOException ioe = new IOException("Unknown I/O error");
        testable.setThrowable(ioe);
        e = assertThrows(UncheckedIOException.class, () -> function.apply(ioe));
        final Throwable t = e.getCause();
        assertNotNull(t);
        assertSame(ioe, t);

        assertEquals(0, function.apply(null).intValue());
    }

    @Test
    public void testApplyBiFunction() {
        final IllegalStateException ise = new IllegalStateException();
        final Testable testable = new Testable(null);
        Throwable e = assertThrows(IllegalStateException.class, () -> Functions.apply(Testable::testInt, testable, ise));
        assertSame(ise, e);

        final Error error = new OutOfMemoryError();
        e = assertThrows(OutOfMemoryError.class, () -> Functions.apply(Testable::testInt, testable, error));
        assertSame(error, e);

        final IOException ioe = new IOException("Unknown I/O error");
        e = assertThrows(UncheckedIOException.class, () -> Functions.apply(Testable::testInt, testable, ioe));
        final Throwable t = e.getCause();
        assertNotNull(t);
        assertSame(ioe, t);

        final Integer i = Functions.apply(Testable::testInt, testable, (Throwable) null);
        assertNotNull(i);
        assertEquals(0, i.intValue());
    }

    @Test
    public void testAsBiFunction() {
        final IllegalStateException ise = new IllegalStateException();
        final Testable testable = new Testable(ise);
        final FailableBiFunction<Testable,Throwable,Integer,Throwable> failableBiFunction = (t, th) -> {
        	t.setThrowable(th);
        	return Integer.valueOf(t.testInt());
        };
        final BiFunction<Testable,Throwable,Integer> biFunction = Functions.asBiFunction(failableBiFunction);
        Throwable e = assertThrows(IllegalStateException.class, () -> biFunction.apply(testable, ise));
        assertSame(ise, e);

        final Error error = new OutOfMemoryError();
        testable.setThrowable(error);
        e = assertThrows(OutOfMemoryError.class, () -> biFunction.apply(testable, error));
        assertSame(error, e);

        final IOException ioe = new IOException("Unknown I/O error");
        testable.setThrowable(ioe);
        e = assertThrows(UncheckedIOException.class, () -> biFunction.apply(testable, ioe));
        final Throwable t = e.getCause();
        assertNotNull(t);
        assertSame(ioe, t);

        assertEquals(0, biFunction.apply(testable, null).intValue());
    }

    @Test
    public void testGetFromSupplier() {
        FailureOnOddInvocations.invocation = 0;
        UndeclaredThrowableException e = assertThrows(UndeclaredThrowableException.class, () ->  Functions.run(FailureOnOddInvocations::new));
        final Throwable cause = e.getCause();
        assertNotNull(cause);
        assertTrue(cause instanceof SomeException);
        assertEquals("Odd Invocation: 1", cause.getMessage());
        final FailureOnOddInvocations instance = Functions.call(FailureOnOddInvocations::new);
        assertNotNull(instance);
    }

    @Test
    public void testAsSupplier() {
        FailureOnOddInvocations.invocation = 0;
        final FailableSupplier<FailureOnOddInvocations,Throwable> failableSupplier = () -> { return new FailureOnOddInvocations(); };
        final Supplier<FailureOnOddInvocations> supplier = Functions.asSupplier(failableSupplier);
        UndeclaredThrowableException e = assertThrows(UndeclaredThrowableException.class, () ->  supplier.get());
        final Throwable cause = e.getCause();
        assertNotNull(cause);
        assertTrue(cause instanceof SomeException);
        assertEquals("Odd Invocation: 1", cause.getMessage());
        final FailureOnOddInvocations instance = supplier.get();
        assertNotNull(instance);
    }

    @Test
    public void testTryWithResources() {
        final CloseableObject co = new CloseableObject();
        final FailableConsumer<Throwable, ? extends Throwable> consumer = co::run;
        final IllegalStateException ise = new IllegalStateException();
        Throwable e = assertThrows(IllegalStateException.class, () -> Functions.tryWithResources(() -> consumer.accept(ise), co::close));
        assertSame(ise, e);

        assertTrue(co.isClosed());
        co.reset();
        final Error error = new OutOfMemoryError();
        e = assertThrows(OutOfMemoryError.class, () -> Functions.tryWithResources(() -> consumer.accept(error), co::close));
        assertSame(error, e);

        assertTrue(co.isClosed());
        co.reset();
        final IOException ioe = new IOException("Unknown I/O error");
        UncheckedIOException uioe = assertThrows(UncheckedIOException.class, () ->  Functions.tryWithResources(() -> consumer.accept(ioe), co::close));
        final IOException cause = uioe.getCause();
        assertSame(ioe, cause);

        assertTrue(co.isClosed());
        co.reset();
        Functions.tryWithResources(() -> consumer.accept(null), co::close);
        assertTrue(co.isClosed());
    }
}
