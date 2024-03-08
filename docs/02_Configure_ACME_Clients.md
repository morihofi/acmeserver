# 2. Configure ACME clients

> Do only proceed here if you completed step 1. Otherwise, you may encounter errors in certificate validation.

## What is an ACME client?
The ACME (Automated Certificate Management Environment) client is a software tool used for automating interactions between certificate authorities and web servers, enabling the automatic issuance and renewal of SSL/TLS certificates. This is essential for enabling secure, encrypted communication over the internet. The ACME client simplifies the process of obtaining certificates and maintaining HTTPS compliance for websites, reducing manual workload and potential for human error.

You can find an official list of ACME clients [here](https://letsencrypt.org/docs/client-options/)

## GetHttpsForFree
If you want to know, what the ACME client does, the built-in modified version (to work with this server) of GetHttpsForFree on the ACME Server's home may help you to understand.

## [Certbot](https://certbot.eff.org)
Certbot is a free, open-source software tool for automatically using Let's Encrypt certificates on manually-administered websites to enable HTTPS. It is the recommended client by Let's Encrypt for obtaining and renewing SSL/TLS certificates.

Sample command for issue a certificate for a [nginx](http://nginx.org/)-WebServer
```bash
certbot -n --nginx -d sample.example.com --server https://acme.example.com/acme/myprovisioner/directory --agree-tos --email your.email@example.com
```
You can find a detailed guide [for use with nginx here](https://www.nginx.com/blog/using-free-ssltls-certificates-from-lets-encrypt-with-nginx/) and a [guide to certbot here](https://eff-certbot.readthedocs.io/en/latest/using.html). 

The only important thing is, that you use the `--server`-argument. Otherwise, it goes to the official Let's Encrypt one.
If you want to do it manually, just replace `--nginx` with `--manual`

## acme.sh
acme.sh is another client implementation of the ACME Protocol

```bash
acme.sh --register-account --server https://acme.example.com/acme/myprovisioner/directory
acme.sh --update-account --server https://acme.example.com/acme/myprovisioner/directory --accountemail your.email@example.com
acme.sh --issue --server https://acme.example.com/acme/myprovisioner/directory -d sample.example.com --nginx
```