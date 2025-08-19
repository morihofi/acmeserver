# Event Bus Flow

This document explains how lifecycle and ACME events are emitted by the server. The diagram below shows the typical
order in which events are published.

```mermaid
sequenceDiagram
    autonumber
    participant Main
    participant WebServer
    participant EventBus

    Main->>EventBus: ServerInitializedEvent
    Main->>WebServer: startServer
    WebServer->>EventBus: ServerStartedEvent
    WebServer->>EventBus: BeforeAcmeApiRequestEvent
    WebServer->>EventBus: AcmeExceptionEvent (on error)
    WebServer-->>EventBus: ServerShutdownEvent
```

For ACME operations an order similar to the following is used:

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant API
    participant EventBus

    API->>EventBus: NewAcmeOrderEvent
    API->>EventBus: BeforeChallengeEvent
    API->>EventBus: AfterChallengeEvent
    API->>EventBus: AcmeCertificateIssuanceRequestedEvent
    API->>EventBus: BeforeAcmeCertificateCreatedEvent
    API->>EventBus: AcmeCertificateCreatedEvent
     API->>EventBus: CertificateRevokedEvent
    API->>EventBus: AcmeNonceRedeemedEvent
```

Each event is dispatched to all registered listeners via the server instance's `EventBus`.

## Module cleanup

Modules that register custom event listeners must deregister them in their
`onUnLoad()` method. The `EventBus` offers `unregisterAll(Object)` and the
module registry invokes it during module unloading to ensure no stale
listeners remain.
