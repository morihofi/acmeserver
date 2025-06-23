# Using gRPC clustering on Kubernetes

ACME Server can replicate events between multiple instances using gRPC. This allows background jobs to run on only one node of the cluster.

## settings.json configuration

Add the `grpc` section to your configuration. Instance ID and peers are discovered automatically via mDNS:

```json
"grpc": {
  "enabled": true,
  "host": "0.0.0.0",
  "port": 50051
}
```

Instance IDs are generated automatically and peers are discovered via mDNS, so no additional configuration is required.

## Kubernetes

Expose the gRPC port in your service definition. The instances advertise themselves via mDNS and automatically discover each other. Leader election chooses the instance with the smallest UUID to run background jobs such as certificate renewal.
