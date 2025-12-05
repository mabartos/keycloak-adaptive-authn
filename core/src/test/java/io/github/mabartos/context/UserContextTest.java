package io.github.mabartos.context;

import io.github.mabartos.spi.context.UserContext;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class UserContextTest {

    @Test
    public void testUserContextBasicProperties() {
        TestUserContext context = new TestUserContext(true, 100, false, "test-data");

        assertThat(context.requiresUser(), is(true));
        assertThat(context.getPriority(), is(100));
        assertThat(context.alwaysFetch(), is(true));
        assertThat(context.isRemote(), is(false));
    }

    @Test
    public void testUserContextInitAndGet() {
        TestUserContext context = new TestUserContext(false, 50, false, "sample-data");

        assertThat(context.isInitialized(), is(false));

        Optional<String> data = context.initData();
        assertThat(data.isPresent(), is(true));
        assertThat(data.get(), is("sample-data"));
        assertThat(context.isInitialized(), is(true));

        Optional<String> cachedData = context.getData();
        assertThat(cachedData.isPresent(), is(true));
        assertThat(cachedData.get(), is("sample-data"));
    }

    @Test
    public void testUserContextAlwaysFetch() {
        TestUserContext context = new TestUserContext(false, 50, true, "fresh-data");

        context.initData();
        assertThat(context.isInitialized(), is(true));

        // Even though initialized, getData should re-fetch when alwaysFetch is true
        Optional<String> data = context.getData();
        assertThat(data.isPresent(), is(true));
    }

    @Test
    public void testUserContextEmptyData() {
        TestUserContext context = new TestUserContext(false, 50, false, null);

        Optional<String> data = context.initData();
        assertThat(data.isPresent(), is(false));
        assertThat(context.isInitialized(), is(true));
    }

    @Test
    public void testUserContextPriorityComparison() {
        TestUserContext highPriority = new TestUserContext(false, 100, false, "high");
        TestUserContext lowPriority = new TestUserContext(false, 50, false, "low");

        assertThat(highPriority.getPriority() > lowPriority.getPriority(), is(true));
    }

    @Test
    public void testUserContextDefaultLocal() {
        TestUserContext context = new TestUserContext(false, 50, false, "data");

        assertThat(context.isRemote(), is(false));
        assertThat(context.alwaysFetch(), is(true));
    }

    @Test
    public void testUserContextRemote() {
        UserContext<?> context = new RemoteUserContext();

        assertThat(context.isRemote(), is(true));
        assertThat(context.alwaysFetch(), is(false));
    }

    // Test implementation of UserContext
    static class TestUserContext implements UserContext<String> {
        private final boolean requiresUser;
        private final int priority;
        private final boolean isRemote;
        private final String data;
        private boolean initialized = false;

        TestUserContext(boolean requiresUser, int priority, boolean isRemote, String data) {
            this.requiresUser = requiresUser;
            this.priority = priority;
            this.isRemote = isRemote;
            this.data = data;
        }

        @Override
        public boolean requiresUser() {
            return requiresUser;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }

        @Override
        public Optional<String> initData() {
            initialized = true;
            return Optional.ofNullable(data);
        }

        @Override
        public Optional<String> getData() {
            if (!initialized || alwaysFetch()) {
                return initData();
            }
            return Optional.ofNullable(data);
        }

        @Override
        public void close() {
        }
    }

    static class RemoteUserContext implements UserContext<String> {
        @Override
        public boolean requiresUser() {
            return false;
        }

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public boolean isInitialized() {
            return false;
        }

        @Override
        public Optional<String> initData() {
            return Optional.empty();
        }

        @Override
        public Optional<String> getData() {
            return Optional.empty();
        }

        @Override
        public boolean isRemote() {
            return true;
        }

        @Override
        public void close() {
        }
    }
}
