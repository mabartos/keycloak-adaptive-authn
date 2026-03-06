package io.github.mabartos.spi.context;

import java.util.Optional;

public interface ChainedUserContext<T> {

    Optional<UserContext<T>> getDelegate();

    void setDelegate(UserContext<?> delegate);
}
