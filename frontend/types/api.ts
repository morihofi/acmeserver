export type ApiProvisionersListResponse = {
  name: string;
  directoryUrl: string;
};

export type ApiProvisionersStatsResponse = {
  name?: string;
  acmeAccounts: number;
  certificatesIssued: number;
  certificatesRevoked: number;
  certificatesIssueWaiting: number;
};

export type ApiServerInfoResponse = {
  metadataInfo: ApiServerInfoMetadataResponse;
};

export type ApiServerInfoMetadataResponse = {
  version: string;
  buildtime: string;
  gitcommit: string;
  javaversion: string;
  os: string;
  jvmUptime: number;
  jvmStartTime: number;
  startupTime: number;
  host: string;
  httpsPort: string;
  update: ApiServerInfoMetadataUpdateResponse;
};

export type ApiServerInfoMetadataUpdateResponse = {
  updateAvailable: boolean;
  releaseUrl: string;
};

export type ApiProvisionersByNameInfoResponse = {
  "terms-of-service": string;
  "website": string;
  "allow-ip": boolean;
  "allow-dns-wildcards": boolean;
};
