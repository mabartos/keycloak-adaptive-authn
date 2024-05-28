FROM registry.access.redhat.com/ubi9 AS ubi-micro-build

ADD /target/unpacked/ /tmp/keycloak/
ADD /data/adaptive-realm.json /tmp/keycloak/

RUN mv /tmp/keycloak/keycloak-* /opt/keycloak
RUN mv /tmp/keycloak/adaptive-realm.json /opt/keycloak
RUN chmod -R g+rwX /opt/keycloak

ADD ubi-null.sh /tmp/
RUN bash /tmp/ubi-null.sh java-17-openjdk-headless glibc-langpack-en findutils

FROM registry.access.redhat.com/ubi9-micro as keycloak-base
ENV LANG en_US.UTF-8

# Flag for determining app is running in container
ENV KC_RUN_IN_CONTAINER true

COPY --from=ubi-micro-build /tmp/null/rootfs/ /
COPY --from=ubi-micro-build --chown=1000:0 /opt/keycloak /opt/keycloak

RUN echo "keycloak:x:0:root" >> /etc/group && \
    echo "keycloak:x:1000:0:keycloak user:/opt/keycloak:/sbin/nologin" >> /etc/passwd

USER 1000

RUN /opt/keycloak/bin/kc.sh build
RUN /opt/keycloak/bin/kc.sh import --file=/opt/keycloak/adaptive-realm.json

ENV KEYCLOAK_ADMIN admin
ENV KEYCLOAK_ADMIN_PASSWORD admin

EXPOSE 8080
EXPOSE 8443
EXPOSE 9000

ENTRYPOINT [ "/opt/keycloak/bin/kc.sh" ]