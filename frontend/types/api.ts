export type ApiProvisionersListResponse = {
  name: string;
  directoryUrl: string;
};

export type ApiProvisionersStatsResponse = {
  name: string;
  acmeAccounts: number;
  certificatesIssued: number;
  certificatesRevoked: number;
  certificatesIssueWaiting: number;
};
