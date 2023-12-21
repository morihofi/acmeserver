# How to run this ACME Server with an already existing Root CA

## Prerequisites
- existing Root CA with certificate and private key (both in PEM Format)
- Private key should not be password protected

## 1. Prepare directories
Inside the `serverdata`-folder, create a folder called `_rootCA`

```
serverdata/
 |- _rootCA/
```

## 2. Place your certificate
Copy your Root CA files into the newly created folder `rootCA`. The naming of these files is very important.

- Root CA must have the filename `root_ca_certificate.pem`
- Private key of CA must have the filename `private_key.pem`
- Public key of CA must have the filename `public_key.pem`

Your folder structure should look like this:

```
serverdata/
 |- _rootCA/
       |- root_ca_certificate.pem
       |- private_key.pem
       |- public_key.pem
```


### Public key notice
In most cases, you don't have the public key of your certificate as a single file. But this key is required by this ACME Server.
It is really easy to extract the public key out of your certificate. You just need `openssl` to extract the keys.

Just execute the following command inside the `_rootCA` directory:
```bash
openssl x509 -pubkey -noout -in root_ca_certificate.pem  > public_key.pem
```



That's all!